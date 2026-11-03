package com.weiji.wang.feature.recipe

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
import coil.compose.AsyncImage
import com.weiji.wang.R
import com.weiji.wang.core.data.MenuRepository
import com.weiji.wang.core.data.RecipeRepository
import com.weiji.wang.core.data.db.IngredientEntity
import com.weiji.wang.core.data.db.MediaEntity
import com.weiji.wang.core.data.db.NoteEntity
import com.weiji.wang.core.data.db.RecipeWithDetails
import com.weiji.wang.core.data.db.StepEntity
import com.weiji.wang.core.model.IngredientScaler
import com.weiji.wang.core.model.MediaKind
import com.weiji.wang.ui.LocalContainer
import com.weiji.wang.ui.components.CircleIconButton
import com.weiji.wang.ui.components.ConfirmDialog
import com.weiji.wang.ui.components.EmptyState
import com.weiji.wang.ui.components.MetaChip
import com.weiji.wang.ui.components.PrimaryButton
import com.weiji.wang.ui.components.RecipeCover
import com.weiji.wang.ui.components.SectionHeader
import com.weiji.wang.ui.components.SegmentedControl
import com.weiji.wang.ui.components.SourceBar
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

class RecipeDetailViewModel(
    private val recipeId: String,
    private val repo: RecipeRepository,
    private val menu: MenuRepository,
) : ViewModel() {
    private val loadedState = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = loadedState

    val details = repo.observeDetails(recipeId)
        .onEach { loadedState.value = true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val notes = repo.observeNotes(recipeId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val isToday = menu.observeToday()
        .map { entries -> entries.any { it.recipeId == recipeId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun toggleFavorite(current: Boolean) {
        viewModelScope.launch { repo.setFavorite(recipeId, !current) }
    }

    /** 今晚做这道：写入今天菜单与采购；同一天重复点击是幂等的。 */
    fun addToday(servings: Int, onResult: (Boolean) -> Unit) {
        viewModelScope.launch { onResult(menu.addToday(recipeId, servings)) }
    }

    fun saveNote(stepId: String?, existingId: String?, text: String) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            if (existingId != null) {
                repo.updateNote(existingId, clean)
            } else {
                repo.addNote(recipeId, stepId, clean)
            }
        }
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
            initializer { RecipeDetailViewModel(recipeId, container.recipes, container.menu) }
        },
    )
    val details by vm.details.collectAsStateWithLifecycle()
    val notes by vm.notes.collectAsStateWithLifecycle()
    val isToday by vm.isToday.collectAsStateWithLifecycle()
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

    DetailContent(nav = nav, vm = vm, recipe = recipe, notes = notes, isToday = isToday)
}

@Composable
private fun DetailContent(
    nav: NavHostController,
    vm: RecipeDetailViewModel,
    recipe: RecipeWithDetails,
    notes: List<NoteEntity>,
    isToday: Boolean,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var servings by rememberSaveable(recipe.recipe.id) {
        mutableIntStateOf(recipe.recipe.servings.coerceIn(1, 12))
    }
    var showDelete by remember { mutableStateOf(false) }
    var noteEditor by remember { mutableStateOf<NoteEditorState?>(null) }
    var viewer by remember { mutableStateOf<ImageViewerTarget?>(null) }
    var stepVideo by remember { mutableStateOf<String?>(null) }
    var fullscreenOriginal by remember { mutableStateOf(false) }

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

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item {
                RecipeCoverHeader(
                    nav = nav,
                    coverPath = recipe.recipe.coverPath,
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
            item {
                RecipeHeader(
                    recipe = recipe,
                    servings = servings,
                    tab = tab,
                    onSelectTab = { tab = it },
                )
            }

            when (tab) {
                0 -> stepsContent(
                    recipe = recipe,
                    notes = notes,
                    timers = timerList,
                    videoPlayer = videoPlayer,
                    videoFullscreenActive = fullscreenOriginal,
                    onTimerToggle = { stepId -> toggleTimer(stepId) },
                    onTimerRemove = { stepId -> timers.remove(stepId) },
                    onNoteEdit = { stepId, existing ->
                        noteEditor = NoteEditorState(
                            stepId = stepId,
                            existing = existing,
                            title = if (existing == null) "记一笔" else "编辑备注",
                        )
                    },
                    onImageClick = { paths, initial ->
                        viewer = ImageViewerTarget(paths, initial)
                    },
                    onVideoClick = { path -> stepVideo = path },
                    onFullscreenVideo = { fullscreenOriginal = true },
                )
                1 -> ingredientsContent(
                    recipe = recipe,
                    servings = servings,
                    isToday = isToday,
                    onServings = { servings = it.coerceIn(1, 12) },
                    onAddToday = {
                        vm.addToday(servings) { ok ->
                            scope.launch {
                                snackbar.showSnackbar(
                                    if (ok) "已加入今天吃什么" else "加入失败，请稍后重试",
                                )
                            }
                        }
                    },
                )
                else -> originalContent(recipe)
            }
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        )
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

    noteEditor?.let { editor ->
        NoteEditorDialog(
            title = editor.title,
            description = if (editor.stepId != null) {
                "给这一步的备注，下次做的时候还能看到。"
            } else {
                "给整道菜的备注，下次做的时候还能看到。"
            },
            initialText = editor.existing?.text.orEmpty(),
            onDismiss = { noteEditor = null },
            onSave = { text ->
                vm.saveNote(editor.stepId, editor.existing?.id, text)
                noteEditor = null
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

@Composable
private fun RecipeCoverHeader(
    nav: NavHostController,
    coverPath: String?,
    favorite: Boolean,
    onFavorite: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onOpenCover: () -> Unit,
) {
    val wide = LocalConfiguration.current.screenWidthDp >= 600
    Box {
        RecipeCover(
            relativePath = coverPath,
            modifier = Modifier
                .fillMaxWidth()
                .then(if (wide) Modifier.height(320.dp) else Modifier.aspectRatio(16f / 9f)),
            shape = RoundedCornerShape(0.dp),
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(enabled = coverPath != null, onClick = onOpenCover),
        )
        CircleIconButton(
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "返回",
            onClick = { nav.popBackStack() },
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(8.dp),
            size = 40.dp,
            iconSize = 22.dp,
            background = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f),
        )
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CircleIconButton(
                icon = if (favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = if (favorite) "取消收藏" else "收藏",
                onClick = onFavorite,
                size = 40.dp,
                iconSize = 20.dp,
                background = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f),
                tint = if (favorite) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            CircleIconButton(
                icon = Icons.Filled.Edit,
                contentDescription = "编辑",
                onClick = onEdit,
                size = 40.dp,
                iconSize = 20.dp,
                background = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            CircleIconButton(
                icon = Icons.Filled.Delete,
                contentDescription = "删除",
                onClick = onDelete,
                size = 40.dp,
                iconSize = 20.dp,
                background = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RecipeHeader(
    recipe: RecipeWithDetails,
    servings: Int,
    tab: Int,
    onSelectTab: (Int) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(14.dp))
        Text(
            text = recipe.recipe.title,
            style = MaterialTheme.typography.headlineLarge,
        )
        if (recipe.recipe.subtitle.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = recipe.recipe.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(6.dp))
        SourceBar(recipe.source)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetaChip("${servings} 人份")
            recipe.recipe.totalMinutes?.let { MetaChip("约 $it 分钟") }
            if (recipe.media.any { it.kind == MediaKind.VIDEO.name }) MetaChip("含视频")
        }
        Spacer(Modifier.height(14.dp))
        SegmentedControl(
            options = listOf("步骤", "食材", "原文"),
            selectedIndex = tab,
            onSelect = onSelectTab,
        )
        Spacer(Modifier.height(12.dp))
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.stepsContent(
    recipe: RecipeWithDetails,
    notes: List<NoteEntity>,
    timers: List<StepTimer>,
    videoPlayer: ExoPlayer?,
    videoFullscreenActive: Boolean,
    onTimerToggle: (String) -> Unit,
    onTimerRemove: (String) -> Unit,
    onNoteEdit: (String?, NoteEntity?) -> Unit,
    onImageClick: (List<String>, Int) -> Unit,
    onVideoClick: (String) -> Unit,
    onFullscreenVideo: () -> Unit,
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

    if (videoPlayer != null) {
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                Text(
                    text = "原视频",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                if (videoFullscreenActive) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "全屏播放中",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    VideoPlayerCard(
                        player = videoPlayer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        onFullscreen = onFullscreenVideo,
                    )
                }
            }
        }
    }

    if (steps.isEmpty()) {
        item {
            EmptyInfoCard("还没有拆出步骤。可以切到「原文」看来源全文，或点右上角编辑补充。")
        }
    } else {
        itemsIndexed(steps, key = { _, step -> step.id }) { index, step ->
            val stepNotes = notes.filter { it.stepId == step.id }
                .sortedByDescending { it.createdAt }
            val media = stepMedia(recipe, step, index)
            StepCard(
                number = index + 1,
                text = step.text,
                durationSeconds = step.durationSeconds,
                timer = timers.firstOrNull { it.stepId == step.id },
                media = media,
                note = stepNotes.firstOrNull(),
                noteCount = stepNotes.size,
                onTimerClick = { onTimerToggle(step.id) },
                onAddNote = { onNoteEdit(step.id, null) },
                onEditNote = { stepNotes.firstOrNull()?.let { onNoteEdit(step.id, it) } },
                onImageClick = onImageClick,
                onVideoClick = onVideoClick,
            )
        }
    }

    item {
        Spacer(Modifier.height(10.dp))
        SectionHeader(
            title = "我的备注",
            actionLabel = "记一笔",
            onAction = { onNoteEdit(null, null) },
        )
    }
    val wholeNotes = notes.filter { it.stepId == null }.sortedByDescending { it.createdAt }
    if (wholeNotes.isEmpty()) {
        item { EmptyNoteCard() }
    } else {
        itemsIndexed(wholeNotes, key = { _, note -> note.id }) { _, note ->
            WholeNoteCard(note = note, onEdit = { onNoteEdit(null, note) })
        }
    }
    item { Spacer(Modifier.height(16.dp)) }
}

@Composable
private fun StepCard(
    number: Int,
    text: String,
    durationSeconds: Int?,
    timer: StepTimer?,
    media: List<StepMedia>,
    note: NoteEntity?,
    noteCount: Int,
    onTimerClick: () -> Unit,
    onAddNote: () -> Unit,
    onEditNote: () -> Unit,
    onImageClick: (List<String>, Int) -> Unit,
    onVideoClick: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "$number",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = "第 $number 步",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.weight(1f))
            if (durationSeconds != null) {
                StepTimerCapsule(
                    timer = timer,
                    durationSeconds = durationSeconds,
                    onClick = onTimerClick,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = text.ifBlank { "按图示操作" },
            style = MaterialTheme.typography.bodyLarge,
        )
        if (media.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            val images = media.filter { !it.isVideo }.map { it.relativePath }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(media, key = { _, item -> item.id }) { mediaIndex, item ->
                    MediaThumb(
                        media = item,
                        order = mediaIndex + 1,
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
        if (note != null) {
            StepNoteCard(note = note, count = noteCount, onEdit = onEditNote)
        } else {
            AddStepNote(onClick = onAddNote)
        }
    }
}

@Composable
private fun StepTimerCapsule(
    timer: StepTimer?,
    durationSeconds: Int,
    onClick: () -> Unit,
) {
    val highlighted = timer?.running == true || timer?.finished == true
    val label = when {
        timer == null -> "${formatStepDuration(durationSeconds)} · 计时"
        timer.finished -> "时间到"
        timer.running -> formatCountdown(timer.remainingSeconds)
        else -> "继续 · ${formatCountdown(timer.remainingSeconds)}"
    }
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(
                if (highlighted) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                else MaterialTheme.colorScheme.surfaceVariant,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (highlighted) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun MediaThumb(
    media: StepMedia,
    order: Int,
    onClick: () -> Unit,
) {
    val files = LocalContainer.current.files
    val file = remember(media.relativePath, files) { files.resolve(media.relativePath) }
    val exists = remember(file) { file.exists() && file.length() > 0L }
    Box(
        modifier = Modifier
            .size(width = 104.dp, height = 78.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (exists && !media.isVideo) {
            AsyncImage(
                model = file,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        } else {
            Icon(
                painter = painterResource(R.drawable.ic_bowl),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(4.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.Black.copy(alpha = 0.45f))
                .padding(horizontal = 4.dp, vertical = 1.dp),
        ) {
            Text(
                text = "$order",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
            )
        }
        if (media.isVideo) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "播放视频",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun StepNoteCard(note: NoteEntity, count: Int, onEdit: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .height(IntrinsicSize.Min),
    ) {
        Box(
            Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.primary),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp, vertical = 8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = buildString {
                        append("备注 · ${formatNoteDate(note.createdAt)}")
                        if (count > 1) append(" · 共 $count 条")
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "编辑",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onEdit)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = note.text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun AddStepNote(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .clip(RoundedCornerShape(8.dp))
            .dashedBorder(MaterialTheme.colorScheme.outline, 8.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp),
    ) {
        Text(
            text = "＋ 添加这一步的备注",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun WholeNoteCard(note: NoteEntity, onEdit: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .height(IntrinsicSize.Min),
    ) {
        Box(
            Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.primary),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formatNoteDateTime(note.createdAt),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "编辑",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onEdit)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = note.text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun EmptyNoteCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(20.dp),
    ) {
        Text(
            text = "还没有备注。做完一次，记一句「哪里不一样」，下次就不用重新想。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun NoteEditorDialog(
    title: String,
    description: String,
    initialText: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var text by rememberSaveable(initialText) { mutableStateOf(initialText) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    minLines = 2,
                    maxLines = 6,
                    shape = RoundedCornerShape(12.dp),
                    placeholder = { Text("比如：盐少放一点，下次多炒 1 分钟") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(text) },
                enabled = text.isNotBlank(),
            ) {
                Text(
                    text = "保存",
                    color = if (text.isNotBlank()) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.tertiary,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = MaterialTheme.colorScheme.primary)
            }
        },
    )
}

private fun androidx.compose.foundation.lazy.LazyListScope.ingredientsContent(
    recipe: RecipeWithDetails,
    servings: Int,
    isToday: Boolean,
    onServings: (Int) -> Unit,
    onAddToday: () -> Unit,
) {
    item {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "份量",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            ServingsStepper(servings, onServings)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = "按 ${recipe.recipe.servings} 人份记录，已换算为 $servings 人份",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(Modifier.height(8.dp))
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
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 4.dp),
                    )
                }
            }
            itemsIndexed(items) { index, item ->
                IngredientRow(
                    item = item,
                    baseServings = recipe.recipe.servings,
                    servings = servings,
                )
                if (index < items.size - 1) {
                    Box(
                        Modifier
                            .padding(start = 20.dp)
                            .fillMaxWidth()
                            .height(0.5.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant),
                    )
                }
            }
        }
    }

    item {
        Spacer(Modifier.height(16.dp))
        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
            Column {
                PrimaryButton(
                    text = if (isToday) "已加入今天吃什么" else "今晚做这道",
                    onClick = onAddToday,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "加入今天吃什么，并把食材加入采购清单",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ServingsStepper(value: Int, onChange: (Int) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        StepButton("−") { onChange(value - 1) }
        Text(
            text = "$value 人",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 6.dp),
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
        Text(label, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun IngredientRow(item: IngredientEntity, baseServings: Int, servings: Int) {
    val amount = ingredientAmount(item, baseServings, servings)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = item.name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = amount,
            style = MaterialTheme.typography.bodyLarge,
            color = if (amount == "适量" || amount == "少许") MaterialTheme.colorScheme.tertiary
            else MaterialTheme.colorScheme.onSurface,
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

private fun androidx.compose.foundation.lazy.LazyListScope.originalContent(recipe: RecipeWithDetails) {
    val body = recipe.source?.textBody.orEmpty().ifBlank {
        recipe.source?.shareText.orEmpty()
    }
    item {
        Column(modifier = Modifier.padding(16.dp)) {
            if (body.isBlank()) {
                Text(
                    text = "没有抓取到原文。可以在编辑页补充，或点上方原链接查看。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                SelectionContainer {
                    Text(
                        text = body,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            Text(
                text = "自动解析可能有遗漏。以原文和原视频为准。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }
    }
}

@Composable
private fun EmptyInfoCard(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(20.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private data class StepMedia(
    val id: String,
    val relativePath: String,
    val isVideo: Boolean,
)

private data class NoteEditorState(
    val stepId: String?,
    val existing: NoteEntity?,
    val title: String,
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

private fun Modifier.dashedBorder(color: Color, cornerRadius: Dp): Modifier = drawBehind {
    drawRoundRect(
        color = color,
        cornerRadius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx()),
        style = Stroke(
            width = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(
                floatArrayOf(6.dp.toPx(), 4.dp.toPx()),
            ),
        ),
    )
}

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
