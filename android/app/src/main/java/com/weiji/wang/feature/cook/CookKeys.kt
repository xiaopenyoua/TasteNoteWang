package com.weiji.wang.feature.cook

/**
 * 音量键翻步的全局桥接：由 MainActivity 拦截，CookModeScreen 注册/注销。
 * 只在意音量键，不影响系统音量（在烹饪页内）。
 */
object CookKeys {
    var enabled: Boolean = false
    var onNext: (() -> Unit)? = null
    var onPrev: (() -> Unit)? = null

    fun bind(next: () -> Unit, prev: () -> Unit) {
        onNext = next
        onPrev = prev
        enabled = true
    }

    fun unbind() {
        onNext = null
        onPrev = null
        enabled = false
    }
}
