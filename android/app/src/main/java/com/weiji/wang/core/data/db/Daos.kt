package com.weiji.wang.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeDao {

    @Query(
        """
        SELECT r.id, r.title, r.coverPath, r.servings, r.totalMinutes, r.lastCookedAt,
               r.createdAt, r.favorite, s.platform AS platform, s.authorName AS author
        FROM recipes r
        LEFT JOIN sources s ON s.recipeId = r.id
        WHERE r.archived = 0
        ORDER BY r.favorite DESC, r.updatedAt DESC
        """,
    )
    fun observeCards(): Flow<List<RecipeCard>>

    @Query(
        """
        SELECT r.id, r.title, r.coverPath, r.servings, r.totalMinutes, r.lastCookedAt,
               r.createdAt, r.favorite, s.platform AS platform, s.authorName AS author
        FROM recipes r
        LEFT JOIN sources s ON s.recipeId = r.id
        WHERE r.archived = 0 AND (
            r.title LIKE '%' || :query || '%'
            OR IFNULL(s.authorName, '') LIKE '%' || :query || '%'
            OR IFNULL(s.textBody, '') LIKE '%' || :query || '%'
            OR EXISTS (SELECT 1 FROM ingredients i WHERE i.recipeId = r.id AND i.name LIKE '%' || :query || '%')
        )
        ORDER BY r.favorite DESC, r.updatedAt DESC
        """,
    )
    fun search(query: String): Flow<List<RecipeCard>>

    @Query(
        """
        SELECT r.id, r.title, r.coverPath, r.servings, r.totalMinutes, r.lastCookedAt,
               r.createdAt, r.favorite, s.platform AS platform, s.authorName AS author
        FROM recipes r
        LEFT JOIN sources s ON s.recipeId = r.id
        WHERE r.archived = 0 AND r.lastCookedAt IS NOT NULL
        ORDER BY r.lastCookedAt DESC LIMIT :limit
        """,
    )
    fun observeRecentlyCooked(limit: Int = 6): Flow<List<RecipeCard>>

    @Query(
        """
        SELECT r.id, r.title, r.coverPath, r.servings, r.totalMinutes, r.lastCookedAt,
               r.createdAt, r.favorite, s.platform AS platform, s.authorName AS author
        FROM recipes r
        LEFT JOIN sources s ON s.recipeId = r.id
        WHERE r.archived = 0
        ORDER BY CASE WHEN r.lastCookedAt IS NULL THEN 0 ELSE 1 END,
                 r.lastCookedAt ASC, r.createdAt DESC
        LIMIT :limit
        """,
    )
    fun observeSuggestions(limit: Int = 3): Flow<List<RecipeCard>>

    @Transaction
    @Query("SELECT * FROM recipes WHERE id = :id")
    fun observeDetails(id: String): Flow<RecipeWithDetails?>

    @Transaction
    @Query("SELECT * FROM recipes WHERE id = :id")
    suspend fun details(id: String): RecipeWithDetails?

    @Query("SELECT COUNT(*) FROM recipes WHERE archived = 0")
    fun observeCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(recipe: RecipeEntity)

    @Query("DELETE FROM recipes WHERE id = :id")
    suspend fun delete(id: String)

    @Query("UPDATE recipes SET favorite = :favorite, updatedAt = :now WHERE id = :id")
    suspend fun setFavorite(id: String, favorite: Boolean, now: Long)

    @Query("UPDATE recipes SET lastCookedAt = :at, updatedAt = :at WHERE id = :id")
    suspend fun markCooked(id: String, at: Long)

    @Query("UPDATE recipes SET coverPath = :path, updatedAt = :now WHERE id = :id")
    suspend fun setCover(id: String, path: String?, now: Long)

    @Query("SELECT * FROM recipes WHERE id = :id")
    suspend fun get(id: String): RecipeEntity?

    @Query("SELECT * FROM recipes ORDER BY createdAt")
    suspend fun all(): List<RecipeEntity>
}

