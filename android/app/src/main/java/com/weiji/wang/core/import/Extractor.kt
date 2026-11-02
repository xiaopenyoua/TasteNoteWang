package com.weiji.wang.core.import

import com.weiji.wang.core.model.FetchStatus
import com.weiji.wang.core.model.MediaKind
import com.weiji.wang.core.model.Platform

data class RemoteMedia(
    val kind: MediaKind,
    val url: String,
    val mime: String,
    val width: Int? = null,
    val height: Int? = null,
    val durationMs: Long? = null,
    val sizeBytes: Long? = null,
    val watermarkFree: Boolean = true,
    val headers: Map<String, String> = emptyMap(),
    val suggestedName: String? = null,
)

data class ExtractedContent(
    val platform: Platform,
    val canonicalUrl: String,
    val contentId: String?,
    val title: String,
    val author: String?,
    val authorId: String? = null,
    val textBody: String = "",
    val tags: List<String> = emptyList(),
    val coverUrl: String? = null,
    val media: List<RemoteMedia> = emptyList(),
    val fetchStatus: FetchStatus = FetchStatus.OK,
    val message: String? = null,
    val durationMs: Long? = null,
    val requiresWebView: Boolean = false,
    val webViewUrl: String? = null,
)

interface PlatformExtractor {
    val platform: Platform
    suspend fun extract(link: ParsedLink): ExtractedContent
}

class ExtractException(
    val state: com.weiji.wang.core.model.ImportState,
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

interface WebViewContentParser {
    fun parse(html: String, url: String, link: ParsedLink): ExtractedContent?
}

object WebViewParsers {
    val parsers: Map<Platform, WebViewContentParser> = mapOf(
        Platform.DOUYIN to DouyinWebViewParser,
        Platform.XIAOHONGSHU to XhsWebViewParser,
    )
}
