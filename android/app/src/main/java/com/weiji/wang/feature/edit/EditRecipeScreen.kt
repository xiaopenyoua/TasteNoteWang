package com.weiji.wang.feature.edit

import android.content.Context
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import com.weiji.wang.core.data.MenuRepository
import com.weiji.wang.core.data.RecipeRepository
import com.weiji.wang.core.data.db.IngredientEntity
import com.weiji.wang.core.data.db.MediaEntity
import com.weiji.wang.core.data.db.RecipeEntity
import com.weiji.wang.core.data.db.RecipeWithDetails
import com.weiji.wang.core.data.db.SourceEntity
import com.weiji.wang.core.data.db.StepEntity
import com.weiji.wang.core.import.LinkParser
import com.weiji.wang.core.model.MediaKind
import com.weiji.wang.core.model.Platform
import com.weiji.wang.core.model.Ulid
import com.weiji.wang.core.storage.FileStore
import com.weiji.wang.core.storage.MimeTypes
import com.weiji.wang.core.storage.VideoFrames
import com.weiji.wang.ui.LocalContainer
import com.weiji.wang.ui.components.ConfirmDialog
import com.weiji.wang.ui.components.PageHeader
import com.weiji.wang.ui.components.PrimaryButton
import com.weiji.wang.ui.components.SecondaryButton
import com.weiji.wang.ui.components.focusRing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val MAX_STEP_MEDIA = 20
private const val MAX_STEP_CHARS = 300
private const val CONTENT_MAX_WIDTH = 760

