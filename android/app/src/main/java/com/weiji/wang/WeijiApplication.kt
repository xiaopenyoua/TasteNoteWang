package com.weiji.wang

import android.app.Application
import com.weiji.wang.core.AppContainer

class WeijiApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // 清理上次进程留下的 .part 与分享缓存；不碰 recipes/ 成品
        Thread { container.files.cleanTempFiles() }.start()
    }
}
