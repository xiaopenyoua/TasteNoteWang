package com.weiji.wang.feature.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import com.weiji.wang.BuildConfig
import com.weiji.wang.core.AppContainer
import com.weiji.wang.core.data.prefs.ThemeMode
import com.weiji.wang.core.model.Platform
import com.weiji.wang.ui.LocalContainer
import com.weiji.wang.ui.components.PageHeader
import com.weiji.wang.ui.components.SectionHeader
import com.weiji.wang.ui.components.SegmentedControl
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SettingsViewModel(private val container: AppContainer) : ViewModel() {
    val storageBytes = container.recipes.observeStorageBytes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)
    val theme = container.settings.theme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.SYSTEM)
    val wifiOnly = container.settings.wifiOnly
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)
    val autoDownload = container.settings.autoDownloadMedia
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    var cookieRevision by mutableIntStateOf(0)
        private set

    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch { container.settings.setTheme(mode) }
    }

    fun setWifiOnly(value: Boolean) {
        viewModelScope.launch { container.settings.setWifiOnly(value) }
    }

    fun setAutoDownload(value: Boolean) {
        viewModelScope.launch { container.settings.setAutoDownloadMedia(value) }
    }

    fun hasCookie(platform: Platform): Boolean = container.cookies.has(platform)

    fun clearCookies() {
        container.cookies.clearAll()
        cookieRevision += 1
    }

    fun export(uri: android.net.Uri, onDone: (String) -> Unit) {
        viewModelScope.launch {
            runCatching { container.backup.exportTo(uri, includeMedia = true) }
                .onSuccess { onDone("已导出 ${it.recipes} 道菜谱") }
                .onFailure { onDone("导出失败：${it.message}") }
        }
    }

    fun restore(uri: android.net.Uri, onDone: (String) -> Unit) {
        viewModelScope.launch {
            runCatching { container.backup.restoreFrom(uri) }
                .onSuccess { onDone("恢复完成：${it.imported} 道，跳过 ${it.skipped} 道") }
                .onFailure { onDone("恢复失败：${it.message}") }
        }
    }
}

@Composable
fun SettingsScreen(nav: NavHostController) {
    val container = LocalContainer.current
    val vm: SettingsViewModel = viewModel(
        factory = viewModelFactory { initializer { SettingsViewModel(container) } },
    )
    val bytes by vm.storageBytes.collectAsStateWithLifecycle()
    val theme by vm.theme.collectAsStateWithLifecycle()
    val wifiOnly by vm.wifiOnly.collectAsStateWithLifecycle()
    val autoDownload by vm.autoDownload.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val cookieRevision = vm.cookieRevision

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri ->
        if (uri != null) {
            vm.export(uri) { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            vm.restore(uri) { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp),
    ) {
        item { PageHeader(title = "我的", subtitle = "所有内容只保存在本机") }

        item { SectionHeader(title = "数据") }
        item {
            SettingsGroup {
                SettingsRow(
                    title = "存储空间",
                    value = formatBytes(bytes),
                )
                HairlineRow()
                SettingsRow(
                    title = "导出备份",
                    value = "含图片与视频",
                    onClick = {
                        val name = "weiji-backup-${SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())}.zip"
                        exportLauncher.launch(name)
                    },
                )
                HairlineRow()
                SettingsRow(
                    title = "从备份恢复",
                    value = "选择 .zip",
                    onClick = { importLauncher.launch(arrayOf("application/zip")) },
                )
            }
        }

        item { SectionHeader(title = "收录") }
        item {
            SettingsGroup {
                SettingsRow(
                    title = "仅 Wi-Fi 下载媒体",
                    trailing = {
                        Switch(checked = wifiOnly, onCheckedChange = vm::setWifiOnly)
                    },
                )
                HairlineRow()
                SettingsRow(
                    title = "自动下载视频与图片",
                    trailing = {
                        Switch(checked = autoDownload, onCheckedChange = vm::setAutoDownload)
                    },
                )
                HairlineRow()
                SettingsRow(
                    title = "抖音登录",
                    value = if (vm.hasCookie(Platform.DOUYIN)) "已登录" else "未登录",
                    onClick = { nav.navigate("login/DOUYIN") },
                )
                HairlineRow()
                SettingsRow(
                    title = "小红书登录",
                    value = if (vm.hasCookie(Platform.XIAOHONGSHU)) "已登录" else "未登录",
                    onClick = { nav.navigate("login/XIAOHONGSHU") },
                )
                HairlineRow()
                SettingsRow(
                    title = "B 站登录",
                    value = if (vm.hasCookie(Platform.BILIBILI)) "已登录" else "未登录",
                    onClick = { nav.navigate("login/BILIBILI") },
                )
                HairlineRow()
                SettingsRow(
                    title = "清除全部登录信息",
                    danger = true,
                    onClick = {
                        vm.clearCookies()
                        Toast.makeText(context, "已清除", Toast.LENGTH_SHORT).show()
                    },
                )
            }
        }

        item { SectionHeader(title = "体验") }
        item {
            SettingsGroup {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("外观", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(8.dp))
                    SegmentedControl(
                        options = listOf("跟随系统", "浅色", "深色"),
                        selectedIndex = when (theme) {
                            ThemeMode.SYSTEM -> 0
                            ThemeMode.LIGHT -> 1
                            ThemeMode.DARK -> 2
                        },
                        onSelect = {
                            vm.setTheme(
                                when (it) {
                                    0 -> ThemeMode.SYSTEM
                                    1 -> ThemeMode.LIGHT
                                    else -> ThemeMode.DARK
                                },
                            )
                        },
                    )
                }
            }
        }

        item { SectionHeader(title = "关于") }
        item {
            SettingsGroup {
                SettingsRow(title = "版本", value = BuildConfig.VERSION_NAME)
                HairlineRow()
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "味记・王 是一个只给自己用的厨房笔记。没有账号、没有广告、没有上传。平台登录信息仅用于你主动发起的链接解析，加密保存在本机，不会进入备份。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "卸载应用会删除本机数据；换机或卸载前，请先导出备份。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "请只用于个人离线查看；下载内容与账号风险由使用者承担。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsGroup(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface),
    ) {
        content()
    }
}

@Composable
private fun SettingsRow(
    title: String,
    value: String? = null,
    danger: Boolean = false,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (trailing != null) {
            trailing()
        } else if (value != null) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HairlineRow() {
    Box(
        Modifier
            .padding(start = 16.dp)
            .fillMaxWidth()
            .height(0.5.dp)
            .background(MaterialTheme.colorScheme.outlineVariant),
    )
}

private fun formatBytes(bytes: Long): String = when {
    bytes <= 0 -> "暂无媒体"
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "%.0f KB".format(bytes / 1024.0)
    bytes < 1024L * 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024))
    else -> "%.2f GB".format(bytes / (1024.0 * 1024 * 1024))
}
