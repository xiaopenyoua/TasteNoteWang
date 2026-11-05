package com.weiji.wang

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.weiji.wang.feature.recipe.VideoFullscreenDialog
import java.io.File

/**
 * 仅 debug 的播放器宿主：直接打开全屏播放器，供仪器化测试旋转、手势与铺满校验。
 *
 * 配置变更声明与 `MainActivity` 保持一致，保证旋转时不重建 Activity，和线上路径相同。
 * 测试视频来自 `src/debug/assets/player-test.mp4`（纯洋红 12 秒），首次启动拷贝到 filesDir。
 */
class PlayerHarnessActivity : ComponentActivity() {

    lateinit var player: ExoPlayer
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val video = File(filesDir, VIDEO_NAME).apply {
            parentFile?.mkdirs()
            if (!exists() || length() == 0L) {
                assets.open(VIDEO_NAME).use { input ->
                    outputStream().use { output -> input.copyTo(output) }
                }
            }
        }
        player = ExoPlayer.Builder(this).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.fromFile(video)))
            prepare()
            playWhenReady = false
        }
        setContent {
            VideoFullscreenDialog(player = player, onDismiss = {})
        }
    }

    override fun onDestroy() {
        player.release()
        super.onDestroy()
    }

    private companion object {
        const val VIDEO_NAME = "player-test.mp4"
    }
}
