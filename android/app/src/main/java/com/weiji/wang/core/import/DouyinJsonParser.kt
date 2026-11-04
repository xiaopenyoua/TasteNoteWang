package com.weiji.wang.core.import

import com.weiji.wang.core.model.FetchStatus
import com.weiji.wang.core.model.MediaKind
import com.weiji.wang.core.model.Platform
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * 只解析抖音公开接口的响应体，不发明字段。
 *
 * 当前匿名可达的两种结构（2026-11-04 实测）：
 * - `{"status_code":0,"item_list":[{"aweme_id":...}]}` —— 分享页 `/web/api/v2/aweme/iteminfo/` 的响应；
 * - `{"aweme_detail":{"aweme_id":...}}` —— detail 老路径，也见过 `aweme_detail.aweme_detail` 二次包裹。
 *
 * 无水印规则：`video.play_addr`（含 `bit_rate[].play_addr`）/ `images[].url_list`；
 * **不取** `download_addr`（带水印）。视频直链优先 `douyinvod` / `byteicdn` / `video/tos`，
 * 因为 `/aweme/v1/play/` 是带签名的跳转口，App 里容易被判 403。
 *
 * 只有媒体直链、没有任何 JSON 时走 [synthesize]：分享页仍会真实播放一次视频，
 * 直链能从 XHR/performance/DOM 里捞到，标题/作者/封面用 SEO 兜底结果。
 */
object DouyinJsonParser {

    private const val MAX_WRAPPER_DEPTH = 6

    /** 直连 CDN 的标识；命中即优先，避免走需要 page 签名的跳转口。 */
    private val cdnMarkers = listOf("douyinvod", "byteicdn", "video/tos")

    /** 图文原图模板标识；命中说明是作品原图而不是头像/推荐位缩略图。 */
    private val originImageMarkers = listOf("aweme-images", "douyinpic", "byteimg")

    private val watermarkMarkers = listOf("tplv-dy-water", "-water", "playwm")

    /** 判断响应体值不值得解析，避免把任意 JSON 交给递归。 */
    private val bodyMarkers = listOf("aweme_id", "item_list", "aweme_list", "aweme_detail")

    // ------------------------------------------------------------------ JSON

    fun parse(body: String, link: ParsedLink): ExtractedContent? {
        if (body.isBlank()) return null
        if (bodyMarkers.none { body.contains(it) }) return null
        val root = Jsons.parse(body) ?: return null
        val items = awemeItems(root)
        if (items.isEmpty()) return null
        // 页面会预取相关推荐：链接带 id 时只认 id 相同的那一条，宁可失败也不串号。
        val item = pickItem(items, link) ?: return null

        val desc = item.str("desc").orEmpty()
        val author = item.path("author").str("nickname") ?: link.authorHint
        val authorId = item.path("author").str("uid")
            ?: item.path("author").str("sec_uid")
        val awemeId = item.str("aweme_id") ?: item.str("item_id") ?: link.contentId
        val video = item.field("video")
        val durationMs = video?.long("duration")?.takeIf { it > 0 }
            ?: item.long("duration")?.takeIf { it > 0 }

        val chosenVideo = pickVideoUrl(collectVideoUrls(video))
        val images = collectImageUrls(item)
        val cover = collectCoverUrl(video, item) ?: images.firstOrNull()

        val media = buildList {
            chosenVideo?.let {
                add(
                    RemoteMedia(
                        kind = MediaKind.VIDEO,
                        url = https(it),
                        mime = "video/mp4",
                        durationMs = durationMs,
                        sizeBytes = video?.long("data_size")?.takeIf { size -> size > 0 },
                        watermarkFree = true,
                        headers = mediaHeaders(),
                        suggestedName = "video.mp4",
                    ),
                )
            }
            images.forEach {
                add(
                    RemoteMedia(
                        kind = MediaKind.STEP,
                        url = https(it),
                        mime = "image/jpeg",
                        watermarkFree = true,
                        headers = imageHeaders(),
                    ),
                )
            }
        }
        if (media.isEmpty()) return null

        val hasVideo = chosenVideo != null
        return ExtractedContent(
            platform = Platform.DOUYIN,
            canonicalUrl = canonicalUrl(awemeId, hasVideo, link.url),
            contentId = awemeId ?: link.contentId,
            title = titleOf(desc, link),
            author = author,
            authorId = authorId,
            textBody = desc,
            tags = hashtags(item),
            coverUrl = cover?.let { https(it) },
            media = media,
            fetchStatus = FetchStatus.OK,
            durationMs = durationMs,
            requiresWebView = false,
        )
    }

