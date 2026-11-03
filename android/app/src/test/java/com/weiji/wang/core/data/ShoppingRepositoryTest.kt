package com.weiji.wang.core.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.weiji.wang.core.data.db.IngredientEntity
import com.weiji.wang.core.data.db.RecipeEntity
import com.weiji.wang.core.data.db.WeijiDatabase
import com.weiji.wang.core.storage.FileStore
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ShoppingRepositoryTest {

    private lateinit var db: WeijiDatabase
    private lateinit var shopping: ShoppingRepository

    private val recipeId = "01SHOPTEST000000000000000"

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, WeijiDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        shopping = ShoppingRepository(db, FileStore(context), now = { 2_000L })
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun seedRecipe(servings: Int = 2): List<IngredientEntity> {
        db.recipeDao().upsert(
            RecipeEntity(
                id = recipeId,
                title = "土豆牛肉",
                servings = servings,
                createdAt = 1_000,
                updatedAt = 1_000,
            ),
        )
        val ingredients = listOf(
            IngredientEntity("i1", recipeId, 0, null, "牛肉", 400.0, "克", "牛肉：400克", true),
            IngredientEntity("i2", recipeId, 1, null, "土豆", 300.0, "克", "土豆：300克", true),
        )
        db.ingredientDao().insertAll(ingredients)
        return ingredients
    }

    private suspend fun items() = db.shoppingDao().items(shopping.ensureList().id)

    @Test
    fun `add from recipe scales servings and records source`() = runTest {
        val ingredients = seedRecipe(servings = 2)

        shopping.addFromRecipe(recipeId, ingredients, servings = 4, date = "2026-11-03")

        val rows = items()
        assertEquals(2, rows.size)
        assertTrue(rows.all { it.recipeId == recipeId && it.sourceDate == "2026-11-03" })
        assertEquals(800.0, rows.first { it.name == "牛肉" }.amount!!, 0.001)
        assertEquals(600.0, rows.first { it.name == "土豆" }.amount!!, 0.001)
        assertEquals("肉禽水产", rows.first { it.name == "牛肉" }.category)
        assertEquals("蔬菜", rows.first { it.name == "土豆" }.category)
    }

    @Test
    fun `manual item has no recipe source`() = runTest {
        shopping.addManual(" 盐 ", null, null)

        val row = items().single()
        assertEquals("盐", row.name)
        assertNull(row.recipeId)
        assertNull(row.sourceDate)
        assertNull(row.category)
    }

    @Test
    fun `editing an item detaches it from the recipe`() = runTest {
        val ingredients = seedRecipe()
        shopping.addFromRecipe(recipeId, ingredients, servings = 2, date = "2026-11-03")
        val before = items().first { it.name == "牛肉" }

        shopping.updateItem(before.id, "牛肉片", 500.0, "克")

        val after = db.shoppingDao().item(before.id)!!
        assertEquals("牛肉片", after.name)
        assertEquals(500.0, after.amount!!, 0.001)
        assertEquals("克", after.unit)
        assertNull(after.recipeId)
        assertNull(after.sourceDate)
    }

    @Test
    fun `remove recipe contributions only touches the matching day`() = runTest {
        val ingredients = seedRecipe()
        shopping.addFromRecipe(recipeId, ingredients, servings = 2, date = "2026-11-03")
        shopping.addFromRecipe(recipeId, ingredients, servings = 2, date = "2026-11-04")
        shopping.addManual("盐", null, null)

        shopping.removeRecipeContributions(recipeId, "2026-11-03")

        val rows = items()
        assertEquals(3, rows.size)
        assertTrue(rows.none { it.sourceDate == "2026-11-03" })
        assertEquals(2, rows.count { it.sourceDate == "2026-11-04" })
        assertEquals(1, rows.count { it.recipeId == null })
    }
}
