package com.weiji.wang.core.import.extractor

import com.weiji.wang.core.import.CookieStore
import com.weiji.wang.core.import.DouyinJsonLdParser
import com.weiji.wang.core.import.DouyinWebViewParser
import com.weiji.wang.core.import.ExtractedContent
import com.weiji.wang.core.import.HttpClients
import com.weiji.wang.core.import.LinkParser
import com.weiji.wang.core.import.ParsedLink
import com.weiji.wang.core.import.PlatformExtractor
import com.weiji.wang.core.import.fetchText
import com.weiji.wang.core.import.resolveFinalUrl
import com.weiji.wang.core.model.FetchStatus
import com.weiji.wang.core.model.MediaKind
import com.weiji.wang.core.model.Platform
import okhttp3.OkHttpClient
import org.jsoup.Jsoup

/**
 * 抖音：短链 -> 分享页 -> 三级降级。
 *
 * 2026-11-04 实测：
 * - 短链 302 到 `https://www.iesdouyin.com/share/video/{id}/?...`，分享页 200/35KB；
 *   `window._ROUTER_DATA` 只剩 itemId 与 AB 参数，**不再含 play_addr**。
 * - `www.douyin.com/video/{id}` 桌面 UA 是 JS 反爬壳；Googlebot 的 SEO 页只有标题/封面/正文，无直链。
 * - OkHttp 直连 `iteminfo` 200/0 字节，`/aweme/v1/web/aweme/detail/` 403 Argus。
 *
 * 所以视频直链只能由**移动分享页自己**发签名请求后拿到，WebView 只负责加载分享页并嗅探；
 * SEO/JSON-LD 的标题、作者、封面、食材仍然保留，作为超时后的兜底。
 */
class DouyinExtractor(
    private val http: OkHttpClient,
    private val cookies: CookieStore,
) : PlatformExtractor {

    override val platform = Platform.DOUYIN

    override suspend fun extract(link: ParsedLink): ExtractedContent {
        val finalUrl = if (link.url.contains("v.douyin.com")) {
            http.resolveFinalUrl(link.url, headers())
        } else {
            link.url
        }
        val page = runCatching { http.fetchText(finalUrl, headers()) }.getOrNull()
        val html = page?.body.orEmpty()
        val resolvedUrl = page?.finalUrl ?: finalUrl

        val contentId = link.contentId
            ?: Regex("""(?:share/)?(?:video|note|slides)/(\d{15,25})""")
                .find(resolvedUrl)?.groupValues?.get(1)
        val isNote = resolvedUrl.contains("/note/") ||
            resolvedUrl.contains("/slides/") ||
            link.shareText.contains("图文作品") ||
            link.shareText.contains("图文")
        val kind = if (isNote) "note" else "video"
        // 短链 302 后的分享地址带 did/iid/share_sign/ts 等参数，用完即失效但也可能是
        // 分享页能正常渲染的唯一凭据（2026-11-04 实测：丢掉参数后无头环境直接出“抱歉出错了”）。
        // 所以：resolvedUrl 已经是 iesdouyin 分享页就原样用，否则才构造干净地址。
        val webViewUrl = when {
            resolvedUrl.startsWith("https://www.iesdouyin.com/share/") -> resolvedUrl
            contentId != null -> sharePageUrl(contentId, isNote)
            else -> resolvedUrl
        }

        val fromPageHtml = runCatching { DouyinWebViewParser.parse(html, resolvedUrl, link) }
            .getOrNull()
        val pageHasVideo = fromPageHtml?.media?.any { it.kind == MediaKind.VIDEO } == true

        // 0) 分享页 HTML 里偶尔直接内联了播放地址：视频已经到手，不用再渲染
        if (pageHasVideo) return fromPageHtml!!

        // 1) SEO / JSON-LD 兜底（匿名、单条、用户主动触发）
        val seo = if (contentId != null) {
            runCatching {
                http.fetchText(
                    "https://www.douyin.com/$kind/$contentId",
                    mapOf(
                        "User-Agent" to HttpClients.CRAWLER_UA,
                        "Accept-Language" to "zh-CN,zh;q=0.9",
                    ),
                )
            }.getOrNull()?.let { response ->
                DouyinJsonLdParser.parse(response.body, link.copy(contentId = contentId), kind)
            }
        } else {
            null
        }

        // 2) 图文：SEO 的原图足够生成菜谱，不需要进 WebView
        if (kind == "note") {
            val note = sequenceOf(seo, fromPageHtml)
                .filterNotNull()
                .firstOrNull { it.media.isNotEmpty() }
            if (note != null) {
                return note.copy(
                    canonicalUrl = contentId?.let { "https://www.douyin.com/note/$it" }
                        ?: note.canonicalUrl,
                    requiresWebView = false,
                )
            }
        }

        // 3) 视频：SEO 只有标题/封面/食材，直链必须由分享页渲染后嗅探
        if (seo != null) {
            return seo.copy(
                canonicalUrl = contentId?.let { "https://www.douyin.com/video/$it" } ?: seo.canonicalUrl,
                requiresWebView = true,
                webViewUrl = webViewUrl,
                message = "正在用内置浏览器读取抖音分享页；如果被风控，标题、封面和食材已经保存",
            )
        }

        // 4) 基础元数据 + 交给 WebView
        val doc = runCatching { Jsoup.parse(html) }.getOrNull()
        val ogTitle = doc?.selectFirst("meta[property=og:title]")?.attr("content")
            ?.takeIf { it.isNotBlank() }
        val ogDesc = doc?.selectFirst("meta[property=og:description]")?.attr("content").orEmpty()
        val pageTitle = doc?.title()?.takeIf { it.isNotBlank() && it != "抖音" }
        val title = link.titleHint ?: ogTitle ?: pageTitle ?: "抖音作品"

        return ExtractedContent(
            platform = platform,
            canonicalUrl = finalUrl,
            contentId = contentId,
            title = title.take(80),
            author = link.authorHint,
            textBody = ogDesc.ifBlank { LinkParser.cleanShareNoise(link.shareText) },
            coverUrl = null,
            media = emptyList(),
            fetchStatus = FetchStatus.PARTIAL,
            message = "抖音需要页面渲染，正在用内置浏览器解析",
            requiresWebView = true,
            webViewUrl = webViewUrl,
        )
    }

    /**
     * 移动分享页地址：视频 `share/video/{id}/`，图文 `share/slides/{id}/`。
     * `www.douyin.com/video/{id}` 是 JS 反爬壳，直接加载拿不到任何媒体。
     */
    private fun sharePageUrl(id: String, isNote: Boolean): String =
        if (isNote) {
            "https://www.iesdouyin.com/share/slides/$id/"
        } else {
            "https://www.iesdouyin.com/share/video/$id/"
        }

    private fun headers(): Map<String, String> = buildMap {
        put("User-Agent", HttpClients.MOBILE_UA)
        put("Referer", "https://www.douyin.com/")
        cookies.cookieFor(Platform.DOUYIN)?.let { put("Cookie", it) }
    }
}
