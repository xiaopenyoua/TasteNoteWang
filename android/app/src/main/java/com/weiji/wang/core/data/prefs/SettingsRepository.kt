package com.weiji.wang.core.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

class SettingsRepository(private val context: Context) {

    private val themeKey = stringPreferencesKey("theme_mode")
    private val wifiOnlyKey = booleanPreferencesKey("wifi_only")
    private val autoDownloadKey = booleanPreferencesKey("auto_download_media")
    private val cookKeepOnKey = booleanPreferencesKey("cook_keep_on")
    private val volumeKeysKey = booleanPreferencesKey("cook_volume_keys")

    val theme: Flow<ThemeMode> = context.dataStore.data.map {
        runCatching { ThemeMode.valueOf(it[themeKey] ?: ThemeMode.SYSTEM.name) }.getOrDefault(ThemeMode.SYSTEM)
    }

    val wifiOnly: Flow<Boolean> = context.dataStore.data.map { it[wifiOnlyKey] ?: true }

    val autoDownloadMedia: Flow<Boolean> = context.dataStore.data.map { it[autoDownloadKey] ?: true }

    val cookKeepScreenOn: Flow<Boolean> = context.dataStore.data.map { it[cookKeepOnKey] ?: true }

    val cookVolumeKeys: Flow<Boolean> = context.dataStore.data.map { it[volumeKeysKey] ?: true }

    suspend fun setTheme(value: ThemeMode) {
        context.dataStore.edit { it[themeKey] = value.name }
    }

    suspend fun setWifiOnly(value: Boolean) {
        context.dataStore.edit { it[wifiOnlyKey] = value }
    }

    suspend fun setAutoDownloadMedia(value: Boolean) {
        context.dataStore.edit { it[autoDownloadKey] = value }
    }

    suspend fun setCookKeepScreenOn(value: Boolean) {
        context.dataStore.edit { it[cookKeepOnKey] = value }
    }

    suspend fun setCookVolumeKeys(value: Boolean) {
        context.dataStore.edit { it[volumeKeysKey] = value }
    }
}
