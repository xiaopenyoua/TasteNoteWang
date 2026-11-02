package com.weiji.wang.core.import

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

data class HttpResult(
    val body: String,
    val finalUrl: String,
    val code: Int,
)

suspend fun OkHttpClient.fetchText(
    url: String,
    headers: Map<String, String> = emptyMap(),
    method: String = "GET",
): HttpResult = withContext(Dispatchers.IO) {
    val builder = Request.Builder()
        .url(url)
        .header("User-Agent", HttpClients.MOBILE_UA)
        .header("Accept-Language", "zh-CN,zh;q=0.9")
    headers.forEach { (key, value) -> builder.header(key, value) }
    if (method == "HEAD") builder.head() else builder.get()
    this@fetchText.newCall(builder.build()).execute().use { response ->
        HttpResult(
            body = if (method == "HEAD") "" else response.body?.string().orEmpty(),
            finalUrl = response.request.url.toString(),
            code = response.code,
        )
    }
}

suspend fun OkHttpClient.resolveFinalUrl(url: String, headers: Map<String, String> = emptyMap()): String =
    fetchText(url, headers, method = "HEAD").finalUrl
