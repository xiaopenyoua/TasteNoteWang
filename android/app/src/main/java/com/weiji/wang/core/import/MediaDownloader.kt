package com.weiji.wang.core.import

import com.weiji.wang.core.model.MediaKind
import com.weiji.wang.core.storage.FileStore
import com.weiji.wang.core.storage.SavedFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.FileOutputStream
import java.util.TreeMap

class MediaDownloader(
    private val http: OkHttpClient,
    private val files: FileStore,
) {
    /**
     * 单条媒体下载：.part + Range 续传 + 原子 rename。
     * onProgress 传 0-100；-1 表示总长度未知。
     */
    suspend fun download(
        recipeId: String,
        media: RemoteMedia,
        fileName: String,
        requiredBytes: Long? = null,
        onProgress: ((Int) -> Unit)? = null,
    ): DownloadOutcome = withContext(Dispatchers.IO) {
        if (media.url.isBlank()) return@withContext DownloadOutcome.Skipped("没有下载地址")
        val need = requiredBytes ?: media.sizeBytes ?: 20L * 1024 * 1024
        if (files.freeBytes() < need + 100L * 1024 * 1024) {
            return@withContext DownloadOutcome.Skipped("存储空间不足")
        }

        val dir = files.ensureRecipeDir(recipeId)
        val target = dir.resolve(fileName)
        val part = dir.resolve("$fileName.part")
        var existing = if (part.exists()) part.length() else 0L
        if (target.exists() && target.length() > 0 && !part.exists()) {
            return@withContext DownloadOutcome.Done(files.register(recipeId, fileName))
        }

        val requestBuilder = Request.Builder().url(media.url)
        requestHeaders(media).forEach { (key, value) -> requestBuilder.header(key, value) }
        if (existing > 0) requestBuilder.header("Range", "bytes=$existing-")

        try {
            http.newCall(requestBuilder.build()).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext DownloadOutcome.Failed(failureReason(response.code))
                }
                val contentType = response.body?.contentType()?.toString().orEmpty()
                if (media.kind == MediaKind.VIDEO && contentType.contains("text/html", ignoreCase = true)) {
                    // 直链失效时 CDN 会回一个验证页/登录页，200 也可能不是视频
                    return@withContext DownloadOutcome.Failed("取到的是网页而不是视频（直链可能已过期，重新解析一次）")
                }
                val append = response.code == 206 && existing > 0
                if (!append) {
                    existing = 0
                    part.delete()
                }
                val body = response.body ?: return@withContext DownloadOutcome.Failed("响应为空")
                val total = body.contentLength().takeIf { it > 0 }?.plus(existing)
                    ?: media.sizeBytes
                    ?: -1L
                var downloaded = existing
                FileOutputStream(part, append).use { out ->
                    body.byteStream().use { input ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            val read = input.read(buffer)
                            if (read <= 0) break
                            out.write(buffer, 0, read)
                            downloaded += read
                            onProgress?.invoke(if (total > 0) ((downloaded * 100) / total).toInt() else -1)
                        }
                    }
                    out.flush()
                    out.fd.sync()
                }
                if (downloaded <= 0L) {
                    part.delete()
                    return@withContext DownloadOutcome.Failed("响应为空（0 字节，可能被风控拦截）")
                }
            }
            if (target.exists()) target.delete()
            if (!part.renameTo(target)) {
                part.copyTo(target, overwrite = true)
                part.delete()
            }
            DownloadOutcome.Done(files.register(recipeId, fileName))
        } catch (t: Throwable) {
            DownloadOutcome.Failed(t.message ?: "下载失败")
        }
    }

    /**
     * 默认头 + `RemoteMedia.headers` 覆盖。
     *
     * 抖音 CDN 需要 `Referer`/`Origin`（缺了返回 403），视频请求统一补上；
     * `RemoteMedia.headers` 优先，B 站/小红书的平台头不会被抖音默认头污染。
     */
    private fun requestHeaders(media: RemoteMedia): Map<String, String> {
        val merged = TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
        merged["User-Agent"] = HttpClients.MOBILE_UA
        merged["Accept"] = "*/*"
        if (media.kind == MediaKind.VIDEO && isDouyinCdn(media.url)) {
            merged["Referer"] = "https://www.douyin.com/"
            merged["Origin"] = "https://www.douyin.com"
        }
        media.headers.forEach { (key, value) -> merged[key] = value }
        return merged
    }

    private fun isDouyinCdn(url: String): Boolean =
        listOf("douyinvod", "byteicdn", "byteimg", "douyin", "iesdouyin", "amemv")
            .any { url.contains(it, ignoreCase = true) }

    /** 失败原因要能指导下一步动作，不能只报一个数字。 */
    private fun failureReason(code: Int): String = when (code) {
        403 -> "HTTP 403（CDN 拒绝下载，直链可能已过期，重新解析一次）"
        404 -> "HTTP 404（直链已失效，重新解析一次）"
        416 -> "HTTP 416（续传区间无效，删除缓存后重试）"
        in 500..599 -> "HTTP $code（CDN 暂时不可用，稍后重试）"
        else -> "HTTP $code"
    }

    sealed interface DownloadOutcome {
        data class Done(val file: SavedFile) : DownloadOutcome
        data class Skipped(val reason: String) : DownloadOutcome
        data class Failed(val reason: String) : DownloadOutcome
    }
}
