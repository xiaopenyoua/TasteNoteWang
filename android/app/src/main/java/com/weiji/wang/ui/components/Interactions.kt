package com.weiji.wang.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val RevealWidth = 88.dp
private val OpenThreshold = 40.dp
private val DeleteDrag = 24.dp

/**
 * 左滑露出删除按钮的行容器。
 * - 拖动超过 40dp 打开到 88dp；已打开时继续左滑 24dp 直接触发删除。
 * - 点击内容收起；同一时间只应由调用方保持一行展开。
 */
@Composable
fun SwipeToRevealDelete(
    revealed: Boolean,
    onRevealedChange: (Boolean) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(16.dp),
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val revealPx = with(density) { RevealWidth.toPx() }
    val thresholdPx = with(density) { OpenThreshold.toPx() }
    val deleteDragPx = with(density) { DeleteDrag.toPx() }
    val offset = remember { Animatable(0f) }
    var extraDrag by remember { mutableFloatStateOf(0f) }
    // 收起时不画删除底：卡片和底是同色圆角，叠在一起会露出一像素红边。
    val showDelete by remember { derivedStateOf { offset.value < -0.5f } }

    LaunchedEffect(revealed) {
        offset.animateTo(if (revealed) -revealPx else 0f, tween(durationMillis = 180))
    }

    Box(modifier = modifier.clip(shape)) {
        if (showDelete) {
            Row(
                modifier = Modifier.matchParentSize(),
                horizontalArrangement = Arrangement.End,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(RevealWidth)
                        .background(MaterialTheme.colorScheme.error)
                        .clickable(enabled = revealed) { onDelete() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "删除",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onError,
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(offset.value.roundToInt(), 0) }
                .pointerInput(revealed) {
                    detectHorizontalDragGestures(
                        onDragStart = { extraDrag = 0f },
                        onDragEnd = {
                            extraDrag = 0f
                            val open = offset.value <= -thresholdPx
                            scope.launch {
                                offset.animateTo(if (open) -revealPx else 0f, tween(durationMillis = 160))
                            }
                            if (open != revealed) onRevealedChange(open)
                        },
                        onDragCancel = {
                            extraDrag = 0f
                            val open = offset.value <= -thresholdPx
                            scope.launch {
                                offset.animateTo(if (open) -revealPx else 0f, tween(durationMillis = 160))
                            }
                            if (open != revealed) onRevealedChange(open)
                        },
                    ) { change, dragAmount ->
                        change.consume()
                        if (revealed && dragAmount < 0f) {
                            extraDrag += -dragAmount
                            if (extraDrag >= deleteDragPx) {
                                extraDrag = 0f
                                onDelete()
                                return@detectHorizontalDragGestures
                            }
                        } else if (dragAmount > 0f) {
                            extraDrag = 0f
                        }
                        val target = (offset.value + dragAmount).coerceIn(-revealPx, 0f)
                        scope.launch { offset.snapTo(target) }
                    }
                },
        ) {
            content()
            if (revealed) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .pointerInput(Unit) {
                            detectTapGestures { onRevealedChange(false) }
                        },
                )
            }
        }
    }
}

/** 统一确认对话框；危险动作确认按钮用 error 色。 */
@Composable
fun ConfirmDialog(
    title: String,
    body: String,
    confirmLabel: String = "删除",
    dismissLabel: String = "取消",
    destructive: Boolean = true,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(text = title, style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        confirmButton = {
            TextButton(onClick = { onDismiss(); onConfirm() }) {
                Text(
                    text = confirmLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (destructive) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = dismissLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        },
    )
}

/** 36/40dp 圆形图标按钮；页头返回、日历入口等共用。 */
@Composable
fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    iconSize: Dp = 22.dp,
    background: Color = MaterialTheme.colorScheme.surface,
    tint: Color = MaterialTheme.colorScheme.onSurface,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(background)
            .focusRing(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(iconSize),
        )
    }
}

/** 长按菜谱卡后的底部菜单：今晚做这道 / 收藏 / 删除。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeActionSheet(
    title: String,
    favorite: Boolean,
    onDismiss: () -> Unit,
    onAddToday: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "加入今天吃什么，并把食材加入采购",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            SheetAction(text = "今晚做这道", onClick = onAddToday)
            SheetAction(
                text = if (favorite) "取消收藏" else "收藏",
                onClick = onToggleFavorite,
            )
            SheetAction(text = "删除菜谱", danger = true, onClick = onDelete)
        }
    }
}

@Composable
private fun SheetAction(
    text: String,
    danger: Boolean = false,
    onClick: () -> Unit,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 14.dp),
    )
}
