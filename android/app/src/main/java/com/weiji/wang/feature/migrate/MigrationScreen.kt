package com.weiji.wang.feature.migrate

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import com.weiji.wang.core.AppContainer
import com.weiji.wang.core.storage.ExportResult
import com.weiji.wang.core.storage.MigrationEstimate
import com.weiji.wang.core.storage.MigrationManager
import com.weiji.wang.core.storage.MigrationPreview
import com.weiji.wang.core.storage.RestoreResult
import com.weiji.wang.feature.storage.formatBytes
import com.weiji.wang.ui.LocalContainer
import com.weiji.wang.ui.components.CircleIconButton
import com.weiji.wang.ui.components.ConfirmDialog
import com.weiji.wang.ui.components.PageHeader
import com.weiji.wang.ui.components.PrimaryButton
import com.weiji.wang.ui.components.SecondaryButton
import com.weiji.wang.ui.components.SectionHeader
import com.weiji.wang.ui.components.SegmentedControl
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class MigrationMode { HUB, EXPORT, IMPORT }

class MigrationViewModel(container: AppContainer) : ViewModel() {
    private val manager = MigrationManager(
        context = container.appContext,
        db = container.database,
        backup = container.backup,
        files = container.files,
    )

    var estimate by mutableStateOf<MigrationEstimate?>(null)
        private set
    var estimateError by mutableStateOf<String?>(null)
        private set
    var preview by mutableStateOf<MigrationPreview?>(null)
        private set
    var previewError by mutableStateOf<String?>(null)
        private set
    var previewLoading by mutableStateOf(false)
        private set
    var busy by mutableStateOf(false)
        private set
    var exportResult by mutableStateOf<ExportResult?>(null)
        private set
    var restoreResult by mutableStateOf<RestoreResult?>(null)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    init {
        loadEstimate()
    }

    fun loadEstimate() {
        viewModelScope.launch {
            estimateError = null
            runCatching { manager.estimate() }
                .onSuccess { estimate = it }
                .onFailure { estimateError = it.message ?: "读取失败" }
        }
    }

    fun export(uri: Uri, includeMedia: Boolean) {
        if (busy) return
        viewModelScope.launch {
            busy = true
            error = null
            try {
                exportResult = manager.exportTo(uri, includeMedia)
            } catch (t: Throwable) {
                error = t.message ?: "打包失败"
            } finally {
                busy = false
            }
        }
    }

    fun loadPreview(uri: Uri) {
        viewModelScope.launch {
            preview = null
            previewError = null
            previewLoading = true
            runCatching { manager.preview(uri) }
                .onSuccess { preview = it }
                .onFailure { previewError = it.message ?: "无法读取迁移包" }
            previewLoading = false
        }
    }

    fun restore(uri: Uri, replaceAll: Boolean) {
        if (busy) return
        viewModelScope.launch {
            busy = true
            error = null
            try {
                restoreResult = manager.restoreFrom(uri, replaceAll)
            } catch (t: Throwable) {
                error = t.message ?: "导入失败"
            } finally {
                busy = false
            }
        }
    }

    fun clearError() {
        error = null
    }

    fun resetOutcome() {
        error = null
        exportResult = null
        restoreResult = null
        previewError = null
        loadEstimate()
    }
}

