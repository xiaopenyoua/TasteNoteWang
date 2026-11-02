package com.weiji.wang.core.import

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.weiji.wang.core.model.Platform

/**
 * 平台登录态只存在本机，加密保存；绝不写日志、绝不进导出包。
 * 加密不可用时降级为普通私有 SharedPreferences，保证应用可用。
 */
class CookieStore(context: Context) {

    private val prefs: SharedPreferences = runCatching {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        ) as SharedPreferences
    }.getOrElse {
        context.getSharedPreferences(FALLBACK_FILE, Context.MODE_PRIVATE)
    }

    fun cookieFor(platform: Platform): String? =
        prefs.getString(key(platform), null)?.takeIf { it.isNotBlank() }

    fun save(platform: Platform, cookie: String) {
        prefs.edit().putString(key(platform), cookie).apply()
    }

    fun has(platform: Platform): Boolean = cookieFor(platform) != null

    fun clear(platform: Platform) {
        prefs.edit().remove(key(platform)).apply()
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    fun lastUpdated(platform: Platform): Long = prefs.getLong("${key(platform)}_at", 0L)

    fun saveWithTime(platform: Platform, cookie: String, at: Long) {
        prefs.edit().putString(key(platform), cookie).putLong("${key(platform)}_at", at).apply()
    }

    private fun key(platform: Platform) = "cookie_${platform.key.lowercase()}"

    companion object {
        private const val FILE_NAME = "weiji_secure_cookies"
        private const val FALLBACK_FILE = "weiji_cookies_fallback"
    }
}
