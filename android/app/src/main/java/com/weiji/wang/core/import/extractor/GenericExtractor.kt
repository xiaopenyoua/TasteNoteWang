package com.weiji.wang.core.import.extractor

import com.weiji.wang.core.import.ExtractedContent
import com.weiji.wang.core.import.LinkParser
import com.weiji.wang.core.import.ParsedLink
import com.weiji.wang.core.import.PlatformExtractor
import com.weiji.wang.core.model.FetchStatus
import com.weiji.wang.core.model.Platform

class GenericExtractor : PlatformExtractor {
    override val platform = Platform.OTHER

    override suspend fun extract(link: ParsedLink): ExtractedContent = ExtractedContent(
        platform = Platform.OTHER,
        canonicalUrl = link.url,
        contentId = null,
        title = link.titleHint ?: "未命名菜谱",
        author = link.authorHint,
        textBody = LinkParser.cleanShareNoise(link.shareText),
        media = emptyList(),
        fetchStatus = FetchStatus.MANUAL,
        message = "这个链接暂不支持自动解析，已保留文字与链接",
    )
}
