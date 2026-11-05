package com.weiji.wang

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.weiji.wang.core.data.db.DraftEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 2026-11-05 用户反馈：粘贴链接后不应自动解析；点「最近收录」里的解析中记录也不应重新解析。
 * 解析只能由「解析」按钮触发。
 */
@RunWith(AndroidJUnit4::class)
class CaptureManualParseTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val app: WeijiApplication
        get() = ApplicationProvider.getApplicationContext()

    @Before
    fun clearData() = runBlocking {
        withContext(Dispatchers.IO) { app.container.database.clearAllTables() }
    }

    private fun openCapture() {
        compose.onAllNodesWithText("菜谱").onFirst().performClick()
        compose.onNodeWithContentDescription("收录").performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("手动创建").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun typingLinkDoesNotStartParsingUntilButtonClicked() {
        openCapture()
        compose.onNodeWithTag("capture-input").performTextClearance()
        compose.onNodeWithTag("capture-input").performTextInput("https://v.douyin.com/7ESUV_w26M0/")

        Thread.sleep(1_500)
        compose.waitForIdle()
        assertTrue(
            "输入链接后不应自动开始解析",
            compose.onAllNodesWithText("正在读取页面…").fetchSemanticsNodes().isEmpty(),
        )

        compose.onNodeWithText("解析").performClick()
        compose.waitUntil(3_000) {
            compose.onAllNodesWithText("正在读取页面…").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun clickingResolvingDraftFillsInputWithoutParsing() {
        val shareText = "https://v.douyin.com/wrCuEvuKwVk/"
        runBlocking {
            withContext(Dispatchers.IO) {
                app.container.database.draftDao().upsert(
                    DraftEntity(
                        id = "01TESTDRAFTMANUAL000000000",
                        shareText = shareText,
                        url = shareText,
                        platform = "DOUYIN",
                        state = "RESOLVING",
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis(),
                    ),
                )
            }
        }

        openCapture()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText(shareText).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText(shareText).performClick()
        compose.waitForIdle()

        val text = compose.onNodeWithTag("capture-input")
            .fetchSemanticsNode().config[SemanticsProperties.EditableText].text
        assertEquals(shareText, text)
        assertTrue(
            "点击解析中的记录不应触发解析",
            compose.onAllNodesWithText("正在读取页面…").fetchSemanticsNodes().isEmpty(),
        )
    }
}
