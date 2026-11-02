package com.weiji.wang.core.storage

import android.content.Context
import android.net.Uri

/**
 * 分享进来的 Uri 可能是 content://（系统分享）或 file://（我们复制到缓存的临时文件）。
 * file:// 的 ContentResolver.getType() 常返回 null，需要按扩展名兜底。
 */
object MimeTypes {
    fun of(context: Context, uri: Uri): String {
        context.contentResolver.getType(uri)?.takeIf { it.isNotBlank() }?.let { return it }
        val path = (uri.lastPathSegment ?: uri.path ?: "").lowercase()
        return when {
            path.endsWith(".jpg") || path.endsWith(".jpeg") -> "image/jpeg"
            path.endsWith(".png") -> "image/png"
            path.endsWith(".webp") -> "image/webp"
            path.endsWith(".gif") -> "image/gif"
            path.endsWith(".heic") || path.endsWith(".heif") -> "image/heic"
            path.endsWith(".mp4") -> "video/mp4"
            path.endsWith(".mov") -> "video/quicktime"
            path.endsWith(".mkv") -> "video/x-matroska"
            path.endsWith(".3gp") -> "video/3gpp"
            path.endsWith(".m4v") -> "video/x-m4v"
            else -> "application/octet-stream"
        }
    }
}
