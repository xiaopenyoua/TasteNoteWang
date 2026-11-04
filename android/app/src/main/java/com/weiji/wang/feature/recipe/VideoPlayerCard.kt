package com.weiji.wang.feature.recipe

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
// 工程开了 nonTransitiveRClass，exo_* 控件 id 只在 media3 自己的 R 里。
import androidx.media3.ui.R
import com.weiji.wang.ui.LocalContainer
import com.weiji.wang.ui.components.CircleIconButton
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

    Box(modifier = modifier.clip(RoundedCornerShape(14.dp))) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = true
                    controllerShowTimeoutMs = 3_000
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
                view.findViewById<View>(R.id.exo_fullscreen)?.visibility =
                    if (currentOnFullscreen != null) View.VISIBLE else View.GONE
            },
        )
    }
}

/** 全屏视频：复用传入的 ExoPlayer，不重建、不释放，退出后内联播放器继续。 */
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun VideoFullscreenDialog(player: ExoPlayer, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        ImmersiveSystemBars()
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        this.player = player
                        useController = true
                        controllerShowTimeoutMs = 3_000
                        setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                        setShowNextButton(false)
                        setShowPreviousButton(false)
                        setShowShuffleButton(false)
                        setShowSubtitleButton(false)
                        setShowVrButton(false)
                        // 全屏里只留顶部关闭按钮：不再显示播放器自带的全屏按钮。
                        applyLocalVideoChrome(showFullscreen = false)
                    }
                },
                update = { view -> view.player = player },
            )
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
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
            ),
        ) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black),
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

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
