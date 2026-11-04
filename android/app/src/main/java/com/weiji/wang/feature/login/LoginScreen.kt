package com.weiji.wang.feature.login

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Base64
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.weiji.wang.core.import.HttpClients
import com.weiji.wang.core.model.Platform
import com.weiji.wang.ui.LocalContainer
import com.weiji.wang.ui.components.CircleIconButton
import com.weiji.wang.ui.components.PageHeader
import com.weiji.wang.ui.components.PrimaryButton
import com.weiji.wang.ui.components.SecondaryButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONTokener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume

private data class LoginTarget(
    val platform: Platform,
    val title: String,
    val startUrl: String,
    val userAgent: String,
    val cookieUrls: List<String>,
    val authKeys: List<String>,
    val notice: String? = null,
    val subtitle: String = "登录信息只保存在本机，用于解析你主动提交的链接",
)

private fun targetOf(key: String): LoginTarget = when (key.uppercase()) {
    "DOUYIN" -> LoginTarget(
        platform = Platform.DOUYIN,
        title = "抖音",
        // 移动版登录页只有验证码/密码，扫码入口只在桌面版；UA 只在登录页切桌面。
        startUrl = "https://www.douyin.com/login_page?service=https%3A%2F%2Fwww.douyin.com%2F",
        userAgent = HttpClients.DESKTOP_UA,
        cookieUrls = listOf("https://www.douyin.com", "https://douyin.com"),
        authKeys = listOf("sessionid", "sessionid_ss", "sid_tt", "sid_guard"),
        notice = "推荐扫码：在页面顶部切到「扫码登录」，点应用里的「保存二维码到相册」，" +
            "再打开抖音「扫一扫」→ 相册选择。同一台手机也能完成。" +
            "扫码登录不会让抖音 App 掉线；短信验证码会被当成新设备，可能要求二次扫码，也可能让 App 退出登录。",
        subtitle = "不登录也能解析大多数公开视频；登录只针对受限内容",
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

    val scope = rememberCoroutineScope()
    var savingQr by remember { mutableStateOf(false) }
    // 抖音二维码直接从登录页 DOM 里取 data URI，不截整屏：相册里只有二维码，识别率更高。
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var qrSrc by remember { mutableStateOf("") }
    var qrError by remember { mutableStateOf<String?>(null) }
    var showWebLogin by rememberSaveable { mutableStateOf(false) }

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

    // 桌面版登录页在 mount 时就会请求二维码；轮询 DOM 拿它的 data URI，二维码刷新时自动跟着换。
    LaunchedEffect(target, pageLoaded, webView) {
        if (target.platform != Platform.DOUYIN) return@LaunchedEffect
        var waited = 0
        while (true) {
            val view = webView
            if (pageLoaded && view != null) {
                val src = runCatching { view.evaluateString(QR_IMAGE_JS) }.getOrNull().orEmpty()
                if (src.isNotBlank() && src != qrSrc) {
                    val decoded = decodeQrDataUri(src)?.let { raw ->
                        // 原图 128px，放大 4 倍用最近邻保持模块边缘锐利；相册里的图也直接用这份。
                        if (raw.width < 480) {
                            Bitmap.createScaledBitmap(raw, raw.width * 4, raw.height * 4, false)
                        } else {
                            raw
                        }
                    }
                    if (decoded != null) {
                        qrBitmap = decoded
                        qrSrc = src
                        qrError = null
                        waited = 0
                    }
                } else if (qrBitmap == null) {
                    waited += 1
                    if (waited > 12) {
                        qrError = "二维码没有出来，点「刷新二维码」重试"
                    }
                }
            }
            delay(1_200)
        }
    }


    fun saveQrToGallery(bitmap: Bitmap) {
        if (savingQr) return
        savingQr = true
        val name = "味记-抖音登录二维码-" + qrTimestamp()
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { saveBitmapToGallery(context, bitmap, name) }
            }
            savingQr = false
            result.fold(
                onSuccess = { uri ->
                    Toast.makeText(
                        context,
                        if (uri != null) "已存到相册：打开抖音 → 扫一扫 → 相册选择"
                        else "保存失败：系统相册没有返回可写入的位置",
                        Toast.LENGTH_LONG,
                    ).show()
                },
                onFailure = { error ->
                    Toast.makeText(
                        context,
                        "保存失败：${error.message ?: "相册不可写"}",
                        Toast.LENGTH_LONG,
                    ).show()
                },
            )
        }
    }

    /** 保存的是二维码原图（放大 4 倍保持模块边缘锐利），不是整屏截图。 */
    fun captureAndSaveQr() {
        val qr = qrBitmap
        if (qr == null) {
            Toast.makeText(context, "二维码还没出来，等一秒再试或点刷新", Toast.LENGTH_SHORT).show()
            return
        }
        saveQrToGallery(qr)
    }

    val writePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            captureAndSaveQr()
        } else {
            Toast.makeText(context, "没有相册写入权限，保存不了二维码", Toast.LENGTH_LONG).show()
        }
    }

    fun requestSaveQr() {
        if (!pageLoaded) {
            Toast.makeText(context, "页面还没加载完，等二维码出来再保存", Toast.LENGTH_SHORT).show()
            return
        }
        // API 29 起走 MediaStore 的 RELATIVE_PATH，不再需要存储权限。
        val needsLegacyPermission = Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
            ) != PackageManager.PERMISSION_GRANTED
        if (needsLegacyPermission) {
            writePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            captureAndSaveQr()
        }
    }

    fun openDouyinApp() {
        val launch = context.packageManager.getLaunchIntentForPackage(DOUYIN_PACKAGE)
        if (launch == null) {
            Toast.makeText(context, "未安装抖音", Toast.LENGTH_SHORT).show()
            return
        }
        runCatching { context.startActivity(launch) }
            .onFailure { Toast.makeText(context, "未安装抖音", Toast.LENGTH_SHORT).show() }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        PageHeader(
            title = "登录${target.title}",
            subtitle = target.subtitle,
            actions = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircleIconButton(
                        icon = Icons.Filled.Refresh,
                        contentDescription = "刷新",
                        onClick = {
                            detected = false
                            slowHint = false
                            qrBitmap = null
                            qrSrc = ""
                            qrError = null
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

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            AndroidView(
                // 桌面版登录页在 <900px 视口会把二维码面板整个省掉（2026-11-04 实测：
                // 390/768 宽无 QR，900/1024 宽有）。所以扫码模式下给 WebView 一个 1024dp
                // 的宽视口再完全透明盖住；页面照常跑 JS/轮询，只是不显示。
                modifier = if (target.platform == Platform.DOUYIN && !showWebLogin) {
                    Modifier
                        .requiredWidth(1024.dp)
                        .requiredHeight(900.dp)
                        .alpha(0f)
                } else {
                    Modifier.fillMaxSize()
                },
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
                update = { view ->
                    webView = view
                    // 二维码面板盖在上面时禁用底层 WebView 的触控，避免点击穿透到桌面登录页。
                    view.isEnabled = target.platform != Platform.DOUYIN || showWebLogin
                },
            )
            if (target.platform == Platform.DOUYIN && !showWebLogin) {
                DouyinQrPane(
                    qr = qrBitmap,
                    pageLoaded = pageLoaded,
                    error = qrError,
                    saving = savingQr,
                    onSave = { requestSaveQr() },
                    onOpenDouyin = { openDouyinApp() },
                    onRefresh = {
                        qrBitmap = null
                        qrSrc = ""
                        qrError = null
                        detected = false
                        pageLoaded = false
                        webView?.reload()
                    },
                    onUseWeb = { showWebLogin = true },
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                )
            }
        }

        Column(modifier = Modifier.padding(16.dp)) {
            if (target.platform == Platform.DOUYIN && showWebLogin) {
                Text(
                    text = "← 返回扫码登录",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showWebLogin = false }
                        .padding(vertical = 6.dp),
                )
                Spacer(Modifier.height(6.dp))
            }
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

/** 抖音扫码登录：二维码由桌面版登录页生成，这里只负责展示与保存，页面继续在底层轮询扫码结果。 */
@Composable
private fun DouyinQrPane(
    qr: Bitmap?,
    pageLoaded: Boolean,
    error: String?,
    saving: Boolean,
    onSave: () -> Unit,
    onOpenDouyin: () -> Unit,
    onRefresh: () -> Unit,
    onUseWeb: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "扫码登录",
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "打开抖音「扫一扫」，或先保存到相册再从相册选择。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .size(240.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(ComposeColor.White)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
                .padding(10.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (qr != null) {
                Image(
                    bitmap = qr.asImageBitmap(),
                    contentDescription = "抖音登录二维码",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                )
            } else {
                CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.5.dp)
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = when {
                error != null -> error
                qr != null -> "二维码 2 分钟内有效，过期后会自动换新"
                pageLoaded -> "正在生成二维码…"
                else -> "正在打开抖音登录页…"
            },
            style = MaterialTheme.typography.labelSmall,
            color = if (error != null) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QrAction(
                text = if (saving) "正在保存…" else "保存到相册",
                enabled = qr != null && !saving,
                onClick = onSave,
            )
            QrAction(
                text = "打开抖音",
                enabled = true,
                onClick = onOpenDouyin,
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = "同一台手机也能完成：保存到相册 → 打开抖音 → 扫一扫 → 相册选择。" +
                "扫码登录不会让抖音 App 掉线；短信验证码会被当成新设备，可能要求二次扫码，" +
                "也可能让抖音 App 退出登录。",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = "刷新二维码",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onRefresh)
                .padding(horizontal = 10.dp, vertical = 7.dp),
        )
        Text(
            text = "改用网页登录（验证码 / 密码）",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onUseWeb)
                .padding(horizontal = 10.dp, vertical = 7.dp),
        )
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

/** 桌面版登录页把二维码渲染成 data:image/png;base64 的 <img>，直接取原图比截屏干净。 */
private const val QR_IMAGE_JS = """
(function () {
  try {
    var img = document.querySelector('#douyin_login_comp_scan_code img[aria-label="二维码"]')
      || document.querySelector('#douyin_login_comp_scan_code img')
      || document.querySelector('img[aria-label="二维码"]');
    if (!img) return '';
    var src = img.getAttribute('src') || img.src || '';
    return src.indexOf('data:image') === 0 ? src : '';
  } catch (e) { return ''; }
})();
"""

private suspend fun WebView.evaluateString(js: String): String? =
    suspendCancellableCoroutine { continuation ->
        evaluateJavascript(js) { raw ->
            if (continuation.isActive) {
                val value = runCatching {
                    (JSONTokener(raw).nextValue() as? String)?.takeIf { it.isNotBlank() }
                }.getOrNull()
                continuation.resume(value)
            }
        }
    }

private fun decodeQrDataUri(src: String): Bitmap? {
    val marker = "base64,"
    val index = src.indexOf(marker)
    if (index < 0) return null
    return runCatching {
        val bytes = Base64.decode(src.substring(index + marker.length), Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }.getOrNull()
}

/** 抖音「扫一扫 → 相册」只认系统相册里的图片，所以二维码要落到这里。 */
private const val DOUYIN_PACKAGE = "com.ss.android.ugc.aweme"
private const val QR_ALBUM_DIR = "Pictures/味记"

private fun qrTimestamp(): String =
    SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())

@Suppress("DEPRECATION")
private fun saveBitmapToGallery(context: Context, bitmap: Bitmap, name: String): Uri? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        saveViaMediaStore(context.contentResolver, bitmap, name)
    } else {
        MediaStore.Images.Media
            .insertImage(context.contentResolver, bitmap, name, QR_ALBUM_DIR)
            ?.let(Uri::parse)
    }

/** API 29+：RELATIVE_PATH 直接进 Pictures/味记；写完再清 IS_PENDING，相册里不会出现半张图。 */
private fun saveViaMediaStore(resolver: ContentResolver, bitmap: Bitmap, name: String): Uri {
    val pending = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "$name.png")
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        put(MediaStore.Images.Media.RELATIVE_PATH, QR_ALBUM_DIR)
        put(MediaStore.Images.Media.IS_PENDING, 1)
    }
    val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    val uri = resolver.insert(collection, pending) ?: error("相册不可写")
    try {
        val stream = resolver.openOutputStream(uri) ?: error("相册不可写")
        stream.use { out ->
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) error("二维码编码失败")
        }
        resolver.update(
            uri,
            ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) },
            null,
            null,
        )
    } catch (t: Throwable) {
        runCatching { resolver.delete(uri, null, null) }
        throw t
    }
    return uri
}

/** 二维码相关的两枚动作按钮：等宽、最小 48dp 触控高度，未就绪时置灰不可点。 */
@Composable
private fun RowScope.QrAction(text: String, enabled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = Modifier
            .weight(1f)
            .heightIn(min = 48.dp)
            .clip(shape)
            .border(
                1.dp,
                if (enabled) MaterialTheme.colorScheme.outline
                else MaterialTheme.colorScheme.outlineVariant,
                shape,
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (enabled) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            textAlign = TextAlign.Center,
        )
    }
}

