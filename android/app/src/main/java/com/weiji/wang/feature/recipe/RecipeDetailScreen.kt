package com.weiji.wang.feature.recipe

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.Image
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.NavHostController
import com.weiji.wang.R
import com.weiji.wang.core.data.RecipeRepository
import com.weiji.wang.core.data.db.IngredientEntity
import com.weiji.wang.core.data.db.MediaEntity
import com.weiji.wang.core.data.db.NoteEntity
import com.weiji.wang.core.data.db.RecipeWithDetails
import com.weiji.wang.core.data.db.SourceEntity
import com.weiji.wang.core.data.db.StepEntity
import com.weiji.wang.core.model.IngredientScaler
import com.weiji.wang.core.model.MediaKind
import com.weiji.wang.core.model.Platform
import com.weiji.wang.ui.LocalContainer
import com.weiji.wang.ui.components.CircleIconButton
import com.weiji.wang.ui.components.ConfirmDialog
import com.weiji.wang.ui.components.EmptyState
import com.weiji.wang.ui.components.RecipeCover
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 编辑部方向的局部配色：只作用于菜谱预览页，不改全局主题。
 * 纸张/墨色跟随当前 App 主题亮度；深色下换成暖黑纸与米白墨，避免整屏反色。
 */
private data class EditorialColors(
    val paper: Color,
    val ink: Color,
    val ink2: Color,
    val ink3: Color,
    val accent: Color,
    val hairline: Color,
    val surface: Color,
)

private val LightEditorial = EditorialColors(
    paper = Color(0xFFFBF8F3),
    ink = Color(0xFF191512),
    ink2 = Color(0xFF6F645B),
    ink3 = Color(0xFF9A8B7D),
    accent = Color(0xFFB3471E),
    hairline = Color(0x1F191512),
    surface = Color(0xFFFFFFFF),
)

private val DarkEditorial = EditorialColors(
    paper = Color(0xFF12100E),
    ink = Color(0xFFF5EFE6),
    ink2 = Color(0xFFB8ADA2),
    ink3 = Color(0xFF8F8378),
    accent = Color(0xFFE0713F),
    hairline = Color(0x33F5EFE6),
    surface = Color(0xFF1E1A16),
)

@Composable
private fun editorialColors(): EditorialColors =
    if (MaterialTheme.colorScheme.background.luminance() < 0.4f) DarkEditorial else LightEditorial

private val EditorialSerif = FontFamily.Serif

private fun stepNumber(n: Int): String = n.toString().padStart(2, '0')

@Composable
private fun editorialKicker(recipe: RecipeWithDetails): String {
    val platform = recipe.source?.let { Platform.fromKey(it.platform).label } ?: "手动"
    val pace = recipe.recipe.totalMinutes?.let { if (it <= 30) "快手" else "慢炖" } ?: "家常"
    return "$platform · $pace"
}

class RecipeDetailViewModel(
    private val recipeId: String,
    private val repo: RecipeRepository,
) : ViewModel() {
    private val loadedState = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = loadedState

    val details = repo.observeDetails(recipeId)
        .onEach { loadedState.value = true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val notes = repo.observeNotes(recipeId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun toggleFavorite(current: Boolean) {
        viewModelScope.launch { repo.setFavorite(recipeId, !current) }
    }

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            repo.deleteRecipe(recipeId)
            onDone()
        }
    }
}

