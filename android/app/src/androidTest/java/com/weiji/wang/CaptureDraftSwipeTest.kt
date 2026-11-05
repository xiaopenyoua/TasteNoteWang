package com.weiji.wang

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.weiji.wang.core.data.db.DraftEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CaptureDraftSwipeTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val app: WeijiApplication
        get() = ApplicationProvider.getApplicationContext()

    private val draftId = "01TESTDRAFT00000000000000"

    @Before
    fun seed() = runBlocking {
        withContext(Dispatchers.IO) {
            val container = app.container
            container.database.clearAllTables()
            container.database.draftDao().upsert(
                DraftEntity(
                    id = draftId,
                    shareText = "测试草稿",
                    url = "https://v.douyin.com/test/",
                    platform = "DOUYIN",
                    state = "FAILED",
                    error = "解析失败",
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                ),
            )
        }
    }

    @Test
    fun recentImportSwipesLeftToDelete() {
        compose.onAllNodesWithText("菜谱").onFirst().performClick()
        compose.onNodeWithContentDescription("收录").performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("测试草稿").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("测试草稿").assertIsDisplayed()

        compose.onNodeWithText("测试草稿").performTouchInput { swipeLeft() }
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("删除").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("删除").performClick()

        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("测试草稿").fetchSemanticsNodes().isEmpty()
        }
    }
}
