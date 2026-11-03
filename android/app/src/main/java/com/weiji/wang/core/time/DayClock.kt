package com.weiji.wang.core.time

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import java.time.Instant
import java.time.ZoneId

/**
 * 本机日期时钟：今天是 `yyyy-MM-dd`（本地时区）。
 * 前台跨过午夜、以及每次重新收集时都会拿到新的日期，供菜单/采购按日刷新。
 */
object DayClock {

    fun todayKey(now: Long = System.currentTimeMillis()): String =
        Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate().toString()

    fun flow(now: () -> Long = { System.currentTimeMillis() }): Flow<String> =
        kotlinx.coroutines.flow.flow {
            while (true) {
                emit(todayKey(now()))
                delay(POLL_INTERVAL_MS)
            }
        }.distinctUntilChanged()

    private const val POLL_INTERVAL_MS = 30_000L
}
