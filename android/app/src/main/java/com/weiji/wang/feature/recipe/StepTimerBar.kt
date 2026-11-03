package com.weiji.wang.feature.recipe

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class StepTimer(
    val stepId: String,
    val stepIndex: Int,
    val totalSeconds: Int,
    val remainingSeconds: Int,
    val running: Boolean,
    val finished: Boolean = false,
)

/** 步骤页顶部横向计时条：点胶囊暂停/继续，点 × 移除；多个计时器并行。 */
@Composable
fun StepTimerBar(
    timers: List<StepTimer>,
    onToggle: (String) -> Unit,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (timers.isEmpty()) return
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(timers, key = { it.stepId }) { timer ->
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
                    .clickable { onToggle(timer.stepId) }
                    .padding(start = 10.dp, end = 4.dp, top = 5.dp, bottom = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = timerLabel(timer),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
                        .clickable { onRemove(timer.stepId) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "×",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

fun timerLabel(timer: StepTimer): String = when {
    timer.finished -> "第 ${timer.stepIndex} 步 · 时间到"
    timer.running -> "第 ${timer.stepIndex} 步 · ${formatCountdown(timer.remainingSeconds)}"
    else -> "第 ${timer.stepIndex} 步 · 已暂停 ${formatCountdown(timer.remainingSeconds)}"
}

fun formatCountdown(seconds: Int): String {
    val value = seconds.coerceAtLeast(0)
    return "%02d:%02d".format(value / 60, value % 60)
}

/** 步骤时长胶囊文案：整分钟显示「10 分」。 */
fun formatStepDuration(seconds: Int): String = when {
    seconds >= 60 && seconds % 60 == 0 -> "${seconds / 60} 分"
    seconds >= 60 -> "${seconds / 60} 分 ${seconds % 60} 秒"
    else -> "$seconds 秒"
}