@Composable
fun RecipeDetailScreen(nav: NavHostController, recipeId: String) {
    val container = LocalContainer.current
    val vm: RecipeDetailViewModel = viewModel(
        key = "recipe-$recipeId",
        factory = viewModelFactory {
            initializer { RecipeDetailViewModel(recipeId, container.recipes) }
        },
    )
    val details by vm.details.collectAsStateWithLifecycle()
    val notes by vm.notes.collectAsStateWithLifecycle()
    val loaded by vm.loaded.collectAsStateWithLifecycle()

    if (!loaded) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("载入中…", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val recipe = details
    if (recipe == null) {
        EmptyState(
            title = "菜谱不存在",
            body = "它可能已经被删除。",
            primaryLabel = "返回",
            onPrimary = { nav.popBackStack() },
        )
        return
    }

    DetailContent(nav = nav, vm = vm, recipe = recipe, notes = notes)
}

@Composable
private fun DetailContent(
    nav: NavHostController,
    vm: RecipeDetailViewModel,
    recipe: RecipeWithDetails,
    notes: List<NoteEntity>,
) {
    val context = LocalContext.current
    val view = LocalView.current
    var servings by rememberSaveable(recipe.recipe.id) {
        mutableIntStateOf(recipe.recipe.servings.coerceIn(1, 12))
    }
    var showDelete by remember { mutableStateOf(false) }
    var viewer by remember { mutableStateOf<ImageViewerTarget?>(null) }
    var stepVideo by remember { mutableStateOf<String?>(null) }
    var fullscreenOriginal by remember { mutableStateOf(false) }
    var showOriginal by rememberSaveable(recipe.recipe.id) { mutableStateOf(false) }

    val steps = remember(recipe.steps) { recipe.steps.sortedBy { it.position } }
    val originalVideo = remember(recipe.media) {
        recipe.media
            .filter { it.kind == MediaKind.VIDEO.name && it.stepId == null }
            .minByOrNull { it.createdAt }
    }
    val videoPlayer = rememberVideoPlayer(originalVideo?.relativePath)

    val timers = remember { mutableStateMapOf<String, StepTimer>() }
    val timerList = timers.values.sortedBy { it.stepIndex }
    val anyTimerRunning = timerList.any { it.running }
    val lifecycleOwner = LocalLifecycleOwner.current
    var isForeground by remember {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { isForeground = true }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { isForeground = false }

    LaunchedEffect(anyTimerRunning, isForeground) {
        if (!isForeground) return@LaunchedEffect
        while (true) {
            delay(1_000)
            if (timers.values.none { it.running }) break
            timers.keys.toList().forEach { key ->
                val timer = timers[key] ?: return@forEach
                if (!timer.running) return@forEach
                val remaining = timer.remainingSeconds - 1
                if (remaining <= 0) {
                    timers[key] = timer.copy(remainingSeconds = 0, running = false, finished = true)
                    vibrateOnce(context, view)
                } else {
                    timers[key] = timer.copy(remainingSeconds = remaining)
                }
            }
        }
    }

    fun toggleTimer(stepId: String) {
        val index = steps.indexOfFirst { it.id == stepId }
        if (index < 0) return
        val duration = steps[index].durationSeconds ?: return
        val existing = timers[stepId]
        timers[stepId] = when {
            existing == null -> StepTimer(stepId, index + 1, duration, duration, running = true)
            existing.finished -> existing.copy(
                remainingSeconds = existing.totalSeconds,
                running = true,
                finished = false,
            )
            existing.running -> existing.copy(running = false)
            else -> existing.copy(running = true)
        }
    }

    val colors = editorialColors()
    Box(Modifier.fillMaxSize().background(colors.paper)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 36.dp),
        ) {
            item {
                RecipeCoverHeader(
                    recipe = recipe,
                    stepCount = steps.size,
                    nav = nav,
                    favorite = recipe.recipe.favorite,
                    onFavorite = { vm.toggleFavorite(recipe.recipe.favorite) },
                    onEdit = { nav.navigate("edit/${recipe.recipe.id}") },
                    onDelete = { showDelete = true },
                    onOpenCover = {
                        val cover = recipe.recipe.coverPath
                        if (cover != null) viewer = ImageViewerTarget(listOf(cover), 0)
                    },
                )
            }
            item { RecipeHeader(recipe = recipe, servings = servings) }

            if (videoPlayer != null) {
                item {
                    OriginalVideoSection(
                        player = videoPlayer,
                        playingFullscreen = fullscreenOriginal,
                        onFullscreen = { fullscreenOriginal = true },
                    )
                }
            }

            item { EditorialSectionHeader("01", "食材", "$servings 人份") }
            ingredientsContent(
                recipe = recipe,
                servings = servings,
                onServings = { servings = it.coerceIn(1, 12) },
            )

            item {
                EditorialSectionHeader(
                    "02",
                    "步骤",
                    if (steps.isEmpty()) null else "${steps.size} 步 · ${recipe.recipe.totalMinutes?.let { "$it 分钟" } ?: "按步骤来"}",
                )
            }
            stepsContent(
                recipe = recipe,
                notes = notes,
                timers = timerList,
                onTimerToggle = { stepId -> toggleTimer(stepId) },
                onTimerRemove = { stepId -> timers.remove(stepId) },
                onImageClick = { paths, initial -> viewer = ImageViewerTarget(paths, initial) },
                onVideoClick = { path -> stepVideo = path },
            )

            item {
                OriginalTextSection(
                    recipe = recipe,
                    expanded = showOriginal,
                    onToggle = { showOriginal = !showOriginal },
                )
            }

            noteItems(notes)
            item { EditorialFooter(recipe.source) }
        }

    }

    if (showDelete) {
        ConfirmDialog(
            title = "删除这道菜谱？",
            body = "会同时删除已下载的图片和视频；「吃过的菜」里的历史快照会保留。",
            onDismiss = { showDelete = false },
            onConfirm = {
                showDelete = false
                vm.delete { nav.popBackStack() }
            },
        )
    }

    viewer?.let { target ->
        ImageViewerDialog(
            paths = target.paths,
            initialIndex = target.index,
            onDismiss = { viewer = null },
        )
    }

    stepVideo?.let { path ->
        LocalVideoFullscreenDialog(
            relativePath = path,
            onDismiss = { stepVideo = null },
        )
    }

    if (fullscreenOriginal && videoPlayer != null) {
        VideoFullscreenDialog(
            player = videoPlayer,
            onDismiss = { fullscreenOriginal = false },
        )
    }
}

