package com.weiji.wang.core.import.extractor

import com.weiji.wang.core.import.CookieStore
import com.weiji.wang.core.import.ExtractException
import com.weiji.wang.core.import.ExtractedContent
import com.weiji.wang.core.import.HttpClients
import com.weiji.wang.core.import.LinkParser
import com.weiji.wang.core.import.ParsedLink
import com.weiji.wang.core.import.PlatformExtractor
import com.weiji.wang.core.import.XhsWebViewParser
import com.weiji.wang.core.import.fetchText
import com.weiji.wang.core.import.resolveFinalUrl
import com.weiji.wang.core.model.FetchStatus
import com.weiji.wang.core.model.ImportState
import com.weiji.wang.core.model.Platform
import okhttp3.OkHttpClient

/**
 * 小红书：xhslink 短链 -> 带 xsec_token 的笔记页 -> __INITIAL_STATE__。
 * 2026-10-30 实测匿名可读（带 xsec_token）；风控时会返回空数据，需要用户登录。
 */
class XhsExtractor(
    private val http: OkHttpClient,
    private val cookies: CookieStore,
) : PlatformExtractor {

    override val platform = Platform.XIAOHONGSHU

    override suspend fun extract(link: ParsedLink): ExtractedContent {
        val finalUrl = if (link.url.contains("xhslink")) {
            http.resolveFinalUrl(link.url, headers())
        } else {
            link.url
        }
        val result = http.fetchText(finalUrl, headers())
        val parsed = XhsWebViewParser.parse(result.body, result.finalUrl, link)
        if (parsed == null) {
            val blocked = listOf("300031", "300015", "461", "登录", "安全验证").any { it in result.body }
            throw ExtractException(
                if (blocked) ImportState.NEEDS_LOGIN else ImportState.FAILED,
                if (blocked) "小红书要求登录或安全验证" else "没有从页面里找到笔记数据",
            )
        }
        return parsed.copy(
            canonicalUrl = result.finalUrl,
            title = parsed.title.ifBlank { link.titleHint ?: "小红书笔记" },
            author = parsed.author ?: link.authorHint,
            textBody = parsed.textBody.ifBlank { LinkParser.cleanShareNoise(link.shareText) },
        )
    }

    private fun headers(): Map<String, String> = buildMap {
        put("User-Agent", HttpClients.MOBILE_UA)
        put("Referer", "https://www.xiaohongshu.com/")
        cookies.cookieFor(Platform.XIAOHONGSHU)?.let { put("Cookie", it) }
    }
}
