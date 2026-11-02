package com.weiji.wang.feature.login

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavHostController
import com.weiji.wang.core.import.HttpClients
import com.weiji.wang.core.model.Platform
import com.weiji.wang.ui.LocalContainer
import com.weiji.wang.ui.components.PageHeader
import com.weiji.wang.ui.components.PrimaryButton
import com.weiji.wang.ui.components.SecondaryButton

private data class LoginTarget(
    val platform: Platform,
    val domain: String,
    val startUrl: String,
    val title: String,
)

private fun targetOf(key: String): LoginTarget = when (key.uppercase()) {
    "DOUYIN" -> LoginTarget(Platform.DOUYIN, "https://www.douyin.com", "https://www.douyin.com/", "抖音")
    "XIAOHONGSHU" -> LoginTarget(Platform.XIAOHONGSHU, "https://www.xiaohongshu.com", "https://www.xiaohongshu.com/", "小红书")
    "BILIBILI" -> LoginTarget(Platform.BILIBILI, "https://www.bilibili.com", "https://passport.bilibili.com/login", "B 站")
    else -> LoginTarget(Platform.OTHER, "https://www.douyin.com", "https://www.douyin.com/", "平台")
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LoginScreen(nav: NavHostController, platformKey: String) {
    val container = LocalContainer.current
    val target = remember(platformKey) { targetOf(platformKey) }
    val context = LocalContext.current
    var webView by remember { mutableStateOf<WebView?>(null) }
    var loggedIn by remember { mutableStateOf(container.cookies.has(target.platform)) }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { webView?.stopLoading(); webView?.destroy() }
            webView = null
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        PageHeader(
            title = "登录${target.title}",
            subtitle = "登录信息只保存在本机，用于解析你主动提交的链接",
            actions = {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .padding(8.dp),
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "关闭",
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { nav.popBackStack() }
                            .padding(4.dp),
                    )
                }
            },
        )

        AndroidView(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.databaseEnabled = true
                    settings.userAgentString = HttpClients.MOBILE_UA
                    CookieManager.getInstance().setAcceptCookie(true)
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                    webViewClient = WebViewClient()
                    loadUrl(target.startUrl)
                    webView = this
                }
            },
            update = { webView = it },
        )

        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "在页面里完成登录，然后点下面按钮。味记不会读取你的账号或密码，只保存平台发放的 Cookie。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))
            Row {
                Box(modifier = Modifier.weight(1f)) {
                    PrimaryButton(
                        text = if (loggedIn) "已保存，返回" else "我已登录",
                        onClick = {
                            val cookie = runCatching {
                                CookieManager.getInstance().getCookie(target.domain)
                            }.getOrNull()
                            if (cookie.isNullOrBlank()) {
                                Toast.makeText(context, "还没有读到登录信息，请先完成登录", Toast.LENGTH_SHORT).show()
                            } else {
                                container.cookies.saveWithTime(target.platform, cookie, System.currentTimeMillis())
                                loggedIn = true
                                Toast.makeText(context, "已保存登录信息", Toast.LENGTH_SHORT).show()
                                nav.popBackStack()
                            }
                        },
                    )
                }
                Spacer(Modifier.padding(horizontal = 5.dp))
                SecondaryButton(
                    text = "清除",
                    onClick = {
                        container.cookies.clear(target.platform)
                        CookieManager.getInstance().removeAllCookies(null)
                        CookieManager.getInstance().flush()
                        loggedIn = false
                        Toast.makeText(context, "已清除", Toast.LENGTH_SHORT).show()
                    },
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "提示：登录态会过期。解析突然失败时，回来重新登录一次即可。",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }
    }
}
