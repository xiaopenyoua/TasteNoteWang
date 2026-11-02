package com.weiji.wang.feature.recipe

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.weiji.wang.ui.LocalContainer

/**
 * 本地视频播放：ExoPlayer + 系统控制器。
 * 只播放已下载到本机的文件，不做网络流；退出页面即释放。
 */
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun VideoPlayerCard(
    relativePath: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val container = LocalContainer.current
    val file = remember(relativePath, container) { container.files.resolve(relativePath) }
    if (!file.exists() || file.length() == 0L) {
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

    val player = remember(file.absolutePath) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
            prepare()
            playWhenReady = false
        }
    }

    DisposableEffect(player) {
        onDispose {
            player.stop()
            player.release()
        }
    }

    AndroidView(
        modifier = modifier.clip(RoundedCornerShape(14.dp)),
        factory = { ctx ->
            PlayerView(ctx).apply {
                this.player = player
                useController = true
                setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                setShowNextButton(false)
                setShowPreviousButton(false)
            }
        },
        update = { view -> view.player = player },
    )
}
