package com.weiji.wang.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        RecipeEntity::class,
        SourceEntity::class,
        IngredientEntity::class,
        StepEntity::class,
        MediaEntity::class,
        CookLogEntity::class,
        RevisionEntity::class,
        ShoppingListEntity::class,
        ShoppingItemEntity::class,
        TagEntity::class,
        RecipeTagEntity::class,
        DraftEntity::class,
        NoteEntity::class,
        MealMenuEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class WeijiDatabase : RoomDatabase() {
    abstract fun recipeDao(): RecipeDao
    abstract fun ingredientDao(): IngredientDao
    abstract fun stepDao(): StepDao
    abstract fun mediaDao(): MediaDao
    abstract fun sourceDao(): SourceDao
    abstract fun cookLogDao(): CookLogDao
    abstract fun revisionDao(): RevisionDao
    abstract fun shoppingDao(): ShoppingDao
    abstract fun tagDao(): TagDao
    abstract fun draftDao(): DraftDao
    abstract fun noteDao(): NoteDao
    abstract fun mealMenuDao(): MealMenuDao

    companion object {
        const val NAME = "weiji.db"

        /** v1 → v2：每步多媒体、备注、每日菜单快照与采购来源日期。 */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE media ADD COLUMN stepId TEXT")
                db.execSQL("ALTER TABLE media ADD COLUMN position INTEGER NOT NULL DEFAULT 0")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `notes` (
                        `id` TEXT NOT NULL,
                        `recipeId` TEXT NOT NULL,
                        `stepId` TEXT,
                        `text` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_notes_recipeId ON notes (recipeId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_notes_stepId ON notes (stepId)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `meal_menu` (
                        `date` TEXT NOT NULL,
                        `recipeId` TEXT NOT NULL,
                        `snapshotJson` TEXT NOT NULL,
                        `coverCachePath` TEXT,
                        `addedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`date`, `recipeId`)
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_meal_menu_date ON meal_menu (date)")

                db.execSQL("ALTER TABLE shopping_items ADD COLUMN sourceDate TEXT")
            }
        }
    }
}
