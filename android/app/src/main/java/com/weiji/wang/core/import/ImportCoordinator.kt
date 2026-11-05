package com.weiji.wang.core.import

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import com.weiji.wang.core.data.RecipeRepository
import com.weiji.wang.core.data.db.DraftEntity
import com.weiji.wang.core.data.db.IngredientEntity
import com.weiji.wang.core.data.db.MediaEntity
import com.weiji.wang.core.data.db.RecipeEntity
import com.weiji.wang.core.data.db.SourceEntity
import com.weiji.wang.core.data.db.StepEntity
import com.weiji.wang.core.data.db.WeijiDatabase
import com.weiji.wang.core.data.prefs.SettingsRepository
import com.weiji.wang.core.import.MediaDownloader.DownloadOutcome
import com.weiji.wang.core.import.extractor.ExtractorRegistry
import com.weiji.wang.core.model.FetchStatus
import com.weiji.wang.core.model.ImportState
import com.weiji.wang.core.model.MediaKind
import com.weiji.wang.core.model.Platform
import com.weiji.wang.core.model.Ulid
import com.weiji.wang.core.storage.FileStore
import com.weiji.wang.core.storage.VideoFrames
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.io.File
import java.io.FileInputStream

data class PreparedImport(
    val draftId: String,
    val link: ParsedLink,
    val content: ExtractedContent,
)

data class ImportResult(
    val recipeId: String,
    val partial: Boolean,
    val message: String? = null,
)

