package com.weiji.wang.feature.login

import android.annotation.SuppressLint
import android.graphics.Color
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import com.weiji.wang.ui.components.CircleIconButton
import com.weiji.wang.ui.components.PageHeader
import com.weiji.wang.ui.components.PrimaryButton
import com.weiji.wang.ui.components.SecondaryButton
import kotlinx.coroutines.delay

private data class LoginTarget(
    val platform: Platform,
    val title: String,
    val startUrl: String,
    val userAgent: String,
    val cookieUrls: List<String>,
    val authKeys: List<String>,
    val notice: String? = null,
)

private fun targetOf(key: String): LoginTarget = when (key.uppercase()) {
    "DOUYIN" -> LoginTarget(
        platform = Platform.DOUYIN,
        title = "抖音",
        startUrl = "https://www.douyin.com/",
        userAgent = HttpClients.MOBILE_UA,
        cookieUrls = listOf("https://www.douyin.com", "https://douyin.com"),
        authKeys = listOf("sessionid", "sessionid_ss", "sid_tt", "sid_guard"),
    )

    "XIAOHONGSHU" -> LoginTarget(
        platform = Platform.XIAOHONGSHU,
        title = "小红书",
        startUrl = "https://www.xiaohongshu.com/login",
        userAgent = HttpClients.DESKTOP_UA,
        cookieUrls = listOf("https://www.xiaohongshu.com"),
        authKeys = listOf("web_session"),
        notice = "小红书网页登录可能受限：页面可能要求打开 App 或触发风控。" +
            "系统浏览器登录不会把 Cookie 带回应用；拿不到 web_session 时请只保留链接。",
    )

    "BILIBILI" -> LoginTarget(
        platform = Platform.BILIBILI,
        title = "B 站",
        startUrl = "https://passport.bilibili.com/h5-app/passport/login",
        userAgent = HttpClients.MOBILE_UA,
        cookieUrls = listOf("https://passport.bilibili.com", "https://www.bilibili.com"),
        authKeys = listOf("SESSDATA"),
        notice = "B 站使用移动版 H5 登录，只支持手机号 / 密码；本应用不提供扫码登录。",
    )

    else -> LoginTarget(
        platform = Platform.OTHER,
        title = "平台",
        startUrl = "https://www.douyin.com/",
        userAgent = HttpClients.MOBILE_UA,
        cookieUrls = listOf("https://www.douyin.com"),
        authKeys = listOf("sessionid"),
    )
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LoginScreen(nav: NavHostController, platformKey: String) {
    val container = LocalContainer.current
    val target = remember(platformKey) { targetOf(platformKey) }
    val context = LocalContext.current
    var webView by remember { mutableStateOf<WebView?>(null) }
    var pageLoaded by remember { mutableStateOf(false) }
    var detected by remember { mutableStateOf(false) }
    var saved by remember {
        mutableStateOf(
            hasAuthCookie(container.cookies.cookieFor(target.platform), target.authKeys),
        )
    }
    var slowHint by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { webView?.stopLoading(); webView?.destroy() }
            webView = null
        }
    }

    LaunchedEffect(target, pageLoaded) {
        if (!pageLoaded) return@LaunchedEffect
        var hits = 0
        while (true) {
            val authenticated = hasAuthCookie(readCookie(target), target.authKeys)
            hits = if (authenticated) hits + 1 else 0
            if (hits >= 2) {
                detected = true
                return@LaunchedEffect
            }
            delay(1_000)
        }
    }

    LaunchedEffect(pageLoaded) {
        if (!pageLoaded) return@LaunchedEffect
        delay(12_000)
        if (!detected && !saved) slowHint = true
    }

    Column(modifier = Modifier.fillMaxSize()) {
        PageHeader(
            title = "登录${target.title}",
            subtitle = "登录信息只保存在本机，用于解析你主动提交的链接",
            actions = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircleIconButton(
                        icon = Icons.Filled.Refresh,
                        contentDescription = "刷新",
                        onClick = {
                            detected = false
                            slowHint = false
                            pageLoaded = false
                            webView?.reload()
                        },
                    )
                    CircleIconButton(
                        icon = Icons.Filled.Close,
                        contentDescription = "关闭",
                        onClick = { nav.popBackStack() },
                    )
                }
            },
        )

        if (!pageLoaded) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(2.dp))
        }

        AndroidView(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.databaseEnabled = true
                    settings.userAgentString = target.userAgent
                    settings.setSupportZoom(true)
                    settings.builtInZoomControls = true
                    settings.displayZoomControls = false
                    setBackgroundColor(Color.WHITE)
                    CookieManager.getInstance().setAcceptCookie(true)
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            pageLoaded = true
                        }

                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?,
                        ): Boolean {
                            val scheme = request?.url?.scheme?.lowercase().orEmpty()
                            return if (scheme.isNotEmpty() && scheme !in browserSchemes) {
                                Toast.makeText(
                                    context,
                                    "页面想打开 App，已拦截，请使用页面内登录入口",
                                    Toast.LENGTH_SHORT,
                                ).show()
                                true
                            } else {
                                false
                            }
                        }
                    }
                    loadUrl(target.startUrl)
                    webView = this
                }
            },
            update = { webView = it },
        )

        Column(modifier = Modifier.padding(16.dp)) {
            target.notice?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
                Spacer(Modifier.height(8.dp))
            }
            Text(
                text = "在页面里完成登录，然后点下面按钮。味记不会读取你的账号或密码，只保存平台发放的认证 Cookie。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (slowHint && !detected) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "还没有检测到认证 Cookie。若页面已完成登录，请稍等或点右上角刷新；" +
                        "仍失败时，该平台可能限制了网页登录。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            if (detected || saved) MaterialTheme.colorScheme.tertiary
                            else MaterialTheme.colorScheme.primary,
                        ),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = when {
                        detected -> "已检测到登录信息"
                        saved -> "已保存登录信息"
                        else -> "未检测到登录信息"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton(
                    text = "清除",
                    onClick = {
                        clearPlatformCookies(target)
                        CookieManager.getInstance().flush()
                        container.cookies.clear(target.platform)
                        detected = false
                        saved = false
                        slowHint = false
                        webView?.reload()
                        Toast.makeText(context, "已清除本平台登录信息", Toast.LENGTH_SHORT).show()
                    },
                )
                Box(modifier = Modifier.weight(1f)) {
                    PrimaryButton(
                        text = if (saved && !detected) "已保存，返回" else "保存并返回",
                        enabled = detected || saved,
                        onClick = {
                            val cookie = readCookie(target)
                            if (!hasAuthCookie(cookie, target.authKeys)) {
                                Toast.makeText(
                                    context,
                                    "还没有检测到认证 Cookie，请先完成登录",
                                    Toast.LENGTH_SHORT,
                                ).show()
                            } else {
                                CookieManager.getInstance().flush()
                                container.cookies.saveWithTime(
                                    target.platform,
                                    cookie,
                                    System.currentTimeMillis(),
                                )
                                Toast.makeText(context, "已保存登录信息", Toast.LENGTH_SHORT).show()
                                nav.popBackStack()
                            }
                        },
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "提示：登录态会过期。解析突然失败时，回来重新登录一次即可。",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }
    }
}

private val browserSchemes = setOf("http", "https", "about", "data", "javascript", "file", "blob")

private fun readCookie(target: LoginTarget): String =
    target.cookieUrls.mapNotNull { url ->
        runCatching { CookieManager.getInstance().getCookie(url) }.getOrNull()
    }.filter { it.isNotBlank() }.joinToString("; ")

private fun hasAuthCookie(cookie: String?, keys: List<String>): Boolean {
    if (cookie.isNullOrBlank()) return false
    return cookie.split(';').any { part ->
        val name = part.substringBefore('=').trim()
        val value = part.substringAfter('=', "").trim()
        name in keys && value.isNotEmpty()
    }
}

private fun clearPlatformCookies(target: LoginTarget) {
    target.cookieUrls.forEach { url ->
        target.authKeys.forEach { key ->
            runCatching {
                CookieManager.getInstance().setCookie(url, "$key=; Max-Age=0; Path=/")
            }
        }
    }
}
