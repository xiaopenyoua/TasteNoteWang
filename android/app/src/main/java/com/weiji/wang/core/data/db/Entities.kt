package com.weiji.wang.core.data.db

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "recipes", indices = [Index("lastCookedAt"), Index("updatedAt"), Index("favorite")])
data class RecipeEntity(
    @PrimaryKey val id: String,
    val title: String,
    val subtitle: String = "",
    val coverPath: String? = null,
    val servings: Int = 2,
    val totalMinutes: Int? = null,
    val difficulty: Int = 1,
    val notes: String = "",
    val sourceId: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val lastCookedAt: Long? = null,
    val favorite: Boolean = false,
    val archived: Boolean = false,
)

@Entity(tableName = "sources", indices = [Index(value = ["recipeId"], unique = true)])
data class SourceEntity(
    @PrimaryKey val id: String,
    val recipeId: String,
    val platform: String,
    val originalUrl: String,
    val canonicalUrl: String? = null,
    val platformContentId: String? = null,
    val authorName: String? = null,
    val authorId: String? = null,
    val sourceTitle: String? = null,
    val textBody: String? = null,
    val remoteCoverUrl: String? = null,
    val shareText: String? = null,
    val importedAt: Long,
    val fetchStatus: String = "MANUAL",
)

@Entity(tableName = "ingredients", indices = [Index("recipeId"), Index("name")])
data class IngredientEntity(
    @PrimaryKey val id: String,
    val recipeId: String,
    val position: Int,
    val groupName: String? = null,
    val name: String,
    val amount: Double? = null,
    val unit: String? = null,
    val rawText: String,
    val scalable: Boolean = true,
    val optional: Boolean = false,
)

@Entity(tableName = "steps", indices = [Index("recipeId")])
data class StepEntity(
    @PrimaryKey val id: String,
    val recipeId: String,
    val position: Int,
    val text: String,
    val durationSeconds: Int? = null,
    val mediaPath: String? = null,
    val videoTimestampMs: Long? = null,
)

@Entity(tableName = "media", indices = [Index("recipeId")])
data class MediaEntity(
    @PrimaryKey val id: String,
    val recipeId: String,
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
    val createdAt: Long,
    /** null = 菜谱级媒体（封面/原视频）；非空 = 某一步骤的媒体。 */
    val stepId: String? = null,
    /** 同一步骤内的排序。 */
    @ColumnInfo(defaultValue = "0") val position: Int = 0,
)

@Entity(tableName = "cook_logs", indices = [Index("recipeId"), Index("cookedAt")])
data class CookLogEntity(
    @PrimaryKey val id: String,
    val recipeId: String,
    val cookedAt: Long,
    val servings: Int,
    val rating: Int? = null,
    val notes: String = "",
    val photoPath: String? = null,
    val durationMinutes: Int? = null,
)

@Entity(tableName = "revisions", indices = [Index("recipeId")])
data class RevisionEntity(
    @PrimaryKey val id: String,
    val recipeId: String,
    val createdAt: Long,
    val title: String,
    val ingredientsJson: String,
    val stepsJson: String,
    val notes: String = "",
)

@Entity(tableName = "shopping_lists")
data class ShoppingListEntity(
    @PrimaryKey val id: String,
    val name: String,
    val createdAt: Long,
    val archived: Boolean = false,
)

@Entity(tableName = "shopping_items", indices = [Index("listId")])
data class ShoppingItemEntity(
    @PrimaryKey val id: String,
    val listId: String,
    val name: String,
    val amount: Double? = null,
    val unit: String? = null,
    val category: String? = null,
    val checked: Boolean = false,
    val recipeId: String? = null,
    val createdAt: Long,
    /** 来源日期（本地 yyyy-MM-dd）；null = 手动添加或已脱离菜谱。 */
    val sourceDate: String? = null,
)

@Entity(tableName = "notes", indices = [Index("recipeId"), Index("stepId")])
data class NoteEntity(
    @PrimaryKey val id: String,
    val recipeId: String,
    /** null = 整道菜的备注；非空 = 某一步的备注。 */
    val stepId: String? = null,
    val text: String,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "meal_menu",
    primaryKeys = ["date", "recipeId"],
    indices = [Index("date")],
)
data class MealMenuEntity(
    /** 本地日期 yyyy-MM-dd；date = 今天 即今天菜单。 */
    val date: String,
    val recipeId: String,
    val snapshotJson: String,
    val coverCachePath: String? = null,
    val addedAt: Long,
)

@Entity(tableName = "tags", indices = [Index(value = ["name"], unique = true)])
data class TagEntity(
    @PrimaryKey val id: String,
    val name: String,
    val kind: String = "USER",
)

@Entity(tableName = "recipe_tags", primaryKeys = ["recipeId", "tagId"], indices = [Index("tagId")])
data class RecipeTagEntity(
    val recipeId: String,
    val tagId: String,
)

@Entity(tableName = "drafts", indices = [Index("createdAt"), Index("state")])
data class DraftEntity(
    @PrimaryKey val id: String,
    val shareText: String,
    val url: String? = null,
    val platform: String? = null,
    val state: String,
    val error: String? = null,
    val recipeId: String? = null,
    val progress: Int = 0,
    val createdAt: Long,
    val updatedAt: Long,
)

data class RecipeWithDetails(
    @Embedded val recipe: RecipeEntity,
    @Relation(parentColumn = "id", entityColumn = "recipeId")
    val ingredients: List<IngredientEntity>,
    @Relation(parentColumn = "id", entityColumn = "recipeId")
    val steps: List<StepEntity>,
    @Relation(parentColumn = "id", entityColumn = "recipeId")
    val media: List<MediaEntity>,
    @Relation(parentColumn = "sourceId", entityColumn = "id")
    val source: SourceEntity?,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(RecipeTagEntity::class, parentColumn = "recipeId", entityColumn = "tagId"),
    )
    val tags: List<TagEntity>,
)

data class RecipeCard(
    val id: String,
    val title: String,
    val coverPath: String?,
    val servings: Int,
    val totalMinutes: Int?,
    val lastCookedAt: Long?,
    val createdAt: Long,
    val favorite: Boolean,
    val platform: String?,
    val author: String?,
)

data class ShoppingItemWithList(
    val id: String,
    val listId: String,
    val name: String,
    val amount: Double?,
    val unit: String?,
    val category: String?,
    val checked: Boolean,
    val recipeId: String?,
)
