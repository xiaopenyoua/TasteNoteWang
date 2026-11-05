package com.weiji.wang.feature.recipe

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.Window
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
// 工程开了 nonTransitiveRClass，exo_* 控件 id 只在 media3 自己的 R 里。
import androidx.media3.ui.R
import com.weiji.wang.ui.LocalContainer
import com.weiji.wang.ui.components.CircleIconButton
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 本地视频播放：ExoPlayer + 系统控制器。
 * 只播放已下载到本机的文件，不做网络流；由调用方持有 player，退出页面即释放。
 */
@Composable
fun rememberVideoPlayer(relativePath: String?): ExoPlayer? {
    val context = LocalContext.current
    val container = LocalContainer.current
    val file = remember(relativePath, container) {
        relativePath?.let { path ->
            container.files.resolve(path).takeIf { it.exists() && it.length() > 0L }
        }
    }
    val player = remember(file?.absolutePath, context) {
        file?.let {
            ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(Uri.fromFile(it)))
                prepare()
                playWhenReady = false
            }
        }
    }
    DisposableEffect(player) {
        onDispose {
            player?.stop()
            player?.release()
        }
    }
    return player
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun VideoPlayerCard(
    player: ExoPlayer?,
    modifier: Modifier = Modifier,
    onFullscreen: (() -> Unit)? = null,
) {
    if (player == null) {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "视频文件不存在，可在编辑页重新导入",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )
        }
        return
    }

    val currentOnFullscreen by rememberUpdatedState(onFullscreen)

    Box(modifier = modifier.clip(RoundedCornerShape(14.dp)).background(Color.Black)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = true
                    controllerShowTimeoutMs = 3_000
                    // 卡片保持原始比例：视频完整可见，多余区域露黑底，不裁画面。
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                    setShowNextButton(false)
                    setShowPreviousButton(false)
                    setShowShuffleButton(false)
                    setShowSubtitleButton(false)
                    setShowVrButton(false)
                    // 全屏只用播放器底栏自带的按钮（右下角），不再叠加 Compose 按钮。
                    // 回调始终注册，但内部读最新状态；没有回调时按钮在收敛时置 GONE。
                    setFullscreenButtonClickListener { currentOnFullscreen?.invoke() }
                    applyLocalVideoChrome(showFullscreen = currentOnFullscreen != null)
                }
            },
            update = { view ->
                view.player = player
                view.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                view.findViewById<View>(R.id.exo_fullscreen)?.visibility =
                    if (currentOnFullscreen != null) View.VISIBLE else View.GONE
            },
        )
    }
}

/**
 * 播放器手势（只用于全屏）：
 * - 单击：显示/隐藏操作按钮与进度条。用 onSingleTapConfirmed 判定，双击不会被拆成两次单击；
 * - 双击：播放/暂停，不改变控件显隐；控件正显示时只重置自动隐藏计时；
 * - 左侧 1/3 上下滑亮度、右侧 1/3 上下滑音量，中间 1/3 不响应上下滑；
 * - 任意位置左右滑调整进度。
 *
 * 落在进度条、播放按钮等可见控件上的触摸原样交给控件处理，不参与上面的手势。
 * 同时关掉 PlayerView 自带的触摸显隐，避免它把双击解释成两次显隐切换。
 */
@androidx.annotation.OptIn(UnstableApi::class)
private class GesturePlayerView(context: Context) : PlayerView(context) {

    var onDoubleTap: (() -> Unit)? = null
    var onVerticalDragStart: ((Int) -> Unit)? = null
    var onVerticalDrag: ((Int, Float) -> Unit)? = null
    var onSeekStart: (() -> Unit)? = null
    var onSeek: ((Float) -> Unit)? = null

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var downX = 0f
    private var downY = 0f
    private var mode = MODE_NONE
    private var zone = ZONE_NONE
    private var gestureOwnedByControls = false

