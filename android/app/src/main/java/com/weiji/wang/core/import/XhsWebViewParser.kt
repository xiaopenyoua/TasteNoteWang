package com.weiji.wang.core.import

import com.weiji.wang.core.model.FetchStatus
import com.weiji.wang.core.model.MediaKind
import com.weiji.wang.core.model.Platform

/**
 * 小红书笔记页解析。已验证 2026-10-30：
 * - 页面内 window.__INITIAL_STATE__ 的 noteData.data.noteData 是笔记本体；
 * - imageList[].fileId 拼 https://ci.xiaohongshu.com/{fileId} 得到无水印原图；
 * - video.media.stream.h264[0].masterUrl 是视频流。
 */
object XhsWebViewParser : WebViewContentParser {

    override fun parse(html: String, url: String, link: ParsedLink): ExtractedContent? {
        val root = Jsons.embeddedJson(html, "__INITIAL_STATE__") ?: return null
        val note = root.path("noteData", "data", "noteData") ?: return null
        if (note.obj() == null) return null

        val noteId = note.str("noteId") ?: link.contentId
        val type = note.str("type") ?: "normal"
        val desc = note.str("desc").orEmpty()
        val title = note.str("title")
            ?.takeIf { it.isNotBlank() }
            ?: desc.lineSequence().firstOrNull { it.isNotBlank() }?.take(40)
            ?: link.titleHint
            ?: "小红书笔记"

        val user = note.field("user")
        val author = user.str("nickName") ?: user.str("nickname") ?: link.authorHint
        val authorId = user.str("userId")
        val tags = note.field("tagList")?.arr()?.mapNotNull {
            it.str("name")?.takeIf { name -> name.isNotBlank() }
        } ?: emptyList()

        val images = note.field("imageList")?.arr()?.mapNotNull { element ->
            val fileId = element.str("fileId")
            val infoList = element.field("infoList")?.arr()?.mapNotNull { info ->
                info.str("url")?.let { info.str("imageScene") to it }
            }?.toMap()
            val original = if (!fileId.isNullOrBlank()) {
                "https://ci.xiaohongshu.com/$fileId"
            } else {
                infoList?.get("H5_DTL") ?: element.str("url")
            }
            original?.let {
                RemoteMedia(
                    kind = MediaKind.STEP,
                    url = it.replaceFirst("http://", "https://"),
                    mime = "image/jpeg",
                    width = element.int("width"),
                    height = element.int("height"),
                    watermarkFree = true,
                    headers = imageHeaders(),
                )
            }
        } ?: emptyList()

        val video = note.field("video")
        val stream = video.path("media", "stream")
        val h264 = stream.field("h264")?.arr()?.firstOrNull()
        val videoUrl = (h264?.str("masterUrl")
            ?: h264?.strings("backupUrls")?.firstOrNull()
            ?: video.str("masterUrl"))
            ?.replaceFirst("http://", "https://")
        val durationSeconds = video.field("capa")?.int("duration")
            ?: video.path("media", "video").int("duration")
            ?: h264?.int("duration")

        val media = buildList {
            if (!videoUrl.isNullOrBlank()) {
                add(
                    RemoteMedia(
                        kind = MediaKind.VIDEO,
                        url = videoUrl,
                        mime = "video/mp4",
                        width = h264?.int("width"),
                        height = h264?.int("height"),
                        durationMs = durationSeconds?.let { it * 1000L },
                        sizeBytes = h264?.long("size"),
                        watermarkFree = true,
                        headers = mediaHeaders(),
                        suggestedName = "video.mp4",
                    ),
                )
            }
            addAll(images)
        }

        val cover = images.firstOrNull()?.url
            ?: video.str("image")?.let { Jsons.firstUrl(it) }

        return ExtractedContent(
            platform = Platform.XIAOHONGSHU,
            canonicalUrl = url,
            contentId = noteId,
            title = title.trim(),
            author = author,
            authorId = authorId,
            textBody = desc,
            tags = tags,
            coverUrl = cover,
            media = media,
            fetchStatus = if (media.isEmpty()) FetchStatus.PARTIAL else FetchStatus.OK,
            durationMs = durationSeconds?.let { it * 1000L },
            requiresWebView = false,
        )
    }

    fun imageHeaders(): Map<String, String> = mapOf(
        "User-Agent" to HttpClients.MOBILE_UA,
        "Referer" to "https://www.xiaohongshu.com/",
    )

    fun mediaHeaders(): Map<String, String> = imageHeaders()
}