/** 编辑部方向的分节标题：大号衬线标题 + 编号 + 一条墨色细线。 */
@Composable
private fun EditorialSectionHeader(
    number: String,
    title: String,
    note: String? = null,
) {
    val c = editorialColors()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 22.dp, end = 22.dp, top = 34.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = number,
                fontFamily = EditorialSerif,
                fontSize = 12.sp,
                letterSpacing = 0.8.sp,
                color = c.accent,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = title,
                fontFamily = EditorialSerif,
                fontSize = 25.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.3).sp,
                color = c.ink,
            )
            Spacer(Modifier.weight(1f))
            note?.let {
                Text(
                    text = it,
                    fontSize = 9.5.sp,
                    letterSpacing = 0.8.sp,
                    color = c.ink3,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(c.ink),
        )
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun EditorialMetaItem(value: String, label: String) {
    val c = editorialColors()
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = c.ink,
        )
        Spacer(Modifier.width(2.dp))
        Text(
            text = label,
            fontSize = 11.5.sp,
            color = c.ink2,
        )
    }
}

@Composable
private fun EditorialMetaDivider() {
    val c = editorialColors()
    Box(
        Modifier
            .width(1.dp)
            .height(12.dp)
            .background(c.hairline),
    )
}

@Composable
private fun EditorialFooter(source: SourceEntity?) {
    val c = editorialColors()
    val handler = LocalUriHandler.current
    val platform = source?.let { Platform.fromKey(it.platform).label }
    val author = source?.authorName?.takeIf { it.isNotBlank() }
    val left = listOfNotNull("源", platform, author?.let { "@$it" }).joinToString(" · ")
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 22.dp, end = 22.dp, top = 34.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(c.hairline),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = source != null) {
                    source?.let { runCatching { handler.openUri(it.originalUrl) } }
                }
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (source == null) "手动创建 · 无来源链接" else left,
                fontSize = 10.sp,
                letterSpacing = 0.6.sp,
                color = c.ink3,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (source != null) {
                Text(
                    text = "原链接 ↗",
                    fontSize = 10.sp,
                    letterSpacing = 0.6.sp,
                    color = c.ink3,
                )
            }
        }
    }
}

@Composable
private fun OriginalVideoSection(
    player: ExoPlayer,
    playingFullscreen: Boolean,
    onFullscreen: () -> Unit,
) {
    val c = editorialColors()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 22.dp, end = 22.dp, top = 30.dp),
    ) {
        Text(
            text = "原视频",
            fontSize = 9.5.sp,
            letterSpacing = 1.8.sp,
            fontWeight = FontWeight.SemiBold,
            color = c.ink3,
        )
        Spacer(Modifier.height(8.dp))
        if (playingFullscreen) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(c.ink.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "全屏播放中",
                    fontSize = 13.sp,
                    color = c.ink2,
                )
            }
        } else {
            VideoPlayerCard(
                player = player,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(12.dp)),
                onFullscreen = onFullscreen,
            )
        }
    }
}