    private val detector = GestureDetector(
        context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                if (isControllerFullyVisible) hideController() else showController()
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                onDoubleTap?.invoke()
                // 双击只切播放/暂停；控件若正显示，仅续上自动隐藏计时，不改变显隐。
                if (isControllerFullyVisible) showController()
                return true
            }
        },
    )

    init {
        // 显隐完全由单击决定：双击不弹控件，播放/暂停也不自动弹控件。
        setControllerHideOnTouch(false)
        setControllerAutoShow(false)
    }

    /**
     * 吞掉未被子控件消费的触摸：View 默认在 ACTION_UP 调 performClick，
     * 而 PlayerView.performClick 会切控件显隐，让单击在 300ms 判定前先闪一下。
     */
    override fun onTouchEvent(event: MotionEvent): Boolean = true

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                mode = MODE_NONE
                zone = ZONE_NONE
                gestureOwnedByControls = isOnVisibleControls(event.x, event.y)
                if (!gestureOwnedByControls) detector.onTouchEvent(event)
            }

            MotionEvent.ACTION_MOVE -> if (!gestureOwnedByControls) {
                detector.onTouchEvent(event)
                val dx = event.x - downX
                val dy = event.y - downY
                if (mode == MODE_NONE) {
                    if (abs(dx) > touchSlop && abs(dx) > abs(dy)) {
                        mode = MODE_SEEK
                        // 以越过 touchSlop 的位置为起点，避免开头一段无效位移。
                        downX = event.x
                        onSeekStart?.invoke()
                    } else if (abs(dy) > touchSlop && abs(dy) > abs(dx)) {
                        mode = MODE_VERTICAL
                        downY = event.y
                        zone = when {
                            downX < width / 3f -> ZONE_BRIGHTNESS
                            downX > width * 2f / 3f -> ZONE_VOLUME
                            else -> ZONE_NONE
                        }
                        onVerticalDragStart?.invoke(zone)
                    }
                }
                when (mode) {
                    MODE_SEEK -> if (width > 0) onSeek?.invoke((event.x - downX) / width)
                    MODE_VERTICAL -> if (height > 0) {
                        onVerticalDrag?.invoke(zone, (downY - event.y) / height)
                    }
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (!gestureOwnedByControls) detector.onTouchEvent(event)
                mode = MODE_NONE
                zone = ZONE_NONE
                gestureOwnedByControls = false
            }
        }
        return super.dispatchTouchEvent(event)
    }

    /** 控件可见时，进度条与按钮所在的矩形直接交给控件。 */
    private fun isOnVisibleControls(x: Float, y: Float): Boolean {
        if (!isControllerFullyVisible) return false
        val targets = intArrayOf(
            R.id.exo_bottom_bar,
            R.id.exo_progress,
            R.id.exo_center_controls,
            R.id.exo_minimal_controls,
        )
        return targets.any { id ->
            val view = findViewById<View>(id)
            view != null && view.isShown && contains(view, x, y)
        }
    }

    private fun contains(view: View, x: Float, y: Float): Boolean {
        val target = IntArray(2)
        val self = IntArray(2)
        view.getLocationOnScreen(target)
        getLocationOnScreen(self)
        val left = (target[0] - self[0]).toFloat()
        val top = (target[1] - self[1]).toFloat()
        return x >= left && x <= left + view.width && y >= top && y <= top + view.height
    }

    private companion object {
        const val MODE_NONE = 0
        const val MODE_VERTICAL = 1
        const val MODE_SEEK = 2
        const val ZONE_NONE = 0
        const val ZONE_BRIGHTNESS = 1
        const val ZONE_VOLUME = 2
    }
}

/** 全屏 Dialog 的窗口与整屏内容尺寸。 */
private data class FullscreenDialogFrame(val window: Window?, val size: DpSize)

/**
 * 把当前 Dialog 窗口铺到屏幕物理四边，并给出内容应使用的整屏尺寸。
 *
 * Compose 的 Dialog 内容测量上限是 Configuration 的可用区域（不含系统栏），只设
 * `MATCH_PARENT` 仍会在状态栏/导航栏位置各留一条底色。这里同时做四件事：窗口
 * MATCH_PARENT、`FLAG_LAYOUT_IN_SCREEN`、忽略系统栏 inset、刘海区域 ALWAYS；
 * 调用方再用返回的 size 以 `requiredSize` 撑开内容，画面才真正对齐屏幕四边。
 */
@Composable
private fun rememberFullscreenDialogFrame(): FullscreenDialogFrame {
    val context = LocalContext.current
    val localView = LocalView.current
    val configuration = LocalConfiguration.current
    // Dialog 有独立 Window：亮度、布局都要写它，不是 Activity 的 Window。
    val window = remember(localView) {
        localView.findDialogWindow() ?: context.findActivity()?.window
    }
    LaunchedEffect(window, configuration.orientation, configuration.screenWidthDp) {
        window?.let { w ->
            w.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN)
            val attrs = w.attributes
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                attrs.setFitInsetsTypes(0)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                attrs.layoutInDisplayCutoutMode =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                    } else {
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                    }
            }
            w.attributes = attrs
            w.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
            )
        }
    }
    val widthPx: Int
    val heightPx: Int
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val bounds = context.getSystemService(WindowManager::class.java)
            .currentWindowMetrics.bounds
        widthPx = bounds.width()
        heightPx = bounds.height()
    } else {
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        context.getSystemService(WindowManager::class.java).defaultDisplay.getRealMetrics(metrics)
        widthPx = metrics.widthPixels
        heightPx = metrics.heightPixels
    }
    val size = with(LocalDensity.current) { DpSize(widthPx.toDp(), heightPx.toDp()) }
    return FullscreenDialogFrame(window, size)
}