internal class EditViewModel(
    private val recipeId: String,
    private val repo: RecipeRepository,
    private val files: FileStore,
    private val menu: MenuRepository,
    private val appContext: Context,
) : ViewModel() {
    val isNew: Boolean = recipeId == "new" || recipeId.isBlank()

    var loaded by mutableStateOf(isNew)
        private set
    var title by mutableStateOf("")
        private set
    var servingsText by mutableStateOf("2")
        private set
    var minutesText by mutableStateOf("")
        private set
    var notes by mutableStateOf("")
        private set
    var sourceUrl by mutableStateOf("")
        private set

    var error by mutableStateOf<String?>(null)
        private set
    var saveError by mutableStateOf<String?>(null)
        private set
    var loadError by mutableStateOf<String?>(null)
        private set
    var isSaving by mutableStateOf(false)
        private set
    var dirty by mutableStateOf(false)
        private set
    var saveAttempted by mutableStateOf(false)
        private set
    var duplicateNames by mutableStateOf<List<String>?>(null)
        private set

    val ingredients = mutableStateListOf<IngredientDraft>()
    val steps = mutableStateListOf<StepDraft>()
    val importableSteps = mutableStateListOf<String>()

    var cover by mutableStateOf<MediaSlot?>(null)
        private set
    var originalVideo by mutableStateOf<MediaSlot?>(null)
        private set

    private var existing: RecipeWithDetails? = null
    private val removedMediaIds = mutableSetOf<String>()
    private val removedFallbackPaths = mutableSetOf<String>()
    private var pendingSave: ((String) -> Unit)? = null

    init {
        if (!isNew) {
            viewModelScope.launch {
                val details = runCatching { repo.details(recipeId) }.getOrNull()
                existing = details
                if (details != null) {
                    fill(details)
                } else {
                    loadError = "没有找到这道菜谱，可以重新保存创建。"
                }
                loaded = true
            }
        }
    }

    fun updateTitle(value: String) {
        title = value
        clearError()
    }

    fun updateServings(value: String) {
        servingsText = value.filter { it.isDigit() }
        clearError()
    }

    fun updateMinutes(value: String) {
        minutesText = value.filter { it.isDigit() }
        clearError()
    }

    fun updateNotes(value: String) {
        notes = value
        error = null
        saveError = null
        dirty = true
    }

    fun updateSourceUrl(value: String) {
        sourceUrl = value
        error = null
        saveError = null
        dirty = true
    }

    fun addIngredient() {
        ingredients += IngredientDraft()
        dirty = true
    }

    fun updateIngredientName(key: String, value: String) {
        updateIngredient(key) { it.copy(name = value) }
    }

    fun updateIngredientAmount(key: String, value: String) {
        updateIngredient(key) { it.copy(amountText = value) }
    }

    fun updateIngredientUnit(key: String, value: String) {
        updateIngredient(key) { it.copy(unitText = value) }
    }

    fun removeIngredient(key: String) {
        ingredients.removeAll { it.key == key }
        dirty = true
    }

    fun addStep() {
        steps += StepDraft()
        dirty = true
    }

    fun updateStepText(key: String, value: String) {
        val index = steps.indexOfFirst { it.key == key }
        if (index < 0) return
        steps[index] = steps[index].copy(text = value.take(MAX_STEP_CHARS))
        dirty = true
    }

    fun removeStep(key: String) {
        val step = steps.firstOrNull { it.key == key } ?: return
        step.media.forEach(::detachStepMedia)
        steps.removeAll { it.key == key }
        dirty = true
    }

    fun moveStep(key: String, delta: Int) {
        val index = steps.indexOfFirst { it.key == key }
        val target = index + delta
        if (index < 0 || target !in steps.indices) return
        val item = steps[index]
        steps[index] = steps[target]
        steps[target] = item
        dirty = true
    }

    fun addStepMedia(stepKey: String, uris: List<Uri>) {
        val index = steps.indexOfFirst { it.key == stepKey }
        if (index < 0 || uris.isEmpty()) return
        val step = steps[index]
        val room = (MAX_STEP_MEDIA - step.media.size).coerceAtLeast(0)
        if (room == 0) return
        val additions = uris.take(room).map { uri ->
            StepMediaDraft(uri = uri.toString(), mime = mimeOf(uri))
        }
        steps[index] = step.copy(media = step.media + additions)
        dirty = true
    }

    fun removeStepMedia(stepKey: String, mediaKey: String) {
        val index = steps.indexOfFirst { it.key == stepKey }
        if (index < 0) return
        val step = steps[index]
        val media = step.media.firstOrNull { it.key == mediaKey } ?: return
        detachStepMedia(media)
        steps[index] = step.copy(media = step.media.filterNot { it.key == mediaKey })
        dirty = true
    }

    fun moveStepMedia(stepKey: String, mediaKey: String, delta: Int) {
        val stepIndex = steps.indexOfFirst { it.key == stepKey }
        if (stepIndex < 0) return
        val step = steps[stepIndex]
        val index = step.media.indexOfFirst { it.key == mediaKey }
        val target = index + delta
        if (index < 0 || target !in step.media.indices) return
        val list = step.media.toMutableList()
        val item = list[index]
        list[index] = list[target]
        list[target] = item
        steps[stepIndex] = step.copy(media = list)
        dirty = true
    }

    fun setCover(uri: Uri) {
        detachSlot(cover)
        cover = MediaSlot(uri = uri.toString(), mime = mimeOf(uri))
        dirty = true
    }

    fun removeCover() {
        detachSlot(cover)
        cover = null
        dirty = true
    }

    fun setOriginalVideo(uri: Uri) {
        detachSlot(originalVideo)
        originalVideo = MediaSlot(uri = uri.toString(), mime = mimeOf(uri))
        dirty = true
    }

    fun removeOriginalVideo() {
        detachSlot(originalVideo)
        originalVideo = null
        dirty = true
    }

    fun appendImportedSteps() {
        if (importableSteps.isEmpty()) return
        steps += importableSteps.map { StepDraft(text = it.take(MAX_STEP_CHARS)) }
        dirty = true
    }

    fun replaceWithImportedSteps() {
        if (importableSteps.isEmpty()) return
        steps.forEach { step -> step.media.forEach(::detachStepMedia) }
        steps.clear()
        steps += importableSteps.map { StepDraft(text = it.take(MAX_STEP_CHARS)) }
        dirty = true
    }

    fun requestSave(onDone: (String) -> Unit) {
        if (isSaving) return
        saveAttempted = true
        val validation = validationError()
        error = validation
        if (validation != null) return
        val duplicates = duplicateIngredientNames()
        if (duplicates.isNotEmpty()) {
            pendingSave = onDone
            duplicateNames = duplicates
            return
        }
        launchSave(onDone)
    }

    fun confirmDuplicateSave() {
        val callback = pendingSave
        pendingSave = null
        duplicateNames = null
        if (callback != null) launchSave(callback)
    }

    fun cancelDuplicateSave() {
        pendingSave = null
        duplicateNames = null
    }

    private fun launchSave(onDone: (String) -> Unit) {
        isSaving = true
        saveError = null
        viewModelScope.launch {
            runCatching { persist() }
                .onSuccess { id ->
                    runCatching { menu.refreshSnapshot(id) }
                    isSaving = false
                    dirty = false
                    onDone(id)
                }
                .onFailure { throwable ->
                    isSaving = false
                    saveError = throwable.message?.takeIf { it.isNotBlank() } ?: "保存失败，请重试"
                }
        }
    }

    private fun fill(details: RecipeWithDetails) {
        title = details.recipe.title
        servingsText = details.recipe.servings.toString()
        minutesText = details.recipe.totalMinutes?.toString().orEmpty()
        notes = details.recipe.notes
        sourceUrl = details.source?.originalUrl.orEmpty()

        ingredients.clear()
        details.ingredients.sortedBy { it.position }.forEach { item ->
            val amount = amountTextOf(item)
            ingredients += IngredientDraft(
                original = item,
                originalAmountText = amount,
                originalUnitText = item.unit.orEmpty(),
                name = item.name,
                amountText = amount,
                unitText = item.unit.orEmpty(),
            )
        }

        val coverPath = details.recipe.coverPath
        val coverMedia = details.media.firstOrNull {
            it.kind == MediaKind.COVER.name && it.relativePath == coverPath
        }
        cover = coverPath?.let {
            MediaSlot(
                existingId = coverMedia?.id,
                existingPath = it,
                existingMime = coverMedia?.mime,
            )
        }

        val video = details.media
            .filter { it.kind == MediaKind.VIDEO.name && it.stepId == null }
            .minByOrNull { it.createdAt }
        originalVideo = video?.let {
            MediaSlot(existingId = it.id, existingPath = it.relativePath, existingMime = it.mime)
        }

        steps.clear()
        val legacyMedia = details.media
            .filter { it.kind == MediaKind.STEP.name && it.stepId == null }
            .sortedBy { it.createdAt }
        val usedLegacyIds = mutableSetOf<String>()
        details.steps.sortedBy { it.position }.forEachIndexed { index, step ->
            val own = details.media
                .filter { it.stepId == step.id }
                .sortedWith(compareBy({ it.position }, { it.createdAt }))
            val media = if (own.isNotEmpty()) {
                own.map { it.toStepMediaDraft() }
            } else {
                val fallbackPath = step.mediaPath?.takeIf { it.isNotBlank() }
                when {
                    fallbackPath != null -> {
                        val matched = legacyMedia.firstOrNull { it.relativePath == fallbackPath }
                            ?: details.media.firstOrNull {
                                it.relativePath == fallbackPath &&
                                    it.stepId == null &&
                                    it.kind == MediaKind.PHOTO.name
                            }
                        matched?.let { usedLegacyIds += it.id }
                        listOf(
                            StepMediaDraft(
                                existingId = matched?.id,
                                existingPath = fallbackPath,
                                existingMime = matched?.mime ?: "image/webp",
                            ),
                        )
                    }
                    else -> {
                        val legacy = legacyMedia.getOrNull(index)?.takeIf { it.id !in usedLegacyIds }
                        if (legacy != null) {
                            usedLegacyIds += legacy.id
                            listOf(legacy.toStepMediaDraft())
                        } else {
                            emptyList()
                        }
                    }
                }
            }
            steps += StepDraft(
                id = step.id,
                text = step.text,
                durationSeconds = step.durationSeconds,
                media = media,
            )
        }

        importableSteps.clear()
        importableSteps.addAll(parseOriginalSteps(details.source?.textBody))
    }

    private suspend fun persist(): String {
        val id = existing?.recipe?.id ?: Ulid.next()
        val timestamp = System.currentTimeMillis()
        val validSteps = steps.filter { it.text.isNotBlank() || it.media.isNotEmpty() }

        val picked = withContext(Dispatchers.IO) {
            val stepMedia = validSteps.flatMap { step ->
                step.media.mapIndexedNotNull { index, item ->
                    item.uri?.let { uri ->
                        savePicked(id, uri, item.mime, step.id, index, timestamp)
                    }
                }
            }
            val coverSlot = cover
            val coverMedia = coverSlot?.uri?.let {
                saveCoverPicked(id, it, coverSlot.mime, timestamp)
            }
            val videoSlot = originalVideo
            val videoMedia = videoSlot?.uri?.let {
                savePicked(id, it, videoSlot.mime, stepId = null, position = 0, timestamp = timestamp)
            }
            PickedMedia(stepMedia, coverMedia, videoMedia)
        }

        val newPaths = picked.stepMedia.map { it.relativePath } +
            listOfNotNull(picked.coverMedia?.relativePath, picked.videoMedia?.relativePath)

        try {
            val baseline = existing?.media.orEmpty().filter { it.id !in removedMediaIds }
            val assignments = mutableMapOf<String, Pair<String, Int>>()
            validSteps.forEach { step ->
                step.media.forEachIndexed { index, item ->
                    item.existingId?.let { assignments[it] = step.id to index }
                }
            }
            val adjustedBaseline = baseline.map { media ->
                assignments[media.id]?.let { (stepId, position) ->
                    media.copy(stepId = stepId, position = position)
                } ?: media
            }
            val knownPaths = baseline.map { it.relativePath }.toSet()
            val fallbackMedia = validSteps.flatMap { step ->
                step.media.mapIndexedNotNull { index, item ->
                    val path = item.existingPath
                    if (item.existingId != null || path.isNullOrBlank() || path in knownPaths) {
                        return@mapIndexedNotNull null
                    }
                    val file = files.resolve(path)
                    if (!file.exists() || file.length() == 0L) return@mapIndexedNotNull null
                    MediaEntity(
                        id = Ulid.next(),
                        recipeId = id,
                        kind = MediaKind.STEP.name,
                        relativePath = path,
                        mime = item.existingMime?.takeIf { it.isNotBlank() } ?: "image/webp",
                        sizeBytes = file.length(),
                        createdAt = timestamp,
                        stepId = step.id,
                        position = index,
                    )
                }
            }
            val mediaList = adjustedBaseline + fallbackMedia + picked.stepMedia +
                listOfNotNull(picked.coverMedia, picked.videoMedia)

            val coverPath = picked.coverMedia?.relativePath ?: cover?.existingPath
            val cleanTitle = title.trim()
            val servings = servingsText.toIntOrNull()?.coerceIn(1, 12)
                ?: existing?.recipe?.servings
                ?: 2
            val minutes = minutesText.toIntOrNull()?.takeIf { it > 0 }
            val source = buildSource(id, timestamp)
            val base = existing?.recipe
            val recipe = if (base != null) {
                base.copy(
                    title = cleanTitle,
                    servings = servings,
                    totalMinutes = minutes,
                    notes = notes,
                    coverPath = coverPath,
                    sourceId = source?.id,
                    updatedAt = timestamp,
                )
            } else {
                RecipeEntity(
                    id = id,
                    title = cleanTitle,
                    coverPath = coverPath,
                    servings = servings,
                    totalMinutes = minutes,
                    notes = notes,
                    sourceId = source?.id,
                    createdAt = timestamp,
                    updatedAt = timestamp,
                )
            }

            val recipeIngredients = ingredients
                .filter {
                    it.name.isNotBlank() || it.amountText.isNotBlank() || it.unitText.isNotBlank()
                }
                .mapIndexed { index, draft -> draft.toEntity(id, index) }
            val stepEntities = validSteps.mapIndexed { index, step ->
                StepEntity(
                    id = step.id,
                    recipeId = id,
                    position = index,
                    text = step.text.trim(),
                    durationSeconds = step.durationSeconds,
                    mediaPath = null,
                )
            }

            repo.saveRecipe(
                recipe = recipe,
                ingredientList = recipeIngredients,
                stepList = stepEntities,
                source = source,
                mediaList = mediaList,
            )

            val keptPaths = mediaList.map { it.relativePath }.toSet()
            val removedPaths = buildSet {
                removedMediaIds.forEach { mediaId ->
                    existing?.media?.firstOrNull { it.id == mediaId }?.let { add(it.relativePath) }
                }
                addAll(removedFallbackPaths)
                val oldCover = existing?.recipe?.coverPath
                if (oldCover != null && oldCover != coverPath) add(oldCover)
            }
            withContext(Dispatchers.IO) {
                removedPaths.filter { it !in keptPaths }.forEach { files.deleteFile(it) }
            }
            return id
        } catch (throwable: Throwable) {
            withContext(Dispatchers.IO) { newPaths.forEach { files.deleteFile(it) } }
            throw throwable
        }
    }

    private fun savePicked(
        recipeId: String,
        uriString: String,
        mimeHint: String?,
        stepId: String?,
        position: Int,
        timestamp: Long,
    ): MediaEntity {
        val uri = Uri.parse(uriString)
        val mime = mimeHint?.takeIf { it.isNotBlank() } ?: mimeOf(uri)
        val isVideo = mime.startsWith("video/")
        val input = appContext.contentResolver.openInputStream(uri)
            ?: error("无法读取所选文件")
        return input.use { stream ->
            if (isVideo) {
                val name = mediaFileName(mime, video = true, prefix = "media")
                val saved = files.saveStream(recipeId, name, stream, mime)
                MediaEntity(
                    id = Ulid.next(),
                    recipeId = recipeId,
                    kind = MediaKind.VIDEO.name,
                    relativePath = saved.relativePath,
                    mime = mime,
                    width = saved.width,
                    height = saved.height,
                    durationMs = VideoFrames.durationMs(files.resolve(saved.relativePath)),
                    sizeBytes = saved.sizeBytes,
                    sha256 = saved.sha256,
                    createdAt = timestamp,
                    stepId = stepId,
                    position = position,
                )
            } else {
                val name = mediaFileName(mime, video = false, prefix = "media")
                val saved = files.saveImage(recipeId, name, stream, mime)
                MediaEntity(
                    id = Ulid.next(),
                    recipeId = recipeId,
                    kind = if (stepId != null) MediaKind.STEP.name else MediaKind.COVER.name,
                    relativePath = saved.relativePath,
                    mime = if (mime == "image/gif") "image/gif" else "image/webp",
                    width = saved.width,
                    height = saved.height,
                    sizeBytes = saved.sizeBytes,
                    sha256 = saved.sha256,
                    createdAt = timestamp,
                    stepId = stepId,
                    position = position,
                )
            }
        }
    }

    private fun saveCoverPicked(
        recipeId: String,
        uriString: String,
        mimeHint: String?,
        timestamp: Long,
    ): MediaEntity {
        val uri = Uri.parse(uriString)
        val mime = mimeHint?.takeIf { it.isNotBlank() } ?: mimeOf(uri)
        if (!mime.startsWith("video/")) {
            return savePicked(recipeId, uriString, mime, stepId = null, position = 0, timestamp = timestamp)
        }
        val tempName = mediaFileName(mime, video = true, prefix = "cover-src")
        val input = appContext.contentResolver.openInputStream(uri)
            ?: error("无法读取所选视频")
        val temp = input.use { files.saveStream(recipeId, tempName, it, mime) }
        val frame = VideoFrames.extract(files.resolve(temp.relativePath), atMs = 1_000L)
        if (frame == null) {
            return MediaEntity(
                id = Ulid.next(),
                recipeId = recipeId,
                kind = MediaKind.COVER.name,
                relativePath = temp.relativePath,
                mime = mime,
                sizeBytes = temp.sizeBytes,
                sha256 = temp.sha256,
                createdAt = timestamp,
                position = 0,
            )
        }
        val saved = try {
            files.saveBitmap(
                recipeId,
                mediaFileName("image/webp", video = false, prefix = "cover"),
                frame,
            )
        } finally {
            frame.recycle()
        }
        files.deleteFile(temp.relativePath)
        return MediaEntity(
            id = Ulid.next(),
            recipeId = recipeId,
            kind = MediaKind.COVER.name,
            relativePath = saved.relativePath,
            mime = "image/webp",
            width = saved.width,
            height = saved.height,
            sizeBytes = saved.sizeBytes,
            sha256 = saved.sha256,
            createdAt = timestamp,
            position = 0,
        )
    }

    private fun mediaFileName(mime: String, video: Boolean, prefix: String): String {
        val extension = when {
            mime == "image/gif" -> "gif"
            video -> when (mime) {
                "video/quicktime" -> "mov"
                "video/webm" -> "webm"
                "video/x-matroska" -> "mkv"
                else -> "mp4"
            }
            else -> "webp"
        }
        return "$prefix-${Ulid.next()}.$extension"
    }

    private fun buildSource(recipeId: String, now: Long): SourceEntity? {
        val url = sourceUrl.trim()
        if (url.isEmpty()) return null
        val old = existing?.source?.takeIf { it.originalUrl == url }
        return SourceEntity(
            id = old?.id ?: Ulid.next(),
            recipeId = recipeId,
            platform = old?.platform ?: LinkParser.detect(url).key,
            originalUrl = url,
            canonicalUrl = old?.canonicalUrl,
            platformContentId = old?.platformContentId
                ?: LinkParser.extractContentId(Platform.OTHER, url),
            authorName = old?.authorName,
            authorId = old?.authorId,
            sourceTitle = old?.sourceTitle ?: title,
            textBody = old?.textBody,
            remoteCoverUrl = old?.remoteCoverUrl,
            shareText = old?.shareText,
            importedAt = old?.importedAt ?: now,
            fetchStatus = old?.fetchStatus ?: "MANUAL",
        )
    }

    private fun validationError(): String? {
        if (title.trim().isEmpty()) return "菜名不能为空"
        val blankNamed = ingredients.any {
            it.name.isBlank() && (it.amountText.isNotBlank() || it.unitText.isNotBlank())
        }
        if (blankNamed) return "食材名称不能为空"
        return null
    }

    private fun duplicateIngredientNames(): List<String> = ingredients
        .map { it.name.trim() }
        .filter { it.isNotEmpty() }
        .groupingBy { it }
        .eachCount()
        .filterValues { it > 1 }
        .keys
        .toList()

    private fun updateIngredient(key: String, transform: (IngredientDraft) -> IngredientDraft) {
        val index = ingredients.indexOfFirst { it.key == key }
        if (index < 0) return
        ingredients[index] = transform(ingredients[index])
        clearError()
    }

    private fun clearError() {
        error = null
        saveError = null
        dirty = true
    }

    private fun detachStepMedia(media: StepMediaDraft) {
        media.existingId?.let { removedMediaIds += it }
        if (media.existingId == null) {
            media.existingPath?.let { removedFallbackPaths += it }
        }
    }

    private fun detachSlot(slot: MediaSlot?) {
        slot ?: return
        slot.existingId?.let { removedMediaIds += it }
        if (slot.existingId == null) {
            slot.existingPath?.let { removedFallbackPaths += it }
        }
    }

    private fun mimeOf(uri: Uri): String =
        appContext.contentResolver.getType(uri)?.takeIf { it.isNotBlank() }
            ?: MimeTypes.of(appContext, uri)
}

