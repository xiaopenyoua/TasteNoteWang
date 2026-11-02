package com.weiji.wang.feature.capture

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import okhttp3.Request
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import com.weiji.wang.core.data.db.DraftEntity
import com.weiji.wang.core.import.DouyinJsonParser
import com.weiji.wang.core.import.DouyinWebViewParser
import com.weiji.wang.core.import.ExtractException
import com.weiji.wang.core.import.ExtractedContent
import com.weiji.wang.core.import.HttpClients
import com.weiji.wang.core.import.ImportCoordinator
import com.weiji.wang.core.import.ImportResult
import com.weiji.wang.core.import.LinkParser
import com.weiji.wang.core.import.PreparedImport
import com.weiji.wang.core.model.ImportState
import com.weiji.wang.core.model.Platform
import com.weiji.wang.ui.LocalContainer
import com.weiji.wang.ui.components.PageHeader
import com.weiji.wang.ui.components.PlatformBadge
import com.weiji.wang.ui.components.PrimaryButton
import com.weiji.wang.ui.components.SecondaryButton
import com.weiji.wang.ui.components.SectionHeader
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.json.JSONTokener

sealed interface CaptureState {
    data object Idle : CaptureState
    data object Preparing : CaptureState
    data class NeedsWebView(val prepared: PreparedImport, val content: ExtractedContent) : CaptureState
    data class Downloading(val percent: Int, val label: String) : CaptureState
    data class Success(val result: ImportResult) : CaptureState
    data class Failed(val message: String, val needsLogin: Boolean, val canRetry: Boolean) : CaptureState
}

class CaptureViewModel(
    private val importer: ImportCoordinator,
) : ViewModel() {
    var state by mutableStateOf<CaptureState>(CaptureState.Idle)
        private set

    val drafts = importer.observeDrafts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private var lastText: String = ""

    fun analyze(text: String) {
        if (text.isBlank()) {
            state = CaptureState.Failed("先粘贴一条链接", false, false)
            return
        }
        lastText = text
        state = CaptureState.Preparing
        viewModelScope.launch {
            try {
                val prepared = importer.prepare(text)
                if (prepared.content.requiresWebView) {
                    state = CaptureState.NeedsWebView(prepared, prepared.content)
                } else {
                    finalize(prepared, prepared.content)
                }
            } catch (e: ExtractException) {
                state = CaptureState.Failed(
                    e.message ?: "解析失败",
                    needsLogin = e.state == ImportState.NEEDS_LOGIN,
                    canRetry = true,
                )
            } catch (t: Throwable) {
                state = CaptureState.Failed(t.message ?: "解析失败", false, true)
            }
        }
    }

    fun onWebViewExtracted(content: ExtractedContent) {
        val current = state as? CaptureState.NeedsWebView ?: return
        viewModelScope.launch { finalize(current.prepared, content) }
    }

    fun retry() {
        if (lastText.isNotBlank()) analyze(lastText)
    }

    fun importLocal(uris: List<android.net.Uri>) {
        if (uris.isEmpty()) return
        state = CaptureState.Downloading(0, "正在导入图片/视频…")
        viewModelScope.launch {
            try {
                val result = importer.importLocal(uris, "")
                state = CaptureState.Success(result)
            } catch (t: Throwable) {
                state = CaptureState.Failed(t.message ?: "导入媒体失败", false, true)
            }
        }
    }

    fun onWebViewTimeout() {
        val current = state as? CaptureState.NeedsWebView ?: return
        viewModelScope.launch {
            finalize(
                current.prepared,
                current.content.copy(
                    requiresWebView = false,
                    fetchStatus = com.weiji.wang.core.model.FetchStatus.PARTIAL,
                    message = "页面加载超时，已保留链接、封面和文字；视频可以稍后重试。",
                ),
            )
        }
    }

    fun onWebViewFailed(reason: String) {
        val current = state as? CaptureState.NeedsWebView ?: return
        viewModelScope.launch {
            finalize(
                current.prepared,
                current.content.copy(
                    requiresWebView = false,
                    fetchStatus = com.weiji.wang.core.model.FetchStatus.PARTIAL,
                    message = "页面没有加载成功（$reason）。已保留链接、封面和文字，视频可以稍后重试。",
                ),
            )
        }
    }

    fun reset() {
        state = CaptureState.Idle
    }

    fun discard(draftId: String) {
        viewModelScope.launch { importer.discard(draftId) }
    }

    private suspend fun finalize(prepared: PreparedImport, content: ExtractedContent) {
        state = CaptureState.Downloading(0, "准备下载")
        try {
            val result = importer.finalize(prepared, content) { percent, label ->
                state = CaptureState.Downloading(percent, label)
            }
            state = CaptureState.Success(result)
        } catch (t: Throwable) {
            state = CaptureState.Failed(t.message ?: "保存失败", false, true)
        }
    }
}

