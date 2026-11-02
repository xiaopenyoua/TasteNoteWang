package com.weiji.wang.core.import

import com.weiji.wang.core.model.FetchStatus
import com.weiji.wang.core.model.MediaKind
import com.weiji.wang.core.model.Platform
import org.jsoup.Jsoup

/**
 * 抖音 JSON-LD 兜底（匿名、单条、用户主动触发）。
 * 2026-10-30 实测：Googlebot UA 打开 www.douyin.com/{video|note}/{id} 会返回
 * VideoObject / article 结构化数据。视频不给直链，但 name 里常带完整食材；
 * 图文的 articleBody + image[] 足以生成菜谱。
 *
 * 这是 WebView 被风控（200/0 字节）时的最后一级降级，不做批量、不遍历。
 */
object DouyinJsonLdParser {

    private val durationRegex = Regex("""PT(?:(\d+)H)?(?:(\d+)M)?(?:(\d+)S)?""")
    private val splitMarkers = listOf("食材：", "食材:", "材料：", "材料:", "用料：", "用料:", "配料：", "配料:")
    private val watermarkMarker = listOf("-water", "water-v2", "tplv-dy-water")

    fun parse(html: String, link: ParsedLink, kind: String): ExtractedContent? {
        val doc = runCatching { Jsoup.parse(html) }.getOrNull() ?: return null
        val scripts = doc.select("script[type=application/ld+json]")
        for (script in scripts) {
            val root = Jsons.parse(script.data()) ?: continue
            when (root.str("@type")) {
                "VideoObject" -> parseVideo(root, link, kind)?.let { return it }
                "article", "NewsArticle", "BlogPosting" -> parseArticle(root, link)?.let { return it }
                else -> Unit
            }
        }
        return null
    }

    private fun parseVideo(root: kotlinx.serialization.json.JsonElement, link: ParsedLink, kind: String): ExtractedContent? {
        val rawName = (root.str("name") ?: root.str("headline") ?: link.titleHint ?: return null)
            .removeSuffix(" - 抖音")
            .trim()
        val description = root.str("description").orEmpty()
        val marker = splitMarkers.firstOrNull { rawName.contains(it) }
        val idx = marker?.let { rawName.indexOf(it) }?.takeIf { it > 0 }
        val title = (if (idx != null) rawName.substring(0, idx) else rawName.take(60))
            .trim().takeIf { it.isNotBlank() } ?: "抖音视频"
        val ingredientBlock = marker?.let {
            "食材\n" + rawName.substringAfter(it).replace("、", "\n")
        }.orEmpty()
        val body = listOf(ingredientBlock, description).filter { it.isNotBlank() }.joinToString("\n")

        val cover = firstUrl(root.field("thumbnailUrl"))
        val author = root.path("creator").str("name") ?: link.authorHint
        val durationMs = root.str("duration")?.let { parseIsoDuration(it) }
        val media = cover?.let {
            listOf(
                RemoteMedia(
                    kind = MediaKind.COVER,
                    url = it.replaceFirst("http://", "https://"),
                    mime = "image/jpeg",
                    watermarkFree = !isWatermarked(it),
                    headers = imageHeaders(),
                    suggestedName = "cover.jpg",
                ),
            )
        } ?: emptyList()

        return ExtractedContent(
            platform = Platform.DOUYIN,
            canonicalUrl = link.url,
            contentId = link.contentId,
            title = title,
            author = author,
            textBody = body,
            tags = parseKeywords(root),
            coverUrl = cover,
            media = media,
            fetchStatus = FetchStatus.PARTIAL,
            message = if (kind == "video") "视频需要登录抖音后重试；已保存标题、封面、食材和文字" else null,
            durationMs = durationMs,
            requiresWebView = kind == "video",
            webViewUrl = null,
        )
    }

    private fun parseArticle(root: kotlinx.serialization.json.JsonElement, link: ParsedLink): ExtractedContent? {
        val headline = root.str("headline")?.takeIf { it.isNotBlank() }
            ?: root.str("name")?.takeIf { it.isNotBlank() }
            ?: link.titleHint
            ?: return null
        val body = listOf(
            root.str("articleBody").orEmpty(),
            root.str("description").orEmpty(),
        ).filter { it.isNotBlank() }.joinToString("\n")
        val images = urlList(root.field("image"))
            .filter { !isWatermarked(it) }
            .map {
                RemoteMedia(
                    kind = MediaKind.STEP,
                    url = it.replaceFirst("http://", "https://"),
                    mime = "image/jpeg",
                    watermarkFree = true,
                    headers = imageHeaders(),
                )
            }
        val author = root.path("author").str("name")
            ?: root.path("creator").str("name")
            ?: link.authorHint
        val cover = images.firstOrNull()?.url

        return ExtractedContent(
            platform = Platform.DOUYIN,
            canonicalUrl = link.url,
            contentId = link.contentId,
            title = headline.trim().take(80),
            author = author,
            textBody = body,
            tags = parseKeywords(root),
            coverUrl = cover,
            media = images,
            fetchStatus = if (images.isEmpty()) FetchStatus.PARTIAL else FetchStatus.OK,
            requiresWebView = false,
        )
    }

    private fun firstUrl(element: kotlinx.serialization.json.JsonElement?): String? =
        urlList(element).firstOrNull()

    private fun urlList(element: kotlinx.serialization.json.JsonElement?): List<String> {
        if (element == null) return emptyList()
        return when {
            element is kotlinx.serialization.json.JsonPrimitive ->
                element.content?.takeIf { it.startsWith("http") }?.let { listOf(it) } ?: emptyList()
            element is kotlinx.serialization.json.JsonArray ->
                element.flatMap { urlList(it) }
            element is kotlinx.serialization.json.JsonObject ->
                listOfNotNull(element.str("url")?.takeIf { it.startsWith("http") })
            else -> emptyList()
        }
    }

    private fun parseKeywords(root: kotlinx.serialization.json.JsonElement): List<String> {
        val raw = root.str("keywords") ?: return emptyList()
        return raw.split(',', '，', '#').map { it.trim() }.filter { it.isNotBlank() }.take(10)
    }

    private fun isWatermarked(url: String): Boolean =
        watermarkMarker.any { url.contains(it, ignoreCase = true) }

    private fun parseIsoDuration(value: String): Long? {
        val match = durationRegex.find(value) ?: return null
        val h = match.groupValues[1].toLongOrNull() ?: 0
        val m = match.groupValues[2].toLongOrNull() ?: 0
        val s = match.groupValues[3].toLongOrNull() ?: 0
        val total = (h * 3600 + m * 60 + s) * 1000
        return total.takeIf { it > 0 }
    }

    fun imageHeaders(): Map<String, String> = mapOf(
        "User-Agent" to HttpClients.MOBILE_UA,
        "Referer" to "https://www.douyin.com/",
    )
}
