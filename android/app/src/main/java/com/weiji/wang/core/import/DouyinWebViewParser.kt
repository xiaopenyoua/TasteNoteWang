package com.weiji.wang.core.import

import com.weiji.wang.core.model.FetchStatus
import com.weiji.wang.core.model.MediaKind
import com.weiji.wang.core.model.Platform
import org.jsoup.Jsoup

/**
 * 抖音分享页不服务端渲染（2026-10-30 实测 _ROUTER_DATA 只有页面元信息，详情接口 403 Argus），
 * 所以必须由 WebView 先渲染，再把渲染后的 HTML / 拦截到的媒体 URL 交给这里解析。
 */
object DouyinWebViewParser : WebViewContentParser {

    private val mediaUrl = Regex(
        """https?://[^\s"'\\<>]+?(?:\.mp4|/aweme/v1/play/|douyinvod\.com|byteicdn\.com)[^\s"'\\<>]*""",
        RegexOption.IGNORE_CASE,
    )
    // 只收真正的作品原图；页面上的头像、作者其他作品缩略图（cropcenter:300:400）一律不要
    private val imageUrl = Regex(
        """https?://[^\s"'\\<>]+?~tplv-dy-aweme-images[^\s"'\\<>]*""",
        RegexOption.IGNORE_CASE,
    )
    private val badUrl = listOf(
        "avatar", "aweme-avatar", "emoji", ".js", ".css", "favicon",
        "cropcenter", "100x100", "aweme-client-static-resource", "icon_play",
        "select_m_station", "douyinstatic", "douyin-pc-web", "uuu_",
        "media-audio", "/audio", ".m4a",
    )

    override fun parse(html: String, url: String, link: ParsedLink): ExtractedContent? {
        val doc = runCatching { Jsoup.parse(html, url) }.getOrNull()
        val title = doc?.selectFirst("meta[property=og:title]")?.attr("content")
            ?.takeIf { it.isNotBlank() }
            ?: doc?.selectFirst("meta[name=title]")?.attr("content")?.takeIf { it.isNotBlank() }
            ?: doc?.title()?.takeIf { it.isNotBlank() && it != "抖音" }
            ?: link.titleHint
            ?: "抖音作品"

        val description = doc?.selectFirst("meta[property=og:description]")?.attr("content")
            ?: doc?.selectFirst("meta[name=description]")?.attr("content")
            ?: ""

        val videoCandidates = linkedSetOf<String>()
        doc?.select("video[src]")?.eachAttr("src")?.forEach { if (isMedia(it)) videoCandidates += it }
        doc?.select("source[src]")?.eachAttr("src")?.forEach { if (isMedia(it)) videoCandidates += it }
        mediaUrl.findAll(html).map { it.value }.forEach { if (isMedia(it)) videoCandidates += it }

        val imageCandidates = linkedSetOf<String>()
        // 不扫描页面上的 <img>（里面混着头像、作者其他作品缩略图）；
        // 只从内联 JSON 的 ~tplv-dy-aweme-images 原图模板取。
        imageUrl.findAll(html).map { it.value }.forEach { if (isImage(it)) imageCandidates += it }
        doc?.selectFirst("meta[property=og:image]")?.attr("content")
            ?.takeIf { it.startsWith("http") }
            ?.let { imageCandidates += it }

        val video = videoCandidates.firstOrNull()
        val images = imageCandidates.take(20)
        if (video == null && images.isEmpty()) return null

        val media = buildList {
            video?.let {
                add(
                    RemoteMedia(
                        kind = MediaKind.VIDEO,
                        url = it.replaceFirst("http://", "https://"),
                        mime = "video/mp4",
                        watermarkFree = !it.contains("watermark", ignoreCase = true),
                        headers = headers(),
                        suggestedName = "video.mp4",
                    ),
                )
            }
            images.forEach {
                add(
                    RemoteMedia(
                        kind = MediaKind.STEP,
                        url = it.replaceFirst("http://", "https://"),
                        mime = "image/jpeg",
                        watermarkFree = true,
                        headers = headers(),
                    ),
                )
            }
        }

        val contentId = link.contentId
            ?: Regex("""(?:video|note|slides)/(\d{15,25})""").find(url)?.groupValues?.get(1)

        return ExtractedContent(
            platform = Platform.DOUYIN,
            canonicalUrl = url,
            contentId = contentId,
            title = title.trim().take(80),
            author = link.authorHint,
            textBody = description.ifBlank { LinkParser.cleanShareNoise(link.shareText) },
            coverUrl = images.firstOrNull(),
            media = media,
            fetchStatus = if (media.isEmpty()) FetchStatus.PARTIAL else FetchStatus.OK,
            requiresWebView = false,
        )
    }

    private fun isMedia(url: String): Boolean =
        url.startsWith("http") && badUrl.none { url.contains(it, ignoreCase = true) } &&
            (url.contains("douyinvod", true) ||
                url.contains("byteicdn", true) ||
                url.contains("/aweme/v1/play", true))

    private fun isImage(url: String): Boolean =
        url.startsWith("http") && badUrl.none { url.contains(it, ignoreCase = true) } &&
            url.contains("~tplv-dy-aweme-images", ignoreCase = true)

    fun headers(): Map<String, String> = mapOf(
        "User-Agent" to HttpClients.MOBILE_UA,
        "Referer" to "https://www.douyin.com/",
    )
}
