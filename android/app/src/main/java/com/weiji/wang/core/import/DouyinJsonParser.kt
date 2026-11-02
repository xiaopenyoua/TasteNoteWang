package com.weiji.wang.core.import

import com.weiji.wang.core.model.FetchStatus
import com.weiji.wang.core.model.MediaKind
import com.weiji.wang.core.model.Platform

/**
 * 解析抖音 detail 接口返回的 JSON（由 WebView 页面自己签名请求，我们只读响应）。
 * 无水印规则：video.play_addr / images[].url_list；不要用 download_addr / download_url_list。
 */
object DouyinJsonParser {

    fun parse(body: String, link: ParsedLink): ExtractedContent? {
        if (!body.contains("aweme_detail") && !body.contains("aweme_list")) return null
        val root = Jsons.parse(body) ?: return null
        val list = root.field("aweme_list")?.arr()
            ?: root.field("data")?.field("aweme_list")?.arr()
        val item = root.field("aweme_detail")
            ?: root.field("data")?.field("aweme_detail")
            ?: list?.firstOrNull { it.str("aweme_id") == link.contentId }
            ?: list?.firstOrNull()
            ?: return null

        val desc = item.str("desc").orEmpty()
        val author = item.path("author").str("nickname") ?: link.authorHint
        val authorId = item.path("author").str("uid") ?: item.path("author").str("sec_uid")
        val awemeId = item.str("aweme_id") ?: link.contentId
        val video = item.field("video")
        val durationMs = video?.long("duration")?.takeIf { it > 0 }

        val videoUrls = linkedSetOf<String>()
        video?.path("play_addr")?.strings("url_list")?.forEach { if (it.startsWith("http")) videoUrls += it }
        video?.field("bit_rate")?.arr()?.forEach { gear ->
            gear.path("play_addr")?.strings("url_list")?.forEach { if (it.startsWith("http")) videoUrls += it }
        }
        val chosenVideo = videoUrls.firstOrNull { it.contains(".mp4", true) || it.contains("play", true) }
            ?: videoUrls.firstOrNull()

        val imageUrls = item.field("images")?.arr()?.mapNotNull { image ->
            image.strings("url_list").firstOrNull { it.startsWith("http") }
        } ?: emptyList()

        val cover = video?.path("cover")?.strings("url_list")?.firstOrNull { it.startsWith("http") }
            ?: imageUrls.firstOrNull()

        val media = buildList {
            chosenVideo?.let {
                add(
                    RemoteMedia(
                        kind = MediaKind.VIDEO,
                        url = it.replaceFirst("http://", "https://"),
                        mime = "video/mp4",
                        durationMs = durationMs,
                        watermarkFree = true,
                        headers = headers(),
                        suggestedName = "video.mp4",
                    ),
                )
            }
            imageUrls.forEach {
                add(
                    RemoteMedia(
                        kind = MediaKind.STEP,
                        url = it.replaceFirst("http://", "https://"),
                        mime = "image/jpeg",
                        watermarkFree = true,
                        headers = imageHeaders(),
                    ),
                )
            }
        }
        if (media.isEmpty()) return null

        val title = desc.lineSequence().firstOrNull { it.isNotBlank() }?.trim()?.take(80)
            ?.takeIf { it.length >= 2 }
            ?: link.titleHint
            ?: "抖音作品"

        return ExtractedContent(
            platform = Platform.DOUYIN,
            canonicalUrl = awemeId?.let { "https://www.douyin.com/video/$it" }
                ?: "https://www.douyin.com/",
            contentId = awemeId,
            title = title,
            author = author,
            authorId = authorId,
            textBody = desc,
            tags = emptyList(),
            coverUrl = cover,
            media = media,
            fetchStatus = FetchStatus.OK,
            durationMs = durationMs,
            requiresWebView = false,
        )
    }

    fun headers(): Map<String, String> = mapOf(
        "User-Agent" to HttpClients.MOBILE_UA,
        "Referer" to "https://www.douyin.com/",
    )

    fun imageHeaders(): Map<String, String> = mapOf(
        "User-Agent" to HttpClients.MOBILE_UA,
        "Referer" to "https://www.douyin.com/",
    )
}
