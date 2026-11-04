package com.weiji.wang.core.data

import androidx.room.withTransaction
import com.weiji.wang.core.data.db.MealMenuEntity
import com.weiji.wang.core.data.db.WeijiDatabase
import com.weiji.wang.core.storage.FileStore
import com.weiji.wang.core.storage.RecipeSnapshot
import com.weiji.wang.core.time.DayClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDate

data class MenuEntry(
    val date: String,
    val recipeId: String,
    val addedAt: Long,
    val coverPath: String?,
    val snapshot: RecipeSnapshot.Snapshot?,
)

/**
 * 每日菜单：`date = 今天` 即「今天吃什么」，其余日期是只读历史。
 * 加入时保存完整快照与 720px 封面；快照独立于菜谱，删除菜谱不影响历史。
 */
class MenuRepository(
    private val db: WeijiDatabase,
    private val files: FileStore,
    private val shopping: ShoppingRepository,
    private val now: () -> Long = { System.currentTimeMillis() },
) {
    private val menu = db.mealMenuDao()

    /** 跟随本机日期；跨过午夜后自动查询新的一天。 */
    fun observeToday(): Flow<List<MenuEntry>> =
        DayClock.flow(now).flatMapLatest { observeDay(it) }

    /** 是否存过任何一天的菜单（用于「吃过的菜」整页空态）。 */
    fun observeHasAny(): Flow<Boolean> = menu.observeCount().map { it > 0 }

    fun observeDay(date: String): Flow<List<MenuEntry>> =
        menu.observe(date).map { rows -> rows.map(::toEntry) }

    fun observeMonth(year: Int, month: Int): Flow<List<MenuEntry>> {
        val first = LocalDate.of(year, month, 1)
        val last = first.withDayOfMonth(first.lengthOfMonth())
        return menu.observeRange(first.toString(), last.toString())
            .map { rows -> rows.map(::toEntry) }
    }

    /**
     * 加入今天食谱：写当天菜单 + 快照 + 封面，按份量写采购行，并更新 lastCookedAt。
     * 同一天重复加入是幂等的，不重复插入、不重复写采购。
     */
    suspend fun addToday(recipeId: String, servings: Int? = null): Boolean {
        val date = DayClock.todayKey(now())
        if (menu.get(date, recipeId) != null) return true
        val details = db.recipeDao().details(recipeId) ?: return false
        val targetServings = servings ?: details.recipe.servings
        val coverPath = withContext(Dispatchers.IO) {
            files.saveHistoryCover(recipeId, details.recipe.coverPath, date)
        }
        val snapshotJson = RecipeSnapshot.toJson(
            recipe = details.recipe,
            source = details.source,
            ingredients = details.ingredients,
            steps = details.steps,
            media = details.media,
            exportedAt = now(),
        )
        db.withTransaction {
            menu.upsert(
                MealMenuEntity(
                    date = date,
                    recipeId = recipeId,
                    snapshotJson = snapshotJson,
                    coverCachePath = coverPath,
                    addedAt = now(),
                ),
            )
            shopping.addFromRecipe(recipeId, details.ingredients, targetServings, date)
            db.recipeDao().markCooked(recipeId, now())
        }
        return true
    }

    /** 移除今天菜单：删除当天行与该菜谱当天写入的采购行；历史快照保留。 */
    suspend fun removeToday(recipeId: String) {
        val date = DayClock.todayKey(now())
        db.withTransaction {
            menu.delete(date, recipeId)
            shopping.removeRecipeContributions(recipeId, date)
        }
    }

    suspend fun isToday(recipeId: String): Boolean =
        menu.get(DayClock.todayKey(now()), recipeId) != null

    /** 菜谱保存在今天菜单里时刷新快照；不在今天菜单则什么都不做。 */
    suspend fun refreshSnapshot(recipeId: String) {
        val date = DayClock.todayKey(now())
        val existing = menu.get(date, recipeId) ?: return
        val details = db.recipeDao().details(recipeId) ?: return
        val snapshotJson = RecipeSnapshot.toJson(
            recipe = details.recipe,
            source = details.source,
            ingredients = details.ingredients,
            steps = details.steps,
            media = details.media,
            exportedAt = now(),
        )
        menu.upsert(existing.copy(snapshotJson = snapshotJson))
    }

    private fun toEntry(entity: MealMenuEntity): MenuEntry = MenuEntry(
        date = entity.date,
        recipeId = entity.recipeId,
        addedAt = entity.addedAt,
        coverPath = entity.coverCachePath,
        snapshot = RecipeSnapshot.fromJson(entity.snapshotJson),
    )
}
