package com.weiji.wang.core.storage

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.weiji.wang.core.data.db.WeijiDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.util.zip.ZipInputStream

data class MigrationEstimate(
    val bytes: Long,
    val files: Int,
    val recipes: Int,
)

data class MigrationPreview(
    val fileName: String?,
    val sizeBytes: Long,
    val exportedAt: Long?,
    val recipeCount: Int,
    val mediaFiles: Int,
    val includesMedia: Boolean,
)

/**
 * 数据迁移：复用 [BackupManager] 的流式 zip 能力，补充数据估算与迁移包预览。
 * 迁移包不包含平台 Cookie；分卷与断点续传暂未实现。
 */
class MigrationManager(
    private val context: Context,
    private val db: WeijiDatabase,
    private val backup: BackupManager,
    private val files: FileStore,
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun estimate(): MigrationEstimate = withContext(Dispatchers.IO) {
        val entries = files.recipesDir.walkTopDown().filter { it.isFile }.toList()
        MigrationEstimate(
            bytes = entries.sumOf { it.length() },
            files = entries.size,
            recipes = db.recipeDao().all().size,
        )
    }

    suspend fun exportTo(uri: Uri, includeMedia: Boolean): ExportResult =
        backup.exportTo(uri, includeMedia)

    suspend fun restoreFrom(uri: Uri, replaceAll: Boolean = false): RestoreResult =
        backup.restoreFrom(uri, replaceAll)

    suspend fun preview(uri: Uri): MigrationPreview = withContext(Dispatchers.IO) {
        var manifestText: String? = null
        context.contentResolver.openInputStream(uri)?.use { input ->
            ZipInputStream(input.buffered()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (entry.name == MANIFEST) {
                        manifestText = zip.readBytes().toString(Charsets.UTF_8)
                        break
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        } ?: error("无法读取迁移包")

        val manifest = json.parseToJsonElement(
            manifestText ?: error("迁移包缺少 manifest.json"),
        ).jsonObject
        val recipes = manifest["recipes"]?.jsonArray
        MigrationPreview(
            fileName = displayName(uri),
            sizeBytes = querySize(uri),
            exportedAt = manifest["exportedAt"]?.jsonPrimitive?.longOrNull,
            recipeCount = manifest["recipeCount"]?.jsonPrimitive?.intOrNull ?: 0,
            mediaFiles = recipes?.sumOf {
                it.jsonObject["fileCount"]?.jsonPrimitive?.intOrNull ?: 0
            } ?: 0,
            includesMedia = manifest["includesMedia"]?.jsonPrimitive?.booleanOrNull ?: false,
        )
    }

    private fun displayName(uri: Uri): String? = runCatching {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) cursor.getString(index) else null
            } else {
                null
            }
        }
    }.getOrNull()

    private fun querySize(uri: Uri): Long = runCatching {
        context.contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: 0L
    }.getOrDefault(0L)

    private companion object {
        const val MANIFEST = "manifest.json"
    }
}