@Composable
fun CaptureScreen(nav: NavHostController, initialText: String, initialUris: List<String> = emptyList()) {
    val container = LocalContainer.current
    val vm: CaptureViewModel = viewModel(
        factory = viewModelFactory { initializer { CaptureViewModel(container.importer) } },
    )
    val state = vm.state
    val drafts by vm.drafts.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current
    var text by rememberSaveable { mutableStateOf(initialText.ifBlank { "" }) }
    var autoStarted by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        if (text.isBlank()) {
            val clip = runCatching { clipboard.getText()?.text }.getOrNull().orEmpty()
            if (clip.contains("http")) text = clip
        }
    }

    LaunchedEffect(initialUris) {
        if (initialUris.isNotEmpty()) {
            vm.importLocal(initialUris.map { android.net.Uri.parse(android.net.Uri.decode(it)) })
        }
    }

    LaunchedEffect(text) {
        if (!autoStarted && text.isNotBlank() && text.contains("http")) {
            autoStarted = true
            delay(250)
            vm.analyze(text)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 40.dp),
    ) {
        item {
            PageHeader(
                title = "收录",
                subtitle = "粘贴一条抖音、小红书或 B 站链接",
                actions = {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable { nav.popBackStack() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "关闭")
                    }
                },
            )
        }
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it; if (state !is CaptureState.Idle) vm.reset() },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                    maxLines = 8,
                    shape = RoundedCornerShape(16.dp),
                    label = { Text("分享文案或链接") },
                    placeholder = { Text("例：复制打开抖音，看看【…】https://v.douyin.com/…") },
                )
                Spacer(Modifier.height(10.dp))
                val parsed = remember(text) { LinkParser.parse(text) }
                if (parsed != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PlatformBadge(parsed.platform)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = parsed.titleHint ?: parsed.url,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        PrimaryButton(
                            text = when (state) {
                                is CaptureState.Preparing, is CaptureState.Downloading -> "解析中…"
                                else -> "解析"
                            },
                            enabled = text.isNotBlank() && state !is CaptureState.Preparing && state !is CaptureState.Downloading,
                            onClick = { vm.analyze(text) },
                        )
                    }
                    SecondaryButton(text = "手动创建", onClick = { nav.navigate("edit/new") })
                }
                Spacer(Modifier.height(16.dp))
            }
        }

        item {
            when (val s = state) {
                is CaptureState.Idle -> Unit
                is CaptureState.Preparing -> StatusCard(title = "正在读取页面…", body = "本地解析，不上传你的链接。")
                is CaptureState.NeedsWebView -> {
                    StatusCard(
                        title = "正在用内置浏览器读取抖音",
                        body = "抖音需要页面渲染才能拿到视频地址。首次可能需要登录，登录信息只保存在本机。",
                    )
                    DouyinWebView(
                        url = s.content.webViewUrl ?: s.content.canonicalUrl,
                        fallbackUrl = s.content.canonicalUrl,
                        cookie = container.cookies.cookieFor(Platform.DOUYIN),
                        link = s.prepared.link,
                        onExtracted = vm::onWebViewExtracted,
                        onTimeout = vm::onWebViewTimeout,
                        onFailed = vm::onWebViewFailed,
                    )
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        SecondaryButton(
                            text = "去登录抖音",
                            onClick = { nav.navigate("login/DOUYIN") },
                        )
                        SecondaryButton(
                            text = "只保留链接",
                            onClick = {
                                vm.onWebViewExtracted(
                                    s.content.copy(
                                        requiresWebView = false,
                                        message = "已保留链接和文字，媒体稍后补",
                                    ),
                                )
                            },
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                }
                is CaptureState.Downloading -> Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    StatusCard(title = "正在保存", body = s.label)
                    LinearProgressIndicator(
                        progress = { if (s.percent < 0) 0.1f else s.percent / 100f },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(12.dp))
                }
                is CaptureState.Success -> Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    StatusCard(
                        title = if (s.result.partial) "已保存（部分内容）" else "收录完成",
                        body = s.result.message ?: "视频、图片和文字都已保存到本机。",
                        success = true,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(modifier = Modifier.weight(1f)) {
                            PrimaryButton("打开菜谱", onClick = {
                                nav.navigate("recipe/${s.result.recipeId}") { popUpTo("capture") { inclusive = true } }
                            })
                        }
                        SecondaryButton("再收一条", onClick = { text = ""; vm.reset() })
                    }
                    Spacer(Modifier.height(12.dp))
                }
                is CaptureState.Failed -> Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    StatusCard(title = "没有完整收录", body = s.message, error = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(modifier = Modifier.weight(1f)) {
                            SecondaryButton("重试", onClick = vm::retry)
                        }
                        if (s.needsLogin) {
                            Box(modifier = Modifier.weight(1f)) {
                                PrimaryButton("去登录", onClick = { nav.navigate("login/DOUYIN") })
                            }
                        } else {
                            Box(modifier = Modifier.weight(1f)) {
                                PrimaryButton("手动创建", onClick = { nav.navigate("edit/new") })
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
        }

        if (drafts.isNotEmpty()) {
            item { SectionHeader(title = "最近收录") }
            items(drafts, key = { it.id }) { draft ->
                DraftRow(
                    draft = draft,
                    onRetry = { vm.analyze(draft.shareText) },
                    onDiscard = { vm.discard(draft.id) },
                    onOpen = { draft.recipeId?.let { nav.navigate("recipe/$it") } },
                )
            }
        }
    }
}

@Composable
private fun StatusCard(
    title: String,
    body: String,
    success: Boolean = false,
    error: Boolean = false,
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            if (success) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(10.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun DraftRow(
    draft: DraftEntity,
    onRetry: () -> Unit,
    onDiscard: () -> Unit,
    onOpen: () -> Unit,
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable { if (draft.recipeId != null) onOpen() else onRetry() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlatformBadge(Platform.fromKey(draft.platform), compact = true)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = LinkParser.cleanShareNoise(draft.shareText).lineSequence().firstOrNull().orEmpty()
                    .ifBlank { draft.url ?: "${Platform.fromKey(draft.platform).label}内容" },
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
            )
            Text(
                text = stateLabel(draft.state, draft.error),
                style = MaterialTheme.typography.labelSmall,
                color = if (draft.state == ImportState.FAILED.name) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.tertiary,
            )
        }
        if (draft.state == ImportState.READY.name || draft.state == ImportState.PARTIAL.name) {
            Icon(
                Icons.Filled.Refresh,
                contentDescription = "删除",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(24.dp)
                    .clickable(onClick = onDiscard),
            )
        } else {
            Icon(
                Icons.Filled.Refresh,
                contentDescription = "重试",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(24.dp)
                    .clickable(onClick = onRetry),
            )
        }
    }
}

private fun stateLabel(state: String, error: String?): String = when (state) {
    ImportState.RECEIVED.name -> "等待解析"
    ImportState.RESOLVING.name -> "解析中"
    ImportState.NEEDS_LOGIN.name -> "需要登录"
    ImportState.EXTRACTED.name -> "已读取"
    ImportState.DOWNLOADING.name -> "下载中"
    ImportState.PROCESSING.name -> "处理中"
    ImportState.READY.name -> "已完成"
    ImportState.PARTIAL.name -> "部分完成"
    else -> error ?: "失败"
}

private const val HOOK_JS = """
(function(){
  if (window.__weiji_hooked) return;
  window.__weiji_hooked = true;
  window.__weiji_bodies = [];
  function push(u, b) {
    try {
      if (!u || !b) return;
      if (u.indexOf('/aweme/') >= 0 || u.indexOf('detail') >= 0) {
        window.__weiji_bodies.push(b);
        if (window.__weiji_bodies.length > 12) window.__weiji_bodies.shift();
      }
    } catch(e) {}
  }
  var of = window.fetch;
  if (of) {
    window.fetch = function() {
      return of.apply(this, arguments).then(function(resp){
        try {
          var u = resp.url || '';
          if (u.indexOf('/aweme/') >= 0 || u.indexOf('detail') >= 0) {
            resp.clone().text().then(function(t){ push(u, t); });
          }
        } catch(e) {}
        return resp;
      });
    };
  }
  var oo = XMLHttpRequest.prototype.open;
  var os = XMLHttpRequest.prototype.send;
  XMLHttpRequest.prototype.open = function(m, u) {
    this.__weiji_url = u;
    return oo.apply(this, arguments);
  };
  XMLHttpRequest.prototype.send = function() {
    var x = this;
    x.addEventListener('load', function(){
      try {
        var u = x.__weiji_url || '';
        if (u.indexOf('/aweme/') >= 0 || u.indexOf('detail') >= 0) {
          push(u, x.responseText);
        }
      } catch(e) {}
    });
    return os.apply(this, arguments);
  };
})();
"""

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun DouyinWebView(
    url: String,
    fallbackUrl: String,
    cookie: String?,
    link: com.weiji.wang.core.import.ParsedLink,
    onExtracted: (ExtractedContent) -> Unit,
    onTimeout: () -> Unit,
    onFailed: (String) -> Unit,
) {
    val context = LocalContext.current
    val captured = remember { linkedSetOf<String>() }
    val requestLog = remember { java.util.concurrent.atomic.AtomicInteger(0) }
    val http = remember { HttpClients.create() }
    var done by remember { mutableStateOf(false) }
    var triedFallback by remember { mutableStateOf(false) }
    var webView by remember { mutableStateOf<WebView?>(null) }

    fun replayUrl(target: String): String? = runCatching {
        val mergedCookie = listOfNotNull(
            cookie,
            CookieManager.getInstance().getCookie(target),
        ).joinToString("; ").ifBlank { null }
        val builder = Request.Builder()
            .url(target)
            .header("User-Agent", HttpClients.DESKTOP_UA)
            .header("Referer", "https://www.douyin.com/")
        if (mergedCookie != null) builder.header("Cookie", mergedCookie)
        http.newCall(builder.build()).execute().use { response ->
            android.util.Log.d(
                "WeijiImport",
                "replayUrl ${response.code} len=${response.body?.contentLength() ?: -1}",
            )
            if (response.isSuccessful) response.body?.string() else null
        }
    }.getOrNull()

    fun replayDetail(request: WebResourceRequest): String? = runCatching {
        if (request.method != "GET") return@runCatching null
        val target = request.url.toString()
        val mergedCookie = listOfNotNull(
            cookie,
            CookieManager.getInstance().getCookie(target),
        ).joinToString("; ").ifBlank { null }
        val builder = Request.Builder().url(target)
        request.requestHeaders.forEach { (key, value) ->
            if (key.equals("Host", true) || key.equals("Content-Length", true) ||
                key.equals("Connection", true) || key.equals("Accept-Encoding", true) ||
                key.equals("Cookie", true)
            ) {
                return@forEach
            }
            builder.header(key, value)
        }
        if (builder.build().header("User-Agent") == null) {
            builder.header("User-Agent", HttpClients.DESKTOP_UA)
        }
        if (mergedCookie != null) builder.header("Cookie", mergedCookie)
        http.newCall(builder.build()).execute().use { response ->
            android.util.Log.d(
                "WeijiImport",
                "replayDetail ${response.code} len=${response.body?.contentLength() ?: -1}",
            )
            if (response.isSuccessful) response.body?.string() else null
        }
    }.getOrNull()

    DisposableEffect(url) {
        onDispose {
            runCatching { webView?.stopLoading(); webView?.destroy() }
            webView = null
        }
    }

    LaunchedEffect(url) {
        delay(15_000)
        if (!done) onTimeout()
    }

    AndroidView(
        modifier = Modifier.size(1.dp).alpha(0.01f),
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.databaseEnabled = true
                settings.userAgentString = HttpClients.DESKTOP_UA
                settings.mediaPlaybackRequiresUserGesture = false
                settings.loadsImagesAutomatically = true
                settings.blockNetworkImage = false
                CookieManager.getInstance().setAcceptCookie(true)
                if (!cookie.isNullOrBlank()) {
                    runCatching {
                        CookieManager.getInstance().setCookie("https://www.douyin.com", cookie)
                        CookieManager.getInstance().setCookie("https://www.iesdouyin.com", cookie)
                    }
                }
                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest?,
                    ): WebResourceResponse? {
                        val requestUrl = request?.url?.toString().orEmpty()
                        val index = requestLog.getAndIncrement()
                        if (index < 60 ||
                            requestUrl.contains("aweme") ||
                            requestUrl.contains("/detail") ||
                            requestUrl.contains("play?")
                        ) {
                            android.util.Log.d("WeijiImport", "req[$index]: ${requestUrl.take(220)}")
                        }
                        val isContentApi = requestUrl.contains("aweme/detail") ||
                            requestUrl.contains("aweme/post") ||
                            requestUrl.contains("iteminfo")
                        if (isContentApi) {
                            android.util.Log.d("WeijiImport", "content api: ${requestUrl.take(180)}")
                            // 关键：不要把页面自己的签名请求替换成我们的响应，
                            // 否则页面拿不到数据、视频不会加载。后台重放只作为备用。
                            val req = request
                            if (req != null) {
                                Thread {
                                    val body = replayDetail(req)
                                    if (!body.isNullOrEmpty()) {
                                        android.util.Log.d("WeijiImport", "replay body: ${body.length}")
                                        val parsed = runCatching {
                                            DouyinJsonParser.parse(body, link)
                                        }.getOrNull()
                                        if (parsed != null) {
                                            view?.post {
                                                if (!done) {
                                                    done = true
                                                    onExtracted(parsed)
                                                }
                                            }
                                        }
                                    }
                                }.start()
                            }
                            return null
                        }
                        request?.url?.toString()?.let { candidate ->
                            if (candidate.contains("douyinvod", true) ||
                                candidate.contains("byteicdn", true) ||
                                candidate.contains("aweme/v1/play", true) ||
                                candidate.contains("douyinpic", true)
                            ) {
                                captured += candidate
                            }
                        }
                        return null
                    }

                    override fun onPageStarted(
                        view: WebView?,
                        startedUrl: String?,
                        favicon: android.graphics.Bitmap?,
                    ) {
                        super.onPageStarted(view, startedUrl, favicon)
                        android.util.Log.d("WeijiImport", "page start: ${startedUrl?.take(160)}")
                        view?.evaluateJavascript(HOOK_JS, null)
                    }

                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?,
                    ): Boolean {
                        val target = request?.url?.toString().orEmpty()
                        if (target.startsWith("http://") || target.startsWith("https://")) return false
                        android.util.Log.d("WeijiImport", "block scheme: ${target.take(140)}")
                        return true
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: android.webkit.WebResourceError?,
                    ) {
                        if (request?.isForMainFrame != true) return
                        val failedUrl = request.url?.toString().orEmpty()
                        android.util.Log.d(
                            "WeijiImport",
                            "page error ${error?.errorCode}: ${error?.description} @ $failedUrl",
                        )
                        // 非 http(s) 的主文档错误（例如页面尝试拉起 App）不算加载失败
                        if (!failedUrl.startsWith("http")) return
                        if (!triedFallback && fallbackUrl != failedUrl) {
                            triedFallback = true
                            android.util.Log.d("WeijiImport", "fallback -> $fallbackUrl")
                            view?.loadUrl(fallbackUrl)
                            return
                        }
                        if (!done) {
                            done = true
                            onFailed(error?.description?.toString() ?: "页面加载失败")
                        }
                    }

                    override fun onReceivedHttpError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        errorResponse: android.webkit.WebResourceResponse?,
                    ) {
                        if (request?.isForMainFrame == true) {
                            android.util.Log.d(
                                "WeijiImport",
                                "http error ${errorResponse?.statusCode} for ${request.url}",
                            )
                        }
                    }

                    override fun onPageFinished(view: WebView?, finishedUrl: String?) {
                        if (done) return
                        val js = """
                            (function(){
                              try {
                                var v = Array.from(document.querySelectorAll('video')).map(function(x){return x.currentSrc || x.src || '';});
                                var i = Array.from(document.querySelectorAll('img')).map(function(x){return x.currentSrc || x.src || '';});
                                var html = document.documentElement.outerHTML;
                                var bodies = (window.__weiji_bodies || []).slice(-12);
                                var res = performance.getEntriesByType('resource').map(function(e){return e.name;}).filter(function(n){return /douyinvod|byteicdn|aweme\/v1\/play|aweme\/detail|aweme_detail/.test(n);});
                                return JSON.stringify({videos:v, images:i, html:html, bodies:bodies, resources:res});
                              } catch(e) { return ''; }
                            })();
                        """.trimIndent()

                        fun probe(attempt: Int) {
                            runCatching {
                                view?.evaluateJavascript(js) { raw ->
                                val payload = runCatching {
                                    (JSONTokener(raw).nextValue() as? String).orEmpty()
                                }.getOrDefault("")
                                val obj = runCatching { JSONObject(payload) }.getOrNull()

                                var parsed: ExtractedContent? = null
                                val bodies = obj?.optJSONArray("bodies")
                                android.util.Log.d(
                                    "WeijiImport",
                                    "probe: bodies=${bodies?.length() ?: 0} html=${obj?.optString("html")?.length ?: 0}",
                                )
                                bodies?.let { arr ->
                                    for (i in arr.length() - 1 downTo 0) {
                                        val rawBody = arr.optString(i)
                                        android.util.Log.d(
                                            "WeijiImport",
                                            "body[$i]: ${rawBody.length} ${rawBody.take(90)}",
                                        )
                                        parsed = DouyinJsonParser.parse(rawBody, link)
                                        if (parsed != null) break
                                    }
                                }
                                if (parsed == null) {
                                    val resources = obj?.optJSONArray("resources")
                                    android.util.Log.d(
                                        "WeijiImport",
                                        "resources=${resources?.length() ?: 0}",
                                    )
                                    resources?.let { arr ->
                                        for (i in 0 until arr.length()) {
                                            val u = arr.optString(i)
                                            android.util.Log.d("WeijiImport", "res[$i]: ${u.take(160)}")
                                            if (u.contains("aweme/detail") || u.contains("aweme_detail")) {
                                                Thread {
                                                    val detailBody = replayUrl(u)
                                                    if (!detailBody.isNullOrEmpty()) {
                                                        android.util.Log.d(
                                                            "WeijiImport",
                                                            "resource replay: ${detailBody.length}",
                                                        )
                                                        val c = runCatching {
                                                            DouyinJsonParser.parse(detailBody, link)
                                                        }.getOrNull()
                                                        if (c != null) {
                                                            view?.post {
                                                                if (!done) {
                                                                    done = true
                                                                    onExtracted(c)
                                                                }
                                                            }
                                                        }
                                                    }
                                                }.start()
                                            }
                                        }
                                    }
                                    val combined = buildString {
                                        append(obj?.optString("html").orEmpty())
                                        append('\n')
                                        obj?.optJSONArray("videos")?.let { arr ->
                                            for (i in 0 until arr.length()) append(arr.optString(i)).append('\n')
                                        }
                                        obj?.optJSONArray("images")?.let { arr ->
                                            for (i in 0 until arr.length()) append(arr.optString(i)).append('\n')
                                        }
                                        resources?.let { arr ->
                                            for (i in 0 until arr.length()) append(arr.optString(i)).append('\n')
                                        }
                                        captured.forEach { append(it).append('\n') }
                                    }
                                    parsed = DouyinWebViewParser.parse(
                                        combined,
                                        finishedUrl ?: url,
                                        link,
                                    )
                                }
                                if (parsed != null && !done) {
                                    done = true
                                    onExtracted(parsed!!)
                                } else if (attempt < 2) {
                                    view?.postDelayed({ probe(attempt + 1) }, 4_000)
                                } else if ((obj?.optJSONArray("resources")?.length() ?: 0) > 0) {
                                    // 页面发出了 detail 请求但没有可解析内容：判定为风控/需要登录
                                    if (!done) {
                                        done = true
                                        onFailed("视频需要登录抖音后重试")
                                    }
                                }
                            }
                            }.onFailure {
                                android.util.Log.d("WeijiImport", "probe failed: ${it.message}")
                                if (!done) {
                                    done = true
                                    onFailed(it.message ?: "页面渲染失败")
                                }
                            }
                        }
                        view?.postDelayed({ probe(0) }, 2_500)
                    }
                }
                loadUrl(url)
                webView = this
            }
        },
        update = { it -> webView = it },
    )
}
