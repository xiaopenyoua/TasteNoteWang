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
                textBody = "食材：牛肉 400克\n步骤：切块\n下锅翻炒",
                importedAt = now,
                fetchStatus = "OK",
            )
            val ingredients = listOf(
                IngredientEntity("i1", recipeId, 0, null, "牛肉", 400.0, "克", "400克 牛肉", true),
                IngredientEntity("i2", recipeId, 1, null, "土豆", 300.0, "克", "300克 土豆", true),
            )
            val steps = listOf(
                StepEntity("s1", recipeId, 0, "切块", 60, null, null),
                StepEntity("s2", recipeId, 1, "下锅翻炒", null, null, null),
            )
            container.recipes.saveRecipe(recipe, ingredients, steps, source, emptyList())
        }
    }

    private fun openRecipes() {
        compose.onAllNodesWithText("菜谱").onFirst().performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("测试菜谱").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun openDetail() {
        openRecipes()
        compose.onAllNodesWithText("测试菜谱").onFirst().performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("步骤").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasText("第 1 步"))
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("第 1 步").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun homeShowsTodayAndNavigationWorks() {
        compose.onAllNodesWithText("今天").onFirst().assertIsDisplayed()
        openRecipes()
        compose.onAllNodesWithText("采购").onFirst().performClick()
        compose.onAllNodesWithText("采购").onFirst().assertIsDisplayed()
        compose.onAllNodesWithText("我的").onFirst().performClick()
        compose.onAllNodesWithText("我的").onFirst().assertIsDisplayed()
    }

    @Test
    fun recipeDetailShowsStepsFirstAndNoCookMode() {
        openDetail()
        compose.onNodeWithText("第 1 步").assertIsDisplayed()
        compose.onNodeWithText("步骤").assertIsDisplayed()
        compose.onNodeWithText("食材").assertIsDisplayed()
        compose.onNodeWithText("原文").assertIsDisplayed()
        compose.onNodeWithText("开始做").assertDoesNotExist()
    }

    @Test
    fun addToTodayUpdatesHomeAndShopping() {
        openDetail()
        compose.onNodeWithText("食材").performClick()
        compose.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasText("今晚做这道"))
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("今晚做这道").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("今晚做这道").performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("已加入今天吃什么").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onAllNodesWithText("今天").onFirst().performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("测试菜谱").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onAllNodesWithText("测试菜谱").onFirst().assertIsDisplayed()
        compose.onAllNodesWithText("采购").onFirst().performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("牛肉").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun historyScreenOpens() {
        compose.onNodeWithContentDescription("吃过的菜").performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("按天回看每天的菜单").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("按天回看每天的菜单").assertIsDisplayed()
    }

    @Test
    fun searchFiltersRecipes() {
        openRecipes()
        compose.onNodeWithText("搜菜名、食材、作者").performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onAllNodes(hasSetTextAction()).onFirst().performTextInput("土豆")
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("测试菜谱").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onAllNodesWithText("测试菜谱").onFirst().assertIsDisplayed()
    }

    @Test
    fun shoppingAddsManualItemAndOpensEditor() {
        compose.onAllNodesWithText("采购").onFirst().performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithContentDescription("添加采购项").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithContentDescription("添加采购项").performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size >= 2
        }
        compose.onAllNodes(hasSetTextAction())[0].performTextInput("牛肉")
        compose.onAllNodes(hasSetTextAction())[1].performTextInput("500")
        compose.onNodeWithText("添加").performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("牛肉").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasText("牛肉"))
        compose.onAllNodesWithText("牛肉").onFirst().performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("编辑采购项").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("编辑采购项").assertIsDisplayed()
    }

    @Test
    fun editScreenSavesAndReturnsToDetail() {
        openDetail()
        compose.onNodeWithContentDescription("编辑").performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("保存").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onAllNodesWithText("保存").onFirst().performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("步骤").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasText("第 1 步"))
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("第 1 步").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("第 1 步").assertIsDisplayed()
    }

    @Test
    fun favoriteToggles() {
        openDetail()
        compose.onNodeWithContentDescription("收藏").performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithContentDescription("取消收藏").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithContentDescription("取消收藏").assertIsDisplayed()
    }

    @Test
    fun captureScreenOpensFromFab() {
        openRecipes()
        compose.onNodeWithContentDescription("收录").performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("解析").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("解析").assertIsDisplayed()
    }

    @Test
    fun shoppingEmptyStateIsShownBeforeAdding() {
        compose.onAllNodesWithText("采购").onFirst().performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("采购清单是空的").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("采购清单是空的").assertIsDisplayed()
    }
}