internal data class IngredientDraft(
    val key: String = Ulid.next(),
    val original: IngredientEntity? = null,
    val originalAmountText: String = "",
    val originalUnitText: String = "",
    val name: String = "",
    val amountText: String = "",
    val unitText: String = "",
)

internal data class StepDraft(
    val key: String = Ulid.next(),
    val id: String = Ulid.next(),
    val text: String = "",
    val durationSeconds: Int? = null,
    val media: List<StepMediaDraft> = emptyList(),
)

private data class PickedMedia(
    val stepMedia: List<MediaEntity>,
    val coverMedia: MediaEntity?,
    val videoMedia: MediaEntity?,
)

private val numericAmount = Regex("""^\s*\d+(?:\.\d+)?(?:\s*[-~～至]\s*\d+(?:\.\d+)?)?\s*$""")
private val rangeAmount = Regex("""^\s*(\d+(?:\.\d+)?)\s*[-~～至]\s*(\d+(?:\.\d+)?)\s*$""")
private val leadingRange = Regex("""^\s*(\d+(?:\.\d+)?)\s*[-~～至]\s*(\d+(?:\.\d+)?)""")
private val vagueWords = listOf("适量", "少许", "若干", "随意", "按需", "一点", "少量")
private val stepHeaderWords = listOf("步骤", "做法", "制作", "教程", "流程", "烹饪过程", "操作")
private val numberedStepRegex = Regex(
    """^\s*(?:第[一二三四五六七八九十\d]+步[：:.、]?|(\d{1,2})[.、)）:：]|[①②③④⑤⑥⑦⑧⑨⑩])\s*(.+)$""",
)
private val sentenceBoundary = Regex("""(?<=[。！？!?；;])""")

