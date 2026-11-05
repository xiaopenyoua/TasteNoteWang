package com.weiji.wang.core.update

import com.weiji.wang.core.import.HttpClients
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.security.MessageDigest

data class UpdateInfo(
    val version: String,
    val tag: String,
    val notes: String,
    val pageUrl: String,
    val apkUrl: String,
    val apkSize: Long,
    val sha256: String?,
)

/**
 * 在线更新：读 GitHub 的 latest release，下载 APK 并交给系统安装器。
 *
 * - 只认 `releases/latest`（草稿与预发布不会出现在这里）；
 * - APK 取资产里第一个 `.apk`，优先文件名带 `weiji` 的；
 * - 有 `digest`（`sha256:…`）就校验，没有就只校验大小。
 */
class UpdateChecker(
    private val http: OkHttpClient = HttpClients.create(),
    private val repo: String = DEFAULT_REPO,
) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun latest(): UpdateInfo? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://api.github.com/repos/$repo/releases/latest")
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "Weiji-Android")
            .build()
        http.newCall(request).execute().use { response ->
            if (response.code == 404) return@withContext null
            if (!response.isSuccessful) {
                error("检查更新失败（GitHub HTTP ${response.code}）")
            }
            parseRelease(response.body?.string().orEmpty())
        }
    }

    /** 单独拆出来，方便用真实响应做单测。 */
    fun parseRelease(body: String): UpdateInfo? {
        if (body.isBlank()) return null
        val root = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull() ?: return null
        val tag = root.string("tag_name")?.trim().orEmpty()
        val version = normalizeVersion(tag)
        if (version.isBlank()) return null
        val assets = runCatching { root["assets"]?.jsonArray }.getOrNull().orEmpty()
        val apk = assets.asSequence()
            .mapNotNull { runCatching { it.jsonObject }.getOrNull() }
            .filter { it.string("name")?.endsWith(".apk", ignoreCase = true) == true }
            .sortedByDescending { it.string("name")?.contains("weiji", ignoreCase = true) == true }
            .firstOrNull() ?: return null
        val url = apk.string("browser_download_url")?.takeIf { it.startsWith("https://") } ?: return null
        return UpdateInfo(
            version = version,
            tag = tag,
            notes = root.string("body").orEmpty().trim().take(MAX_NOTES),
            pageUrl = root.string("html_url").orEmpty(),
            apkUrl = url,
            apkSize = apk.string("size")?.toLongOrNull() ?: 0L,
            sha256 = apk.string("digest")
                ?.removePrefix("sha256:")
                ?.takeIf { it.length == 64 },
        )
    }

    suspend fun download(
        info: UpdateInfo,
        directory: File,
        onProgress: (Int) -> Unit,
    ): File = withContext(Dispatchers.IO) {
        directory.mkdirs()
        val target = File(directory, "weiji-${info.version}.apk")
        val part = File(directory, "${target.name}.part")
        val request = Request.Builder()
            .url(info.apkUrl)
            .header("Accept", "application/octet-stream")
            .header("User-Agent", "Weiji-Android")
            .build()
        val digest = MessageDigest.getInstance("SHA-256")
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("下载失败（GitHub HTTP ${response.code}）")
            val body = response.body ?: error("下载失败：响应为空")
            val total = body.contentLength().takeIf { it > 0 } ?: info.apkSize
            body.byteStream().use { input ->
                part.outputStream().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var read = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count <= 0) break
                        output.write(buffer, 0, count)
                        digest.update(buffer, 0, count)
                        read += count
                        if (total > 0) {
                            onProgress(((read * 100) / total).toInt().coerceIn(0, 100))
                        }
                    }
                    output.fd.sync()
                }
            }
        }
        if (info.apkSize > 0 && part.length() != info.apkSize) {
            part.delete()
            error("下载不完整：${part.length()} / ${info.apkSize} 字节")
        }
        val actual = digest.digest().joinToString("") { "%02x".format(it) }
        if (info.sha256 != null && !actual.equals(info.sha256, ignoreCase = true)) {
            part.delete()
            error("APK 校验失败，请重试")
        }
        if (target.exists()) target.delete()
        if (!part.renameTo(target)) {
            part.copyTo(target, overwrite = true)
            part.delete()
        }
        target
    }

    companion object {
        const val DEFAULT_REPO = "xiaopenyoua/TasteNoteWang"
        private const val MAX_NOTES = 600

        /** `v1.0.0` / `1.0.0` / `1.0.0-debug` 都归一成 `1.0.0`。 */
        fun normalizeVersion(raw: String): String =
            raw.trim().removePrefix("v").substringBefore('-').substringBefore('+')

        fun isNewer(latest: String, current: String): Boolean =
            compareVersions(latest, current) > 0

        fun compareVersions(left: String, right: String): Int {
            val a = numbers(left)
            val b = numbers(right)
            for (index in 0 until maxOf(a.size, b.size, 3)) {
                val diff = (a.getOrElse(index) { 0 }) - (b.getOrElse(index) { 0 })
                if (diff != 0) return diff
            }
            // 数字相同时，正式版高于同号预发布版。
            val aPre = left.contains('-')
            val bPre = right.contains('-')
            return when {
                aPre == bPre -> 0
                aPre -> -1
                else -> 1
            }
        }

        private fun numbers(version: String): List<Int> =
            normalizeVersion(version).split('.').map { part ->
                part.takeWhile { it.isDigit() }.toIntOrNull() ?: 0
            }
    }
}

private fun JsonObject.string(key: String): String? =
    runCatching { this[key]?.jsonPrimitive?.content }.getOrNull()
