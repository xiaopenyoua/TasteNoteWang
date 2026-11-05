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
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.weiji.wang.core.data.db.DraftEntity
import com.weiji.wang.core.import.DouyinJsonParser
import com.weiji.wang.core.import.DouyinPlayUrls
import com.weiji.wang.core.import.DouyinWebViewParser
import com.weiji.wang.core.import.ExtractException
import com.weiji.wang.core.import.ExtractedContent
import com.weiji.wang.core.import.HttpClients
import com.weiji.wang.core.import.ImportCoordinator
import com.weiji.wang.core.import.ImportResult
import com.weiji.wang.core.import.LinkParser
import com.weiji.wang.core.import.PreparedImport
import com.weiji.wang.core.model.ImportState
import com.weiji.wang.core.model.MediaKind
import com.weiji.wang.core.model.Platform
import com.weiji.wang.ui.LocalContainer
import com.weiji.wang.ui.components.PageHeader
import com.weiji.wang.ui.components.PlatformBadge
import com.weiji.wang.ui.components.PrimaryButton
import com.weiji.wang.ui.components.SecondaryButton
import com.weiji.wang.ui.components.SectionHeader
import com.weiji.wang.ui.components.SwipeToRevealDelete
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
                    message = "等了约 40 秒仍没拿到视频直链（分享页风控或网络慢）。" +
                        "链接、封面和文字已保存：可以点「重试」再收一次，" +
                        "或先「去登录抖音」扫码保存登录态后再收；" +
                        "也可以在抖音里把视频保存到相册，再用「分享 → 味记」导入视频。",
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
                    message = "分享页没有加载成功（$reason）。链接和文字已保存：" +
                        "可以重试，或去登录抖音后再收；也可以在抖音里保存视频后分享到味记。",
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
    // 同一时间只展开一条「最近收录」的左滑删除。
    var openDraftId by rememberSaveable { mutableStateOf<String?>(null) }
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
                        title = "正在用内置浏览器读取抖音分享页",
                        body = "抖音的播放地址由页面自己签名请求，通常要十几秒；首次可能需要登录，登录信息只保存在本机。",
                    )
                    DouyinWebView(
                        url = s.content.webViewUrl ?: s.content.canonicalUrl,
                        fallbackUrl = s.content.canonicalUrl,
                        cookie = container.cookies.cookieFor(Platform.DOUYIN),
                        link = s.prepared.link,
                        seo = s.content,
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
                    revealed = openDraftId == draft.id,
                    onRevealedChange = { revealed ->
                        openDraftId = when {
                            revealed -> draft.id
                            openDraftId == draft.id -> null
                            else -> openDraftId
                        }
                    },
                    onRetry = { vm.analyze(draft.shareText) },
                    onDiscard = {
                        openDraftId = null
                        vm.discard(draft.id)
                    },
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
    revealed: Boolean,
    onRevealedChange: (Boolean) -> Unit,
    onRetry: () -> Unit,
    onDiscard: () -> Unit,
    onOpen: () -> Unit,
) {
    SwipeToRevealDelete(
        revealed = revealed,
        onRevealedChange = onRevealedChange,
        onDelete = onDiscard,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(if (revealed) DraftCardShape else RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surface)
                .clickable {
                    if (revealed) {
                        onRevealedChange(false)
                    } else if (draft.recipeId != null) {
                        onOpen()
                    } else {
                        onRetry()
                    }
                }
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
                Text(
                    text = "查看",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            } else {
                Text(
                    text = "重试",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

// 右侧不圆角：左滑后与删除按钮拼成一条，外层容器负责圆外轮廓。
private val DraftCardShape = RoundedCornerShape(
    topStart = 14.dp,
    topEnd = 0.dp,
    bottomEnd = 0.dp,
    bottomStart = 14.dp,
)

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

/**
 * document-start 注入（每个新文档都在页面脚本之前装好，reload 也不丢）：
 * 抖音分享页用页面自己的安全 SDK 签名请求 iteminfo/detail，我们只被动读取响应体，
 * 不重放签名、不替换页面响应（替换后页面拿不到数据，视频反而不会加载）。
 */
private const val HOOK_JS = """
(function(){
  if (window.__weiji_hooked) return;
  window.__weiji_hooked = true;
  window.__weiji_bodies = [];
  window.__weiji_urls = [];
  var API = /\/(?:web\/api\/v2\/aweme\/iteminfo|aweme\/v1\/(?:web\/)?aweme\/(?:detail|post)|aweme\/detail|iteminfo)/;
  function isApi(u) { try { return API.test(u || ''); } catch (e) { return false; } }
  function push(u, b) {
    try {
      if (!u || !isApi(u)) return;
      window.__weiji_urls.push(u);
      if (window.__weiji_urls.length > 12) window.__weiji_urls.shift();
      if (!b) return;
      window.__weiji_bodies.push(b);
      if (window.__weiji_bodies.length > 12) window.__weiji_bodies.shift();
    } catch (e) {}
  }
  var of = window.fetch;
  if (of) {
    window.fetch = function() {
      var args = arguments;
      return of.apply(this, args).then(function(resp){
        try {
          var u = (resp && resp.url) || (args && args[0] && args[0].url) || '';
          if (isApi(u)) {
            resp.clone().text().then(function(t){ push(u, t); }).catch(function(){ push(u, ''); });
          }
        } catch (e) {}
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
        if (!isApi(u)) return;
        var b = '';
        try {
          if (!x.responseType || x.responseType === 'text') b = x.responseText;
          else if (x.responseType === 'json') b = JSON.stringify(x.response);
          else if (typeof x.response === 'string') b = x.response;
        } catch (e) { b = ''; }
        push(u, b);
      } catch (e) {}
    });
    return os.apply(this, arguments);
  };
})();
"""

/**
 * 一次探针把三路一起收回来：
 * 1) XHR/fetch 响应体（iteminfo / detail / aweme_detail）；
 * 2) `performance.getEntriesByType('resource')` 里的媒体直链（douyinvod / byteicdn / video/tos / .mp4）；
 * 3) DOM 里 `<video>.currentSrc`。
 *
 * 分享页的播放器不一定自动播；先 muted + play() 静默触发一次 media 请求，
 * 直链才会出现在 resource timing 里（2026-11-04 实测建议）。顺序必须先是 currentSrc，再读 performance。
 */
private const val PROBE_JS = """
(function(){
  try {
    Array.from(document.querySelectorAll('video')).forEach(function(v){
      try {
        v.muted = true;
        var p = v.play();
        if (p && p.catch) p.catch(function(){});
      } catch (e) {}
    });
    var vids = Array.from(document.querySelectorAll('video')).map(function(x){return x.currentSrc || x.src || x.getAttribute('src') || '';});
    var srcs = Array.from(document.querySelectorAll('video source')).map(function(x){return x.src || '';});
    var entries = performance.getEntriesByType('resource').map(function(e){return e.name;});
    // 只认直连 CDN；/aweme/v1/play 是需要签名的跳转口，不能当视频直链候选
    var media = entries.filter(function(n){ return /douyinvod|byteicdn|video\/tos|\.mp4/.test(n); });
    var apis = entries.filter(function(n){ return /iteminfo|aweme\/detail|aweme\/post|aweme_detail|item_list/.test(n); });
    var imgs = Array.from(document.querySelectorAll('img')).map(function(x){return x.currentSrc || x.src || '';});
    var errText = '';
    try { errText = (document.body && document.body.innerText) ? String(document.body.innerText).slice(0, 400) : ''; } catch (e) {}
    return JSON.stringify({
      videos: vids.concat(srcs).filter(Boolean),
      images: imgs,
      media: media,
      apis: apis,
      hooked: (window.__weiji_urls || []),
      bodies: (window.__weiji_bodies || []).slice(-12),
      err: /抱歉出错了|请尝试在抖音内观看|作品不存在/.test(errText),
      html: document.documentElement.outerHTML
    });
  } catch (e) { return ''; }
})();
"""

/** 探针间隔 / 次数：首次 2.5s，之后每 2.5s，最多覆盖约 35s，硬超时 45s。 */
private const val PROBE_INTERVAL_MS = 2_500L
private const val MAX_PROBE_ATTEMPTS = 14
private const val WEBVIEW_TIMEOUT_MS = 45_000L

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun DouyinWebView(
    url: String,
    fallbackUrl: String,
    cookie: String?,
    link: com.weiji.wang.core.import.ParsedLink,
    seo: ExtractedContent,
    onExtracted: (ExtractedContent) -> Unit,
    onTimeout: () -> Unit,
    onFailed: (String) -> Unit,
) {
    val captured = remember { linkedSetOf<String>() }
    // 页面播放器自己请求的 /aweme/v1/play(wm)/ 跳转口：里面带 video_id，跟一次 302 就是 CDN 直链。
    // 分享页的 reflow 接口只回封面上限数据（无 play_addr），这条是视频唯一的可靠来源。
    val playCandidates = remember { java.util.Collections.synchronizedSet(mutableSetOf<String>()) }
    // DOM 里第一个 <video> 是主播放器，优先于拦截到的推荐位请求。
    val domPlayCandidates = remember { java.util.Collections.synchronizedList(mutableListOf<String>()) }
    val resolvingPlay = remember { java.util.concurrent.atomic.AtomicBoolean(false) }
    val requestLog = remember { java.util.concurrent.atomic.AtomicInteger(0) }
    val http = remember { HttpClients.create() }
    var done by remember { mutableStateOf(false) }
    val probeStarted = remember { java.util.concurrent.atomic.AtomicBoolean(false) }
    val replayed = remember { java.util.Collections.synchronizedSet(mutableSetOf<String>()) }
    // 分享页在风控/非中国 IP 下会直接渲染错误页；这时换旧路径再试一轮（只换一次）
    val switchedLegacy = remember { java.util.concurrent.atomic.AtomicBoolean(false) }
    var triedFallback by remember { mutableStateOf(false) }
    var webView by remember { mutableStateOf<WebView?>(null) }

    fun finish(content: ExtractedContent) {
        if (done) return
        done = true
        onExtracted(content)
    }

    fun isNotePage(pageUrl: String?): Boolean =
        pageUrl?.contains("/slides/") == true ||
            pageUrl?.contains("/note/") == true ||
            url.contains("/share/slides/") ||
            link.shareText.contains("图文")

    /**
     * 视频链接只有拿到 `MediaKind.VIDEO` 才算成功：分享页 HTML 里通常带 og:image，
     * 提前接受“只有封面”的结果就是线上那个「只保存了封面、没有视频」的 bug。
     * 只有图文笔记才接受图片。
     */
    fun acceptParsed(content: ExtractedContent?, pageUrl: String?): ExtractedContent? {
        val parsed = content ?: return null
        val hasVideo = parsed.media.any { it.kind == MediaKind.VIDEO }
        return if (isNotePage(pageUrl)) {
            parsed.takeIf {
                hasVideo || parsed.media.any { m ->
                    m.kind == MediaKind.STEP || m.kind == MediaKind.PHOTO
                }
            }
        } else {
            parsed.takeIf { hasVideo }
        }
    }

    /**
     * `/aweme/v1/play(wm)/?video_id=…` 是分享页播放器发出的 302 跳转口，不需要签名：
     * 去掉 wm 并统一到 www.douyin.com 后跟随重定向，`response.request.url` 就是 douyinvod 直链。
     * 2026-11-05 实测 playwm/play 都返回 302，`play` 才是无水印版本。
     */
    fun resolvePlay(raw: String): String? {
        val noWatermark = DouyinPlayUrls.noWatermark(raw)
        fun follow(target: String): String? = runCatching {
            val request = Request.Builder()
                .url(target)
                .header("User-Agent", HttpClients.MOBILE_UA)
                .header("Referer", "https://www.douyin.com/")
                .header("Accept", "*/*")
                .build()
            http.newCall(request).execute().use { response ->
                val finalUrl = response.request.url.toString()
                val isCdn = DouyinPlayUrls.isCdn(finalUrl)
                android.util.Log.d(
                    "WeijiImport",
                    "play ${response.code} ${if (isCdn) "cdn" else "miss"}: ${finalUrl.take(140)}",
                )
                if (response.isSuccessful && isCdn) finalUrl else null
            }
        }.getOrNull()
        follow(noWatermark)?.let { return it }
        follow(raw)?.let { return it }
        // 有些分享页把 ratio 写成 540p/1080p，目标清晰度不存在时 404；用 video_id 回退 720p。
        val id = DouyinPlayUrls.videoId(raw)
        return id?.let { follow(DouyinPlayUrls.fromVideoId(it)) }
    }

    /** 备用路径：用页面 Cookie 重放同一个接口（页面自己的签名请求我们无法伪造）。 */
    fun replayUrl(target: String): String? = runCatching {
        val mergedCookie = listOfNotNull(
            cookie,
            CookieManager.getInstance().getCookie(target),
        ).joinToString("; ").ifBlank { null }
        val builder = Request.Builder()
            .url(target)
            .header("User-Agent", HttpClients.MOBILE_UA)
            .header("Referer", "https://www.iesdouyin.com/")
            .header("Accept", "*/*")
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
            builder.header("User-Agent", HttpClients.MOBILE_UA)
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
        // 分享页的 iteminfo 请求在页面加载后十几秒才发出（2026-11-04 实测约 13 秒），
        // 太短的超时会停在“只有封面”的结果上。
        delay(WEBVIEW_TIMEOUT_MS)
        if (!done) onTimeout()
    }

    AndroidView(
        modifier = Modifier.size(1.dp).alpha(0.01f),
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.databaseEnabled = true
                // 分享页是按移动端设计的：桌面 UA 会被导向 www.douyin.com 的反爬壳
                settings.userAgentString = HttpClients.MOBILE_UA
                settings.mediaPlaybackRequiresUserGesture = false
                settings.loadsImagesAutomatically = true
                settings.blockNetworkImage = false
                CookieManager.getInstance().setAcceptCookie(true)
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                if (!cookie.isNullOrBlank()) {
                    // CookieManager 一次只接受一对键值；整串塞进去只有第一对生效。
                    runCatching {
                        cookie.split(';').map { it.trim() }.filter { it.contains('=') }.forEach { pair ->
                            CookieManager.getInstance().setCookie("https://www.douyin.com", pair)
                            CookieManager.getInstance().setCookie("https://www.iesdouyin.com", pair)
                        }
                    }
                }
                // 页面首次导航后会 reload；onPageStarted 注入的钩子会随旧文档丢失。
                if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
                    runCatching {
                        WebViewCompat.addDocumentStartJavaScript(this, HOOK_JS, setOf("*"))
                    }
                }
                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest?,
                    ): WebResourceResponse? {
                        val requestUrl = request?.url?.toString().orEmpty()
                        val index = requestLog.getAndIncrement()
                        if (index < 60) {
                            android.util.Log.d("WeijiImport", "req[$index]: ${requestUrl.take(220)}")
                        }
                        if (requestUrl.contains("aweme", true) ||
                            requestUrl.contains("iteminfo", true) ||
                            requestUrl.contains("detail", true) ||
                            requestUrl.contains("play?", true)
                        ) {
                            android.util.Log.d("WeijiImport", "api/hit: ${requestUrl.take(220)}")
                        }
                        val isContentApi = requestUrl.contains("aweme/detail") ||
                            requestUrl.contains("aweme/post") ||
                            requestUrl.contains("iteminfo")
                        if (isContentApi) {
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
                                            view?.post { finish(parsed) }
                                        }
                                    }
                                }.start()
                            }
                            return null
                        }
                        if (requestUrl.contains("/aweme/v1/play", true)) {
                            playCandidates += requestUrl
                            android.util.Log.d("WeijiImport", "play candidate: ${requestUrl.take(200)}")
                        }
                        // 只收直连 CDN；aweme/v1/play 是跳转口，先解析再当直链
                        if (requestUrl.contains("douyinvod", true) ||
                            requestUrl.contains("byteicdn", true) ||
                            requestUrl.contains("video/tos", true) ||
                            requestUrl.contains("douyinpic", true)
                        ) {
                            captured += requestUrl
                        }
                        return null
                    }

                    override fun onPageStarted(
                        view: WebView?,
                        startedUrl: String?,
                        favicon: android.graphics.Bitmap?,
                    ) {
                        super.onPageStarted(view, startedUrl, favicon)
                        // 换页（分享页 → www.douyin.com 旧路径）后要允许重新起一轮探针，
                        // 否则 probeStarted 会一直卡在第一次 onPageFinished 的 true。
                        probeStarted.set(false)
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
                        if (!probeStarted.compareAndSet(false, true)) return
                        val pageUrl = finishedUrl ?: url

                        /**
                         * 三路嗅探结果一次判定：先解析 JSON，再解析页面 HTML，
                         * 最后用「只有直链」的合成路径。任何一步拿到 VIDEO 就收工。
                         */
                        fun probe(attempt: Int) {
                            if (done) return
                            runCatching {
                                view?.evaluateJavascript(PROBE_JS) { raw ->
                                    val payload = runCatching {
                                        (JSONTokener(raw).nextValue() as? String).orEmpty()
                                    }.getOrDefault("")
                                    val obj = runCatching { JSONObject(payload) }.getOrNull()

                                    var parsed: ExtractedContent? = null
                                    val bodies = obj?.optJSONArray("bodies")
                                    android.util.Log.d(
                                        "WeijiImport",
                                        "probe[$attempt]: bodies=${bodies?.length() ?: 0} " +
                                            "html=${obj?.optString("html")?.length ?: 0}",
                                    )

                                    // 1) XHR/fetch 响应体：iteminfo 的 item_list、detail 的 aweme_detail
                                    bodies?.let { arr ->
                                        for (i in arr.length() - 1 downTo 0) {
                                            val body = arr.optString(i)
                                            if (body.length < 32) continue
                                            android.util.Log.d(
                                                "WeijiImport",
                                                "body[$i]: ${body.length} ${body.take(90)}",
                                            )
                                            parsed = acceptParsed(
                                                runCatching {
                                                    DouyinJsonParser.parse(body, link)
                                                }.getOrNull(),
                                                pageUrl,
                                            )
                                            if (parsed != null) break
                                        }
                                    }

                                    // 2) 媒体直链：performance 资源 + DOM video.currentSrc + 拦截到的请求
                                    val sniffed = linkedSetOf<String>()
                                    fun collect(array: org.json.JSONArray?) {
                                        if (array == null) return
                                        for (i in 0 until array.length()) {
                                            val value = array.optString(i)
                                            if (value.startsWith("http")) sniffed += value
                                        }
                                    }
                                    collect(obj?.optJSONArray("media"))
                                    collect(obj?.optJSONArray("videos"))
                                    captured.forEach { sniffed += it }
                                    // DOM 里的 <video>.currentSrc 也可能是 play(wm) 跳转口；
                                    // 主播放器在 DOM 里排第一，优先于拦截到的请求（可能是推荐位）。
                                    obj?.optJSONArray("videos")?.let { arr ->
                                        for (i in 0 until arr.length()) {
                                            val value = arr.optString(i)
                                            if (value.contains("/aweme/v1/play", true)) {
                                                domPlayCandidates += value
                                            }
                                        }
                                    }

                                    if (parsed == null) {
                                        val combined = buildString {
                                            append(obj?.optString("html").orEmpty()).append('\n')
                                            sniffed.forEach { append(it).append('\n') }
                                            val images = obj?.optJSONArray("images")
                                            if (images != null) {
                                                for (i in 0 until images.length()) {
                                                    append(images.optString(i)).append('\n')
                                                }
                                            }
                                        }
                                        parsed = acceptParsed(
                                            runCatching {
                                                DouyinWebViewParser.parse(combined, pageUrl, link)
                                            }.getOrNull(),
                                            pageUrl,
                                        )
                                    }

                                    // 3) 只有直链、没有 JSON：用分享页真实播放的那条地址 + SEO 标题/封面合成
                                    if (parsed == null && sniffed.isNotEmpty()) {
                                        parsed = acceptParsed(
                                            runCatching {
                                                DouyinJsonParser.synthesize(sniffed, seo, link, pageUrl)
                                            }.getOrNull(),
                                            pageUrl,
                                        )
                                    }

                                    // 4) reflow 接口只有封面、没有 play_addr：把播放器的 play(wm) 跳转口解析成 CDN 直链
                                    if (parsed == null && playCandidates.isNotEmpty() &&
                                        resolvingPlay.compareAndSet(false, true)
                                    ) {
                                        // 主播放器（DOM）→ 拦截顺序；不排序，避免推荐位把目标视频换掉。
                                        val candidates = (domPlayCandidates.toList() + playCandidates.toList())
                                            .distinct()
                                        Thread {
                                            try {
                                                val resolved = candidates.firstNotNullOfOrNull { resolvePlay(it) }
                                                if (resolved != null) {
                                                    val content = acceptParsed(
                                                        runCatching {
                                                            DouyinJsonParser.synthesize(
                                                                listOf(resolved),
                                                                seo,
                                                                link,
                                                                pageUrl,
                                                            )
                                                        }.getOrNull(),
                                                        pageUrl,
                                                    )
                                                    if (content != null) view?.post { finish(content) }
                                                }
                                            } finally {
                                                resolvingPlay.set(false)
                                            }
                                        }.start()
                                    }

                                    // 分享页渲染出「抱歉出错了 / 请尝试在抖音内观看 / 作品不存在」时，
                                    // 换旧路径 www.douyin.com/{video|note}/{id} 再试一轮；两轮都空才走超时兜底。
                                    if (parsed == null && obj?.optBoolean("err") == true &&
                                        switchedLegacy.compareAndSet(false, true)
                                    ) {
                                        val legacyId = link.contentId ?: Regex("""/(?:video|note|slides)/(\d{15,25})""")
                                            .find(pageUrl)?.groupValues?.get(1)
                                        if (legacyId != null) {
                                            val legacy = if (isNotePage(pageUrl)) {
                                                "https://www.douyin.com/note/$legacyId"
                                            } else {
                                                "https://www.douyin.com/video/$legacyId"
                                            }
                                            android.util.Log.d(
                                                "WeijiImport",
                                                "share page blocked, retry legacy: $legacy",
                                            )
                                            view?.loadUrl(legacy)
                                        }
                                    }

                                    if (parsed != null) {
                                        finish(parsed!!)
                                        return@evaluateJavascript
                                    }

                                    // JSON 没拿到（例如 200/0 字节）：把页面发出的接口 URL 拿去重放一次
                                    if (bodies == null || bodies.length() == 0) {
                                        val apiList = linkedSetOf<String>()
                                        collectUrls(obj?.optJSONArray("apis"), apiList)
                                        collectUrls(obj?.optJSONArray("hooked"), apiList)
                                        apiList.forEach { apiUrl ->
                                            if (!replayed.add(apiUrl)) return@forEach
                                            Thread {
                                                val body = replayUrl(apiUrl)
                                                if (body.isNullOrEmpty()) return@Thread
                                                android.util.Log.d(
                                                    "WeijiImport",
                                                    "api replay: ${body.length}",
                                                )
                                                val content = acceptParsed(
                                                    runCatching {
                                                        DouyinJsonParser.parse(body, link)
                                                    }.getOrNull(),
                                                    pageUrl,
                                                )
                                                if (content != null) {
                                                    view?.post { finish(content) }
                                                }
                                            }.start()
                                        }
                                    }

                                    if (attempt < MAX_PROBE_ATTEMPTS) {
                                        // 页面可能还在等 iteminfo；继续探测，直到 45s 总超时。
                                        view?.postDelayed(
                                            { probe(attempt + 1) },
                                            PROBE_INTERVAL_MS,
                                        )
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
                        view?.postDelayed({ probe(0) }, PROBE_INTERVAL_MS)
                    }
                }
                loadUrl(url)
                webView = this
            }
        },
        update = { it -> webView = it },
    )
}

/** 小工具：把 JSON 数组里的 URL 收进集合（JVM 上 optString 对 null 安全）。 */
private fun collectUrls(array: org.json.JSONArray?, target: MutableSet<String>) {
    if (array == null) return
    for (i in 0 until array.length()) {
        val value = array.optString(i)
        if (value.startsWith("http")) target += value
    }
}