@Composable
private fun RecipeCoverHeader(
    recipe: RecipeWithDetails,
    stepCount: Int,
    nav: NavHostController,
    favorite: Boolean,
    onFavorite: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onOpenCover: () -> Unit,
) {
    val c = editorialColors()
    val wide = LocalConfiguration.current.screenWidthDp >= 600
    val coverPath = recipe.recipe.coverPath
    val source = recipe.source
    val platform = source?.let { Platform.fromKey(it.platform) } ?: Platform.OTHER
    val author = source?.authorName?.takeIf { it.isNotBlank() }
    val caption = when (platform) {
        Platform.DOUYIN -> "DOUYIN ORIGINAL"
        Platform.XIAOHONGSHU -> "XIAOHONGSHU"
        Platform.BILIBILI -> "BILIBILI"
        Platform.OTHER -> if (source == null) "MANUAL RECIPE" else "ORIGINAL"
    }
    val byline = listOfNotNull(
        author?.let { "@$it" },
        platform.label.takeIf { source != null },
    ).joinToString(" · ")
    var menuOpen by remember { mutableStateOf(false) }

    Box {
        RecipeCover(
            relativePath = coverPath,
            modifier = Modifier
                .fillMaxWidth()
                .then(if (wide) Modifier.height(380.dp) else Modifier.aspectRatio(4f / 5f)),
            shape = RoundedCornerShape(0.dp),
            fallback = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    c.accent.copy(alpha = 0.10f),
                                    c.paper,
                                ),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = recipe.recipe.title.take(1),
                        fontFamily = EditorialSerif,
                        fontSize = 88.sp,
                        color = c.accent.copy(alpha = 0.22f),
                    )
                }
            },
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(enabled = coverPath != null, onClick = onOpenCover),
        )
        // 封面底部渐隐到纸色，和正文无缝接上。
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(150.dp)
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.55f to c.paper.copy(alpha = 0.72f),
                        1f to c.paper,
                    ),
                ),
        )
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 18.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = caption,
                    fontSize = 9.5.sp,
                    letterSpacing = 2.2.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = c.accent,
                    maxLines = 1,
                )
                if (byline.isNotBlank()) {
                    Spacer(Modifier.height(5.dp))
                    Text(
                        text = byline,
                        fontSize = 13.sp,
                        letterSpacing = 0.1.sp,
                        color = c.ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (stepCount > 0) {
                Text(
                    text = "$stepCount STEPS",
                    fontSize = 9.5.sp,
                    letterSpacing = 1.2.sp,
                    color = c.ink3,
                )
            }
        }

        CircleIconButton(
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "返回",
            onClick = { nav.popBackStack() },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp),
            size = 40.dp,
            iconSize = 22.dp,
            background = Color.Black.copy(alpha = 0.28f),
            tint = Color.White,
        )
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CircleIconButton(
                icon = if (favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = if (favorite) "取消收藏" else "收藏",
                onClick = onFavorite,
                size = 40.dp,
                iconSize = 20.dp,
                background = Color.Black.copy(alpha = 0.28f),
                tint = if (favorite) c.accent else Color.White,
            )
            Box {
                CircleIconButton(
                    icon = Icons.Filled.MoreVert,
                    contentDescription = "更多",
                    onClick = { menuOpen = true },
                    size = 40.dp,
                    iconSize = 20.dp,
                    background = Color.Black.copy(alpha = 0.28f),
                    tint = Color.White,
                )
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("编辑") },
                        onClick = {
                            menuOpen = false
                            onEdit()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("删除") },
                        onClick = {
                            menuOpen = false
                            onDelete()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun RecipeHeader(
    recipe: RecipeWithDetails,
    servings: Int,
) {
    val c = editorialColors()
    val handler = LocalUriHandler.current
    val source = recipe.source
    val platform = source?.let { Platform.fromKey(it.platform).label } ?: "手动"
    val author = source?.authorName?.takeIf { it.isNotBlank() }
    val hasVideo = recipe.media.any { it.kind == MediaKind.VIDEO.name }

    Column(modifier = Modifier.padding(horizontal = 22.dp)) {
        Spacer(Modifier.height(24.dp))
        Text(
            text = editorialKicker(recipe),
            fontSize = 10.sp,
            letterSpacing = 1.6.sp,
            fontWeight = FontWeight.SemiBold,
            color = c.accent,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = recipe.recipe.title,
            style = TextStyle(
                fontFamily = EditorialSerif,
                fontSize = 40.sp,
                lineHeight = 44.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.8).sp,
            ),
            color = c.ink,
        )
        if (recipe.recipe.subtitle.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = recipe.recipe.subtitle,
                fontSize = 15.sp,
                lineHeight = 24.sp,
                color = c.ink2,
            )
        }
        Spacer(Modifier.height(20.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = source != null) {
                    source?.let { runCatching { handler.openUri(it.originalUrl) } }
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(c.ink.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = author?.firstOrNull()?.toString()
                        ?: platform.firstOrNull()?.toString()
                        ?: "手",
                    fontFamily = EditorialSerif,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = c.accent,
                )
            }
            Spacer(Modifier.width(9.dp))
            Text(
                text = author?.let { "@$it" } ?: "手动创建",
                fontSize = 13.sp,
                color = c.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (source != null) {
                Text(
                    text = " · $platform",
                    fontSize = 13.sp,
                    color = c.ink2,
                    maxLines = 1,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "原链接 ↗",
                    fontSize = 12.sp,
                    color = c.accent,
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(c.hairline),
        )
        Spacer(Modifier.height(14.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            EditorialMetaItem(servings.toString(), "人份")
            EditorialMetaDivider()
            recipe.recipe.totalMinutes?.let {
                EditorialMetaItem(it.toString(), "分钟")
                EditorialMetaDivider()
            }
            EditorialMetaItem(recipe.ingredients.size.toString(), "项食材")
            if (hasVideo) {
                EditorialMetaDivider()
                Text(
                    text = "原视频",
                    fontSize = 11.5.sp,
                    color = c.ink2,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.stepsContent(
    recipe: RecipeWithDetails,
    notes: List<NoteEntity>,
    timers: List<StepTimer>,
    onTimerToggle: (String) -> Unit,
    onTimerRemove: (String) -> Unit,
    onImageClick: (List<String>, Int) -> Unit,
    onVideoClick: (String) -> Unit,
) {
    val steps = recipe.steps.sortedBy { it.position }

    if (timers.isNotEmpty()) {
        item {
            Box(Modifier.padding(horizontal = 16.dp, vertical = 2.dp)) {
                StepTimerBar(
                    timers = timers,
                    onToggle = onTimerToggle,
                    onRemove = onTimerRemove,
                )
            }
        }
    }

    if (steps.isEmpty()) {
        item { EmptyInfoCard("还没有拆出步骤。点右上角编辑补充，或看下面的原文。") }
        return
    }

    itemsIndexed(steps, key = { _, step -> step.id }) { index, step ->
        val stepNotes = notes.filter { it.stepId == step.id }.sortedByDescending { it.createdAt }
        StepCard(
            number = index + 1,
            text = step.text,
            durationSeconds = step.durationSeconds,
            timer = timers.firstOrNull { it.stepId == step.id },
            media = stepMedia(recipe, step, index),
            notes = stepNotes,
            onTimerClick = { onTimerToggle(step.id) },
            onImageClick = onImageClick,
            onVideoClick = onVideoClick,
        )
    }
}

@Composable
private fun StepCard(
    number: Int,
    text: String,
    durationSeconds: Int?,
    timer: StepTimer?,
    media: List<StepMedia>,
    notes: List<NoteEntity>,
    onTimerClick: () -> Unit,
    onImageClick: (List<String>, Int) -> Unit,
    onVideoClick: (String) -> Unit,
) {
    val c = editorialColors()
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 22.dp, end = 22.dp, top = 14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                text = stepNumber(number),
                fontFamily = EditorialSerif,
                fontSize = 27.sp,
                lineHeight = 30.sp,
                color = c.accent,
                letterSpacing = (-0.2).sp,
                modifier = Modifier.width(38.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = text.ifBlank { "按图示操作" },
                    fontSize = 15.5.sp,
                    lineHeight = 26.sp,
                    color = c.ink,
                )
                if (durationSeconds != null) {
                    Spacer(Modifier.height(8.dp))
                    StepTimerCapsule(
                        timer = timer,
                        durationSeconds = durationSeconds,
                        onClick = onTimerClick,
                    )
                }
            }
        }
        if (media.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            val images = media.filter { !it.isVideo }.map { it.relativePath }
            Column(
                modifier = Modifier.padding(horizontal = 22.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                media.forEachIndexed { mediaIndex, item ->
                    StepMediaBlock(
                        media = item,
                        onClick = {
                            if (item.isVideo) {
                                onVideoClick(item.relativePath)
                            } else {
                                val imageIndex = media.take(mediaIndex).count { !it.isVideo }
                                onImageClick(images, imageIndex)
                            }
                        },
                    )
                }
            }
        }
        notes.forEach { note -> StepNoteCard(note = note) }
    }
}

@Composable
private fun StepTimerCapsule(
    timer: StepTimer?,
    durationSeconds: Int,
    onClick: () -> Unit,
) {
    val c = editorialColors()
    val highlighted = timer?.running == true || timer?.finished == true
    val label = when {
        timer == null -> "${formatStepDuration(durationSeconds)} · 计时"
        timer.finished -> "时间到"
        timer.running -> formatCountdown(timer.remainingSeconds)
        else -> "继续 · ${formatCountdown(timer.remainingSeconds)}"
    }
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .border(
                width = 0.5.dp,
                color = if (highlighted) c.accent.copy(alpha = 0.55f) else c.hairline,
                shape = CircleShape,
            )
            .background(if (highlighted) c.accent.copy(alpha = 0.08f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            fontSize = 10.5.sp,
            letterSpacing = 0.2.sp,
            fontWeight = FontWeight.Medium,
            color = if (highlighted) c.accent else c.ink2,
        )
    }
}

/** 步骤媒体与正文同宽，圆角 12；点一下进全屏查看或播放。 */
@Composable
private fun StepMediaBlock(
    media: StepMedia,
    onClick: () -> Unit,
) {
    val c = editorialColors()
    val files = LocalContainer.current.files
    val file = remember(media.relativePath, files) { files.resolve(media.relativePath) }
    val exists = remember(file) { file.exists() && file.length() > 0L }

    when {
        media.isVideo -> Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = "播放视频",
                tint = Color.White,
                modifier = Modifier.size(46.dp),
            )
        }

        !exists -> Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3f)
                .clip(RoundedCornerShape(12.dp))
                .background(c.ink.copy(alpha = 0.06f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_bowl),
                contentDescription = null,
                tint = c.ink3,
                modifier = Modifier.size(28.dp),
            )
        }

        else -> {
            val painter = rememberAsyncImagePainter(model = file)
            val ratio = painter.intrinsicSize.let { size ->
                if (size.isSpecified && size.width > 0f && size.height > 0f) {
                    (size.width / size.height).coerceIn(0.6f, 2.2f)
                } else {
                    4f / 3f
                }
            }
            Image(
                painter = painter,
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(ratio)
                    .clip(RoundedCornerShape(12.dp))
                    .background(c.ink.copy(alpha = 0.06f))
                    .clickable(onClick = onClick),
            )
        }
    }
}

@Composable
private fun StepNoteCard(note: NoteEntity) {
    val c = editorialColors()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp)
            .padding(top = 12.dp)
            .height(IntrinsicSize.Min),
    ) {
        Box(
            Modifier
                .width(1.5.dp)
                .fillMaxHeight()
                .background(c.accent.copy(alpha = 0.65f)),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
        ) {
            Text(
                text = "备注 · ${formatNoteDate(note.createdAt)}",
                fontSize = 9.5.sp,
                letterSpacing = 0.6.sp,
                color = c.ink3,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = note.text,
                fontSize = 12.sp,
                lineHeight = 19.sp,
                color = c.ink2,
            )
        }
    }
}

@Composable
private fun WholeNoteCard(note: NoteEntity) {
    val c = editorialColors()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp)
            .padding(top = 14.dp)
            .height(IntrinsicSize.Min),
    ) {
        Box(
            Modifier
                .width(1.5.dp)
                .fillMaxHeight()
                .background(c.accent),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp),
        ) {
            Text(
                text = formatNoteDate(note.createdAt),
                fontSize = 9.5.sp,
                letterSpacing = 1.2.sp,
                color = c.ink3,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = note.text,
                fontFamily = EditorialSerif,
                fontSize = 16.5.sp,
                lineHeight = 29.sp,
                color = c.ink,
            )
        }
    }
}