private fun isVagueAmount(text: String): Boolean =
    text.isNotBlank() && !numericAmount.matches(text)

private fun formatNumber(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()

private fun amountTextOf(item: IngredientEntity): String {
    leadingRange.find(item.rawText)?.let { match ->
        return "${match.groupValues[1]}-${match.groupValues[2]}"
    }
    item.amount?.let { return formatNumber(it) }
    return vagueWords.firstOrNull { item.rawText.contains(it) }.orEmpty()
}

private fun parseAmountText(text: String): Double? {
    val trimmed = text.trim()
    trimmed.toDoubleOrNull()?.let { return it }
    val match = rangeAmount.find(trimmed) ?: return null
    val first = match.groupValues[1].toDoubleOrNull() ?: return null
    val second = match.groupValues[2].toDoubleOrNull() ?: return null
    return (first + second) / 2.0
}

private fun rawTextOf(amountText: String, unitText: String, name: String): String {
    val head = amountText.trim() + unitText.trim()
    return listOf(head, name.trim())
        .filter { it.isNotEmpty() }
        .joinToString(" ")
}

private fun IngredientDraft.toEntity(recipeId: String, position: Int): IngredientEntity {
    val original = this.original
    val cleanName = name.trim()
    val cleanAmount = amountText.trim()
    val cleanUnit = unitText.trim()
    if (original != null &&
        cleanName == original.name &&
        cleanAmount == originalAmountText &&
        cleanUnit == originalUnitText
    ) {
        return original.copy(position = position)
    }
    val vague = isVagueAmount(cleanAmount)
    val amount = if (vague) null else parseAmountText(cleanAmount)
    return IngredientEntity(
        id = original?.id ?: Ulid.next(),
        recipeId = recipeId,
        position = position,
        groupName = original?.groupName,
        name = cleanName,
        amount = amount,
        unit = if (vague) null else cleanUnit.ifBlank { null },
        rawText = rawTextOf(cleanAmount, if (vague) "" else cleanUnit, cleanName),
        scalable = amount != null && !vague,
        optional = original?.optional ?: false,
    )
}

private fun parseOriginalSteps(text: String?): List<String> {
    if (text.isNullOrBlank()) return emptyList()
    val lines = text.replace("\r\n", "\n").replace('\r', '\n')
        .split('\n')
        .map { it.trim() }
    val headerIndex = lines.indexOfFirst { line ->
        val stripped = line.trim(' ', '：', ':', '#', '*', '【', '】')
        stripped.length <= 20 && stepHeaderWords.any { stripped.startsWith(it) }
    }
    val body = if (headerIndex >= 0) lines.drop(headerIndex + 1) else lines
    val numbered = body.mapNotNull { line ->
        numberedStepRegex.find(line)?.groupValues?.get(2)?.trim()?.takeIf { it.length >= 2 }
    }
    if (numbered.isNotEmpty()) return numbered

    val paragraphs = mutableListOf<String>()
    val current = StringBuilder()
    body.forEach { line ->
        if (line.isBlank()) {
            if (current.isNotBlank()) {
                paragraphs += current.toString().trim()
                current.clear()
            }
        } else {
            if (current.isNotEmpty()) current.append('\n')
            current.append(line)
        }
    }
    if (current.isNotBlank()) paragraphs += current.toString().trim()
    val cleaned = paragraphs.filter { it.length >= 2 }
    if (cleaned.size > 1) return cleaned
    val single = cleaned.firstOrNull() ?: return emptyList()
    val sentences = single.split(sentenceBoundary).map { it.trim() }.filter { it.length >= 2 }
    return if (sentences.size > 1) sentences else listOf(single)
}

@Composable
fun EditRecipeScreen(nav: NavHostController, recipeId: String) {
    val container = LocalContainer.current
    val vm: EditViewModel = viewModel(
        key = "edit-$recipeId",
        factory = viewModelFactory {
            initializer {
                EditViewModel(
                    recipeId = recipeId,
                    repo = container.recipes,
                    files = container.files,
                    menu = container.menu,
                    appContext = container.appContext,
                )
            }
        },
    )

    var pendingStepKey by rememberSaveable { mutableStateOf<String?>(null) }
    var showImportSheet by remember { mutableStateOf(false) }
    var showDiscard by remember { mutableStateOf(false) }

    val stepMediaLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(MAX_STEP_MEDIA),
    ) { uris ->
        val key = pendingStepKey
        pendingStepKey = null
        if (key != null && uris.isNotEmpty()) vm.addStepMedia(key, uris)
    }
    val coverLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> uri?.let(vm::setCover) }
    val videoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> uri?.let(vm::setOriginalVideo) }

    val pickCover: () -> Unit = {
        coverLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo),
        )
    }
    val pickVideo: () -> Unit = {
        videoLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly),
        )
    }
    val pickStepMedia: (String) -> Unit = { key ->
        pendingStepKey = key
        stepMediaLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo),
        )
    }

    val saveAndReturn: (String) -> Unit = { id ->
        val editDestinationId = nav.currentBackStackEntry?.destination?.id
        nav.navigate("recipe/$id") {
            if (editDestinationId != null) {
                popUpTo(editDestinationId) { inclusive = true }
            }
            launchSingleTop = true
        }
    }

    BackHandler {
        when {
            vm.isSaving -> Unit
            vm.dirty -> showDiscard = true
            else -> nav.popBackStack()
        }
    }

    if (!vm.loaded) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("载入中…", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    if (showDiscard) {
        ConfirmDialog(
            title = "放弃修改？",
            body = "未保存的改动会丢失。",
            confirmLabel = "放弃",
            dismissLabel = "继续编辑",
            onDismiss = { showDiscard = false },
            onConfirm = {
                showDiscard = false
                nav.popBackStack()
            },
        )
    }
    vm.duplicateNames?.let { names ->
        ConfirmDialog(
            title = "已有同名食材，仍要添加？",
            body = "重复：${names.joinToString("、")}",
            confirmLabel = "仍要添加",
            dismissLabel = "返回修改",
            destructive = false,
            onDismiss = vm::cancelDuplicateSave,
            onConfirm = vm::confirmDuplicateSave,
        )
    }
    if (showImportSheet) {
        ImportStepsSheet(
            steps = vm.importableSteps,
            onDismiss = { showImportSheet = false },
            onAppend = vm::appendImportedSteps,
            onReplace = vm::replaceWithImportedSteps,
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            Box(
                modifier = Modifier
                    .widthIn(max = CONTENT_MAX_WIDTH.dp)
                    .fillMaxWidth(),
            ) {
                PageHeader(
                    title = if (vm.isNew) "新建菜谱" else "编辑",
                    subtitle = "自动解析的结果可以随时改",
                    actions = {
                        TextButton(
                            onClick = { vm.requestSave(saveAndReturn) },
                            enabled = !vm.isSaving,
                        ) {
                            Text(if (vm.isSaving) "保存中…" else "保存")
                        }
                    },
                )
            }
        }
        if (vm.loadError != null) {
            item {
                EditorBlock {
                    Text(
                        text = vm.loadError.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
        item {
            EditorBlock {
                OutlinedTextField(
                    value = vm.title,
                    onValueChange = vm::updateTitle,
                    label = { Text("菜名") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = editFieldColors(),
                    isError = vm.saveAttempted && vm.title.isBlank(),
                    supportingText = if (vm.saveAttempted && vm.title.isBlank()) {
                        { Text("菜名不能为空") }
                    } else {
                        null
                    },
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = vm.servingsText,
                        onValueChange = vm::updateServings,
                        label = { Text("几人份") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = editFieldColors(),
                    )
                    OutlinedTextField(
                        value = vm.minutesText,
                        onValueChange = vm::updateMinutes,
                        label = { Text("时长（分钟）") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = editFieldColors(),
                    )
                }
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = vm.sourceUrl,
                    onValueChange = vm::updateSourceUrl,
                    label = { Text("来源链接（可选）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = editFieldColors(),
                )
            }
        }
        item { Spacer(Modifier.height(14.dp)) }
        item {
            EditorBlock {
                BlockLabel("封面")
                Spacer(Modifier.height(8.dp))
                CoverPicker(slot = vm.cover, onPick = pickCover, onRemove = vm::removeCover)
            }
        }
        item { Spacer(Modifier.height(14.dp)) }
        item {
            EditorBlock {
                BlockLabel("媒体")
                Spacer(Modifier.height(8.dp))
                OriginalVideoRow(
                    slot = vm.originalVideo,
                    onPick = pickVideo,
                    onRemove = vm::removeOriginalVideo,
                )
            }
        }
        item { Spacer(Modifier.height(14.dp)) }
        item { EditorBlock { IngredientsSection(vm = vm) } }
        item { Spacer(Modifier.height(14.dp)) }
        item {
            EditorBlock {
                StepsSection(
                    vm = vm,
                    onImport = { showImportSheet = true },
                    onPickMedia = pickStepMedia,
                )
            }
        }
        item { Spacer(Modifier.height(14.dp)) }
        item {
            EditorBlock {
                BlockLabel("说明")
                Text(
                    "这道菜的背景或注意事项，不会进入「我的备注」。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = vm.notes,
                    onValueChange = vm::updateNotes,
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = editFieldColors(),
                )
            }
        }
        item { Spacer(Modifier.height(18.dp)) }
        item {
            EditorBlock {
                val message = vm.saveError ?: vm.error
                if (message != null) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Spacer(Modifier.height(8.dp))
                }
                PrimaryButton(
                    text = if (vm.isSaving) "保存中…" else "保存",
                    enabled = !vm.isSaving,
                    onClick = { vm.requestSave(saveAndReturn) },
                )
            }
        }
    }
}

@Composable
private fun EditorBlock(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .widthIn(max = CONTENT_MAX_WIDTH.dp)
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        content = content,
    )
}

@Composable
private fun editFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    disabledBorderColor = MaterialTheme.colorScheme.outlineVariant,
    errorBorderColor = MaterialTheme.colorScheme.error,
)

@Composable
private fun BlockLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp),
    )
}

@Composable
private fun IngredientsSection(vm: EditViewModel) {
    BlockLabel("食材")
    Text(
        "一条一条填；数量可以写 2、2-3、适量，模糊量不用选单位。",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.tertiary,
    )
    Spacer(Modifier.height(6.dp))
    if (vm.ingredients.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp),
        ) {
            Text("还没有食材", style = MaterialTheme.typography.bodyMedium)
            Text(
                "点下方「添加食材」，或从原文里复制过来。",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }
    } else {
        vm.ingredients.forEach { draft ->
            IngredientRow(
                draft = draft,
                showNameError = vm.saveAttempted && draft.name.isBlank() &&
                    (draft.amountText.isNotBlank() || draft.unitText.isNotBlank()),
                onName = { vm.updateIngredientName(draft.key, it) },
                onAmount = { vm.updateIngredientAmount(draft.key, it) },
                onUnit = { vm.updateIngredientUnit(draft.key, it) },
                onDelete = { vm.removeIngredient(draft.key) },
            )
            Spacer(Modifier.height(8.dp))
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .focusRing(RoundedCornerShape(12.dp))
            .clickable { vm.addIngredient() },
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = "＋ 添加食材",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun IngredientRow(
    draft: IngredientDraft,
    showNameError: Boolean,
    onName: (String) -> Unit,
    onAmount: (String) -> Unit,
    onUnit: (String) -> Unit,
    onDelete: () -> Unit,
) {
    val vague = isVagueAmount(draft.amountText)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = draft.name,
            onValueChange = onName,
            placeholder = { Text("名称", style = MaterialTheme.typography.labelMedium) },
            singleLine = true,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(14.dp),
            colors = editFieldColors(),
            isError = showNameError,
            textStyle = MaterialTheme.typography.bodyMedium,
        )
        OutlinedTextField(
            value = draft.amountText,
            onValueChange = onAmount,
            placeholder = { Text("数量", style = MaterialTheme.typography.labelMedium) },
            singleLine = true,
            modifier = Modifier.width(68.dp),
            shape = RoundedCornerShape(14.dp),
            colors = editFieldColors(),
            textStyle = MaterialTheme.typography.bodyMedium,
        )
        OutlinedTextField(
            value = draft.unitText,
            onValueChange = onUnit,
            placeholder = { Text("单位", style = MaterialTheme.typography.labelMedium) },
            singleLine = true,
            enabled = !vague,
            modifier = Modifier.width(56.dp),
            shape = RoundedCornerShape(14.dp),
            colors = editFieldColors(),
            textStyle = MaterialTheme.typography.bodyMedium,
        )
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .focusRing(RoundedCornerShape(12.dp))
                .clickable(onClick = onDelete),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = "删除食材",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun StepsSection(
    vm: EditViewModel,
    onImport: () -> Unit,
    onPickMedia: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "步骤",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        if (vm.importableSteps.isNotEmpty()) {
            Text(
                text = "从原文导入",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onImport)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
    Text(
        "每一步可添加多张图片和视频，混合排列；顺序即展示顺序。",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.tertiary,
    )
    Spacer(Modifier.height(8.dp))
    if (vm.steps.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp),
        ) {
            Text("还没有步骤", style = MaterialTheme.typography.bodyMedium)
            Text(
                "点下方「添加一步」，或从原文导入。",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }
    } else {
        vm.steps.forEachIndexed { index, step ->
            StepCard(
                step = step,
                index = index,
                total = vm.steps.size,
                showImportHint = vm.importableSteps.isNotEmpty(),
                onText = { vm.updateStepText(step.key, it) },
                onMove = { delta -> vm.moveStep(step.key, delta) },
                onDelete = { vm.removeStep(step.key) },
                onPickMedia = { onPickMedia(step.key) },
                onMoveMedia = { mediaKey, delta -> vm.moveStepMedia(step.key, mediaKey, delta) },
                onRemoveMedia = { mediaKey -> vm.removeStepMedia(step.key, mediaKey) },
            )
            Spacer(Modifier.height(6.dp))
        }
    }
    Spacer(Modifier.height(4.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
            .focusRing(RoundedCornerShape(14.dp))
            .clickable { vm.addStep() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "＋ 添加一步",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StepCard(
    step: StepDraft,
    index: Int,
    total: Int,
    showImportHint: Boolean,
    onText: (String) -> Unit,
    onMove: (Int) -> Unit,
    onDelete: () -> Unit,
    onPickMedia: () -> Unit,
    onMoveMedia: (String, Int) -> Unit,
    onRemoveMedia: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "${index + 1}",
                    color = Color.Black,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = "第 ${index + 1} 步",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.weight(1f))
            StepToolButton(
                icon = Icons.Filled.KeyboardArrowUp,
                description = "上移步骤",
                enabled = index > 0,
            ) { onMove(-1) }
            StepToolButton(
                icon = Icons.Filled.KeyboardArrowDown,
                description = "下移步骤",
                enabled = index < total - 1,
            ) { onMove(1) }
            StepToolButton(
                icon = Icons.Filled.Delete,
                description = "删除步骤",
                enabled = true,
            ) { onDelete() }
        }
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = step.text,
            onValueChange = onText,
            minLines = 2,
            maxLines = 10,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = editFieldColors(),
            textStyle = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = "${step.text.length}/$MAX_STEP_CHARS",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.tertiary,
            textAlign = TextAlign.End,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
        )
        if (showImportHint && total == 1 &&
            (step.text.length > 180 || step.text.contains("http"))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            ) {
                Text(
                    text = "这一步像整段导入文案。可从「从原文导入」拆成多步。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            step.media.forEachIndexed { mediaIndex, item ->
                StepMediaTile(
                    item = item,
                    order = mediaIndex + 1,
                    canMoveLeft = mediaIndex > 0,
                    canMoveRight = mediaIndex < step.media.lastIndex,
                    onMoveLeft = { onMoveMedia(item.key, -1) },
                    onMoveRight = { onMoveMedia(item.key, 1) },
                    onRemove = { onRemoveMedia(item.key) },
                )
            }
            if (step.media.size < MAX_STEP_MEDIA) {
                AddMediaTile(onClick = onPickMedia)
            }
        }
    }
}

@Composable
private fun StepToolButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                alpha = if (enabled) 1f else 0.3f,
            ),
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun CoverPicker(
    slot: MediaSlot?,
    onPick: () -> Unit,
    onRemove: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    if (slot == null || slot.isEmpty) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .dashedBorder(MaterialTheme.colorScheme.outline, 16.dp)
                .clickable(onClick = onPick),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "选择封面图片或视频",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "支持 JPG / PNG / MP4",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(shape),
        ) {
            DraftMediaImage(
                existingPath = slot.existingPath,
                uri = slot.uri,
                isVideo = slot.isVideo,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                    .clickable(onClick = onRemove),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "移除封面",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(16.dp),
                )
            }
            Text(
                text = "更换",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                    .clickable(onClick = onPick)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun OriginalVideoRow(
    slot: MediaSlot?,
    onPick: () -> Unit,
    onRemove: () -> Unit,
) {
    val hasVideo = slot != null && !slot.isEmpty
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text("原视频（可选）", style = MaterialTheme.typography.bodyMedium)
            Text(
                text = if (hasVideo) "已添加 · 保存在本机" else "未添加 · 支持 MP4",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (hasVideo) {
            Text(
                text = "更换",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onPick)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
            Text(
                text = "移除",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onRemove)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        } else {
            Text(
                text = "添加视频",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onPick)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImportStepsSheet(
    steps: List<String>,
    onDismiss: () -> Unit,
    onAppend: () -> Unit,
    onReplace: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
        ) {
            Text("从原文导入", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                text = "原文里解析出 ${steps.size} 个步骤。追加会保留现有步骤，替换会清空现有步骤。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Column(
                modifier = Modifier
                    .heightIn(max = 260.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                steps.forEachIndexed { index, text ->
                    Text(
                        text = "${index + 1}. $text",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton(
                    text = "追加",
                    onClick = {
                        onAppend()
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                )
                PrimaryButton(
                    text = "替换",
                    onClick = {
                        onReplace()
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