/** 全屏视频：复用传入的 ExoPlayer，不重建、不释放，退出后内联播放器继续。 */
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun VideoFullscreenDialog(player: ExoPlayer, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            // 用窗口的实际测量规格；窗口随后被设成 MATCH_PARENT，内容才能拿到整屏尺寸。
            usePlatformDefaultWidth = true,
            decorFitsSystemWindows = false,
        ),
    ) {
        ImmersiveSystemBars()
        val frame = rememberFullscreenDialogFrame()
        val window = frame.window
        val context = LocalContext.current
        val audio = remember(context) { context.getSystemService(AudioManager::class.java) }
        val initialBrightness = remember(window) { window?.attributes?.screenBrightness ?: -1f }
        var feedback by remember { mutableStateOf<String?>(null) }
        var feedbackPercent by remember { mutableIntStateOf(-1) }
        var startBrightness by remember { mutableFloatStateOf(0.5f) }
        var startVolume by remember { mutableIntStateOf(0) }
        var seekStart by remember { mutableLongStateOf(0L) }

        DisposableEffect(window) {
            onDispose {
                window?.let {
                    val attrs = it.attributes
                    attrs.screenBrightness = initialBrightness
                    it.attributes = attrs
                }
            }
        }
        LaunchedEffect(feedback) {
            if (feedback != null) {
                delay(900)
                feedback = null
            }
        }

        Box(
            modifier = Modifier
                .requiredSize(frame.size)
                .background(Color.Black),
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    GesturePlayerView(ctx).apply {
                        this.player = player
                        useController = true
                        controllerShowTimeoutMs = 3_000
                        // 全屏只铺满“播放器”本身：视频保持比例完整显示，黑边延伸到屏幕四边。
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                        setShowNextButton(false)
                        setShowPreviousButton(false)
                        setShowShuffleButton(false)
                        setShowSubtitleButton(false)
                        setShowVrButton(false)
                        applyLocalVideoChrome(showFullscreen = false)

                        // 双击播放/暂停；单击仍交给 PlayerView 显示控件与进度条。
                        onDoubleTap = {
                            player.playWhenReady = !player.playWhenReady
                            feedbackPercent = -1
                            feedback = if (player.playWhenReady) "播放" else "暂停"
                        }
                        onVerticalDragStart = { target ->
                            if (target == 1) {
                                val attr = window?.attributes?.screenBrightness ?: -1f
                                startBrightness = if (attr >= 0f) attr else {
                                    Settings.System.getInt(
                                        context.contentResolver,
                                        Settings.System.SCREEN_BRIGHTNESS,
                                        128,
                                    ) / 255f
                                }
                            } else if (target == 2) {
                                startVolume = audio?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
                            }
                        }
                        onVerticalDrag = { target, fraction ->
                            when (target) {
                                1 -> {
                                    val next = (startBrightness + fraction * 1.5f).coerceIn(0.02f, 1f)
                                    window?.let {
                                        val attrs = it.attributes
                                        attrs.screenBrightness = next
                                        it.attributes = attrs
                                    }
                                    feedback = "亮度"
                                    feedbackPercent = (next * 100).roundToInt()
                                }
                                2 -> {
                                    val max = audio?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 0
                                    if (max > 0) {
                                        val next = (startVolume + fraction * max * 1.5f)
                                            .roundToInt().coerceIn(0, max)
                                        audio?.setStreamVolume(AudioManager.STREAM_MUSIC, next, 0)
                                        feedback = "音量"
                                        feedbackPercent = (next * 100f / max).roundToInt()
                                    }
                                }
                            }
                        }
                        onSeekStart = {
                            seekStart = player.currentPosition.coerceAtLeast(0L)
                            feedbackPercent = -1
                            feedback = "进度 ${formatClock(seekStart)}"
                        }
                        onSeek = { fraction ->
                            val duration = player.duration
                            if (duration > 0) {
                                val target = (seekStart + (fraction * duration).toLong())
                                    .coerceIn(0L, duration)
                                player.seekTo(target)
                                feedbackPercent = -1
                                feedback = "进度 ${formatClock(target)} / ${formatClock(duration)}"
                            }
                        }
                    }
                },
                update = { view ->
                    view.player = player
                    view.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                },
            )

            if (feedback != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.Black.copy(alpha = 0.62f))
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                ) {
                    Text(
                        text = if (feedbackPercent >= 0) "$feedback $feedbackPercent%" else feedback.orEmpty(),
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }

            CircleIconButton(
                icon = Icons.Filled.Close,
                contentDescription = "退出全屏",
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(8.dp),
                size = 40.dp,
                iconSize = 20.dp,
                background = Color.White.copy(alpha = 0.12f),
                tint = Color.White,
            )
        }
    }
}

