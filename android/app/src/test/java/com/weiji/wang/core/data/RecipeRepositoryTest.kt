package com.weiji.wang.core.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.weiji.wang.core.data.db.IngredientEntity
import com.weiji.wang.core.data.db.MediaEntity
import com.weiji.wang.core.data.db.RecipeEntity
import com.weiji.wang.core.data.db.SourceEntity
import com.weiji.wang.core.data.db.StepEntity
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RecipeRepositoryTest {

    private lateinit var db: WeijiDatabase
    private lateinit var repo: RecipeRepository
    private lateinit var store: FileStore

    private val recipeId = "01TESTRECIPE0000000000000"

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, WeijiDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        store = FileStore(context)
        repo = RecipeRepository(db, store, now = { 1_000L })
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun seed() {
        val recipe = RecipeEntity(
            id = recipeId,
            title = "孜然土豆牛肉",
            subtitle = "@西关焖烧男",
            coverPath = null,
            servings = 2,
            totalMinutes = 5,
            createdAt = 1_000,
            updatedAt = 1_000,
            sourceId = "src1",
        )
        val source = SourceEntity(
            id = "src1",
            recipeId = recipeId,
            platform = "DOUYIN",
            originalUrl = "https://v.douyin.com/x/",
            canonicalUrl = "https://www.douyin.com/video/123",
            authorName = "西关焖烧男",
            textBody = "食材：牛肉 400克",
            importedAt = 1_000,
            fetchStatus = "OK",
        )
        val ingredients = listOf(
            IngredientEntity("i1", recipeId, 0, null, "牛肉", 400.0, "克", "牛肉：400克", true),
            IngredientEntity("i2", recipeId, 1, null, "土豆", 300.0, "克", "土豆：300克", true),
        )
        val steps = listOf(
            StepEntity("s1", recipeId, 0, "切块", 60, null, 0),
            StepEntity("s2", recipeId, 1, "下锅翻炒", null, null, 12_000),
        )
        val media = listOf(
            MediaEntity(
                "m1", recipeId, "COVER", "recipes/$recipeId/cover.webp", "image/webp",
                1000, 800, null, 1024, "sha", null, true, 1_000,
            ),
        )
        repo.saveRecipe(recipe, ingredients, steps, source, media)
    }

    @Test
    fun `save and observe recipe details`() = runTest {
        seed()
        val details = repo.observeDetails(recipeId).first()!!
        assertEquals("孜然土豆牛肉", details.recipe.title)
        assertEquals("西关焖烧男", details.source?.authorName)
        assertEquals(2, details.ingredients.size)
        assertEquals("牛肉", details.ingredients[0].name)
        assertEquals(2, details.steps.size)
        assertEquals(1, details.media.size)
    }

    @Test
    fun `search matches ingredient name`() = runTest {
        seed()
        val byIngredient = repo.search("土豆").first()
        assertEquals(1, byIngredient.size)
        assertEquals(recipeId, byIngredient.first().id)

        val byAuthor = repo.search("西关").first()
        assertEquals(1, byAuthor.size)

        assertEquals(0, repo.search("不存在").first().size)
    }

    @Test
    fun `record cook updates last cooked and logs`() = runTest {
        seed()
        repo.recordCook(recipeId, servings = 3, rating = 5, notes = "多放了孜然")
        val details = repo.observeDetails(recipeId).first()!!
        assertEquals(1_000L, details.recipe.lastCookedAt)
        val logs = repo.observeCookLogs(recipeId).first()
        assertEquals(1, logs.size)
        assertEquals(5, logs[0].rating)
        assertEquals(3, logs[0].servings)
        assertEquals("多放了孜然", logs[0].notes)
        assertTrue(repo.observeRecentlyCooked().first().any { it.id == recipeId })
    }

    @Test
    fun `favorite and unfavorite`() = runTest {
        seed()
        repo.setFavorite(recipeId, true)
        assertEquals(true, repo.observeDetails(recipeId).first()!!.recipe.favorite)
        repo.setFavorite(recipeId, false)
        assertEquals(false, repo.observeDetails(recipeId).first()!!.recipe.favorite)
    }

    @Test
    fun `adds ingredients to shopping list without duplicates`() = runTest {
        seed()
        val ingredients = repo.observeDetails(recipeId).first()!!.ingredients
        repo.addIngredientsToShopping(recipeId, ingredients)
        repo.addIngredientsToShopping(recipeId, ingredients)
        val list = repo.ensureShoppingList()
        val items = repo.observeShoppingItems(list.id).first()
        assertEquals(2, items.size)
        assertTrue(items.any { it.name == "牛肉" && it.amount == 400.0 })
    }

    @Test
    fun `delete recipe removes details and files`() = runTest {
        seed()
        store.writeTextAtomic(recipeId, "recipe.json", "{}")
        repo.deleteRecipe(recipeId)
        assertNull(repo.details(recipeId))
        assertTrue(repo.search("土豆").first().isEmpty())
        assertTrue(!store.recipeDir(recipeId).exists())
    }

    @Test
    fun `suggestions prefer never cooked`() = runTest {
        seed()
        val suggestions = repo.observeSuggestions(3).first()
        assertEquals(1, suggestions.size)
        assertNull(suggestions.first().lastCookedAt)
    }

    @Test
    fun `save writes portable snapshot next to media`() = runTest {
        seed()
        val snapshotFile = store.recipeDir(recipeId).resolve(com.weiji.wang.core.storage.RecipeSnapshot.FILE_NAME)
        assertTrue(snapshotFile.exists())
        val snapshot = com.weiji.wang.core.storage.RecipeSnapshot.parse(snapshotFile.readText())
        assertNotNull(snapshot)
        assertEquals("孜然土豆牛肉", snapshot!!.recipe.title)
        assertEquals(2, snapshot.ingredients.size)
        assertEquals("西关焖烧男", snapshot.source?.authorName)
    }
}
