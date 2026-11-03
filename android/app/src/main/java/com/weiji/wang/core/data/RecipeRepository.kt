package com.weiji.wang.core.data

import androidx.room.withTransaction
import com.weiji.wang.core.data.db.CookLogEntity
import com.weiji.wang.core.data.db.IngredientEntity
import com.weiji.wang.core.data.db.MediaEntity
import com.weiji.wang.core.data.db.NoteEntity
import com.weiji.wang.core.data.db.RecipeCard
import com.weiji.wang.core.data.db.RecipeEntity
import com.weiji.wang.core.data.db.RecipeWithDetails
import com.weiji.wang.core.data.db.ShoppingItemEntity
import com.weiji.wang.core.data.db.ShoppingListEntity
import com.weiji.wang.core.data.db.SourceEntity
import com.weiji.wang.core.data.db.StepEntity
import com.weiji.wang.core.data.db.WeijiDatabase
import com.weiji.wang.core.model.Ulid
import com.weiji.wang.core.storage.FileStore
import com.weiji.wang.core.storage.RecipeSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.map

class RecipeRepository(
    private val db: WeijiDatabase,
    private val files: FileStore,
    private val now: () -> Long = { System.currentTimeMillis() },
) {
    private val recipes = db.recipeDao()
    private val ingredients = db.ingredientDao()
    private val steps = db.stepDao()
    private val media = db.mediaDao()
    private val sources = db.sourceDao()
    private val cookLogs = db.cookLogDao()
    private val shopping = db.shoppingDao()
    private val tags = db.tagDao()
    private val notes = db.noteDao()

    fun observeCards(): Flow<List<RecipeCard>> = recipes.observeCards()

    fun search(query: String): Flow<List<RecipeCard>> =
        if (query.isBlank()) recipes.observeCards() else recipes.search(query.trim())

    fun observeSuggestions(limit: Int = 3): Flow<List<RecipeCard>> = recipes.observeSuggestions(limit)

    fun observeRecentlyCooked(limit: Int = 6): Flow<List<RecipeCard>> = recipes.observeRecentlyCooked(limit)

    fun observeCount(): Flow<Int> = recipes.observeCount()

    fun observeDetails(id: String): Flow<RecipeWithDetails?> = recipes.observeDetails(id)

    fun observeCookLogs(id: String): Flow<List<CookLogEntity>> = cookLogs.observeForRecipe(id)

    fun observeNotes(recipeId: String): Flow<List<NoteEntity>> = notes.observeForRecipe(recipeId)

    suspend fun addNote(recipeId: String, stepId: String?, text: String): NoteEntity {
        val timestamp = now()
        val note = NoteEntity(
            id = Ulid.next(),
            recipeId = recipeId,
            stepId = stepId,
            text = text.trim(),
            createdAt = timestamp,
            updatedAt = timestamp,
        )
        notes.upsert(note)
        return note
    }

    suspend fun updateNote(id: String, text: String) {
        val existing = notes.get(id) ?: return
        notes.upsert(existing.copy(text = text.trim(), updatedAt = now()))
    }

    suspend fun deleteNote(id: String) = notes.delete(id)

    fun observeStorageBytes(): Flow<Long> = media.observeTotalBytes().map { it ?: 0L }

    suspend fun details(id: String): RecipeWithDetails? = recipes.details(id)

    suspend fun saveRecipe(
        recipe: RecipeEntity,
        ingredientList: List<IngredientEntity>,
        stepList: List<StepEntity>,
        source: SourceEntity?,
        mediaList: List<MediaEntity> = emptyList(),
    ) {
        val timestamp = now()
        db.withTransaction {
            recipes.upsert(recipe.copy(updatedAt = timestamp))
            ingredients.deleteForRecipe(recipe.id)
            steps.deleteForRecipe(recipe.id)
            media.deleteForRecipe(recipe.id)
            ingredients.insertAll(ingredientList)
            steps.insertAll(stepList)
            source?.let { sources.upsert(it) }
            mediaList.forEach { media.insert(it) }
        }
        // 每次保存都刷新可读快照；导出/迁移以它为准，DB 仍是运行时唯一事实源
        withContext(Dispatchers.IO) {
            runCatching {
                files.writeTextAtomic(
                    recipe.id,
                    RecipeSnapshot.FILE_NAME,
                    RecipeSnapshot.toJson(
                        recipe = recipe.copy(updatedAt = timestamp),
                        source = source,
                        ingredients = ingredientList,
                        steps = stepList,
                        media = mediaList,
                    ),
                )
            }
        }
    }

    suspend fun deleteRecipe(id: String) {
        db.withTransaction {
            ingredients.deleteForRecipe(id)
            steps.deleteForRecipe(id)
            media.deleteForRecipe(id)
            sources.deleteForRecipe(id)
            tags.clearLinks(id)
            notes.deleteForRecipe(id)
            // 采购行已经要买的东西不因删菜谱消失：保留并把来源置空
            shopping.detachRecipe(id)
            recipes.delete(id)
        }
        withContext(Dispatchers.IO) { files.deleteRecipe(id) }
    }

    suspend fun setFavorite(id: String, favorite: Boolean) {
        recipes.setFavorite(id, favorite, now())
    }

    suspend fun setCover(id: String, relativePath: String?) {
        recipes.setCover(id, relativePath, now())
    }

    suspend fun markCooked(id: String, at: Long = now()) {
        recipes.markCooked(id, at)
    }

    suspend fun recordCook(
        recipeId: String,
        servings: Int,
        rating: Int?,
        notes: String,
        photoPath: String? = null,
        durationMinutes: Int? = null,
        cookedAt: Long = now(),
    ) {
        val log = CookLogEntity(
            id = Ulid.next(),
            recipeId = recipeId,
            cookedAt = cookedAt,
            servings = servings,
            rating = rating,
            notes = notes,
            photoPath = photoPath,
            durationMinutes = durationMinutes,
        )
        db.withTransaction {
            cookLogs.insert(log)
            recipes.markCooked(recipeId, cookedAt)
        }
    }

    suspend fun createRevision(recipeId: String, title: String, ingredientsJson: String, stepsJson: String, notes: String) {
        db.revisionDao().insert(
            com.weiji.wang.core.data.db.RevisionEntity(
                id = Ulid.next(),
                recipeId = recipeId,
                createdAt = now(),
                title = title,
                ingredientsJson = ingredientsJson,
                stepsJson = stepsJson,
                notes = notes,
            ),
        )
    }

    fun observeLatestRevision(recipeId: String) = db.revisionDao().observeLatest(recipeId)

    suspend fun addIngredientsToShopping(recipeId: String, items: List<IngredientEntity>) {
        val list = shopping.latestList() ?: ShoppingListEntity(
            id = Ulid.next(),
            name = "采购清单",
            createdAt = now(),
        ).also { shopping.upsertList(it) }
        val existing = shopping.items(list.id).map { it.name.trim() to it.unit.orEmpty() }.toSet()
        val newItems = items
            .filter { (it.name.trim() to it.unit.orEmpty()) !in existing }
            .map {
                ShoppingItemEntity(
                    id = Ulid.next(),
                    listId = list.id,
                    name = it.name,
                    amount = it.amount,
                    unit = it.unit,
                    category = guessCategory(it.name),
                    recipeId = recipeId,
                    createdAt = now(),
                )
            }
        if (newItems.isNotEmpty()) shopping.upsertItems(newItems)
    }

    fun observeShoppingLists() = shopping.observeLists()

    fun observeShoppingItems(listId: String) = shopping.observeItems(listId)

    suspend fun ensureShoppingList(): ShoppingListEntity =
        shopping.latestList() ?: ShoppingListEntity(Ulid.next(), "采购清单", now()).also {
            shopping.upsertList(it)
        }

    suspend fun toggleShoppingItem(id: String, checked: Boolean) = shopping.setChecked(id, checked)

    suspend fun deleteShoppingItem(id: String) = shopping.deleteItem(id)

    suspend fun clearChecked(listId: String) = shopping.clearChecked(listId)

    suspend fun clearShoppingList(listId: String) = shopping.clearList(listId)

    suspend fun addShoppingItem(listId: String, name: String, amount: Double?, unit: String?) {
        if (name.isBlank()) return
        shopping.upsertItem(
            ShoppingItemEntity(
                id = Ulid.next(),
                listId = listId,
                name = name.trim(),
                amount = amount,
                unit = unit,
                category = guessCategory(name),
                createdAt = now(),
            ),
        )
    }

    suspend fun findSourceByUrl(url: String): SourceEntity? = sources.findByUrl(url)

    fun observeTags() = tags.observeAll()

    private fun guessCategory(name: String): String = when {
        listOf("牛", "猪", "鸡", "鸭", "羊", "鱼", "虾", "蟹", "肉").any { it in name } -> "肉禽水产"
        listOf("菜", "瓜", "茄", "椒", "葱", "姜", "蒜", "菇", "豆", "薯", "萝卜", "番茄").any { it in name } -> "蔬菜"
        listOf("油", "盐", "酱", "醋", "糖", "料酒", "淀粉", "椒盐", "孜然", "香叶", "八角").any { it in name } -> "调料"
        listOf("蛋", "奶", "芝士", "黄油").any { it in name } -> "蛋奶"
        else -> "其他"
    }
}
