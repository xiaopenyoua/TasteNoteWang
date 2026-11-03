package com.weiji.wang.feature.recipe

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import android.os.Build
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
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
import com.weiji.wang.ui.LocalContainer
import com.weiji.wang.ui.components.CircleIconButton

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
                }
            },
            update = { view -> view.player = player },
        )
        if (onFullscreen != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onFullscreen),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center,
                ) {
                    FullscreenGlyph(modifier = Modifier.size(16.dp), tint = Color.White)
                }
            }
        }
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

/** 四角括号的全屏图标，避免依赖 material-icons-extended。 */
@Composable
internal fun FullscreenGlyph(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Canvas(modifier = modifier) {
        val stroke = 1.6.dp.toPx()
        val inset = 1.5.dp.toPx()
        val arm = size.minDimension * 0.34f
        val x0 = inset
        val y0 = inset
        val x1 = size.width - inset
        val y1 = size.height - inset
        drawLine(tint, Offset(x0, y0), Offset(x0 + arm, y0), stroke)
        drawLine(tint, Offset(x0, y0), Offset(x0, y0 + arm), stroke)
        drawLine(tint, Offset(x1, y0), Offset(x1 - arm, y0), stroke)
        drawLine(tint, Offset(x1, y0), Offset(x1, y0 + arm), stroke)
        drawLine(tint, Offset(x0, y1), Offset(x0 + arm, y1), stroke)
        drawLine(tint, Offset(x0, y1), Offset(x0, y1 - arm), stroke)
        drawLine(tint, Offset(x1, y1), Offset(x1 - arm, y1), stroke)
        drawLine(tint, Offset(x1, y1), Offset(x1, y1 - arm), stroke)
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
