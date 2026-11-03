package com.weiji.wang.feature.recipe

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.weiji.wang.ui.LocalContainer
import com.weiji.wang.ui.components.CircleIconButton
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * 全屏图片查看器：双指缩放 1–4、双击 2.5、拖动平移；
 * 未缩放时左右滑动翻页（同一步的全部图片）。
 */
@Composable
fun ImageViewerDialog(
    paths: List<String>,
    initialIndex: Int,
    onDismiss: () -> Unit,
) {
    if (paths.isEmpty()) return
    var index by rememberSaveable(paths, initialIndex) {
        mutableIntStateOf(initialIndex.coerceIn(0, paths.lastIndex))
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        ImmersiveSystemBars()
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            key(paths[index]) {
                ZoomableImage(
                    relativePath = paths[index],
                    modifier = Modifier.fillMaxSize(),
                    onSwipe = { direction ->
                        val next = index + direction
                        if (next in paths.indices) index = next
                    },
                )
            }
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircleIconButton(
                    icon = Icons.Filled.Close,
                    contentDescription = "关闭",
                    onClick = onDismiss,
                    size = 40.dp,
                    iconSize = 20.dp,
                    background = Color.White.copy(alpha = 0.12f),
                    tint = Color.White,
                )
            }
            if (paths.size > 1) {
                Text(
                    text = "${index + 1} / ${paths.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 24.dp),
                )
            }
        }
    }
}

private class GestureFlags {
    var accumulatedX = 0f
    var swiped = false
}

@Composable
private fun ZoomableImage(
    relativePath: String,
    modifier: Modifier = Modifier,
    onSwipe: (Int) -> Unit,
) {
    val container = LocalContainer.current
    val file = remember(relativePath, container) { container.files.resolve(relativePath) }
    val exists = remember(file) { file.exists() && file.length() > 0L }
    var scale by remember(relativePath) { mutableFloatStateOf(1f) }
    var offsetX by remember(relativePath) { mutableFloatStateOf(0f) }
    var offsetY by remember(relativePath) { mutableFloatStateOf(0f) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    val flags = remember(relativePath) { GestureFlags() }
    val scope = rememberCoroutineScope()
    val swipeThreshold = with(LocalDensity.current) { 56.dp.toPx() }

    Box(
        modifier = modifier
            .onSizeChanged { containerSize = it }
            .pointerInput(relativePath) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val nextScale = (scale * zoom).coerceIn(1f, 4f)
                    scale = nextScale
                    if (nextScale <= 1.01f) {
                        offsetX = 0f
                        offsetY = 0f
                    }
                    if (nextScale > 1.01f) {
                        val maxX = (containerSize.width * (nextScale - 1f)) / 2f
                        val maxY = (containerSize.height * (nextScale - 1f)) / 2f
                        offsetX = (offsetX + pan.x).coerceIn(-maxX, maxX)
                        offsetY = (offsetY + pan.y).coerceIn(-maxY, maxY)
                    } else {
                        flags.accumulatedX += pan.x
                        if (!flags.swiped && abs(flags.accumulatedX) > swipeThreshold) {
                            flags.swiped = true
                            onSwipe(if (flags.accumulatedX < 0f) 1 else -1)
                        }
                    }
                }
            }
            .pointerInput(relativePath) {
                detectTapGestures(
                    onPress = {
                        flags.accumulatedX = 0f
                        flags.swiped = false
                        tryAwaitRelease()
                    },
                    onDoubleTap = {
                        scope.launch {
                            val target = if (scale > 1.01f) 1f else 2.5f
                            animate(
                                initialValue = scale,
                                targetValue = target,
                                animationSpec = tween(durationMillis = 220),
                            ) { value, _ ->
                                scale = value
                            }
                            if (target == 1f) {
                                offsetX = 0f
                                offsetY = 0f
                            }
                        }
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        if (exists) {
            AsyncImage(
                model = file,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offsetX
                        translationY = offsetY
                    },
            )
        } else {
            Text(
                text = "图片文件不存在",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.6f),
            )
        }
    }
}
