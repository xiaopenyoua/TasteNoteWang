package com.weiji.wang.core.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.weiji.wang.core.data.db.IngredientEntity
import com.weiji.wang.core.data.db.RecipeEntity
import com.weiji.wang.core.data.db.ShoppingItemEntity
import com.weiji.wang.core.data.db.WeijiDatabase
import com.weiji.wang.core.storage.FileStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MenuRepositoryTest {

    private lateinit var db: WeijiDatabase
    private lateinit var store: FileStore
    private lateinit var shopping: ShoppingRepository
    private lateinit var menu: MenuRepository
    private lateinit var recipes: RecipeRepository

    private val recipeId = "01MENUTEST000000000000000"

    private var clock: Long = noonOf("2026-11-03")

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, WeijiDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        store = FileStore(context)
        shopping = ShoppingRepository(db, store, now = { clock })
        menu = MenuRepository(db, store, shopping, now = { clock })
        recipes = RecipeRepository(db, store, now = { clock })
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun noonOf(date: String): Long =
        LocalDate.parse(date).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() +
            12 * 60 * 60 * 1000L

    private suspend fun seedRecipe(id: String = recipeId, servings: Int = 2) {
        db.recipeDao().upsert(
            RecipeEntity(
                id = id,
                title = "孜然土豆牛肉",
                servings = servings,
                createdAt = clock,
                updatedAt = clock,
            ),
        )
        db.ingredientDao().insertAll(
            listOf(
                IngredientEntity("i1", id, 0, null, "牛肉", 400.0, "克", "牛肉：400克", true),
                IngredientEntity("i2", id, 1, null, "土豆", 300.0, "克", "土豆：300克", true),
            ),
        )
    }

    private suspend fun shoppingItems(): List<ShoppingItemEntity> {
        val list = db.shoppingDao().latestList() ?: return emptyList()
        return db.shoppingDao().items(list.id)
    }

    @Test
    fun `add today inserts menu entry shopping rows and last cooked`() = runTest {
        seedRecipe()

        assertTrue(menu.addToday(recipeId))

        val entry = db.mealMenuDao().get("2026-11-03", recipeId)
        assertNotNull(entry)
        assertTrue(entry!!.snapshotJson.contains("孜然土豆牛肉"))
        assertEquals(clock, db.recipeDao().get(recipeId)!!.lastCookedAt)

        val items = shoppingItems()
        assertEquals(2, items.size)
        assertTrue(items.all { it.recipeId == recipeId && it.sourceDate == "2026-11-03" })
        assertTrue(items.any { it.name == "牛肉" && it.amount == 400.0 })
    }

    @Test
    fun `adding the same recipe twice is idempotent`() = runTest {
        seedRecipe()

        menu.addToday(recipeId)
        menu.addToday(recipeId)

        assertEquals(1, menu.observeDay("2026-11-03").first().size)
        assertEquals(2, shoppingItems().size)
    }

    @Test
    fun `remove today keeps manual items`() = runTest {
        seedRecipe()
        shopping.addManual("盐", null, null)
        menu.addToday(recipeId)

        menu.removeToday(recipeId)

        val items = shoppingItems()
        assertEquals(1, items.size)
        assertEquals("盐", items[0].name)
        assertNull(items[0].recipeId)
        assertTrue(menu.observeDay("2026-11-03").first().isEmpty())
    }

    @Test
    fun `remove today keeps recipe rows from another date`() = runTest {
        seedRecipe()
        val list = shopping.ensureList()
        db.shoppingDao().upsertItem(
            ShoppingItemEntity(
                id = "old-day",
                listId = list.id,
                name = "牛肉",
                amount = 1.0,
                unit = "份",
                recipeId = recipeId,
                createdAt = clock,
                sourceDate = "2026-11-02",
            ),
        )
        menu.addToday(recipeId)

        menu.removeToday(recipeId)

        val items = shoppingItems()
        assertEquals(1, items.size)
        assertEquals("old-day", items[0].id)
        assertEquals("2026-11-02", items[0].sourceDate)
    }

    @Test
    fun `crossing midnight does not clear shopping`() = runTest {
        seedRecipe()
        menu.addToday(recipeId)
        assertEquals(2, shoppingItems().size)

        clock = noonOf("2026-11-04")

        assertTrue(menu.observeToday().first().isEmpty())
        assertEquals(2, shoppingItems().size)

        menu.addToday(recipeId)
        assertEquals(4, shoppingItems().size)

        menu.removeToday(recipeId)
        val remaining = shoppingItems()
        assertEquals(2, remaining.size)
        assertTrue(remaining.all { it.sourceDate == "2026-11-03" })
    }

    @Test
    fun `deleting recipe keeps menu entry and shopping rows`() = runTest {
        seedRecipe()
        menu.addToday(recipeId)

        recipes.deleteRecipe(recipeId)

        assertNull(db.recipeDao().get(recipeId))
        assertNotNull(db.mealMenuDao().get("2026-11-03", recipeId))
        val items = shoppingItems()
        assertEquals(2, items.size)
        assertTrue(items.all { it.recipeId == null && it.sourceDate == null })
    }
}