@Composable
fun MigrationScreen(nav: NavHostController) {
    val container = LocalContainer.current
    val vm: MigrationViewModel = viewModel(
        factory = viewModelFactory { initializer { MigrationViewModel(container) } },
    )
    var mode by remember { mutableStateOf(MigrationMode.HUB) }
    var allData by remember { mutableStateOf(true) }
    var mergeMode by remember { mutableStateOf(0) }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var showOverwriteConfirm by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri ->
        if (uri != null) vm.export(uri, includeMedia = allData)
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            selectedUri = uri
            mergeMode = 0
            vm.loadPreview(uri)
            mode = MigrationMode.IMPORT
        }
    }

    val migrationFileName = "weiji-migration-${SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())}.zip"

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxWidth()
                .fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp),
        ) {
            item {
                PageHeader(
                    title = "数据迁移",
                    subtitle = "换到新设备时，把全部数据带过去",
                    actions = {
                        CircleIconButton(
                            icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "返回",
                            onClick = {
                                if (!vm.busy) {
                                    val atHub = mode == MigrationMode.HUB &&
                                        vm.exportResult == null &&
                                        vm.restoreResult == null &&
                                        vm.error == null
                                    if (atHub) {
                                        nav.popBackStack()
                                    } else {
                                        vm.resetOutcome()
                                        mode = MigrationMode.HUB
                                    }
                                }
                            },
                        )
                    },
                )
            }

            when {
                vm.busy -> item {
                    ProgressCard(
                        title = if (mode == MigrationMode.IMPORT) "正在导入" else "正在打包",
                        meta = "迁移大文件时需要一些时间，请保持应用在前台。",
                    )
                }

                vm.exportResult != null -> item {
                    val result = vm.exportResult!!
                    ResultCard(
                        title = "迁移包已生成",
                        body = "已写入 ${result.recipes} 道菜谱 · ${formatBytes(result.bytes)}。" +
                            "迁移包内含全部菜谱与视频，迁移完成后请删除。",
                        success = true,
                        primaryLabel = "完成",
                        onPrimary = {
                            vm.resetOutcome()
                            mode = MigrationMode.HUB
                        },
                    )
                }

                vm.restoreResult != null -> item {
                    val result = vm.restoreResult!!
                    ResultCard(
                        title = "导入完成",
                        body = "导入 ${result.imported} 道 · 跳过 ${result.skipped} 道 · 失败 ${result.failed} 道。" +
                            "建议重启应用后检查。",
                        success = true,
                        primaryLabel = "返回中心页",
                        onPrimary = {
                            vm.resetOutcome()
                            mode = MigrationMode.HUB
                        },
                    )
                }

                vm.error != null -> item {
                    ResultCard(
                        title = "迁移中断",
                        body = vm.error.orEmpty(),
                        success = false,
                        primaryLabel = "重试",
                        onPrimary = { vm.clearError() },
                        secondaryLabel = "返回中心页",
                        onSecondary = {
                            vm.resetOutcome()
                            mode = MigrationMode.HUB
                        },
                    )
                }

                mode == MigrationMode.HUB -> item {
                    HubContent(
                        estimate = vm.estimate,
                        estimateError = vm.estimateError,
                        onRetryEstimate = vm::loadEstimate,
                        onExport = {
                            vm.resetOutcome()
                            mode = MigrationMode.EXPORT
                        },
                        onImport = {
                            vm.resetOutcome()
                            mode = MigrationMode.IMPORT
                        },
                    )
                }

                mode == MigrationMode.EXPORT -> item {
                    ExportContent(
                        estimate = vm.estimate,
                        allData = allData,
                        onAllData = { allData = it },
                        onStart = { exportLauncher.launch(migrationFileName) },
                        onBack = { mode = MigrationMode.HUB },
                    )
                }

                else -> item {
                    ImportContent(
                        preview = vm.preview,
                        previewError = vm.previewError,
                        previewLoading = vm.previewLoading,
                        mergeMode = mergeMode,
                        onMergeMode = { mergeMode = it },
                        onChoose = { importLauncher.launch(arrayOf("application/zip")) },
                        onStart = {
                            val uri = selectedUri
                            if (uri != null) {
                                if (mergeMode == 1 && (vm.estimate?.recipes ?: 0) > 0) {
                                    showOverwriteConfirm = true
                                } else {
                                    vm.restore(uri, replaceAll = mergeMode == 1)
                                }
                            }
                        },
                        onBack = { mode = MigrationMode.HUB },
                    )
                }
            }
        }
    }

    if (showOverwriteConfirm) {
        ConfirmDialog(
            title = "覆盖本机数据？",
            body = "本机已有 ${vm.estimate?.recipes ?: 0} 道菜谱。覆盖会先清空本机菜谱、媒体、备注、" +
                "历史与采购清单，再写入迁移包，且不可撤销。只想补齐数据请选择「合并」。",
            confirmLabel = "清空并覆盖",
            onDismiss = { showOverwriteConfirm = false },
            onConfirm = {
                showOverwriteConfirm = false
                selectedUri?.let { vm.restore(it, replaceAll = true) }
            },
        )
    }
}