    /** 递归找“像一条作品”的对象：item_list[0]、aweme_detail、aweme_detail.aweme_detail 都能命中。 */
    private fun awemeItems(element: JsonElement?, depth: Int = 0): List<JsonElement> {
        if (element == null || depth > MAX_WRAPPER_DEPTH) return emptyList()
        return when (element) {
            is JsonObject -> buildList {
                if (looksLikeAweme(element)) add(element)
                element.values.forEach { value -> addAll(awemeItems(value, depth + 1)) }
            }
            is JsonArray -> element.flatMap { awemeItems(it, depth + 1) }
            else -> emptyList()
        }
    }

    private fun looksLikeAweme(obj: JsonObject): Boolean =
        (obj["aweme_id"] != null || obj["item_id"] != null) &&
            (obj["video"] != null || obj["images"] != null || obj["desc"] != null)

    private fun pickItem(items: List<JsonElement>, link: ParsedLink): JsonElement? {
        val target = link.contentId ?: return items.firstOrNull()
        return items.firstOrNull { item ->
            item.str("aweme_id") == target || item.str("item_id") == target
        }
    }

    // ------------------------------------------------------------------ media

    private fun collectVideoUrls(video: JsonElement?): List<String> {
        if (video == null) return emptyList()
        val urls = linkedSetOf<String>()
        fun take(playAddr: JsonElement?) {
            playAddr.strings("url_list").forEach { url ->
                val clean = url.trim()
                if (clean.startsWith("http")) urls += clean
            }
        }
        take(video.field("play_addr"))
        take(video.field("play_addr_h264"))
        take(video.field("play_addr_265"))
        video.field("bit_rate").arr()?.forEach { gear -> take(gear.field("play_addr")) }
        return urls.toList()
    }

    private fun pickVideoUrl(urls: List<String>): String? =
        urls.firstOrNull { url -> cdnMarkers.any { url.contains(it, ignoreCase = true) } }
            ?: urls.firstOrNull { it.contains("mime_type=video_mp4", ignoreCase = true) }
            ?: urls.firstOrNull { it.contains(".mp4", ignoreCase = true) }
            ?: urls.firstOrNull()

    private fun collectImageUrls(item: JsonElement): List<String> =
        item.field("images").arr()?.mapNotNull { image ->
            val urls = image.strings("url_list").map { it.trim() }
                .filter { it.startsWith("http") && !isWatermarked(it) }
            urls.firstOrNull { url -> originImageMarkers.any { url.contains(it, ignoreCase = true) } }
                ?: urls.firstOrNull()
        }?.distinct() ?: emptyList()

    private fun collectCoverUrl(video: JsonElement?, item: JsonElement): String? {
        val candidates = listOfNotNull(
            video?.field("origin_cover"),
            video?.field("cover"),
            video?.field("dynamic_cover"),
            item.field("cover"),
        )
        for (candidate in candidates) {
            val url = candidate.strings("url_list")
                .firstOrNull { it.startsWith("http") && !isWatermarked(it) }
            if (url != null) return url
        }
        return null
    }

    private fun isWatermarked(url: String): Boolean =
        watermarkMarkers.any { url.contains(it, ignoreCase = true) }

    // ------------------------------------------------------------- synthesize

