package com.weiji.wang.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.weiji.wang.core.AppContainer

val LocalContainer = staticCompositionLocalOf<AppContainer> {
    error("AppContainer 未提供")
}
