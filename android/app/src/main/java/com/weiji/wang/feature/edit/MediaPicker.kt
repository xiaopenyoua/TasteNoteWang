package com.weiji.wang.feature.edit

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.weiji.wang.core.data.db.MediaEntity
import com.weiji.wang.core.model.Ulid
import com.weiji.wang.ui.LocalContainer

/** 封面 / 原视频草稿：existing* 来自已保存媒体，uri 是本次新选中的系统照片 Uri。 */
data class MediaSlot(
    val existingId: String? = null,
    val existingPath: String? = null,
    val existingMime: String? = null,
    val uri: String? = null,
    val mime: String? = null,
) {
    val isEmpty: Boolean get() = existingPath.isNullOrBlank() && uri.isNullOrBlank()
    val isVideo: Boolean
        get() = mime.orEmpty().startsWith("video/") || existingMime.orEmpty().startsWith("video/")
}

/** 步骤内的一张图片或视频；existing 表示已落库，uri 表示保存时才复制的新选择。 */
data class StepMediaDraft(
    val key: String = Ulid.next(),
    val existingId: String? = null,
    val existingPath: String? = null,
    val existingMime: String? = null,
    val uri: String? = null,
    val mime: String? = null,
) {
    val isVideo: Boolean
        get() = mime.orEmpty().startsWith("video/") || existingMime.orEmpty().startsWith("video/")
}

fun MediaEntity.toStepMediaDraft(): StepMediaDraft = StepMediaDraft(
    existingId = id,
    existingPath = relativePath,
    existingMime = mime,
)

/** 1dp 虚线描边；用于封面空态与媒体添加入口。 */
fun Modifier.dashedBorder(
    color: Color,
    cornerRadius: Dp,
    strokeWidth: Dp = 1.dp,
): Modifier = drawBehind {
    drawRoundRect(
        color = color,
        cornerRadius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx()),
        style = Stroke(
            width = strokeWidth.toPx(),
            pathEffect = PathEffect.dashPathEffect(
                floatArrayOf(6.dp.toPx(), 4.dp.toPx()),
            ),
        ),
    )
}

/** 本地文件或新选 Uri 的统一预览；视频只显示播放图标，不在编辑页解码。 */
@Composable
fun DraftMediaImage(
    existingPath: String?,
    uri: String?,
    isVideo: Boolean,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val files = LocalContainer.current.files
    val model: Any? = when {
        !uri.isNullOrBlank() -> Uri.parse(uri)
        !existingPath.isNullOrBlank() -> files.resolve(existingPath)
        else -> null
    }
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (model != null && !isVideo) {
            AsyncImage(
                model = model,
                contentDescription = null,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
fun StepMediaTile(
    item: StepMediaDraft,
    order: Int,
    canMoveLeft: Boolean,
    canMoveRight: Boolean,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
    onRemove: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(width = 104.dp, height = 78.dp)
            .clip(RoundedCornerShape(12.dp)),
    ) {
        DraftMediaImage(
            existingPath = item.existingPath,
            uri = item.uri,
            isVideo = item.isVideo,
            modifier = Modifier.fillMaxSize(),
        )
        if (item.isVideo) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(4.dp)
                .size(18.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.55f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "$order",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(2.dp)
                .size(24.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "移除",
                tint = Color.White,
                modifier = Modifier.size(12.dp),
            )
        }
        Text(
            text = if (item.isVideo) "视频" else "图片",
            color = Color.White,
            fontSize = 9.sp,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(4.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(horizontal = 4.dp, vertical = 1.dp),
        )
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            MoveMediaButton(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "左移", canMoveLeft, onMoveLeft)
            MoveMediaButton(Icons.AutoMirrored.Filled.KeyboardArrowRight, "右移", canMoveRight, onMoveRight)
        }
    }
}

@Composable
private fun MoveMediaButton(
    icon: ImageVector,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color.Black.copy(alpha = if (enabled) 0.45f else 0.2f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = Color.White.copy(alpha = if (enabled) 1f else 0.4f),
            modifier = Modifier.size(15.dp),
        )
    }
}

@Composable
fun AddMediaTile(onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .size(width = 104.dp, height = 78.dp)
            .clip(RoundedCornerShape(12.dp))
            .dashedBorder(MaterialTheme.colorScheme.outline, 12.dp)
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "图片/视频",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