@Composable
private fun HubContent(
    estimate: MigrationEstimate?,
    estimateError: String?,
    onRetryEstimate: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (estimateError != null) {
                Text("估算失败", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = estimateError,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                TextButton(onClick = onRetryEstimate) { Text("重试") }
            } else if (estimate == null) {
                Text(
                    text = "计算中…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(text = formatBytes(estimate.bytes), style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${estimate.files} 个文件 · ${estimate.recipes} 道菜谱",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        PrimaryButton(text = "迁出（旧设备）", onClick = onExport)
        Spacer(Modifier.height(10.dp))
        SecondaryButton(text = "迁入（新设备）", onClick = onImport, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
        Text(
            text = "平台登录信息（Cookie）不会迁移，新设备需要重新登录。" +
                "迁移包不加密，内含全部菜谱和视频，迁移完成后请删除。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "当前版本不包含分卷与断点续传；超大文件建议先清理不需要的视频。",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.tertiary,
        )
    }
}

@Composable
private fun ExportContent(
    estimate: MigrationEstimate?,
    allData: Boolean,
    onAllData: (Boolean) -> Unit,
    onStart: () -> Unit,
    onBack: () -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        SectionHeader(title = "范围", horizontalPadding = 0)
        SegmentedControl(
            options = listOf("全部数据", "仅菜谱（不含视频）"),
            selectedIndex = if (allData) 0 else 1,
            onSelect = { onAllData(it == 0) },
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = estimate?.let { "预计 ${formatBytes(it.bytes)} · ${it.files} 个文件" }
                ?: "正在估算…",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (!allData) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "不含视频，实际生成的包会更小。",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = "用系统文件选择器选择保存位置。中断会留下不完整的包，重新打包即可。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(18.dp))
        PrimaryButton(text = "选择位置并开始打包", onClick = onStart)
        Spacer(Modifier.height(10.dp))
        SecondaryButton(text = "返回", onClick = onBack, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun ImportContent(
    preview: MigrationPreview?,
    previewError: String?,
    previewLoading: Boolean,
    mergeMode: Int,
    onMergeMode: (Int) -> Unit,
    onChoose: () -> Unit,
    onStart: () -> Unit,
    onBack: () -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        SecondaryButton(text = "选择迁移包", onClick = onChoose, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(14.dp))
        when {
            previewLoading -> Text(
                text = "正在读取迁移包…",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            previewError != null -> Text(
                text = previewError,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )

            preview != null -> Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp),
            ) {
                Text(
                    text = preview.fileName ?: "迁移包",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = formatBytes(preview.sizeBytes),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = buildString {
                        preview.exportedAt?.let {
                            append("导出 ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(Date(it))} · ")
                        }
                        append("${preview.recipeCount} 道菜谱 · ${preview.mediaFiles} 个媒体文件")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = if (preview.includesMedia) "包含视频与图片" else "不含媒体",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }

            else -> Text(
                text = "还没有选择迁移包。支持从文件管理器分享 .zip 到应用。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(18.dp))
        Text(
            text = "本机已有数据时",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
        SegmentedControl(
            options = listOf("合并", "覆盖"),
            selectedIndex = mergeMode,
            onSelect = onMergeMode,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = if (mergeMode == 1) {
                "覆盖会先清空本机菜谱、媒体、备注、历史与采购清单，再写入迁移包。"
            } else {
                "合并按菜谱 ID 去重：迁移包中更新时间较新的替换本机版本，较旧的跳过。"
            },
            style = MaterialTheme.typography.labelSmall,
            color = if (mergeMode == 1) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(18.dp))
        PrimaryButton(
            text = "开始导入",
            enabled = preview != null,
            onClick = onStart,
        )
        Spacer(Modifier.height(10.dp))
        SecondaryButton(text = "返回", onClick = onBack, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(14.dp))
        Text(
            text = "平台登录信息（Cookie）不会迁移，新设备需要重新登录。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ProgressCard(title: String, meta: String) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(32.dp))
        Spacer(Modifier.height(16.dp))
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            text = meta,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ResultCard(
    title: String,
    body: String,
    success: Boolean,
    primaryLabel: String,
    onPrimary: () -> Unit,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(
                    if (success) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                    else MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (success) Icons.Filled.Check else Icons.Filled.Close,
                contentDescription = null,
                tint = if (success) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(28.dp),
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(text = title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(18.dp))
        PrimaryButton(text = primaryLabel, onClick = onPrimary)
        if (secondaryLabel != null && onSecondary != null) {
            Spacer(Modifier.height(10.dp))
            SecondaryButton(text = secondaryLabel, onClick = onSecondary, modifier = Modifier.fillMaxWidth())
        }
        Spacer(Modifier.height(8.dp))
    }
}
