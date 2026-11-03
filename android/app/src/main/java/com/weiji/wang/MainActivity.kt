package com.weiji.wang

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.weiji.wang.core.data.prefs.ThemeMode
import com.weiji.wang.ui.LocalContainer
import com.weiji.wang.ui.WeijiApp
import com.weiji.wang.ui.theme.WeijiTheme
import com.weiji.wang.feature.cook.CookKeys

class MainActivity : ComponentActivity() {
    companion object {
        const val EXTRA_SHARE_TEXT = "share_text"
        const val EXTRA_SHARE_URIS = "share_uris"
    }

    private val shareText = mutableStateOf<String?>(null)
    private val shareUris = mutableStateOf<List<String>>(emptyList())

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val container = (application as WeijiApplication).container
        readShare(intent)
        setContent {
            val theme by container.settings.theme.collectAsStateWithLifecycle(
                initialValue = ThemeMode.SYSTEM,
            )
            val darkTheme = when (theme) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            CompositionLocalProvider(LocalContainer provides container) {
                WeijiTheme(darkTheme = darkTheme) {
                    WeijiApp(
                        shareText = shareText.value,
                        shareUris = shareUris.value,
                        onShareConsumed = {
                            shareText.value = null
                            shareUris.value = emptyList()
                        },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        readShare(intent)
    }

    private fun readShare(intent: Intent?) {
        shareText.value = intent?.getStringExtra(EXTRA_SHARE_TEXT)
        shareUris.value = intent?.getStringArrayListExtra(EXTRA_SHARE_URIS)?.toList().orEmpty()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (CookKeys.enabled) {
            when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP -> {
                    CookKeys.onNext?.invoke()
                    return true
                }
                KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    CookKeys.onPrev?.invoke()
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }
}
