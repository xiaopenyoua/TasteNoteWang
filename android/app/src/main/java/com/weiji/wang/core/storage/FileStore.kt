package com.weiji.wang.core.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.security.DigestInputStream
import java.security.MessageDigest

data class SavedFile(
    val relativePath: String,
    val sizeBytes: Long,
    val sha256: String,
    val width: Int? = null,
    val height: Int? = null,
)

/**
 * 设备上的唯一落盘入口。规则：
 * - 一条菜谱一个文件夹：recipes/<recipeId>/
 * - 媒体文件名固定、ASCII、可预测：cover.webp / step-01.webp / video.mp4
 * - 写入先落 .part，再原子 rename；崩溃最多留下 .part
 * - DB 只存相对路径，绝不存绝对路径
 */
class FileStore(private val context: Context) {

    val root: File = (context.getExternalFilesDir(null) ?: context.filesDir).resolve("weiji").apply { mkdirs() }
    val recipesDir: File = root.resolve("recipes").apply { mkdirs() }
    val exportsDir: File = root.resolve("exports").apply { mkdirs() }
    val cacheDir: File = context.cacheDir.resolve("import").apply { mkdirs() }

    fun recipeDir(recipeId: String): File = recipesDir.resolve(recipeId)

    fun ensureRecipeDir(recipeId: String): File = recipesDir.resolve(recipeId).apply { mkdirs() }

    fun resolve(relativePath: String): File = root.resolve(relativePath)

    fun exists(relativePath: String?): Boolean =
        !relativePath.isNullOrBlank() && resolve(relativePath).let { it.exists() && it.length() > 0 }

    fun relativeOf(file: File): String = file.relativeTo(root).invariantSeparatorsPath

