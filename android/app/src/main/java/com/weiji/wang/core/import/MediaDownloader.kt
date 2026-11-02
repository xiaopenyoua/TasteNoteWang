package com.weiji.wang.core.import

import com.weiji.wang.core.storage.FileStore
import com.weiji.wang.core.storage.SavedFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.FileOutputStream

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

        val requestBuilder = Request.Builder()
            .url(media.url)
            .header("User-Agent", HttpClients.MOBILE_UA)
            .header("Accept", "*/*")
        media.headers.forEach { (k, v) -> requestBuilder.header(k, v) }
        if (existing > 0) requestBuilder.header("Range", "bytes=$existing-")

        try {
            http.newCall(requestBuilder.build()).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext DownloadOutcome.Failed("HTTP ${response.code}")
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

    sealed interface DownloadOutcome {
        data class Done(val file: SavedFile) : DownloadOutcome
        data class Skipped(val reason: String) : DownloadOutcome
        data class Failed(val reason: String) : DownloadOutcome
    }
}
