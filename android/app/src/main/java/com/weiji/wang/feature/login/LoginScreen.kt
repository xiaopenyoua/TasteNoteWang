package com.weiji.wang.feature.login

import android.annotation.SuppressLint
import android.graphics.Color
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
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
        startUrl = "https://www.douyin.com/login_page?service=https%3A%2F%2Fwww.douyin.com%2F",
        userAgent = HttpClients.MOBILE_UA,
        cookieUrls = listOf("https://www.douyin.com", "https://douyin.com"),
        authKeys = listOf("sessionid", "sessionid_ss", "sid_tt", "sid_guard"),
        notice = "抖音网页登录在电脑上是扫码 / 验证码；手机里可以用验证码登录。" +
            "如果页面一直登录不上，用电脑浏览器登录抖音后复制 Cookie，点下面的「手动粘贴 Cookie」。",
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
        notice = "B 站使用移动版 H5 登录，支持手机号 / 账号密码。" +
            "页面上的协议说明没有单独的勾选框，填好手机号和验证码点「登录」即视为同意。",
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
    var showManual by rememberSaveable { mutableStateOf(false) }
    var showHelp by rememberSaveable { mutableStateOf(false) }
    var manualCookie by rememberSaveable { mutableStateOf("") }

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
                    // 平台登录页都按桌面视口写样式；关掉宽视口时抖音会只渲染出空页。
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true
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
                            view?.evaluateJavascript(pagePatch, null)
                        }

                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?,
                        ): Boolean {
                            val url = request?.url ?: return false
                            val scheme = url.scheme?.lowercase().orEmpty()
                            if (scheme.isEmpty() || scheme in browserSchemes) return false
                            intentFallback(url)?.let { fallback ->
                                view?.loadUrl(fallback)
                                return true
                            }
                            Toast.makeText(
                                context,
                                "页面想跳转到 App，已拦下；请用页面内的登录入口",
                                Toast.LENGTH_SHORT,
                            ).show()
                            return true
                        }
                    }
                    loadUrl(target.startUrl)
                    webView = this
                }
            },
            update = { webView = it },
        )

        Column(modifier = Modifier.padding(16.dp)) {
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
            Spacer(Modifier.height(2.dp))
            Text(
                text = if (showManual) "收起手动粘贴" else "页面登录不了？手动粘贴 Cookie",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { showManual = !showManual }
                    .padding(vertical = 4.dp),
            )
            if (showManual) {
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = manualCookie,
                    onValueChange = { manualCookie = it },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = MaterialTheme.typography.bodySmall,
                    placeholder = {
                        Text(
                            "在电脑浏览器登录后，从开发者工具里复制完整 Cookie 粘贴到这里。",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    },
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "保存这段 Cookie",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (manualCookie.isBlank()) MaterialTheme.colorScheme.tertiary
                    else MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(enabled = manualCookie.isNotBlank()) {
                            val raw = manualCookie.trim()
                            if (!hasAuthCookie(raw, target.authKeys)) {
                                Toast.makeText(
                                    context,
                                    "这段 Cookie 里没有 ${target.authKeys.first()}，请确认复制完整",
                                    Toast.LENGTH_LONG,
                                ).show()
                            } else {
                                setCookiesToWebView(target.cookieUrls, raw)
                                container.cookies.saveWithTime(
                                    target.platform,
                                    raw,
                                    System.currentTimeMillis(),
                                )
                                detected = true
                                saved = true
                                showManual = false
                                Toast.makeText(
                                    context,
                                    "已保存手动粘贴的 Cookie",
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                        }
                        .padding(vertical = 4.dp),
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
            Spacer(Modifier.height(2.dp))
            Text(
                text = if (showHelp) "收起说明" else "说明",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { showHelp = !showHelp }
                    .padding(vertical = 4.dp),
            )
            if (showHelp) {
                target.notice?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                    Spacer(Modifier.height(6.dp))
                }
                Text(
                    text = "在页面里完成登录，然后点上面按钮。味记不会读取你的账号或密码，" +
                        "只保存平台发放的认证 Cookie。登录态会过期，解析突然失败时回来重新登录一次即可。",
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
            }
        }
    }
}

private val browserSchemes = setOf("http", "https", "about", "data", "javascript", "file", "blob")

/**
 * 这个 WebView 内核里 vh / 百分比高度会算成 0（初始包含块高度为 0），平台登录页因此
 * 塌成一团：B 站「登录」按钮压住用户协议，抖音的登录卡片整个跑到屏幕上方看不见。
 * 这里用 window.innerHeight 的像素值兜底（不用 %，% 同样会塌）；vh 正常时结果一致。
 * 同时把协议行里被样式藏起来的勾选框还原出来（不代替用户勾选）。
 */
private const val pagePatch = """
(function () {
  var h = window.innerHeight;
  var style = document.getElementById('weiji-vh');
  if (!style) {
    style = document.createElement('style');
    style.id = 'weiji-vh';
    document.documentElement.appendChild(style);
  }
  style.textContent =
    'html{height:' + h + 'px !important}' +
    'body{min-height:' + h + 'px !important}' +
    '.login-wrap{min-height:' + h + 'px !important}' +
    '.explain-tips input[type=checkbox],.explain-tips [class*=check]{' +
    'opacity:1 !important;visibility:visible !important;' +
    'width:16px !important;height:16px !important;' +
    'min-width:16px !important;min-height:16px !important;display:inline-block !important}';
})();
"""

/** intent:// 里带的 S.browser_fallback_url：没装 App 时回到网页，而不是停在空白页。 */
private fun intentFallback(url: Uri): String? {
    val fragment = url.fragment ?: return null
    val marker = "S.browser_fallback_url="
    val start = fragment.indexOf(marker)
    if (start < 0) return null
    val value = fragment.substring(start + marker.length).substringBefore(';')
    return runCatching { Uri.decode(value) }
        .getOrNull()
        ?.takeIf { it.startsWith("http://") || it.startsWith("https://") }
}

private fun setCookiesToWebView(urls: List<String>, cookie: String) {
    val manager = CookieManager.getInstance()
    cookie.split(';').map { it.trim() }.filter { it.contains('=') }.forEach { pair ->
        urls.forEach { url -> runCatching { manager.setCookie(url, pair) } }
    }
    manager.flush()
}

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
