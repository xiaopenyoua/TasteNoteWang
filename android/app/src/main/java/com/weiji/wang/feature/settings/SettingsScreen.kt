package com.weiji.wang.feature.settings

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
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

enum class AccountLevel { OK, WARN, NONE }

data class AccountStatus(val label: String, val level: AccountLevel)

class SettingsViewModel(private val container: AppContainer) : ViewModel() {
    val storageBytes = container.recipes.observeStorageBytes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)
    val theme = container.settings.theme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.SYSTEM)
    val wifiOnly = container.settings.wifiOnly
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)
    val autoDownload = container.settings.autoDownloadMedia
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch { container.settings.setTheme(mode) }
    }

    fun setWifiOnly(value: Boolean) {
        viewModelScope.launch { container.settings.setWifiOnly(value) }
    }

    fun setAutoDownload(value: Boolean) {
        viewModelScope.launch { container.settings.setAutoDownloadMedia(value) }
    }

    fun accountStatus(platform: Platform): AccountStatus {
        val saved = container.cookies.cookieFor(platform) != null
        if (!saved) {
            return if (platform == Platform.XIAOHONGSHU) {
                AccountStatus("网页登录受限", AccountLevel.WARN)
            } else {
                AccountStatus("未登录", AccountLevel.NONE)
            }
        }
        val updatedAt = container.cookies.lastUpdated(platform)
        if (updatedAt > 0 && System.currentTimeMillis() - updatedAt > STALE_AFTER_MS) {
            return AccountStatus("建议重新登录", AccountLevel.WARN)
        }
        return AccountStatus("已登录", AccountLevel.OK)
    }

    private companion object {
        const val STALE_AFTER_MS = 30L * 24 * 60 * 60 * 1000
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
    var resumeTick by remember { mutableIntStateOf(0) }

    LifecycleResumeEffect(Unit) {
        resumeTick += 1
        onPauseOrDispose { }
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
                    chevron = true,
                    onClick = { nav.navigate("storage") },
                )
                HairlineRow()
                SettingsRow(
                    title = "数据迁移",
                    value = "迁出 / 迁入",
                    chevron = true,
                    onClick = { nav.navigate("migrate") },
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
            }
        }

        item { SectionHeader(title = "平台账号") }
        item {
            SettingsGroup {
                PlatformAccounts.forEachIndexed { index, platform ->
                    if (index > 0) HairlineRow()
                    val status = remember(resumeTick, platform) {
                        vm.accountStatus(platform)
                    }
                    SettingsRow(
                        title = platform.label,
                        chevron = true,
                        trailing = { AccountStatusView(status) },
                        onClick = { nav.navigate("login/${platform.key}") },
                    )
                }
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
                        text = "卸载应用会删除本机数据；换机或卸载前，请先使用「数据迁移」导出。",
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

private val PlatformAccounts = listOf(
    Platform.DOUYIN,
    Platform.XIAOHONGSHU,
    Platform.BILIBILI,
)

@Composable
private fun AccountStatusView(status: AccountStatus) {
    val color = when (status.level) {
        AccountLevel.OK -> MaterialTheme.colorScheme.tertiary
        AccountLevel.WARN -> MaterialTheme.colorScheme.primary
        AccountLevel.NONE -> MaterialTheme.colorScheme.outline
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = status.label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
    chevron: Boolean = false,
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
        if (chevron) {
            Spacer(Modifier.width(6.dp))
            Text(
                text = "›",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.outline,
                fontWeight = FontWeight.Normal,
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
