package com.weiji.wang.core.model

enum class Platform(val key: String, val label: String, val host: String) {
    DOUYIN("DOUYIN", "抖音", "douyin.com"),
    XIAOHONGSHU("XIAOHONGSHU", "小红书", "xiaohongshu.com"),
    BILIBILI("BILIBILI", "B站", "bilibili.com"),
    OTHER("OTHER", "链接", "");

    companion object {
        fun fromKey(key: String?): Platform =
            entries.firstOrNull { it.key == key } ?: OTHER
    }
}

enum class ImportState {
    RECEIVED,
    RESOLVING,
    NEEDS_LOGIN,
    EXTRACTED,
    DOWNLOADING,
    PROCESSING,
    READY,
    PARTIAL,
    FAILED;

    val isTerminal: Boolean
        get() = this == READY || this == PARTIAL || this == FAILED
}

enum class MediaKind { COVER, STEP, PHOTO, VIDEO, AUDIO }

enum class FetchStatus { OK, PARTIAL, MANUAL, FAILED }
