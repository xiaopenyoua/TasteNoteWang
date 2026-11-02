package com.weiji.wang

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.weiji.wang.core.data.db.IngredientEntity
import com.weiji.wang.core.data.db.RecipeEntity
import com.weiji.wang.core.data.db.SourceEntity
import com.weiji.wang.core.data.db.StepEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainFlowTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val app: WeijiApplication
        get() = ApplicationProvider.getApplicationContext()

    private val recipeId = "01TESTRECIPE0000000000000"
    private val now = System.currentTimeMillis()

    @Before
    fun seed() = runBlocking {
        withContext(Dispatchers.IO) {
            val container = app.container
            container.database.clearAllTables()
            val recipe = RecipeEntity(
                id = recipeId,
                title = "测试菜谱",
                subtitle = "@测试作者",
                servings = 2,
                totalMinutes = 5,
                sourceId = "src1",
                createdAt = now,
                updatedAt = now,
            )
            val source = SourceEntity(
                id = "src1",
                recipeId = recipeId,
                platform = "DOUYIN",
                originalUrl = "https://v.douyin.com/test/",
                canonicalUrl = "https://www.douyin.com/video/1",
                authorName = "测试作者",
                textBody = "食材：牛肉 400克",
                importedAt = now,
                fetchStatus = "OK",
            )
            val ingredients = listOf(
                IngredientEntity("i1", recipeId, 0, null, "牛肉", 400.0, "克", "牛肉：400克", true),
                IngredientEntity("i2", recipeId, 1, null, "土豆", 300.0, "克", "土豆：300克", true),
            )
            val steps = listOf(
                StepEntity("s1", recipeId, 0, "切块", 60, null, null),
                StepEntity("s2", recipeId, 1, "下锅翻炒", null, null, null),
            )
            container.recipes.saveRecipe(recipe, ingredients, steps, source, emptyList())
        }
    }

    @Test
    fun homeShowsTodayAndNavigationWorks() {
        compose.onAllNodesWithText("今天").onFirst().assertIsDisplayed()
        compose.onAllNodesWithText("菜谱").onFirst().performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("测试菜谱").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onAllNodesWithText("采购").onFirst().performClick()
        compose.onAllNodesWithText("采购").onFirst().assertIsDisplayed()
        compose.onAllNodesWithText("我的").onFirst().performClick()
        compose.onAllNodesWithText("我的").onFirst().assertIsDisplayed()
    }

    @Test
    fun opensRecipeAndCompletesCookFlow() {
        compose.onAllNodesWithText("菜谱").onFirst().performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("测试菜谱").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onAllNodesWithText("测试菜谱").onFirst().performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("开始做").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onAllNodesWithText("开始做").onFirst().performClick()

        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("第 1 步").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("第 1 步").assertIsDisplayed()
        compose.onNodeWithText("下一步").performClick()
        compose.waitUntil(3_000) {
            compose.onAllNodesWithText("第 2 步").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("做完了").performClick()
        compose.waitUntil(3_000) {
            compose.onAllNodesWithText("记录这次").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("保存").performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("开始做").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("开始做").assertIsDisplayed()
    }

    @Test
    fun captureScreenOpensFromFab() {
        compose.onNodeWithContentDescription("收录").performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("收录").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("解析").assertIsDisplayed()
        compose.onNodeWithText("手动创建").assertIsDisplayed()
    }

    @Test
    fun shoppingEmptyStateIsShown() {
        compose.onAllNodesWithText("采购").onFirst().performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("采购清单是空的").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("采购清单是空的").assertIsDisplayed()
    }

    @Test
    fun searchFiltersRecipes() {
        compose.onAllNodesWithText("菜谱").onFirst().performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("测试菜谱").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNode(hasSetTextAction()).performTextInput("土豆")
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("测试菜谱").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onAllNodesWithText("测试菜谱").onFirst().assertIsDisplayed()
    }

    @Test
    fun favoriteAndAddToShopping() {
        compose.onAllNodesWithText("菜谱").onFirst().performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("测试菜谱").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onAllNodesWithText("测试菜谱").onFirst().performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithContentDescription("收藏").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onAllNodesWithContentDescription("收藏").onFirst().performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithContentDescription("取消收藏").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onAllNodes(hasScrollAction()).onFirst()
            .performScrollToNode(hasText("全部加入采购清单"))
        compose.onAllNodesWithText("全部加入采购清单").onFirst().performClick()
        compose.onAllNodesWithText("采购").onFirst().performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("牛肉").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onAllNodesWithText("牛肉").onFirst().assertIsDisplayed()
        compose.onAllNodesWithText("土豆").onFirst().assertIsDisplayed()
    }

    @Test
    fun editScreenSavesAndReturnsToDetail() {
        compose.onAllNodesWithText("菜谱").onFirst().performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("测试菜谱").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onAllNodesWithText("测试菜谱").onFirst().performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithContentDescription("编辑").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onAllNodesWithContentDescription("编辑").onFirst().performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("保存").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onAllNodesWithText("保存").onFirst().performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("开始做").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("开始做").assertIsDisplayed()
    }

    @Test
    fun deleteRecipeRemovesItFromList() {
        compose.onAllNodesWithText("菜谱").onFirst().performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("测试菜谱").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onAllNodesWithText("测试菜谱").onFirst().performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithContentDescription("删除").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onAllNodesWithContentDescription("删除").onFirst().performClick()
        compose.waitUntil(3_000) {
            compose.onAllNodesWithText("删除这道菜谱？").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("删除").performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("测试菜谱").fetchSemanticsNodes().isEmpty()
        }
    }
}