@Dao
interface IngredientDao {
    @Query("SELECT * FROM ingredients WHERE recipeId = :recipeId ORDER BY position")
    suspend fun forRecipe(recipeId: String): List<IngredientEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<IngredientEntity>)

    @Query("DELETE FROM ingredients WHERE recipeId = :recipeId")
    suspend fun deleteForRecipe(recipeId: String)
}

@Dao
interface StepDao {
    @Query("SELECT * FROM steps WHERE recipeId = :recipeId ORDER BY position")
    suspend fun forRecipe(recipeId: String): List<StepEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<StepEntity>)

    @Query("DELETE FROM steps WHERE recipeId = :recipeId")
    suspend fun deleteForRecipe(recipeId: String)
}

@Dao
interface MediaDao {
    @Query("SELECT * FROM media WHERE recipeId = :recipeId ORDER BY createdAt")
    suspend fun forRecipe(recipeId: String): List<MediaEntity>

    @Query("SELECT * FROM media WHERE stepId = :stepId ORDER BY position")
    suspend fun forStep(stepId: String): List<MediaEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: MediaEntity)

    @Query("DELETE FROM media WHERE recipeId = :recipeId")
    suspend fun deleteForRecipe(recipeId: String)

    @Query("DELETE FROM media WHERE stepId = :stepId")
    suspend fun deleteByStep(stepId: String)

    @Query("SELECT SUM(sizeBytes) FROM media")
    fun observeTotalBytes(): Flow<Long?>
}

@Dao
interface SourceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(source: SourceEntity)

    @Query("SELECT * FROM sources WHERE recipeId = :recipeId")
    suspend fun forRecipe(recipeId: String): SourceEntity?

    @Query("DELETE FROM sources WHERE recipeId = :recipeId")
    suspend fun deleteForRecipe(recipeId: String)

    @Query("SELECT * FROM sources WHERE originalUrl = :url LIMIT 1")
    suspend fun findByUrl(url: String): SourceEntity?
}

@Dao
interface CookLogDao {
    @Query("SELECT * FROM cook_logs WHERE recipeId = :recipeId ORDER BY cookedAt DESC")
    fun observeForRecipe(recipeId: String): Flow<List<CookLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: CookLogEntity)

    @Query("DELETE FROM cook_logs WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface RevisionDao {
    @Query("SELECT * FROM revisions WHERE recipeId = :recipeId ORDER BY createdAt DESC LIMIT 1")
    fun observeLatest(recipeId: String): Flow<RevisionEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(revision: RevisionEntity)
}

@Dao
interface ShoppingDao {
    @Query("SELECT * FROM shopping_lists WHERE archived = 0 ORDER BY createdAt DESC")
    fun observeLists(): Flow<List<ShoppingListEntity>>

    @Query("SELECT * FROM shopping_items WHERE listId = :listId ORDER BY checked, category, createdAt")
    fun observeItems(listId: String): Flow<List<ShoppingItemEntity>>

    @Query("SELECT * FROM shopping_items WHERE listId = :listId ORDER BY checked, category, createdAt")
    suspend fun items(listId: String): List<ShoppingItemEntity>