/** 只读备注：整道菜的备注放在文档末尾。 */
private fun androidx.compose.foundation.lazy.LazyListScope.noteItems(notes: List<NoteEntity>) {
    val wholeNotes = notes.filter { it.stepId == null }.sortedByDescending { it.createdAt }
    if (wholeNotes.isEmpty()) return
    item { EditorialSectionHeader("04", "我的备注") }
    itemsIndexed(wholeNotes, key = { _, note -> note.id }) { _, note ->
        WholeNoteCard(note = note)
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.ingredientsContent(
    recipe: RecipeWithDetails,
    servings: Int,
    onServings: (Int) -> Unit,
) {
    item {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "份量",
                fontSize = 10.sp,
                letterSpacing = 1.2.sp,
                color = editorialColors().ink3,
                modifier = Modifier.weight(1f),
            )
            ServingsStepper(servings, onServings)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = "按 ${recipe.recipe.servings} 人份记录，已换算为 $servings 人份",
            fontSize = 10.5.sp,
            letterSpacing = 0.2.sp,
            color = editorialColors().ink3,
            modifier = Modifier.padding(horizontal = 22.dp),
        )
        Spacer(Modifier.height(12.dp))
    }

    if (recipe.ingredients.isEmpty()) {
        item {
            EmptyInfoCard("还没有食材，点击右上角编辑，从原文里补几行。")
        }
    } else {
        val grouped = recipe.ingredients.groupBy { it.groupName }
        grouped.forEach { (group, items) ->
            if (!group.isNullOrBlank()) {
                item {
                    Text(
                        text = group,
                        fontSize = 9.5.sp,
                        letterSpacing = 1.6.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = editorialColors().ink3,
                        modifier = Modifier.padding(start = 22.dp, top = 18.dp, bottom = 2.dp),
                    )
                }
            }
            itemsIndexed(items) { _, item ->
                IngredientRow(
                    item = item,
                    baseServings = recipe.recipe.servings,
                    servings = servings,
                )
            }
        }
    }

    item { Spacer(Modifier.height(8.dp)) }
}

