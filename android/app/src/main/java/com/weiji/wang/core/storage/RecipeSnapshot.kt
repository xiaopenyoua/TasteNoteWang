package com.weiji.wang.core.storage

import com.weiji.wang.core.data.db.IngredientEntity
import com.weiji.wang.core.data.db.MediaEntity
import com.weiji.wang.core.data.db.RecipeEntity
import com.weiji.wang.core.data.db.SourceEntity
import com.weiji.wang.core.data.db.StepEntity
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * 每条菜谱目录里的 recipe.json：可读的、与数据库解耦的快照。
 * 它不参与运行时读取，只用于导出、备份与未来迁移；DB 仍是唯一事实源。
 */
object RecipeSnapshot {

    const val FILE_NAME = "recipe.json"
    const val VERSION = 1

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    @Serializable
    data class Snapshot(
        val version: Int = VERSION,
        val exportedAt: Long,
        val recipe: RecipeDto,
        val source: SourceDto? = null,
        val ingredients: List<IngredientDto> = emptyList(),
        val steps: List<StepDto> = emptyList(),
        val media: List<MediaDto> = emptyList(),
    )

    @Serializable
    data class RecipeDto(
        val id: String,
        val title: String,
        val subtitle: String = "",
        val coverPath: String? = null,
        val servings: Int = 2,
        val totalMinutes: Int? = null,
        val difficulty: Int = 1,
        val notes: String = "",
        val createdAt: Long,
        val updatedAt: Long,
        val lastCookedAt: Long? = null,
        val favorite: Boolean = false,
    )

    @Serializable
    data class SourceDto(
        val platform: String,
        val originalUrl: String,
        val canonicalUrl: String? = null,
        val platformContentId: String? = null,
        val authorName: String? = null,
        val authorId: String? = null,
        val sourceTitle: String? = null,
        val textBody: String? = null,
        val shareText: String? = null,
        val importedAt: Long,
        val fetchStatus: String = "MANUAL",
    )

    @Serializable
    data class IngredientDto(
        val position: Int,
        val groupName: String? = null,
        val name: String,
        val amount: Double? = null,
        val unit: String? = null,
        val rawText: String,
        val scalable: Boolean = true,
    )

    @Serializable
    data class StepDto(
        val position: Int,
        val text: String,
        val durationSeconds: Int? = null,
        val mediaPath: String? = null,
        val videoTimestampMs: Long? = null,
    )

    @Serializable
    data class MediaDto(
        val kind: String,
        val relativePath: String,
        val mime: String,
        val width: Int? = null,
        val height: Int? = null,
        val durationMs: Long? = null,
        val sizeBytes: Long = 0,
        val sha256: String? = null,
        val originUrl: String? = null,
        val watermarkFree: Boolean = true,
        val stepId: String? = null,
        val position: Int = 0,
    )

    fun toJson(
        recipe: RecipeEntity,
        source: SourceEntity?,
        ingredients: List<IngredientEntity>,
        steps: List<StepEntity>,
        media: List<MediaEntity>,
        exportedAt: Long = System.currentTimeMillis(),
    ): String = json.encodeToString(
        Snapshot(
            exportedAt = exportedAt,
            recipe = RecipeDto(
                id = recipe.id,
                title = recipe.title,
                subtitle = recipe.subtitle,
                coverPath = recipe.coverPath,
                servings = recipe.servings,
                totalMinutes = recipe.totalMinutes,
                difficulty = recipe.difficulty,
                notes = recipe.notes,
                createdAt = recipe.createdAt,
                updatedAt = recipe.updatedAt,
                lastCookedAt = recipe.lastCookedAt,
                favorite = recipe.favorite,
            ),
            source = source?.let {
                SourceDto(
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
            },
            ingredients = ingredients.map {
                IngredientDto(it.position, it.groupName, it.name, it.amount, it.unit, it.rawText, it.scalable)
            },
            steps = steps.map {
                StepDto(it.position, it.text, it.durationSeconds, it.mediaPath, it.videoTimestampMs)
            },
            media = media.map {
                MediaDto(
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
                    stepId = it.stepId,
                    position = it.position,
                )
            },
        ),
    )

    fun parse(text: String): Snapshot? = runCatching { json.decodeFromString<Snapshot>(text) }.getOrNull()

    fun fromJson(text: String): Snapshot? = parse(text)

    fun toMarkdown(snapshot: Snapshot): String = buildString {
        appendLine("# ${snapshot.recipe.title}")
        snapshot.source?.let {
            appendLine()
            appendLine("> 来源：${it.platform} · ${it.authorName ?: "未知作者"}")
            appendLine("> 原链接：${it.originalUrl}")
        }
        appendLine()
        appendLine("## 食材（${snapshot.recipe.servings} 人份）")
        snapshot.ingredients.forEach {
            val amount = it.amount?.let { a -> IngredientScalerText.format(a, it.unit) } ?: it.rawText
            appendLine("- ${it.name}：$amount")
        }
        appendLine()
        appendLine("## 步骤")
        snapshot.steps.forEachIndexed { index, step ->
            appendLine("${index + 1}. ${step.text}")
        }
        if (snapshot.recipe.notes.isNotBlank()) {
            appendLine()
            appendLine("## 备注")
            appendLine(snapshot.recipe.notes)
        }
    }
}

private object IngredientScalerText {
    fun format(amount: Double, unit: String?): String {
        val text = if (amount % 1.0 == 0.0) amount.toInt().toString() else "%.1f".format(amount)
        return if (unit.isNullOrBlank()) text else "$text$unit"
    }
}