    @Query("SELECT * FROM shopping_items WHERE id = :id")
    suspend fun item(id: String): ShoppingItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertList(list: ShoppingListEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertItem(item: ShoppingItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertItems(items: List<ShoppingItemEntity>)

    @Query("UPDATE shopping_items SET checked = :checked WHERE id = :id")
    suspend fun setChecked(id: String, checked: Boolean)

    @Query("DELETE FROM shopping_items WHERE id = :id")
    suspend fun deleteItem(id: String)

    @Query("DELETE FROM shopping_items WHERE listId = :listId AND checked = 1")
    suspend fun clearChecked(listId: String)

    @Query("DELETE FROM shopping_items WHERE listId = :listId")
    suspend fun clearList(listId: String)

    @Query("DELETE FROM shopping_items WHERE recipeId = :recipeId AND sourceDate = :sourceDate")
    suspend fun deleteByRecipeAndDate(recipeId: String, sourceDate: String)

    @Query("UPDATE shopping_items SET recipeId = NULL, sourceDate = NULL WHERE recipeId = :recipeId")
    suspend fun detachRecipe(recipeId: String)

    @Query("SELECT * FROM shopping_lists ORDER BY createdAt DESC LIMIT 1")
    suspend fun latestList(): ShoppingListEntity?

    @Query("SELECT * FROM shopping_lists")
    suspend fun allLists(): List<ShoppingListEntity>

    @Query("SELECT * FROM shopping_items")
    suspend fun allItems(): List<ShoppingItemEntity>
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE recipeId = :recipeId ORDER BY createdAt DESC")
    fun observeForRecipe(recipeId: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun get(id: String): NoteEntity?

    @Upsert
    suspend fun upsert(note: NoteEntity)

    @Query("SELECT * FROM notes")
    suspend fun all(): List<NoteEntity>

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM notes WHERE recipeId = :recipeId")
    suspend fun deleteForRecipe(recipeId: String)
}

@Dao
interface MealMenuDao {
    @Query("SELECT * FROM meal_menu WHERE date = :date ORDER BY addedAt")
    fun observe(date: String): Flow<List<MealMenuEntity>>

    @Query("SELECT * FROM meal_menu WHERE date BETWEEN :from AND :to ORDER BY date, addedAt")
    fun observeRange(from: String, to: String): Flow<List<MealMenuEntity>>

    @Query("SELECT * FROM meal_menu WHERE date = :date AND recipeId = :recipeId LIMIT 1")
    suspend fun get(date: String, recipeId: String): MealMenuEntity?

    @Upsert
    suspend fun upsert(entry: MealMenuEntity)

    @Query("DELETE FROM meal_menu WHERE date = :date AND recipeId = :recipeId")
    suspend fun delete(date: String, recipeId: String)

    @Query("SELECT DISTINCT date FROM meal_menu WHERE date BETWEEN :from AND :to ORDER BY date")
    suspend fun datesInRange(from: String, to: String): List<String>

    @Query("SELECT COUNT(*) FROM meal_menu")
    fun observeCount(): Flow<Int>

    @Query("SELECT * FROM meal_menu ORDER BY date, addedAt")
    suspend fun all(): List<MealMenuEntity>
}

@Dao
interface TagDao {
    @Query("SELECT * FROM tags ORDER BY name")
    fun observeAll(): Flow<List<TagEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(tag: TagEntity)

    @Query("SELECT * FROM tags WHERE name = :name LIMIT 1")
    suspend fun find(name: String): TagEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun link(link: RecipeTagEntity)

    @Query("DELETE FROM recipe_tags WHERE recipeId = :recipeId")
    suspend fun clearLinks(recipeId: String)
}

@Dao
interface DraftDao {
    @Query("SELECT * FROM drafts ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<DraftEntity>>

    @Query("SELECT * FROM drafts ORDER BY createdAt DESC")
    suspend fun all(): List<DraftEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(draft: DraftEntity)

    @Query("SELECT * FROM drafts WHERE id = :id")
    suspend fun get(id: String): DraftEntity?

    @Query("UPDATE drafts SET state = :state, error = :error, progress = :progress, updatedAt = :now WHERE id = :id")
    suspend fun updateState(id: String, state: String, error: String?, progress: Int, now: Long)

    @Query("UPDATE drafts SET recipeId = :recipeId, state = :state, progress = 100, updatedAt = :now WHERE id = :id")
    suspend fun attachRecipe(id: String, recipeId: String, state: String, now: Long)

    @Query("DELETE FROM drafts WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM drafts WHERE state IN ('READY','PARTIAL') AND createdAt < :before")
    suspend fun pruneCompleted(before: Long)
}
