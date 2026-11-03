package com.weiji.wang.feature.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import com.weiji.wang.core.data.MenuEntry
import com.weiji.wang.core.data.MenuRepository
import com.weiji.wang.core.model.Platform
import com.weiji.wang.core.time.DayClock
import com.weiji.wang.ui.LocalContainer
import com.weiji.wang.ui.components.CircleIconButton
import com.weiji.wang.ui.components.EmptyState
import com.weiji.wang.ui.components.PlatformBadge
import com.weiji.wang.ui.components.RecipeCover
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth

class HistoryViewModel(
    menu: MenuRepository,
) : ViewModel() {
    private val month = MutableStateFlow(YearMonth.now())
    val currentMonth: StateFlow<YearMonth> = month.asStateFlow()

    val entries = month
        .flatMapLatest { menu.observeMonth(it.year, it.monthValue) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val hasAny: StateFlow<Boolean?> = menu.observeHasAny()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setMonth(value: YearMonth) {
        month.value = value
    }
}

@Composable
fun HistoryScreen(nav: NavHostController) {
    val container = LocalContainer.current
    val vm: HistoryViewModel = viewModel(
        factory = viewModelFactory {
            initializer { HistoryViewModel(container.menu) }
        },
    )
    val month by vm.currentMonth.collectAsStateWithLifecycle()
    val entries by vm.entries.collectAsStateWithLifecycle()
    val hasAny by vm.hasAny.collectAsStateWithLifecycle()

    val today = remember { DayClock.todayKey() }
    val monthKey = "%04d-%02d".format(month.year, month.monthValue)
    var selected by rememberSaveable { mutableStateOf(today) }
    var awaitingMonth by remember { mutableStateOf(false) }

    LaunchedEffect(monthKey, entries, awaitingMonth) {
        if (awaitingMonth && entries.none { !it.date.startsWith(monthKey) }) {
            selected = entries.maxOfOrNull { it.date } ?: "$monthKey-01"
            awaitingMonth = false
        }
    }

    val dayEntries = entries.filter { it.date == selected }

    fun changeMonth(target: YearMonth) {
        vm.setMonth(target)
        awaitingMonth = true
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp),
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircleIconButton(
                    icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "返回",
                    onClick = { nav.popBackStack() },
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = "吃过的菜",
                        style = MaterialTheme.typography.displayMedium,
                        maxLines = 1,
                    )
                    Text(
                        text = "按天回看每天的菜单",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item {
            MonthCalendar(
                month = month,
                selected = selected,
                today = today,
                recordedDates = entries.map { it.date }.toSet(),
                onSelect = {
                    awaitingMonth = false
                    selected = it
                },
                onChangeMonth = ::changeMonth,
            )
        }

        if (hasAny == false) {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(420.dp)) {
                    EmptyState(
                        title = "还没有吃过的菜",
                        body = "在「今天吃什么」里选几道菜，之后这里会按天留下记录。",
                        primaryLabel = "去安排今天",
                        onPrimary = {
                            nav.navigate("home") {
                                popUpTo("home") { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                    )
                }
            }
        } else {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = dayLabel(selected),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "${dayEntries.size} 道",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
            if (dayEntries.isEmpty()) {
                item {
                    Text(
                        text = "这天没有记录",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
                    )
                }
            } else {
                items(dayEntries, key = { "${it.date}-${it.recipeId}" }) { entry ->
                    HistoryRow(entry) {
                        nav.navigate("history/${entry.date}/${entry.recipeId}")
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun MonthCalendar(
    month: YearMonth,
    selected: String,
    today: String,
    recordedDates: Set<String>,
    onSelect: (String) -> Unit,
    onChangeMonth: (YearMonth) -> Unit,
) {
    val first = month.atDay(1)
    val leading = first.dayOfWeek.value - 1
    val gridStart = first.minusDays(leading.toLong())
    val daysInMonth = month.lengthOfMonth()
    val cellCount = ((leading + daysInMonth + 6) / 7) * 7
    val cells = (0 until cellCount).map { gridStart.plusDays(it.toLong()) }

    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "上个月",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable { onChangeMonth(month.minusMonths(1)) }
                    .padding(4.dp),
            )
            Text(
                text = "${month.year}年${month.monthValue}月",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "下个月",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable { onChangeMonth(month.plusMonths(1)) }
                    .padding(4.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
        Row {
            listOf("一", "二", "三", "四", "五", "六", "日").forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        cells.chunked(7).forEach { week ->
            Row {
                week.forEach { date ->
                    DayCell(
                        date = date,
                        inMonth = date.month == month.month,
                        selected = date.toString() == selected,
                        isToday = date.toString() == today,
                        hasRecord = date.toString() in recordedDates,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (date.month == month.month) {
                                onSelect(date.toString())
                            } else {
                                onChangeMonth(YearMonth.from(date))
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    inMonth: Boolean,
    selected: Boolean,
    isToday: Boolean,
    hasRecord: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val textColor = when {
        selected -> Color.Black
        else -> MaterialTheme.colorScheme.onSurface
    }
    val dotColor = if (selected) Color.Black.copy(alpha = 0.55f) else MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .height(40.dp)
            .padding(2.dp)
            .alpha(if (inMonth) 1f else 0.45f)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(12.dp))
                .then(
                    when {
                        selected -> Modifier.background(MaterialTheme.colorScheme.primary)
                        isToday -> Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                        else -> Modifier
                    },
                ),
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
            )
            if (hasRecord) {
                Spacer(Modifier.height(1.dp))
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(dotColor),
                )
            }
        }
    }
}

@Composable
private fun HistoryRow(entry: MenuEntry, onClick: () -> Unit) {
    val snapshot = entry.snapshot
    val title = snapshot?.recipe?.title ?: "菜谱已删除"
    val servings = snapshot?.recipe?.servings
    val platform = snapshot?.source?.platform?.let { Platform.fromKey(it) }
    val cover = entry.coverPath ?: snapshot?.recipe?.coverPath

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RecipeCover(
            relativePath = cover,
            modifier = Modifier.size(52.dp),
            shape = RoundedCornerShape(10.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (platform != null) {
                    PlatformBadge(platform, compact = true)
                    Spacer(Modifier.width(6.dp))
                }
                if (servings != null) {
                    Text(
                        text = "$servings 人份",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private val weekdays = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")

private fun dayLabel(dateKey: String): String {
    val date = runCatching { LocalDate.parse(dateKey) }.getOrNull() ?: return dateKey
    return "${date.monthValue}月${date.dayOfMonth}日 · ${weekdays[date.dayOfWeek.value - 1]}"
}
