package com.weiji.wang.core.import

import com.weiji.wang.core.model.Platform

data class ParsedLink(
    val platform: Platform,
    val url: String,
    val contentId: String?,
    val shareText: String,
    val titleHint: String?,
    val authorHint: String?,
)

/**
 * 只做本地字符串处理：不发请求、不展开短链。
 * 短链展开在提取器里做，失败不影响这里的结果。
 */
object LinkParser {

    private val urlRegex = Regex("""https?://[^\s，。！？；：、）】》」』"']+""")

    private val senderPrefix = Regex("""^[^:：\n]{1,16}[:：]\s*""")

    fun extractUrls(text: String): List<String> =
        urlRegex.findAll(text).map { it.value.trimEnd('.', ',', ';', '，', '。', '；') }.toList()

    fun parse(text: String, forcedUrl: String? = null): ParsedLink? {
        val url = forcedUrl ?: extractUrls(text).firstOrNull() ?: return null
        val platform = detect(url)
        val contentId = extractContentId(platform, url)
        return ParsedLink(
            platform = platform,
            url = url,
            contentId = contentId,
            shareText = text.trim(),
            titleHint = titleHint(platform, text),
            authorHint = authorHint(platform, text),
        )
    }

    fun detect(url: String): Platform {
        val host = runCatching { java.net.URI(url).host?.lowercase() }.getOrNull() ?: url.lowercase()
        return when {
            "douyin.com" in host || "iesdouyin.com" in host -> Platform.DOUYIN
            "xiaohongshu.com" in host || "xhslink.com" in host || "xhslink.cn" in host -> Platform.XIAOHONGSHU
            "bilibili.com" in host || "b23.tv" in host -> Platform.BILIBILI
            else -> Platform.OTHER
        }
    }

    fun extractContentId(platform: Platform, url: String): String? = when (platform) {
        Platform.DOUYIN ->
            Regex("""/(?:share/)?(?:video|note|slides)/(\d{15,25})""").find(url)?.groupValues?.get(1)
                ?: Regex("""[?&]modal_id=(\d{15,25})""").find(url)?.groupValues?.get(1)
        Platform.XIAOHONGSHU ->
            Regex("""/(?:explore|discovery/item|item)/([0-9a-f]{24})""").find(url)?.groupValues?.get(1)
        Platform.BILIBILI ->
            Regex("""(BV[0-9A-Za-z]{10})""").find(url)?.groupValues?.get(1)
                ?: Regex("""/video/av(\d+)""", RegexOption.IGNORE_CASE).find(url)?.groupValues?.get(1)?.let { "av$it" }
        Platform.OTHER -> null
    }

    fun authorHint(platform: Platform, text: String): String? = when (platform) {
        Platform.DOUYIN ->
            Regex("""【([^】]{1,30}?)的(?:图文)?作品】""").find(text)?.groupValues?.get(1)?.trim()
        Platform.XIAOHONGSHU -> null
        Platform.BILIBILI ->
            Regex("""【([^】]{1,40}?)[-_]哔哩哔哩】""").find(text)?.groupValues?.get(1)?.trim()
        Platform.OTHER -> null
    }

    fun titleHint(platform: Platform, text: String): String? {
        val cleaned = cleanShareNoise(text)
        val byPlatform = when (platform) {
            Platform.DOUYIN -> {
                val afterBracket = cleaned.substringAfter('】', "").trim()
                afterBracket.substringBefore("食材：").substringBefore("食材:").trim().ifBlank { null }
            }
            Platform.XIAOHONGSHU -> cleaned.substringBefore('【').trim()
            Platform.BILIBILI -> {
                Regex("""【([^】]{1,80}?)[-_]哔哩哔哩】""").find(cleaned)?.groupValues?.get(1)?.trim()
            }
            Platform.OTHER -> null
        }
        return byPlatform
            ?.trim(' ', '\n', '-', '|', '：', ':')
            ?.takeIf { it.length >= 2 }
            ?.take(80)
    }

    /** 去掉发送者前缀、URL、平台尾巴和口令碎片，剩下可读文本。 */
    fun cleanShareNoise(text: String): String {
        var s = text.trim()
        // 先删 URL，再删“王强:”这类前缀；否则 https: 会被误认成发送者前缀
        s = urlRegex.replace(s, " ")
        s = senderPrefix.replace(s, "")
        s = s.replace(Regex("""【小红书】[^。\n]*"""), " ")
        s = s.replace(Regex("""\b\d{1,2}[:：]\d{2}\b"""), " ")
        s = s.replace(Regex("""\b\d{1,2}/\d{1,2}\b"""), " ")
        s = s.replace(Regex("""[A-Za-z0-9]{1,4}[:：]?/[A-Za-z0-9.@]{2,}"""), " ")
        s = s.replace(Regex("""\s{2,}"""), " ")
        return s.trim()
    }
}
