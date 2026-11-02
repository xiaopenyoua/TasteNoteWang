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
import com.weiji.wang.core.model.Platform
import okhttp3.OkHttpClient
import org.jsoup.Jsoup

/**
 * 抖音：短链 -> 分享页 -> 三级降级。
 * 1. JSON-LD 匿名兜底（图文直接可导入；视频给标题/封面/食材/文字）
 * 2. 分享页 HTML 里偶尔可读的媒体/图片
 * 3. WebView 由页面自己签名请求 detail，应用拦截响应（UI 层负责）
 *
 * 2026-10-30 实测：匿名 detail 常返回 200/0 字节（风控）；登录后成功率取决于账号。
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
            ?: Regex("""(?:video|note|slides)/(\d{15,25})""").find(resolvedUrl)?.groupValues?.get(1)
        val isNote = resolvedUrl.contains("/note/") ||
            resolvedUrl.contains("/slides/") ||
            link.shareText.contains("图文作品") ||
            link.shareText.contains("图文")
        val kind = if (isNote) "note" else "video"
        val webViewUrl = contentId?.let {
            if (isNote) "https://www.douyin.com/note/$it" else "https://www.douyin.com/video/$it"
        } ?: resolvedUrl

        // 1) JSON-LD：匿名、单条、用户主动触发
        if (contentId != null) {
            val ld = runCatching {
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
            if (ld != null) {
                return if (kind == "note") {
                    ld.copy(canonicalUrl = "https://www.douyin.com/note/$contentId")
                } else {
                    ld.copy(
                        canonicalUrl = "https://www.douyin.com/video/$contentId",
                        requiresWebView = true,
                        webViewUrl = webViewUrl,
                        message = "正在用内置浏览器尝试获取视频；如果被风控，标题、封面和食材已经保存",
                    )
                }
            }
        }

        // 2) 分享页 HTML（通常为空壳，但保留）
        runCatching { DouyinWebViewParser.parse(html, resolvedUrl, link) }
            .getOrNull()
            ?.let { return it }

        // 3) 基础元数据 + 交给 WebView
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

    private fun headers(): Map<String, String> = buildMap {
        put("User-Agent", HttpClients.MOBILE_UA)
        put("Referer", "https://www.douyin.com/")
        cookies.cookieFor(Platform.DOUYIN)?.let { put("Cookie", it) }
    }
}
