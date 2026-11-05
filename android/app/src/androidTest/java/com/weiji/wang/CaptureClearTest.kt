package com.weiji.wang

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CaptureClearTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val app: WeijiApplication
        get() = ApplicationProvider.getApplicationContext()

    @Before
    fun clearData() = runBlocking {
        withContext(Dispatchers.IO) { app.container.database.clearAllTables() }
    }

    @Test
    fun clearButtonEmptiesInput() {
        compose.onAllNodesWithText("菜谱").onFirst().performClick()
        compose.onNodeWithContentDescription("收录").performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("手动创建").fetchSemanticsNodes().isNotEmpty()
        }

        compose.onNodeWithTag("capture-input").performTextInput("临时输入")
        val before = compose.onNodeWithTag("capture-input")
            .fetchSemanticsNode().config[SemanticsProperties.EditableText].text
        assertEquals("临时输入", before)

        compose.onNodeWithText("清空").assertIsDisplayed().performClick()
        val after = compose.onNodeWithTag("capture-input")
            .fetchSemanticsNode().config[SemanticsProperties.EditableText].text
        assertEquals("", after)
        compose.onNodeWithText("解析").assertIsDisplayed()
    }
}
