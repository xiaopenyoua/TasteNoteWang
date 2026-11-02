package com.weiji.wang.core.import.extractor

import com.weiji.wang.core.import.CookieStore
import com.weiji.wang.core.import.PlatformExtractor
import com.weiji.wang.core.model.Platform
import okhttp3.OkHttpClient

class ExtractorRegistry(
    http: OkHttpClient,
    cookies: CookieStore,
) {
    private val extractors: Map<Platform, PlatformExtractor> = listOf(
        DouyinExtractor(http, cookies),
        XhsExtractor(http, cookies),
        BilibiliExtractor(http),
        GenericExtractor(),
    ).associateBy { it.platform }

    fun forPlatform(platform: Platform): PlatformExtractor =
        extractors[platform] ?: extractors.getValue(Platform.OTHER)

    fun all(): Collection<PlatformExtractor> = extractors.values
}
