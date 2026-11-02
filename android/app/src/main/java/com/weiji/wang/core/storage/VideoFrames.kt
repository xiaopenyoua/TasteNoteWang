package com.weiji.wang.core.storage

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import java.io.File

object VideoFrames {
    fun extract(file: File, atMs: Long = 1000L, maxWidth: Int = 1080): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            val frame = if (android.os.Build.VERSION.SDK_INT >= 27) {
                retriever.getScaledFrameAtTime(
                    atMs * 1000,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                    maxWidth,
                    (maxWidth * 9 / 16).coerceAtLeast(1),
                )
            } else {
                @Suppress("DEPRECATION")
                retriever.getFrameAtTime(atMs * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            }
            frame
        } catch (t: Throwable) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    fun durationMs(file: File): Long? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
        } catch (t: Throwable) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }
}