class ImportCoordinator(
    private val context: Context,
    private val db: WeijiDatabase,
    private val files: FileStore,
    private val recipes: RecipeRepository,
    private val extractors: ExtractorRegistry,
    private val settings: SettingsRepository,
    http: OkHttpClient,
) {
    private val drafts = db.draftDao()
    private val downloader = MediaDownloader(http, files)
    private val now: () -> Long = { System.currentTimeMillis() }

    fun observeDrafts(): Flow<List<DraftEntity>> = drafts.observeAll()

    suspend fun prepare(text: String): PreparedImport {
        val link = LinkParser.parse(text)
            ?: throw ExtractException(ImportState.FAILED, "没有识别到链接")
        val draftId = Ulid.next()
        drafts.upsert(
            DraftEntity(
                id = draftId,
                shareText = text,
                url = link.url,
                platform = link.platform.key,
                state = ImportState.RECEIVED.name,
                createdAt = now(),
                updatedAt = now(),
            ),
        )
        drafts.updateState(draftId, ImportState.RESOLVING.name, null, 0, now())
        return try {
            val content = extractors.forPlatform(link.platform).extract(link)
            if (content.requiresWebView) {
                drafts.updateState(draftId, ImportState.RESOLVING.name, content.message, 0, now())
            } else {
                val state = if (content.fetchStatus == FetchStatus.OK) ImportState.EXTRACTED else ImportState.PARTIAL
                drafts.updateState(draftId, state.name, content.message, 0, now())
            }
            PreparedImport(draftId, link, content)
        } catch (e: ExtractException) {
            drafts.updateState(draftId, e.state.name, e.message, 0, now())
            throw e
        }
    }

    /**
     * 真正的落库：下载媒体、压缩图片、解析文字、写 recipe + source + media。
     * 任何一步失败都尽量保留已拿到的数据。
     */
    suspend fun finalize(
        prepared: PreparedImport,
        content: ExtractedContent,
        onProgress: ((Int, String) -> Unit)? = null,
    ): ImportResult = withContext(Dispatchers.IO) {
        val existing = recipes.findSourceByUrl(content.canonicalUrl)
        if (existing != null) {
            drafts.attachRecipe(prepared.draftId, existing.recipeId, ImportState.READY.name, now())
            return@withContext ImportResult(existing.recipeId, false, "这个链接已经收录过了")
        }

        drafts.updateState(prepared.draftId, ImportState.DOWNLOADING.name, null, 0, now())
        val recipeId = Ulid.next()
        val recipeDir = files.recipeDir(recipeId)

        val savedMedia = mutableListOf<MediaEntity>()
        var coverPath: String? = null
        var partial = content.fetchStatus != FetchStatus.OK
        var videoFailure: String? = null
        var mediaSkipReason: String? = null
        var downloadedVideo: File? = null

        val autoDownload = settings.autoDownloadMedia.first()
        val wifiOnly = settings.wifiOnly.first()
        val wifiAvailable = !wifiOnly || isOnWifi()
        val canDownload = autoDownload && wifiAvailable
        if (!canDownload && (content.media.isNotEmpty() || content.coverUrl != null)) {
            partial = true
            mediaSkipReason = when {
                !autoDownload -> "自动下载视频与图片已关闭"
                else -> "仅 Wi-Fi 下载已开启，当前网络不是 Wi-Fi"
            }
        }

        val candidates = content.media.sortedBy {
            when (it.kind) {
                MediaKind.COVER -> 0
                MediaKind.VIDEO -> 1
                else -> 2
            }
        }

        if (canDownload) {
            var stepIndex = 0
            candidates.forEachIndexed { index, remote ->
                onProgress?.invoke(
                    if (candidates.isEmpty()) 0 else index * 100 / candidates.size,
                    "正在下载 ${index + 1}/${candidates.size}",
                )
                when (remote.kind) {
                    MediaKind.COVER, MediaKind.STEP -> {
                        val isCover = remote.kind == MediaKind.COVER || (coverPath == null && remote.kind == MediaKind.STEP)
                        val tempName = if (isCover) "cover.raw" else "step-raw-%02d.img".format(stepIndex + 1)
                        when (val outcome = downloader.download(recipeId, remote, tempName)) {
                            is DownloadOutcome.Done -> {
                                val temp = recipeDir.resolve(tempName)
                                val targetName = if (isCover) {
                                    FileStore.nameForCover()
                                } else {
                                    stepIndex += 1
                                    FileStore.nameForStep(stepIndex)
                                }
                                val saved = runCatching {
                                    FileInputStream(temp).use {
                                        files.saveImage(recipeId, targetName, it, remote.mime)
                                    }
                                }.getOrElse {
                                    val fallback = targetName.substringBeforeLast('.') + ".jpg"
                                    if (temp.exists()) temp.renameTo(recipeDir.resolve(fallback))
                                    files.register(recipeId, fallback)
                                }
                                if (temp.exists()) temp.delete()
                                if (isCover) coverPath = saved.relativePath
                                savedMedia += mediaEntity(
                                    recipeId, if (isCover) MediaKind.COVER else MediaKind.STEP,
                                    saved, remote, now(),
                                )
                            }
                            is DownloadOutcome.Skipped -> partial = true
                            is DownloadOutcome.Failed -> partial = true
                        }
                    }
                    MediaKind.VIDEO -> {
                        val name = FileStore.nameForVideo(1)
                        when (val outcome = downloader.download(recipeId, remote, name, requiredBytes = remote.sizeBytes)) {
                            is DownloadOutcome.Done -> {
                                val file = recipeDir.resolve(name)
                                downloadedVideo = file
                                val duration = remote.durationMs ?: VideoFrames.durationMs(file)
                                savedMedia += mediaEntity(
                                    recipeId, MediaKind.VIDEO,
                                    outcome.file.copy(), remote, now(),
                                ).copy(durationMs = duration, mime = "video/mp4")
                            }
                            is DownloadOutcome.Skipped -> partial = true
                            is DownloadOutcome.Failed -> {
                                partial = true
                                videoFailure = outcome.reason
                            }
                        }
                    }
                    else -> Unit
                }
            }
        }

        if (coverPath == null && downloadedVideo != null) {
            val frame = VideoFrames.extract(downloadedVideo, atMs = 1000L)
            if (frame != null) {
                val saved = files.saveBitmap(recipeId, FileStore.nameForCover(), frame)
                frame.recycle()
                coverPath = saved.relativePath
                savedMedia += MediaEntity(
                    id = Ulid.next(),
                    recipeId = recipeId,
                    kind = MediaKind.COVER.name,
                    relativePath = saved.relativePath,
                    mime = "image/webp",
                    width = saved.width,
                    height = saved.height,
                    sizeBytes = saved.sizeBytes,
                    sha256 = saved.sha256,
                    originUrl = null,
                    watermarkFree = true,
                    createdAt = now(),
                )
            }
        }

        if (canDownload && content.coverUrl != null && coverPath == null) {
            val remoteCover = RemoteMedia(
                kind = MediaKind.COVER,
                url = content.coverUrl,
                mime = "image/jpeg",
                watermarkFree = true,
                headers = content.media.firstOrNull()?.headers ?: emptyMap(),
            )
            when (val outcome = downloader.download(recipeId, remoteCover, "cover.raw")) {
                is DownloadOutcome.Done -> {
                    val temp = recipeDir.resolve("cover.raw")
                    val saved = runCatching {
                        FileInputStream(temp).use { files.saveImage(recipeId, FileStore.nameForCover(), it, "image/jpeg") }
                    }.getOrNull()
                    temp.delete()
                    if (saved != null) {
                        coverPath = saved.relativePath
                        savedMedia += mediaEntity(recipeId, MediaKind.COVER, saved, remoteCover, now())
                    }
                }
                else -> Unit
            }
        }

        val parsed = TextParser.parse(
            title = content.title,
            body = content.textBody,
            author = content.author,
            fallbackTitle = prepared.link.titleHint,
        )
        val recipe = RecipeEntity(
            id = recipeId,
            title = parsed.title,
            subtitle = content.author?.let { "@$it" }.orEmpty(),
            coverPath = coverPath,
            servings = parsed.servings,
            totalMinutes = content.durationMs?.takeIf { it > 0 }?.let { (it / 60000).toInt().takeIf { m -> m > 0 } },
            notes = parsed.notes,
            sourceId = Ulid.next(),
            createdAt = now(),
            updatedAt = now(),
        )
        val source = SourceEntity(
            id = recipe.sourceId!!,
            recipeId = recipeId,
            platform = content.platform.key,
            originalUrl = prepared.link.url,
            canonicalUrl = content.canonicalUrl,
            platformContentId = content.contentId,
            authorName = content.author,
            authorId = content.authorId,
            sourceTitle = content.title,
            textBody = content.textBody,
            remoteCoverUrl = content.coverUrl,
            shareText = prepared.link.shareText,
            importedAt = now(),
            fetchStatus = content.fetchStatus.name,
        )
        val ingredientEntities = parsed.ingredients.mapIndexed { index, item ->
            IngredientEntity(
                id = Ulid.next(),
                recipeId = recipeId,
                position = index,
                groupName = item.group,
                name = item.name,
                amount = item.amount,
                unit = item.unit,
                rawText = item.rawText,
                scalable = !item.vague,
            )
        }
        val stepImages = savedMedia
            .filter { it.kind == MediaKind.STEP.name }
            .sortedBy { it.createdAt }
        val stepEntities = if (parsed.steps.isNotEmpty()) {
            parsed.steps.mapIndexed { index, text ->
                StepEntity(
                    id = Ulid.next(),
                    recipeId = recipeId,
                    position = index,
                    text = text,
                    mediaPath = stepImages.getOrNull(index)?.relativePath,
                )
            }
        } else {
            // 图文教程没有文字步骤时，把图片本身作为步骤，避免丢失教程内容
            stepImages.mapIndexed { index, media ->
                StepEntity(
                    id = Ulid.next(),
                    recipeId = recipeId,
                    position = index,
                    text = "",
                    mediaPath = media.relativePath,
                )
            }
        }

        recipes.saveRecipe(recipe, ingredientEntities, stepEntities, source, savedMedia)
        writeSnapshot(recipe, source, ingredientEntities, stepEntities, savedMedia)

        val finalState = when {
            content.fetchStatus == FetchStatus.MANUAL -> ImportState.PARTIAL
            savedMedia.isEmpty() && content.media.isNotEmpty() -> ImportState.PARTIAL
            partial -> ImportState.PARTIAL
            else -> ImportState.READY
        }
        drafts.attachRecipe(prepared.draftId, recipeId, finalState.name, now())
        onProgress?.invoke(100, "完成")
        return@withContext ImportResult(
            recipeId = recipeId,
            partial = finalState != ImportState.READY,
            // 视频失败原因必须让用户看到：不能被解析阶段的提示文案盖掉。
            message = when {
                mediaSkipReason != null ->
                    "已按设置跳过媒体下载（$mediaSkipReason），只保存了链接和文字；" +
                        "到「设置 → 收录」调整后重新收录即可。"
                videoFailure != null && !content.message.isNullOrBlank() ->
                    "${content.message}；视频下载失败（$videoFailure），封面和文字已保存"
                videoFailure != null -> "视频下载失败（$videoFailure），封面和文字已保存"
                !content.message.isNullOrBlank() -> content.message
                finalState == ImportState.PARTIAL -> "部分内容已保存，可手动补充"
                else -> null
            },
        )
    }

    suspend fun importLocal(uris: List<Uri>, shareText: String): ImportResult = withContext(Dispatchers.IO) {
        val draftId = Ulid.next()
        drafts.upsert(
            DraftEntity(
                id = draftId,
                shareText = shareText,
                url = null,
                platform = Platform.OTHER.key,
                state = ImportState.DOWNLOADING.name,
                createdAt = now(),
                updatedAt = now(),
            ),
        )
        val recipeId = Ulid.next()
        val savedMedia = mutableListOf<MediaEntity>()
        var coverPath: String? = null
        var index = 0
        for (uri in uris) {
            val mime = com.weiji.wang.core.storage.MimeTypes.of(context, uri)
            val name = when {
                mime.startsWith("video/") -> FileStore.nameForVideo(index + 1)
                coverPath == null -> FileStore.nameForCover()
                else -> FileStore.nameForPhoto(index + 1)
            }
            val saved = runCatching { files.copyFromUri(recipeId, name, uri) }.getOrNull()
            if (saved == null) continue
            // 分享入口复制到 cache 的临时文件，导入完成后立即删除
            if (uri.scheme == "file") {
                runCatching { java.io.File(uri.path ?: "").takeIf { it.exists() }?.delete() }
            }
            if (mime.startsWith("image/") && coverPath == null) coverPath = saved.relativePath
            savedMedia += MediaEntity(
                id = Ulid.next(),
                recipeId = recipeId,
                kind = when {
                    mime.startsWith("video/") -> MediaKind.VIDEO.name
                    coverPath == saved.relativePath -> MediaKind.COVER.name
                    else -> MediaKind.PHOTO.name
                },
                relativePath = saved.relativePath,
                mime = mime,
                width = saved.width,
                height = saved.height,
                sizeBytes = saved.sizeBytes,
                sha256 = saved.sha256,
                watermarkFree = true,
                createdAt = now(),
            )
            index += 1
        }

        val title = LinkParser.titleHint(Platform.OTHER, shareText)
            ?: LinkParser.cleanShareNoise(shareText).lineSequence().firstOrNull { it.isNotBlank() }?.take(40)
            ?: "我的菜谱"
        val recipe = RecipeEntity(
            id = recipeId,
            title = title,
            coverPath = coverPath,
            createdAt = now(),
            updatedAt = now(),
        )
        recipes.saveRecipe(recipe, emptyList(), emptyList(), null, savedMedia)
        writeSnapshot(recipe, null, emptyList(), emptyList(), savedMedia)
        drafts.attachRecipe(draftId, recipeId, ImportState.READY.name, now())
        return@withContext ImportResult(recipeId, savedMedia.isEmpty())
    }

    suspend fun retry(draftId: String): PreparedImport? {
        val draft = drafts.get(draftId) ?: return null
        return runCatching { prepare(draft.shareText) }.getOrNull()
    }

    suspend fun discard(draftId: String) {
        drafts.delete(draftId)
    }

    private fun mediaEntity(
        recipeId: String,
        kind: MediaKind,
        saved: com.weiji.wang.core.storage.SavedFile,
        remote: RemoteMedia,
        at: Long,
    ): MediaEntity = MediaEntity(
        id = Ulid.next(),
        recipeId = recipeId,
        kind = kind.name,
        relativePath = saved.relativePath,
        mime = remote.mime,
        width = saved.width ?: remote.width,
        height = saved.height ?: remote.height,
        durationMs = remote.durationMs,
        sizeBytes = saved.sizeBytes,
        sha256 = saved.sha256,
        originUrl = remote.url,
        watermarkFree = remote.watermarkFree,
        createdAt = at,
    )

    private fun writeSnapshot(
        recipe: RecipeEntity,
        source: SourceEntity?,
        ingredients: List<IngredientEntity>,
        steps: List<StepEntity>,
        media: List<MediaEntity>,
    ) {
        runCatching {
            val json = com.weiji.wang.core.storage.RecipeSnapshot.toJson(recipe, source, ingredients, steps, media)
            files.writeTextAtomic(recipe.id, "recipe.json", json)
        }
    }

    private fun isOnWifi(): Boolean {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        ) {
            return true
        }
        // VPN 会接管 activeNetwork，只暴露 TRANSPORT_VPN；此时用「是否计费」判断底层网络。
        // 不修这条时，开着 VPN 的手机即使连的是 Wi-Fi，也会被当成移动网络而跳过所有媒体，
        // 最后只剩封面（cover 兜底不受 canDownload 约束）——用户看到的就是「只有封面」。
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) && !cm.isActiveNetworkMetered
    }
}