    fun saveStream(
        recipeId: String,
        fileName: String,
        input: InputStream,
        mime: String,
        onProgress: ((Long) -> Unit)? = null,
    ): SavedFile {
        val dir = ensureRecipeDir(recipeId)
        val target = dir.resolve(fileName)
        val temp = dir.resolve("$fileName.part")
        val digest = MessageDigest.getInstance("SHA-256")
        var total = 0L
        DigestInputStream(input, digest).use { stream ->
            FileOutputStream(temp).use { out ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val read = stream.read(buffer)
                    if (read <= 0) break
                    out.write(buffer, 0, read)
                    total += read
                    onProgress?.invoke(total)
                }
                out.flush()
                out.fd.sync()
            }
        }
        if (target.exists()) target.delete()
        if (!temp.renameTo(target)) {
            temp.copyTo(target, overwrite = true)
            temp.delete()
        }
        val sha = digest.digest().joinToString("") { "%02x".format(it) }
        return SavedFile(relativeOf(target), total, sha)
    }

    fun saveImage(
        recipeId: String,
        fileName: String,
        input: InputStream,
        mime: String,
        maxEdge: Int = 1600,
        quality: Int = 85,
    ): SavedFile {
        val dir = ensureRecipeDir(recipeId)
        val rawTemp = dir.resolve("$fileName.raw")
        val target = dir.resolve(fileName)
        val digest = MessageDigest.getInstance("SHA-256")
        var total = 0L
        DigestInputStream(input, digest).use { stream ->
            FileOutputStream(rawTemp).use { out ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val read = stream.read(buffer)
                    if (read <= 0) break
                    out.write(buffer, 0, read)
                    total += read
                }
            }
        }

        var width: Int? = null
        var height: Int? = null
        try {
            if (mime == "image/gif") {
                if (target.exists()) target.delete()
                rawTemp.renameTo(target)
            } else {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(rawTemp.absolutePath, bounds)
                val sample = sampleSize(bounds.outWidth, bounds.outHeight, maxEdge)
                val opts = BitmapFactory.Options().apply { inSampleSize = sample }
                var bitmap = BitmapFactory.decodeFile(rawTemp.absolutePath, opts)
                if (bitmap == null) {
                    if (target.exists()) target.delete()
                    rawTemp.copyTo(target, overwrite = true)
                } else {
                    bitmap = applyExifRotation(rawTemp, bitmap)
                    bitmap = scaleDown(bitmap, maxEdge)
                    width = bitmap.width
                    height = bitmap.height
                    FileOutputStream(target).use { out ->
                        val format = if (Build.VERSION.SDK_INT >= 30) {
                            Bitmap.CompressFormat.WEBP_LOSSY
                        } else {
                            @Suppress("DEPRECATION")
                            Bitmap.CompressFormat.WEBP
                        }
                        bitmap.compress(format, quality, out)
                        out.fd.sync()
                    }
                    bitmap.recycle()
                }
                rawTemp.delete()
            }
        } catch (t: Throwable) {
            if (target.exists().not() && rawTemp.exists()) {
                rawTemp.copyTo(target, overwrite = true)
            }
            rawTemp.delete()
        }

        val sha = digest.digest().joinToString("") { "%02x".format(it) }
        return SavedFile(
            relativePath = relativeOf(target),
            sizeBytes = target.length(),
            sha256 = sha,
            width = width ?: totalWidthFallback(target),
            height = height ?: totalHeightFallback(target),
        )
    }

    fun register(recipeId: String, fileName: String): SavedFile {
        val file = ensureRecipeDir(recipeId).resolve(fileName)
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { stream ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = stream.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        val bounds = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
            BitmapFactory.decodeFile(file.absolutePath, this)
        }
        return SavedFile(
            relativePath = relativeOf(file),
            sizeBytes = file.length(),
            sha256 = digest.digest().joinToString("") { "%02x".format(it) },
            width = bounds.outWidth.takeIf { it > 0 },
            height = bounds.outHeight.takeIf { it > 0 },
        )
    }

    fun saveBitmap(recipeId: String, fileName: String, bitmap: Bitmap, quality: Int = 85): SavedFile {
        val dir = ensureRecipeDir(recipeId)
        val target = dir.resolve(fileName)
        val temp = dir.resolve("$fileName.part")
        FileOutputStream(temp).use { out ->
            val format = if (Build.VERSION.SDK_INT >= 30) {
                Bitmap.CompressFormat.WEBP_LOSSY
            } else {
                @Suppress("DEPRECATION")
                Bitmap.CompressFormat.WEBP
            }
            bitmap.compress(format, quality, out)
            out.flush()
            out.fd.sync()
        }
        if (target.exists()) target.delete()
        if (!temp.renameTo(target)) {
            temp.copyTo(target, overwrite = true)
            temp.delete()
        }
        val digest = MessageDigest.getInstance("SHA-256")
        target.inputStream().use { stream ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = stream.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        return SavedFile(
            relativePath = relativeOf(target),
            sizeBytes = target.length(),
            sha256 = digest.digest().joinToString("") { "%02x".format(it) },
            width = bitmap.width,
            height = bitmap.height,
        )
    }

    fun writeTextAtomic(recipeId: String, fileName: String, text: String): File {
        val dir = ensureRecipeDir(recipeId)
        val target = dir.resolve(fileName)
        val temp = dir.resolve("$fileName.part")
        FileOutputStream(temp).use { out ->
            out.write(text.toByteArray(Charsets.UTF_8))
            out.flush()
            out.fd.sync()
        }
        if (target.exists()) target.delete()
        if (!temp.renameTo(target)) {
            temp.copyTo(target, overwrite = true)
            temp.delete()
        }
        return target
    }

    fun copyFromUri(recipeId: String, fileName: String, uri: Uri): SavedFile {
        val input = context.contentResolver.openInputStream(uri)
            ?: error("无法读取分享的文件")
        val mime = MimeTypes.of(context, uri)
        return input.use {
            if (mime.startsWith("image/")) {
                saveImage(recipeId, fileName, it, mime)
            } else {
                saveStream(recipeId, fileName, it, mime)
            }
        }
    }

    fun deleteRecipe(recipeId: String) {
        recipeDir(recipeId).deleteRecursively()
    }

    fun deleteFile(relativePath: String?) {
        if (relativePath.isNullOrBlank()) return
        runCatching { resolve(relativePath).delete() }
    }

    fun recipeBytes(recipeId: String): Long = recipeDir(recipeId).walkTopDown()
        .filter { it.isFile }.sumOf { it.length() }

    fun totalBytes(): Long = root.walkTopDown().filter { it.isFile }.sumOf { it.length() }

    fun freeBytes(): Long {
        val stat = android.os.StatFs(root.absolutePath)
        return stat.availableBlocksLong * stat.blockSizeLong
    }

    fun cleanTempFiles(olderThanMs: Long = 24L * 60 * 60 * 1000) {
        val cutoff = System.currentTimeMillis() - olderThanMs
        root.walkTopDown()
            .filter { it.isFile && it.name.endsWith(".part") && it.lastModified() < cutoff }
            .forEach { runCatching { it.delete() } }
        cacheDir.listFiles()?.forEach { runCatching { it.deleteRecursively() } }
        // ShareReceiverActivity 复制到 cacheDir 根目录的分享临时文件
        context.cacheDir.listFiles()
            ?.filter { it.isFile && it.name.startsWith("share-") && it.lastModified() < cutoff }
            ?.forEach { runCatching { it.delete() } }
    }

    private fun sampleSize(width: Int, height: Int, maxEdge: Int): Int {
        var sample = 1
        var w = width
        var h = height
        while (w / 2 >= maxEdge || h / 2 >= maxEdge) {
            sample *= 2
            w /= 2
            h /= 2
        }
        return sample
    }

    private fun scaleDown(bitmap: Bitmap, maxEdge: Int): Bitmap {
        val max = maxOf(bitmap.width, bitmap.height)
        if (max <= maxEdge) return bitmap
        val ratio = maxEdge.toFloat() / max
        val scaled = Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * ratio).toInt().coerceAtLeast(1),
            (bitmap.height * ratio).toInt().coerceAtLeast(1),
            true,
        )
        if (scaled !== bitmap) bitmap.recycle()
        return scaled
    }

    private fun applyExifRotation(file: File, bitmap: Bitmap): Bitmap {
        return try {
            val exif = ExifInterface(file.absolutePath)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )
            val degrees = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
            if (degrees == 0f) bitmap else {
                val matrix = Matrix().apply { postRotate(degrees) }
                val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                if (rotated !== bitmap) bitmap.recycle()
                rotated
            }
        } catch (t: Throwable) {
            bitmap
        }
    }

    private fun totalWidthFallback(file: File): Int? = runCatching {
        BitmapFactory.Options().apply { inJustDecodeBounds = true; BitmapFactory.decodeFile(file.absolutePath, this) }.outWidth
    }.getOrNull()?.takeIf { it > 0 }

    private fun totalHeightFallback(file: File): Int? = runCatching {
        BitmapFactory.Options().apply { inJustDecodeBounds = true; BitmapFactory.decodeFile(file.absolutePath, this) }.outHeight
    }.getOrNull()?.takeIf { it > 0 }

    companion object {
        fun nameForCover(): String = "cover.webp"
        fun nameForStep(index: Int): String = "step-%02d.webp".format(index)
        fun nameForPhoto(index: Int): String = "photo-%02d.webp".format(index)
        fun nameForVideo(index: Int = 1): String = if (index <= 1) "video.mp4" else "video-%02d.mp4".format(index)
    }
}
