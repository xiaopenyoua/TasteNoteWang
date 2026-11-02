package com.weiji.wang.core.storage

import android.content.Context
import android.net.Uri
import com.weiji.wang.core.data.RecipeRepository
import com.weiji.wang.core.data.db.IngredientEntity
import com.weiji.wang.core.data.db.MediaEntity
import com.weiji.wang.core.data.db.RecipeEntity
import com.weiji.wang.core.data.db.SourceEntity
import com.weiji.wang.core.data.db.StepEntity
import com.weiji.wang.core.data.db.WeijiDatabase
import com.weiji.wang.core.model.Ulid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class ExportResult(val recipes: Int, val bytes: Long)
data class RestoreResult(val imported: Int, val skipped: Int, val failed: Int)

class BackupManager(
    private val context: Context,
    private val db: WeijiDatabase,
    private val recipes: RecipeRepository,
    private val files: FileStore,
) {
    private val json = Json { prettyPrint = true; encodeDefaults = true; ignoreUnknownKeys = true }

    @Serializable
    private data class Manifest(
        val app: String = "weiji",
        val version: Int = 1,
        val exportedAt: Long,
        val recipeCount: Int,
        val includesMedia: Boolean,
        val recipes: List<ManifestRecipe>,
    )

    @Serializable
    private data class ManifestRecipe(
        val id: String,
        val title: String,
        val updatedAt: Long,
        val fileCount: Int,
    )

    suspend fun exportTo(uri: Uri, includeMedia: Boolean = true): ExportResult = withContext(Dispatchers.IO) {
        // 以数据库为准：避免把已删除/遗留的文件夹打进备份
        val recipesInDb = db.recipeDao().all()
        var bytes = 0L
        val entries = mutableListOf<Triple<File, String, ByteArray?>>()
        val manifestRecipes = mutableListOf<ManifestRecipe>()

        for (recipe in recipesInDb) {
            val details = db.recipeDao().details(recipe.id) ?: continue
            val snapshotJson = RecipeSnapshot.toJson(
                recipe = details.recipe,
                source = details.source,
                ingredients = details.ingredients,
                steps = details.steps,
                media = details.media,
            )
            entries += Triple(
                File(files.recipesDir, "${recipe.id}/${RecipeSnapshot.FILE_NAME}"),
                "recipes/${recipe.id}/${RecipeSnapshot.FILE_NAME}",
                snapshotJson.toByteArray(Charsets.UTF_8),
            )
            val markdown = RecipeSnapshot.parse(snapshotJson)
                ?.let { RecipeSnapshot.toMarkdown(it) }
                .orEmpty()
            entries += Triple(
                File(files.recipesDir, "${recipe.id}/recipe.md"),
                "recipes/${recipe.id}/recipe.md",
                markdown.toByteArray(Charsets.UTF_8),
            )
            val dir = files.recipeDir(recipe.id)
            val mediaFiles = dir.listFiles()
                ?.filter {
                    it.isFile && !it.name.endsWith(".part") &&
                        it.name != RecipeSnapshot.FILE_NAME && it.name != "recipe.md"
                }
                .orEmpty()
            if (includeMedia) {
                mediaFiles.forEach { file ->
                    entries += Triple(file, "recipes/${recipe.id}/media/${file.name}", null)
                }
            }
            manifestRecipes += ManifestRecipe(
                id = recipe.id,
                title = recipe.title,
                updatedAt = recipe.updatedAt,
                fileCount = mediaFiles.size,
            )
        }

        val manifest = Manifest(
            exportedAt = System.currentTimeMillis(),
            recipeCount = manifestRecipes.size,
            includesMedia = includeMedia,
            recipes = manifestRecipes,
        )
        val manifestBytes = json.encodeToString(manifest).toByteArray(Charsets.UTF_8)

        val output = context.contentResolver.openOutputStream(uri)
            ?: error("无法写入导出文件")
        output.use { out ->
            ZipOutputStream(out.buffered()).use { zip ->
                zip.putNextEntry(ZipEntry("manifest.json"))
                zip.write(manifestBytes)
                zip.closeEntry()
                entries.forEach { (file, path, bytesOverride) ->
                    zip.putNextEntry(ZipEntry(path))
                    if (bytesOverride != null) {
                        zip.write(bytesOverride)
                    } else {
                        file.inputStream().use { it.copyTo(zip) }
                    }
                    zip.closeEntry()
                }
            }
        }
        bytes = runCatching { context.contentResolver.openFileDescriptor(uri, "r")?.statSize }.getOrNull() ?: 0L
        ExportResult(manifestRecipes.size, bytes)
    }

    suspend fun restoreFrom(uri: Uri): RestoreResult = withContext(Dispatchers.IO) {
        val staging = File(files.cacheDir, "restore-${Ulid.next()}").apply { mkdirs() }
        var imported = 0
        var skipped = 0
        var failed = 0
        try {
            val input = context.contentResolver.openInputStream(uri) ?: error("无法读取备份")
            ZipInputStream(input.buffered()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val name = entry.name
                    if (name.startsWith("/") || name.contains("..")) {
                        zip.closeEntry(); entry = zip.nextEntry; continue
                    }
                    if (!entry.isDirectory) {
                        val target = File(staging, name)
                        val canonical = target.canonicalFile
                        if (!canonical.path.startsWith(staging.canonicalPath)) {
                            zip.closeEntry(); entry = zip.nextEntry; continue
                        }
                        canonical.parentFile?.mkdirs()
                        canonical.outputStream().use { zip.copyTo(it) }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
            val recipeDirs = File(staging, "recipes").listFiles()?.filter { it.isDirectory } ?: emptyList()
            for (dir in recipeDirs) {
                val snapshotFile = dir.resolve(RecipeSnapshot.FILE_NAME)
                val snapshot = snapshotFile.takeIf { it.exists() }
                    ?.let { runCatching { RecipeSnapshot.parse(it.readText()) }.getOrNull() }
                if (snapshot == null) {
                    failed += 1
                    continue
                }
                val existing = db.recipeDao().get(snapshot.recipe.id)
                if (existing != null && existing.updatedAt >= snapshot.recipe.updatedAt) {
                    skipped += 1
                    continue
                }
                runCatching {
                    val targetDir = files.recipeDir(snapshot.recipe.id)
                    targetDir.mkdirs()
                    val mediaDir = dir.resolve("media")
                    if (mediaDir.exists()) {
                        mediaDir.listFiles()?.forEach { file ->
                            val dest = targetDir.resolve(file.name)
                            file.copyTo(dest, overwrite = true)
                        }
                    }
                    snapshotFile.copyTo(targetDir.resolve(RecipeSnapshot.FILE_NAME), overwrite = true)

                    val recipe = RecipeEntity(
                        id = snapshot.recipe.id,
                        title = snapshot.recipe.title,
                        subtitle = snapshot.recipe.subtitle,
                        coverPath = snapshot.recipe.coverPath,
                        servings = snapshot.recipe.servings,
                        totalMinutes = snapshot.recipe.totalMinutes,
                        difficulty = snapshot.recipe.difficulty,
                        notes = snapshot.recipe.notes,
                        sourceId = snapshot.source?.let { Ulid.next() },
                        createdAt = snapshot.recipe.createdAt,
                        updatedAt = snapshot.recipe.updatedAt,
                        lastCookedAt = snapshot.recipe.lastCookedAt,
                        favorite = snapshot.recipe.favorite,
                    )
                    val source = snapshot.source?.let {
                        SourceEntity(
                            id = recipe.sourceId!!,
                            recipeId = recipe.id,
                            platform = it.platform,
                            originalUrl = it.originalUrl,
                            canonicalUrl = it.canonicalUrl,
                            platformContentId = it.platformContentId,
                            authorName = it.authorName,
                            authorId = it.authorId,
                            sourceTitle = it.sourceTitle,
                            textBody = it.textBody,
                            shareText = it.shareText,
                            importedAt = it.importedAt,
                            fetchStatus = it.fetchStatus,
                        )
                    }
                    val ingredients = snapshot.ingredients.map {
                        IngredientEntity(
                            id = Ulid.next(),
                            recipeId = recipe.id,
                            position = it.position,
                            groupName = it.groupName,
                            name = it.name,
                            amount = it.amount,
                            unit = it.unit,
                            rawText = it.rawText,
                            scalable = it.scalable,
                        )
                    }
                    val steps = snapshot.steps.map {
                        StepEntity(
                            id = Ulid.next(),
                            recipeId = recipe.id,
                            position = it.position,
                            text = it.text,
                            durationSeconds = it.durationSeconds,
                            mediaPath = it.mediaPath,
                            videoTimestampMs = it.videoTimestampMs,
                        )
                    }
                    val media = snapshot.media.map {
                        MediaEntity(
                            id = Ulid.next(),
                            recipeId = recipe.id,
                            kind = it.kind,
                            relativePath = it.relativePath,
                            mime = it.mime,
                            width = it.width,
                            height = it.height,
                            durationMs = it.durationMs,
                            sizeBytes = it.sizeBytes,
                            sha256 = it.sha256,
                            originUrl = it.originUrl,
                            watermarkFree = it.watermarkFree,
                            createdAt = System.currentTimeMillis(),
                        )
                    }
                    recipes.saveRecipe(recipe, ingredients, steps, source, media)
                    imported += 1
                }.onFailure { failed += 1 }
            }
        } finally {
            staging.deleteRecursively()
        }
        RestoreResult(imported, skipped, failed)
    }
}