    /**
     * 只有媒体直链、没有 JSON 时的合成路径。
     *
     * 分享页自己播放视频时，CDN 地址会出现在 XHR / `performance.getEntriesByType('resource')` /
     * `video.currentSrc` 里；这里挑最好的那条，标题/作者/封面用 SEO 结果补。
     * `duration` 未知，**不猜**。
     */
    fun synthesize(
        candidateUrls: Collection<String>,
        seo: ExtractedContent?,
        link: ParsedLink,
        pageUrl: String? = null,
    ): ExtractedContent? {
        val videoUrls = candidateUrls
            .map { it.trim() }
            .filter { it.startsWith("http") && it.length < 2_000 }
            .filter { looksLikeVideoUrl(it) }
            .distinct()
        val chosen = pickVideoUrl(videoUrls) ?: return null
        val isNotePage = pageUrl?.contains("/slides/") == true || pageUrl?.contains("/note/") == true

        val contentId = link.contentId
            ?: pageUrl?.let { Regex("""/(?:video|note|slides)/(\d{15,25})""").find(it)?.groupValues?.get(1) }
            ?: seo?.contentId
        val media = buildList {
            add(
                RemoteMedia(
                    kind = MediaKind.VIDEO,
                    url = https(chosen),
                    mime = "video/mp4",
                    durationMs = null,
                    watermarkFree = !isWatermarked(chosen),
                    headers = mediaHeaders(),
                    suggestedName = "video.mp4",
                ),
            )
            val cover = seo?.coverUrl
            if (cover != null) {
                add(
                    RemoteMedia(
                        kind = MediaKind.COVER,
                        url = https(cover),
                        mime = "image/jpeg",
                        watermarkFree = true,
                        headers = imageHeaders(),
                        suggestedName = "cover.jpg",
                    ),
                )
            }
        }

        return ExtractedContent(
            platform = Platform.DOUYIN,
            canonicalUrl = when {
                contentId == null && seo?.canonicalUrl != null -> seo.canonicalUrl
                contentId == null -> link.url
                isNotePage -> "https://www.douyin.com/note/$contentId"
                else -> "https://www.douyin.com/video/$contentId"
            },
            contentId = contentId,
            title = seo?.title?.takeIf { it.isNotBlank() } ?: titleOf("", link),
            author = seo?.author ?: link.authorHint,
            authorId = seo?.authorId,
            textBody = seo?.textBody.orEmpty(),
            tags = seo?.tags.orEmpty(),
            coverUrl = seo?.coverUrl?.let { https(it) },
            media = media,
            fetchStatus = FetchStatus.OK,
            durationMs = null,
            message = null,
            requiresWebView = false,
        )
    }

    /**
     * 嗅探到的直链候选：只认直连 CDN / .mp4。
     * `/aweme/v1/play/` 是需要页面签名的跳转口，性能条目里出现它也不该拿来下载（2026-11-04 实测 403）。
     */
    private fun looksLikeVideoUrl(url: String): Boolean =
        !isWatermarked(url) &&
            (cdnMarkers.any { url.contains(it, ignoreCase = true) } ||
                url.contains(".mp4", ignoreCase = true))

    // ------------------------------------------------------------------ utils

    private fun canonicalUrl(id: String?, hasVideo: Boolean, fallback: String): String = when {
        id == null -> fallback
        hasVideo -> "https://www.douyin.com/video/$id"
        else -> "https://www.douyin.com/note/$id"
    }

    private fun titleOf(desc: String, link: ParsedLink): String =
        desc.lineSequence().firstOrNull { it.isNotBlank() }?.trim()?.take(80)
            ?.takeIf { it.length >= 2 }
            ?: link.titleHint?.takeIf { it.length >= 2 }
            ?: "抖音作品"

    private fun hashtags(item: JsonElement): List<String> =
        item.field("text_extra").arr()
            ?.mapNotNull { it.str("hashtag_name")?.trim() }
            ?.filter { it.isNotBlank() }
            ?.take(10)
            ?: emptyList()

    private fun https(url: String): String = url.replaceFirst("http://", "https://")

    /**
     * 视频请求头：CDN 会检查 Referer/Origin；Accept 用通配值，避免部分节点返回 406。
     */
    fun mediaHeaders(): Map<String, String> = mapOf(
        "User-Agent" to HttpClients.MOBILE_UA,
        "Referer" to "https://www.douyin.com/",
        "Origin" to "https://www.douyin.com",
        "Accept" to "*/*",
    )

    fun imageHeaders(): Map<String, String> = mapOf(
        "User-Agent" to HttpClients.MOBILE_UA,
        "Referer" to "https://www.douyin.com/",
        "Origin" to "https://www.douyin.com",
        "Accept" to "*/*",
    )
}
