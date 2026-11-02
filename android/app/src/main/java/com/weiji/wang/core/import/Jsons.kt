package com.weiji.wang.core.import

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

object Jsons {
    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        allowSpecialFloatingPointValues = true
    }

    fun parse(text: String): JsonElement? = runCatching { json.parseToJsonElement(text) }.getOrNull()

    /** XHS 的 __INITIAL_STATE__ 里有裸 undefined，不是合法 JSON；只在字符串外替换。 */
    fun sanitizeJs(raw: String): String {
        val sb = StringBuilder(raw.length + 16)
        var i = 0
        var inString = false
        var quote = '"'
        while (i < raw.length) {
            val c = raw[i]
            if (inString) {
                sb.append(c)
                if (c == '\\' && i + 1 < raw.length) {
                    sb.append(raw[i + 1])
                    i += 2
                    continue
                }
                if (c == quote) inString = false
                i++
                continue
            }
            when {
                c == '"' || c == '\'' -> {
                    inString = true
                    quote = c
                    sb.append(c)
                    i++
                }
                raw.startsWith("undefined", i) -> {
                    sb.append("null")
                    i += 9
                }
                else -> {
                    sb.append(c)
                    i++
                }
            }
        }
        return sb.toString()
    }

    /** 从 HTML 中抽出名为 __INITIAL_STATE__ / _ROUTER_DATA / RENDER_DATA 的脚本 JSON。 */
    fun embeddedJson(html: String, vararg marker: String): JsonElement? {
        for (name in marker) {
            val patterns = listOf(
                Regex(
                    """(?:window\.)?${Regex.escape(name)}\s*=\s*(\{.*?\})\s*;?\s*</script>""",
                    setOf(RegexOption.DOT_MATCHES_ALL),
                ),
                Regex(
                    """<script[^>]*id=["']${Regex.escape(name)}["'][^>]*>(.*?)</script>""",
                    setOf(RegexOption.DOT_MATCHES_ALL),
                ),
            )
            for (pattern in patterns) {
                val match = pattern.find(html) ?: continue
                val raw = match.groupValues[1].trim()
                parse(sanitizeJs(raw))?.let { return it }
                val decoded = runCatching { java.net.URLDecoder.decode(raw, "UTF-8") }.getOrDefault(raw)
                parse(sanitizeJs(decoded))?.let { return it }
            }
        }
        return null
    }

    fun firstUrl(text: String?): String? =
        text?.trim()?.takeIf { it.startsWith("http") }?.replaceFirst("http://", "https://")
}

// ---- JSON 访问器：顶层扩展函数，便于在所有解析器里直接使用 ----

fun JsonElement?.field(key: String): JsonElement? = (this as? JsonObject)?.get(key)

fun JsonElement?.path(vararg keys: String): JsonElement? {
    var current: JsonElement? = this
    for (key in keys) {
        current = current.field(key) ?: return null
    }
    return current
}

fun JsonElement?.str(key: String): String? = field(key).asString()

fun JsonElement?.asString(): String? {
    val primitive = this as? JsonPrimitive ?: return null
    return primitive.contentOrNull ?: primitive.toString()
}

fun JsonElement?.long(key: String): Long? = field(key).asString()?.trim()?.toLongOrNull()

fun JsonElement?.int(key: String): Int? = field(key).asString()?.trim()?.toIntOrNull()

fun JsonElement?.bool(key: String): Boolean? = field(key).asString()?.trim()?.toBooleanStrictOrNull()

fun JsonElement?.objects(key: String): List<JsonObject> =
    (field(key) as? JsonArray)?.mapNotNull { it as? JsonObject } ?: emptyList()

fun JsonElement?.strings(key: String): List<String> =
    (field(key) as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull } ?: emptyList()

fun JsonElement?.obj(): JsonObject? = this as? JsonObject

fun JsonElement?.arr(): JsonArray? = this as? JsonArray