/** 步骤视频的全屏播放：进入时创建 player，退出时释放。 */
@Composable
fun LocalVideoFullscreenDialog(relativePath: String, onDismiss: () -> Unit) {
    val player = rememberVideoPlayer(relativePath)
    if (player != null) {
        VideoFullscreenDialog(player = player, onDismiss = onDismiss)
    } else {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                // 与播放器同一条全屏路径；窗口随后被设成 MATCH_PARENT。
                usePlatformDefaultWidth = true,
                decorFitsSystemWindows = false,
            ),
        ) {
            ImmersiveSystemBars()
            val frame = rememberFullscreenDialogFrame()
            Box(
                modifier = Modifier
                    .requiredSize(frame.size)
                    .background(Color.Black),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "视频文件不存在",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f),
                )
                CircleIconButton(
                    icon = Icons.Filled.Close,
                    contentDescription = "关闭",
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(8.dp),
                    size = 40.dp,
                    iconSize = 20.dp,
                    background = Color.White.copy(alpha = 0.12f),
                    tint = Color.White,
                )
            }
        }
    }
}

/** 全屏播放/图片查看时隐藏系统栏；离开该窗口即恢复。 */
@Composable
internal fun ImmersiveSystemBars() {
    val view = LocalView.current
    DisposableEffect(view) {
        val platformController =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) view.windowInsetsController else null
        if (platformController != null) {
            platformController.systemBarsBehavior =
                WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            platformController.hide(WindowInsets.Type.systemBars())
            onDispose {
                platformController.show(WindowInsets.Type.systemBars())
            }
        } else {
            val window = view.context.findActivity()?.window
            if (window == null) {
                onDispose { }
            } else {
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                onDispose {
                    controller.show(WindowInsetsCompat.Type.systemBars())
                }
            }
        }
    }
}

/** 本机视频只有一条音轨：设置、倍速溢出、随机、循环、VR、字幕都收掉，只留播放/进度/时间/全屏。 */
private val hiddenChromeIds = intArrayOf(
    R.id.exo_settings,
    R.id.exo_overflow_show,
    R.id.exo_shuffle,
    R.id.exo_repeat_toggle,
    R.id.exo_vr,
    R.id.exo_subtitle,
)

/** 触控目标下限，与 Android 无障碍最小点击区域一致。 */
private const val MIN_TOUCH_DP = 48

/**
 * 收敛播放器自带控件。
 *
 * PlayerView 的控制器在 attach 之后才 inflate，且每次显示都可能重建内部状态，
 * 所以 post 一次兜底、控制器每次可见再收敛一次。按钮统一用 View.GONE 而不是 removeView：
 * 移除后 PlayerControlView 里缓存的控件引用会变成空指针。
 */
@androidx.annotation.OptIn(UnstableApi::class)
private fun PlayerView.applyLocalVideoChrome(showFullscreen: Boolean) {
    val apply = {
        hiddenChromeIds.forEach { id -> findViewById<View>(id)?.visibility = View.GONE }
        findViewById<View>(R.id.exo_fullscreen)?.apply {
            visibility = if (showFullscreen) View.VISIBLE else View.GONE
            ensureMinTouchTarget()
        }
        // 底栏 90% 不透明度 + 底部渐变遮罩：16:9 小窗里图标压得住画面。
        findViewById<View>(R.id.exo_basic_controls)?.alpha = 0.9f
        applyBottomScrim()
    }
    post { apply() }
    setControllerVisibilityListener(
        PlayerView.ControllerVisibilityListener { visibility ->
            if (visibility == View.VISIBLE) apply()
        },
    )
}

/** 把控制条背后的纯色 60% 黑底换成自下而上的渐变，按钮更清楚、画面上部不被压暗。 */
private fun PlayerView.applyBottomScrim() {
    val scrim = findViewById<View>(R.id.exo_controls_background) ?: return
    scrim.background = GradientDrawable(
        GradientDrawable.Orientation.BOTTOM_TOP,
        intArrayOf(0xB3000000.toInt(), 0x33000000, 0x00000000),
    )
}

private fun View.ensureMinTouchTarget() {
    val px = (MIN_TOUCH_DP * resources.displayMetrics.density).roundToInt()
    minimumWidth = px
    minimumHeight = px
    val params = layoutParams ?: return
    if (params.width in 1 until px) params.width = px
    if (params.height in 1 until px) params.height = px
    layoutParams = params
}

private fun formatClock(ms: Long): String {
    val totalSeconds = (ms.coerceAtLeast(0L) / 1000)
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

/** 从 Dialog 内容里的任意 View 向上找到承载它的 Dialog Window。 */
private fun View.findDialogWindow(): android.view.Window? {
    var current: View? = this
    while (current != null) {
        if (current is DialogWindowProvider) return current.window
        current = current.parent as? View
    }
    return null
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
