package com.weiji.wang.core

import android.content.Context
import androidx.room.Room
import com.weiji.wang.core.data.MenuRepository
import com.weiji.wang.core.data.RecipeRepository
import com.weiji.wang.core.data.ShoppingRepository
import com.weiji.wang.core.data.db.WeijiDatabase
import com.weiji.wang.core.data.prefs.SettingsRepository
import com.weiji.wang.core.import.CookieStore
import com.weiji.wang.core.import.HttpClients
import com.weiji.wang.core.import.ImportCoordinator
import com.weiji.wang.core.import.extractor.ExtractorRegistry
import com.weiji.wang.core.storage.BackupManager
import com.weiji.wang.core.storage.FileStore
import com.weiji.wang.core.update.UpdateChecker
import okhttp3.OkHttpClient

class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext

    val database: WeijiDatabase = Room.databaseBuilder(
        appContext,
        WeijiDatabase::class.java,
        WeijiDatabase.NAME,
    ).addMigrations(WeijiDatabase.MIGRATION_1_2)
        .build()

    val files = FileStore(appContext)
    val settings = SettingsRepository(appContext)
    val cookies = CookieStore(appContext)
    val http: OkHttpClient = HttpClients.create()

    val shoppingRepo = ShoppingRepository(database, files)
    val menu = MenuRepository(database, files, shoppingRepo)
    val recipes = RecipeRepository(database, files)
    val extractors = ExtractorRegistry(http, cookies)
    val importer = ImportCoordinator(appContext, database, files, recipes, extractors, settings, http)
    val backup = BackupManager(appContext, database, recipes, files)
    val updates = UpdateChecker(http)
}
