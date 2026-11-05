package com.weiji.wang

import android.app.UiAutomation
import android.content.res.Configuration
import android.graphics.Bitmap
import android.media.AudioManager
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.Window
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.window.DialogWindowProvider
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs

/**
 * 全屏播放器验收：铺满屏幕（竖屏 / 横屏）、单击显隐控件、双击播放/暂停且不动控件、
 * 左右滑进度、左侧亮度、右侧音量。
 *
 * 宿主是 debug 专用的 `PlayerHarnessActivity`（configChanges 与 MainActivity 一致），
 * 视频是 12 秒纯洋红画面：画面若被 letterbox 留黑边，四角像素会是黑色，直接断言洋红即可。
 */
@RunWith(AndroidJUnit4::class)
class VideoPlayerFullscreenTest {

    @get:Rule
    val compose = createAndroidComposeRule<PlayerHarnessActivity>()

    private val activity get() = compose.activity

    /** ExoPlayer 只能在主线程访问，测试线程统一经这里读状态。 */
    private fun <T> playerValue(block: (ExoPlayer) -> T): T {
        val result = AtomicReference<T>()
        compose.runOnUiThread { result.set(block(activity.player)) }
        return result.get()
    }

    @Test
    fun fullscreenFillsScreenInPortraitAndLandscape() {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        try {
            automation.setRotation(UiAutomation.ROTATION_FREEZE_0)
            waitForOrientation(Configuration.ORIENTATION_PORTRAIT)
            renderFirstFrameAndHideController()

            val portrait = screenshot()
            assertVideoFills(portrait, "竖屏")
            assertPlayerMatchesScreen(portrait, "竖屏")

            automation.setRotation(UiAutomation.ROTATION_FREEZE_90)
            waitForOrientation(Configuration.ORIENTATION_LANDSCAPE)
            // 等窗口完成旋转后的重新布局与首帧重绘。
            Thread.sleep(1_200)
            onPlayer { it.hideController() }
            Thread.sleep(300)

            val landscape = screenshot()
            assertVideoFills(landscape, "横屏")
            assertPlayerMatchesScreen(landscape, "横屏")
        } finally {
            automation.setRotation(UiAutomation.ROTATION_FREEZE_0)
        }
    }

    @Test
    fun singleTapTogglesControlsAndProgressBar() {
        onPlayer { it.hideController() }
        Thread.sleep(200)
        assertFalse("初始应为隐藏", playerView().isControllerFullyVisible)

        tapVideo(0.5f, 0.3f)
        Thread.sleep(500)
        assertTrue("单击后应出现控件与进度条", playerView().isControllerFullyVisible)

        tapVideo(0.5f, 0.3f)
        Thread.sleep(500)
        assertFalse("再次单击应收起控件", playerView().isControllerFullyVisible)
    }

    @Test
    fun doubleTapTogglesPlaybackWithoutTouchingControls() {
        onPlayer { it.hideController() }
        Thread.sleep(200)
        assertFalse(playerView().isControllerFullyVisible)

        doubleTapVideo(0.5f, 0.5f)
        Thread.sleep(350)
        assertTrue("双击应开始播放", playerValue { it.playWhenReady })
        assertFalse("控件隐藏时双击，控件仍应隐藏", playerView().isControllerFullyVisible)

        onPlayer { it.showController() }
        Thread.sleep(200)
        assertTrue(playerView().isControllerFullyVisible)

        doubleTapVideo(0.5f, 0.4f)
        Thread.sleep(350)
        assertFalse("双击应暂停", playerValue { it.playWhenReady })
        assertTrue("控件显示时双击，控件仍应显示", playerView().isControllerFullyVisible)
    }

    @Test
    fun horizontalDragSeeks() {
        waitForDuration()
        playerValue { it.seekTo(0) }
        Thread.sleep(300)
        onPlayer { it.hideController() }
        Thread.sleep(150)

        val duration = playerValue { it.duration }
        swipe(fromX = 0.5f, fromY = 0.5f, toX = 0.9f, toY = 0.5f)
        Thread.sleep(400)

        val position = playerValue { it.currentPosition }
        val lowerBound = (duration * 0.2).toLong()
        val upperBound = (duration * 0.6).toLong()
        assertTrue(
            "左右滑应前进约 40% 时长，实际 ${position}ms / ${duration}ms",
            position in lowerBound..upperBound,
        )
    }

    @Test
    fun verticalDragOnLeftChangesBrightness() {
        val window = checkNotNull(dialogWindow(playerView())) { "找不到播放器所在 Dialog 的 Window" }
        compose.runOnUiThread {
            val attrs = window.attributes
            attrs.screenBrightness = 0.3f
            window.attributes = attrs
        }
        onPlayer { it.hideController() }
        Thread.sleep(150)

        swipe(fromX = 0.15f, fromY = 0.8f, toX = 0.15f, toY = 0.2f)
        Thread.sleep(300)

        val brightness = window.attributes.screenBrightness
        assertTrue("左 1/3 上滑应提高亮度，实际 $brightness", brightness > 0.6f)
        compose.onNodeWithText("亮度", substring = true).assertExists()
    }

    @Test
    fun verticalDragOnRightChangesVolume() {
        val audio = activity.getSystemService(AudioManager::class.java)
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val start = (max / 3).coerceAtLeast(1)
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, start, 0)
        onPlayer { it.hideController() }
        Thread.sleep(150)

        swipe(fromX = 0.85f, fromY = 0.8f, toX = 0.85f, toY = 0.2f)
        Thread.sleep(300)

