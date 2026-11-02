package com.weiji.wang.feature.recipes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
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
import com.weiji.wang.ui.components.FilterPill
import com.weiji.wang.ui.components.focusRing
import com.weiji.wang.ui.components.PageHeader
import com.weiji.wang.ui.components.PlatformBadge
import com.weiji.wang.ui.components.RecipeCover
import com.weiji.wang.ui.components.SearchField
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class RecipeFilter(val label: String) {
    ALL("全部"),
    FAVORITE("收藏"),
    NEVER("没做过"),
    QUICK("30 分钟内"),
}

class RecipesViewModel(private val repo: RecipeRepository) : ViewModel() {
    val query = MutableStateFlow("")
    val filter = MutableStateFlow(RecipeFilter.ALL)

    val cards = combine(query, filter) { q, f -> q to f }
        .flatMapLatest { (q, f) ->
            repo.search(q).map { list -> applyFilter(list, f) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setQuery(value: String) {
        query.value = value
    }

    fun setFilter(value: RecipeFilter) {
        filter.value = value
    }

    fun toggleFavorite(card: RecipeCard) {
        viewModelScope.launch { repo.setFavorite(card.id, !card.favorite) }
    }

    private fun applyFilter(list: List<RecipeCard>, filter: RecipeFilter): List<RecipeCard> = when (filter) {
        RecipeFilter.ALL -> list
        RecipeFilter.FAVORITE -> list.filter { it.favorite }
        RecipeFilter.NEVER -> list.filter { it.lastCookedAt == null }
        RecipeFilter.QUICK -> list.filter { (it.totalMinutes ?: 999) <= 30 }
    }
}

@Composable
fun RecipesScreen(nav: NavHostController) {
    val container = LocalContainer.current
    val vm: RecipesViewModel = viewModel(
        factory = viewModelFactory { initializer { RecipesViewModel(container.recipes) } },
    )
    val cards by vm.cards.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val filter by vm.filter.collectAsStateWithLifecycle()
    val width = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp
    val columns = if (width >= 840) 3 else 2

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            PageHeader(
                title = "菜谱",
                subtitle = if (cards.isEmpty()) null else "${cards.size} 道",
                horizontalPadding = 0,
            )
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            SearchField(value = query, onValueChange = vm::setQuery)
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 2.dp),
            ) {
                items(RecipeFilter.entries) { f ->
                    FilterPill(text = f.label, selected = filter == f) { vm.setFilter(f) }
                }
            }
        }
        if (cards.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(modifier = Modifier.fillMaxWidth().height(420.dp)) {
                    EmptyState(
                        title = if (query.isBlank()) "还没有菜谱" else "没有找到",
                        body = if (query.isBlank()) "从抖音、小红书或 B 站分享一条链接进来。" else "换个关键词，或清空筛选。",
                        primaryLabel = if (query.isBlank()) "去收录" else "清空搜索",
                        onPrimary = {
                            if (query.isBlank()) nav.navigate("capture") else vm.setQuery("")
                        },
                    )
                }
            }
        } else {
            items(cards, key = { it.id }) { card ->
                RecipeGridCard(
                    card = card,
                    onClick = { nav.navigate("recipe/${card.id}") },
                    onFavorite = { vm.toggleFavorite(card) },
                )
            }
        }
    }
}

@Composable
private fun RecipeGridCard(card: RecipeCard, onClick: () -> Unit, onFavorite: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .focusRing(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
    ) {
        Box {
            RecipeCover(
                relativePath = card.coverPath,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.82f))
                    .clickable(onClick = onFavorite),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (card.favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = if (card.favorite) "取消收藏" else "收藏",
                    tint = if (card.favorite) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = card.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                minLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (card.platform != null) {
                    PlatformBadge(Platform.fromKey(card.platform), compact = true)
                } else {
                    Text(
                        text = "手动",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = "${card.servings} 人",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
