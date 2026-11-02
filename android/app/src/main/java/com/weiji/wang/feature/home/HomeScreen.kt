package com.weiji.wang.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import com.weiji.wang.core.data.RecipeRepository
import com.weiji.wang.core.data.db.RecipeCard
import com.weiji.wang.core.model.Platform
import com.weiji.wang.ui.LocalContainer
import com.weiji.wang.ui.components.EmptyState
import com.weiji.wang.ui.components.PageHeader
import com.weiji.wang.ui.components.PlatformBadge
import com.weiji.wang.ui.components.RecipeCover
import com.weiji.wang.ui.components.SectionHeader
import com.weiji.wang.ui.components.focusRing
import com.weiji.wang.ui.components.formatDaysSince
import com.weiji.wang.ui.components.formatRelativeDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar

class HomeViewModel(repo: RecipeRepository) : ViewModel() {
    val suggestions = repo.observeSuggestions(3)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val recent = repo.observeRecentlyCooked(6)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val count = repo.observeCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
}

@Composable
fun HomeScreen(nav: NavHostController) {
    val container = LocalContainer.current
    val vm: HomeViewModel = viewModel(
        factory = viewModelFactory { initializer { HomeViewModel(container.recipes) } },
    )
    val suggestions by vm.suggestions.collectAsStateWithLifecycle()
    val recent by vm.recent.collectAsStateWithLifecycle()
    val count by vm.count.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp),
    ) {
        item {
            PageHeader(
                title = "今天",
                subtitle = "${todayLabel()} · ${greeting()}",
            )
        }

        if (count == 0) {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(520.dp)) {
                    EmptyState(
                        title = "今晚吃什么？",
                        body = "把抖音、小红书或 B 站刷到的教程收进来，味记会帮你排成能照着做的步骤。",
                        primaryLabel = "收录第一条",
                        onPrimary = { nav.navigate("capture") },
                        secondaryLabel = "手动创建",
                        onSecondary = { nav.navigate("edit/new") },
                    )
                }
            }
        } else {
            item {
                SectionHeader(
                    title = "今晚吃什么",
                    actionLabel = "全部",
                    onAction = { nav.navigate("recipes") },
                )
            }
            item {
                if (suggestions.isEmpty()) {
                    SuggestionPlaceholder()
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(suggestions, key = { it.id }) { card ->
                            SuggestionCard(card) { nav.navigate("recipe/${card.id}") }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            if (recent.isNotEmpty()) {
                item { SectionHeader(title = "最近做过") }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(recent, key = { it.id }) { card ->
                            RecentCard(card) { nav.navigate("recipe/${card.id}") }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }

            item {
                SectionHeader(title = "快速开始")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    QuickAction("收录链接", Modifier.weight(1f)) { nav.navigate("capture") }
                    QuickAction("手动创建", Modifier.weight(1f)) { nav.navigate("edit/new") }
                    QuickAction("采购清单", Modifier.weight(1f)) { nav.navigate("shopping") }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SuggestionCard(card: RecipeCard, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(264.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .focusRing(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
    ) {
        RecipeCover(
            relativePath = card.coverPath,
            modifier = Modifier
                .fillMaxWidth()
                .height(148.dp),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        )
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = card.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                minLines = 2,
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (card.platform != null) {
                    PlatformBadge(Platform.fromKey(card.platform), compact = true)
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    text = "${card.servings} 人份",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.weight(1f))
                val hint = when {
                    card.lastCookedAt != null -> formatRelativeDate(card.lastCookedAt)
                    formatDaysSince(card.createdAt) >= 7 -> "收藏 ${formatDaysSince(card.createdAt)} 天未做"
                    else -> "还没做过"
                }
                Text(
                    text = hint,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun RecentCard(card: RecipeCard, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .width(228.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .focusRing(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RecipeCover(
            relativePath = card.coverPath,
            modifier = Modifier.size(56.dp),
            shape = RoundedCornerShape(10.dp),
        )
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                text = card.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = card.lastCookedAt?.let { formatRelativeDate(it) } ?: "",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SuggestionPlaceholder() {
    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "再收录一道，就有建议了",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun QuickAction(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .focusRing(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, style = MaterialTheme.typography.labelLarge)
    }
}

private fun todayLabel(): String {
    val cal = Calendar.getInstance()
    val week = listOf("周日", "周一", "周二", "周三", "周四", "周五", "周六")[cal.get(Calendar.DAY_OF_WEEK) - 1]
    return "${cal.get(Calendar.MONTH) + 1}月${cal.get(Calendar.DAY_OF_MONTH)}日 $week"
}

private fun greeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..10 -> "早饭吃点什么？"
        in 11..14 -> "午饭吃点什么？"
        in 15..17 -> "该想晚饭了"
        in 18..21 -> "今晚吃什么？"
        else -> "夜宵要克制"
    }
}
