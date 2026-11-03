package com.weiji.wang.core.data

import com.weiji.wang.core.data.db.IngredientEntity
import com.weiji.wang.core.data.db.ShoppingItemEntity
import com.weiji.wang.core.data.db.ShoppingListEntity
import com.weiji.wang.core.data.db.WeijiDatabase
import com.weiji.wang.core.model.IngredientScaler
import com.weiji.wang.core.model.Ulid
import com.weiji.wang.core.storage.FileStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

/**
 * 采购清单的写入口。采购行写入后即独立于菜谱：
 * - 菜谱只在加入今天菜单时写入一份带 [ShoppingItemEntity.recipeId] + [ShoppingItemEntity.sourceDate] 的行；
 * - 手动项与编辑过的行来源为空；
 * - 合并只发生在读取时（见 core/model/ShoppingMerge.kt）。
 */
class ShoppingRepository(
    private val db: WeijiDatabase,
    @Suppress("UNUSED_PARAMETER") files: FileStore,
    private val now: () -> Long = { System.currentTimeMillis() },
) {
    private val dao = db.shoppingDao()

    suspend fun ensureList(): ShoppingListEntity =
        dao.latestList() ?: ShoppingListEntity(
            id = Ulid.next(),
            name = "采购清单",
            createdAt = now(),
        ).also { dao.upsertList(it) }

    /** 观察当前采购清单的全部原始行。清单尚未创建时为空。 */
    fun observeItems(): Flow<List<ShoppingItemEntity>> =
        dao.observeLists().flatMapLatest { lists ->
            val listId = lists.firstOrNull()?.id
            if (listId == null) flowOf(emptyList()) else dao.observeItems(listId)
        }

    suspend fun addManual(name: String, amount: Double?, unit: String?) {
        if (name.isBlank()) return
        val list = ensureList()
        dao.upsertItem(
            ShoppingItemEntity(
                id = Ulid.next(),
                listId = list.id,
                name = name.trim(),
                amount = amount,
                unit = unit?.trim()?.takeIf { it.isNotEmpty() },
                category = null,
                checked = false,
                recipeId = null,
                createdAt = now(),
                sourceDate = null,
            ),
        )
    }

    /** 把菜谱食材按份量缩放后写进采购；每行记录来源菜谱与来源日期。 */
    suspend fun addFromRecipe(
        recipeId: String,
        ingredients: List<IngredientEntity>,
        servings: Int,
        date: String,
    ) {
        if (ingredients.isEmpty()) return
        val list = ensureList()
        val baseServings = db.recipeDao().get(recipeId)?.servings ?: servings
        dao.upsertItems(
            ingredients.map { ingredient ->
                ShoppingItemEntity(
                    id = Ulid.next(),
                    listId = list.id,
                    name = ingredient.name,
                    amount = IngredientScaler.scale(ingredient.amount, baseServings, servings),
                    unit = ingredient.unit,
                    category = guessCategory(ingredient.name),
                    checked = false,
                    recipeId = recipeId,
                    createdAt = now(),
                    sourceDate = date,
                )
            },
        )
    }

    /** 只改这一条采购项；保存后与菜谱脱离，菜谱后续改动不再影响它。 */
    suspend fun updateItem(id: String, name: String, amount: Double?, unit: String?) {
        if (name.isBlank()) return
        val existing = dao.item(id) ?: return
        dao.upsertItem(
            existing.copy(
                name = name.trim(),
                amount = amount,
                unit = unit?.trim()?.takeIf { it.isNotEmpty() },
                recipeId = null,
                sourceDate = null,
            ),
        )
    }

    suspend fun toggle(id: String, checked: Boolean) = dao.setChecked(id, checked)

    suspend fun delete(id: String) = dao.deleteItem(id)

    suspend fun clearChecked() {
        dao.clearChecked(ensureList().id)
    }

    suspend fun clearAll() {
        dao.clearList(ensureList().id)
    }

    /** 移除某菜谱在某天写入的采购行；手动行与其他来源保留。 */
    suspend fun removeRecipeContributions(recipeId: String, date: String) =
        dao.deleteByRecipeAndDate(recipeId, date)

    /** 删除菜谱时把来源置空，采购行本身保留。 */
    suspend fun detachRecipe(recipeId: String) = dao.detachRecipe(recipeId)

    private fun guessCategory(name: String): String = when {
        listOf("牛", "猪", "鸡", "鸭", "羊", "鱼", "虾", "蟹", "肉").any { it in name } -> "肉禽水产"
        listOf("菜", "瓜", "茄", "椒", "葱", "姜", "蒜", "菇", "豆", "薯", "萝卜", "番茄").any { it in name } -> "蔬菜"
        listOf("油", "盐", "酱", "醋", "糖", "料酒", "淀粉", "椒盐", "孜然", "香叶", "八角").any { it in name } -> "调料"
        listOf("蛋", "奶", "芝士", "黄油").any { it in name } -> "蛋奶"
        else -> "其他"
    }
}