        val after = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
        assertTrue("右 1/3 上滑应提高音量：$start -> $after（max=$max）", after > start)
        compose.onNodeWithText("音量", substring = true).assertExists()
    }

    // ── 工具 ────────────────────────────────────────────────────────────

    private fun playerView(): PlayerView {
        var result: PlayerView? = null
        onView(isAssignableFrom(PlayerView::class.java))
            .inRoot(isDialog())
            .check { view, _ -> result = view as PlayerView }
        return checkNotNull(result) { "全屏 Dialog 里没有 PlayerView" }
    }

    private fun onPlayer(block: (PlayerView) -> Unit) {
        onView(isAssignableFrom(PlayerView::class.java))
            .inRoot(isDialog())
            .perform(object : ViewAction {
                override fun getConstraints() = isAssignableFrom(PlayerView::class.java)
                override fun getDescription() = "在播放器上执行操作"
                override fun perform(uiController: UiController, view: View) {
                    block(view as PlayerView)
                }
            })
    }

    private fun tapVideo(xFraction: Float, yFraction: Float) {
        val view = playerView()
        val t = SystemClock.uptimeMillis()
        val x = view.width * xFraction
        val y = view.height * yFraction
        compose.runOnUiThread {
            dispatch(
                view,
                MotionEvent.obtain(t, t, MotionEvent.ACTION_DOWN, x, y, 0),
                MotionEvent.obtain(t, t + 40, MotionEvent.ACTION_UP, x, y, 0),
            )
        }
    }

    private fun doubleTapVideo(xFraction: Float, yFraction: Float) {
        val view = playerView()
        val t = SystemClock.uptimeMillis()
        val x = view.width * xFraction
        val y = view.height * yFraction
        compose.runOnUiThread {
            dispatch(
                view,
                MotionEvent.obtain(t, t, MotionEvent.ACTION_DOWN, x, y, 0),
                MotionEvent.obtain(t, t + 40, MotionEvent.ACTION_UP, x, y, 0),
                MotionEvent.obtain(t, t + 140, MotionEvent.ACTION_DOWN, x, y, 0),
                MotionEvent.obtain(t, t + 180, MotionEvent.ACTION_UP, x, y, 0),
            )
        }
    }

    private fun swipe(
        fromX: Float,
        fromY: Float,
        toX: Float,
        toY: Float,
        steps: Int = 12,
        durationMs: Long = 240,
    ) {
        val view = playerView()
        val startX = view.width * fromX
        val startY = view.height * fromY
        val endX = view.width * toX
        val endY = view.height * toY
        val t = SystemClock.uptimeMillis()
        val events = mutableListOf<MotionEvent>()
        events += MotionEvent.obtain(t, t, MotionEvent.ACTION_DOWN, startX, startY, 0)
        for (i in 1..steps) {
            val f = i / steps.toFloat()
            events += MotionEvent.obtain(
                t,
                t + (durationMs * f).toLong(),
                MotionEvent.ACTION_MOVE,
                startX + (endX - startX) * f,
                startY + (endY - startY) * f,
                0,
            )
        }
        events += MotionEvent.obtain(t, t + durationMs + 16, MotionEvent.ACTION_UP, endX, endY, 0)
        compose.runOnUiThread { dispatch(view, *events.toTypedArray()) }
    }

    private fun dispatch(view: PlayerView, vararg events: MotionEvent) {
        events.forEach { event ->
            view.dispatchTouchEvent(event)
            event.recycle()
        }
    }

    private fun renderFirstFrameAndHideController() {
        playerValue { it.playWhenReady = true }
        waitUntil(10_000) {
            playerValue { it.currentPosition > 400 && it.videoSize.height > 0 }
        }
        playerValue { it.playWhenReady = false }
        onPlayer { it.hideController() }
        Thread.sleep(600)
    }

    private fun waitForDuration() {
        waitUntil(10_000) { playerValue { it.duration > 0 } }
    }

    private fun screenshot(): Bitmap = checkNotNull(
        InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot(),
    ) { "截屏失败" }

    private fun assertVideoFills(bitmap: Bitmap, label: String) {
        val samples = listOf(
            2 to 2,
            bitmap.width - 3 to 2,
            2 to bitmap.height - 3,
            bitmap.width - 3 to bitmap.height - 3,
            bitmap.width / 2 to 2,
            bitmap.width / 2 to bitmap.height - 3,
        )
        samples.forEach { (x, y) ->
            val color = bitmap.getPixel(x, y)
            val r = (color shr 16) and 0xFF
            val g = (color shr 8) and 0xFF
            val b = color and 0xFF
            assertTrue(
                String.format(
                    Locale.US,
                    "%s (%d,%d) 不是视频画面而是黑边：#%06X",
                    label, x, y, color and 0xFFFFFF,
                ),
                r > 150 && b > 150 && g < 110,
            )
        }
    }

    private fun assertPlayerMatchesScreen(bitmap: Bitmap, label: String) {
        val view = playerView()
        assertTrue(
            "$label PlayerView 未铺满：view=${view.width}x${view.height} screen=${bitmap.width}x${bitmap.height}",
            abs(view.width - bitmap.width) <= 2 && abs(view.height - bitmap.height) <= 2,
        )
    }

    private fun dialogWindow(view: View): Window? {
        var current: View? = view
        while (current != null) {
            if (current is DialogWindowProvider) return current.window
            current = current.parent as? View
        }
        return (view.rootView as? DialogWindowProvider)?.window
    }

    private fun waitForOrientation(orientation: Int) {
        waitUntil(10_000) { activity.resources.configuration.orientation == orientation }
    }

    private fun waitUntil(timeoutMs: Long, condition: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        while (SystemClock.uptimeMillis() < deadline) {
            if (condition()) return
            Thread.sleep(50)
        }
        throw AssertionError("等待条件超时 ${timeoutMs}ms")
    }
}
