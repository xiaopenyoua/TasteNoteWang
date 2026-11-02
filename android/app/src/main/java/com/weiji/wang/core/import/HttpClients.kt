package com.weiji.wang.core.import

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object HttpClients {
    const val MOBILE_UA =
        "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

    /**
     * B 站 CDN 对移动端 UA 返回 403，对桌面 UA 返回 206（2026-10-30 实测）。
     * 抖音/小红书仍用 MOBILE_UA。
     */
    const val DESKTOP_UA =
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    /**
     * 抖音 JSON-LD 兜底（单条、用户主动）。只用于读取公开页面的结构化数据，
     * 不用于批量、不用于遍历。平台改版/收紧时直接失效，属可接受降级。
     */
    const val CRAWLER_UA =
        "Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)"

    fun create(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .callTimeout(45, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .retryOnConnectionFailure(true)
        .build()
}