@Composable
private fun ServingsStepper(value: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        StepButton("−") { onChange(value - 1) }
        Text(
            text = "$value 人",
            fontSize = 13.sp,
            letterSpacing = 0.2.sp,
            color = editorialColors().ink,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        StepButton("+") { onChange(value + 1) }
    }
}

@Composable
private fun StepButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 20.sp,
            fontWeight = FontWeight.Light,
            color = editorialColors().ink2,
        )
    }
}

@Composable
private fun IngredientRow(item: IngredientEntity, baseServings: Int, servings: Int) {
    val c = editorialColors()
    val amount = ingredientAmount(item, baseServings, servings)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = item.name,
            fontSize = 16.sp,
            letterSpacing = 0.1.sp,
            color = c.ink,
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
                .height(12.dp)
                .drawBehind {
                    val y = size.height - 3.dp.toPx()
                    drawLine(
                        color = c.ink.copy(alpha = 0.24f),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(1.dp.toPx(), 4.dp.toPx()),
                            0f,
                        ),
                    )
                },
        )
        Text(
            text = amount,
            fontSize = 14.sp,
            letterSpacing = 0.1.sp,
            color = if (amount == "适量" || amount == "少许") c.accent else c.ink2,
        )
    }
}

private fun ingredientAmount(item: IngredientEntity, baseServings: Int, servings: Int): String {
    if (item.amount == null || !item.scalable) {
        return item.rawText.takeIf { it.length <= 12 } ?: "适量"
    }
    val range = IngredientScaler.scaleRange(item.rawText, baseServings, servings)
    if (range != null) return range
    val scaled = IngredientScaler.scale(item.amount, baseServings, servings) ?: return item.rawText
    return IngredientScaler.format(scaled, item.unit)
}

