package com.weiji.wang.core.import.extractor

import com.weiji.wang.core.import.ExtractException
import com.weiji.wang.core.import.ExtractedContent
import com.weiji.wang.core.import.HttpClients
import com.weiji.wang.core.import.Jsons
import com.weiji.wang.core.import.ParsedLink
import com.weiji.wang.core.import.PlatformExtractor
import com.weiji.wang.core.import.arr
import com.weiji.wang.core.import.fetchText
import com.weiji.wang.core.import.field
import com.weiji.wang.core.import.int
import com.weiji.wang.core.import.long
import com.weiji.wang.core.import.path
import com.weiji.wang.core.import.resolveFinalUrl
import com.weiji.wang.core.import.str
import com.weiji.wang.core.model.FetchStatus
import com.weiji.wang.core.model.ImportState
import com.weiji.wang.core.model.MediaKind
import com.weiji.wang.core.model.Platform
import okhttp3.OkHttpClient

/**
 * B 站：b23 短链 -> BV -> view 接口 -> playurl（fnval=1 durl 单文件）。
 * 2026-10-30 实测：不登录可拿 720p（qn=64）单文件 mp4，音视频已封装，不需要合并。
 */
class BilibiliExtractor(private val http: OkHttpClient) : PlatformExtractor {

    override val platform = Platform.BILIBILI

    private val headers = mapOf(
        "User-Agent" to HttpClients.DESKTOP_UA,
        "Referer" to "https://www.bilibili.com/",
    )

    override suspend fun extract(link: ParsedLink): ExtractedContent {
        val finalUrl = if (link.url.contains("b23.tv")) http.resolveFinalUrl(link.url, headers) else link.url
        val bvid = Regex("""(BV[0-9A-Za-z]{10})""").find(finalUrl)?.groupValues?.get(1)
            ?: link.contentId
            ?: throw ExtractException(ImportState.FAILED, "没有识别到 BV 号")

        val view = http.fetchText(
            "https://api.bilibili.com/x/web-interface/view?bvid=$bvid",
            headers,
        )
        val viewJson = Jsons.parse(view.body)
        val viewCode = viewJson.int("code")
        if (viewCode != 0) {
            throw ExtractException(ImportState.FAILED, "B站接口返回 ${viewCode}: ${viewJson.str("message")}")
        }
        val data = viewJson.field("data") ?: throw ExtractException(ImportState.FAILED, "B站返回为空")

        val title = data.str("title").orEmpty().ifBlank { link.titleHint ?: "B站视频" }
        val author = data.path("owner").str("name") ?: link.authorHint
        val authorId = data.path("owner").str("mid")
        val desc = data.str("desc").orEmpty()
        val cover = data.str("pic")?.replaceFirst("http://", "https://")
        val durationSeconds = data.long("duration")
        val firstPage = data.field("pages")?.arr()?.firstOrNull()
        val cid = firstPage?.long("cid")
        val pageTitle = firstPage?.str("part")?.takeIf { it.isNotBlank() }
        val tname = data.str("tname")

        val media = mutableListOf<com.weiji.wang.core.import.RemoteMedia>()
        cover?.let {
            media += com.weiji.wang.core.import.RemoteMedia(
                kind = MediaKind.COVER,
                url = it,
                mime = "image/jpeg",
                watermarkFree = true,
                headers = headers,
                suggestedName = "cover.jpg",
            )
        }

        if (cid != null) {
            val play = http.fetchText(
                "https://api.bilibili.com/x/player/playurl?bvid=$bvid&cid=$cid&qn=64&fnval=1&fnver=0&fourk=0",
                headers,
            )
            val playJson = Jsons.parse(play.body)
            val playData = playJson.field("data")
            val durls = playData?.field("durl")?.arr()?.mapNotNull { it.str("url") } ?: emptyList()
            durls.forEachIndexed { index, url ->
                media += com.weiji.wang.core.import.RemoteMedia(
                    kind = MediaKind.VIDEO,
                    url = url.replaceFirst("http://", "https://"),
                    mime = "video/mp4",
                    durationMs = durationSeconds?.let { it * 1000L },
                    watermarkFree = true,
                    headers = headers,
                    suggestedName = if (index == 0) "video.mp4" else "video-%02d.mp4".format(index + 1),
                )
            }
        }

        val status = if (media.any { it.kind == MediaKind.VIDEO }) FetchStatus.OK else FetchStatus.PARTIAL
        return ExtractedContent(
            platform = platform,
            canonicalUrl = "https://www.bilibili.com/video/$bvid",
            contentId = bvid,
            title = title,
            author = author,
            authorId = authorId,
            textBody = desc,
            tags = listOfNotNull(tname, pageTitle).distinct(),
            coverUrl = cover,
            media = media,
            fetchStatus = status,
            message = if (status == FetchStatus.PARTIAL) "没有拿到视频流，已保留链接" else null,
            durationMs = durationSeconds?.let { it * 1000L },
        )
    }
}