@Composable
private fun OriginalTextSection(
    recipe: RecipeWithDetails,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    val c = editorialColors()
    val body = recipe.source?.textBody.orEmpty().ifBlank {
        recipe.source?.shareText.orEmpty()
    }
    Column(modifier = Modifier.fillMaxWidth().padding(top = 34.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(horizontal = 22.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = "03",
                fontFamily = EditorialSerif,
                fontSize = 12.sp,
                letterSpacing = 0.8.sp,
                color = c.accent,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "原文",
                fontFamily = EditorialSerif,
                fontSize = 25.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.3).sp,
                color = c.ink,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = if (expanded) "收起" else "展开",
                fontSize = 10.sp,
                letterSpacing = 0.6.sp,
                color = c.accent,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .padding(horizontal = 22.dp)
                .fillMaxWidth()
                .height(1.dp)
                .background(c.ink),
        )
        if (expanded) {
            Column(modifier = Modifier.padding(horizontal = 22.dp, vertical = 8.dp)) {
                if (body.isBlank()) {
                    Text(
                        text = "没有抓取到原文。可以在编辑页补充，或点来源条的原链接查看。",
                        fontSize = 13.sp,
                        lineHeight = 21.sp,
                        color = c.ink2,
                    )
                } else {
                    SelectionContainer {
                        Text(
                            text = body,
                            fontSize = 15.sp,
                            lineHeight = 26.sp,
                            color = c.ink,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "自动解析可能有遗漏。以原文和原视频为准。",
                    fontSize = 10.5.sp,
                    lineHeight = 17.sp,
                    color = c.ink3,
                )
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun EmptyInfoCard(text: String) {
    val c = editorialColors()
    Text(
        text = text,
        fontSize = 13.sp,
        lineHeight = 21.sp,
        color = c.ink2,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 10.dp),
    )
}

private data class StepMedia(
    val id: String,
    val relativePath: String,
    val isVideo: Boolean,
)

private data class ImageViewerTarget(
    val paths: List<String>,
    val index: Int,
)

private fun stepMedia(recipe: RecipeWithDetails, step: StepEntity, index: Int): List<StepMedia> {
    val own = recipe.media
        .filter { it.stepId == step.id }
        .sortedWith(compareBy({ it.position }, { it.createdAt }))
        .map { it.toStepMedia() }
    if (own.isNotEmpty()) return own

    val mediaPath = step.mediaPath
    if (!mediaPath.isNullOrBlank()) {
        return listOf(StepMedia(id = step.id, relativePath = mediaPath, isVideo = false))
    }

    // 旧数据没有 stepId：按导入顺序把 STEP 媒体对应到步骤。
    val legacy = recipe.media
        .filter { it.kind == MediaKind.STEP.name && it.stepId == null }
        .sortedBy { it.createdAt }
    return legacy.getOrNull(index)?.toStepMedia()?.let { listOf(it) } ?: emptyList()
}

private fun MediaEntity.toStepMedia(): StepMedia = StepMedia(
    id = id,
    relativePath = relativePath,
    isVideo = kind == MediaKind.VIDEO.name || mime.startsWith("video/"),
)

private fun formatNoteDate(millis: Long): String =
    SimpleDateFormat("M月d日", Locale.CHINA).format(Date(millis))

private fun formatNoteDateTime(millis: Long): String =
    SimpleDateFormat("M月d日 HH:mm", Locale.CHINA).format(Date(millis))

private fun vibrateOnce(context: Context, view: View) {
    val vibrated = runCatching {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)
                ?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        vibrator?.vibrate(
            VibrationEffect.createOneShot(180L, VibrationEffect.DEFAULT_AMPLITUDE),
        )
        vibrator != null
    }.getOrDefault(false)
    if (!vibrated) {
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }
}
