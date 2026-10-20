package com.hwinzniej.musichelper.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.hwinzniej.musichelper.data.dao.MusicDao
import com.hwinzniej.musichelper.data.model.Music

@Database(entities = [Music::class], version = 4, exportSchema = false)
abstract class MusicDatabase : RoomDatabase() {
    abstract fun musicDao(): MusicDao

    companion object {
        /** v4：新增「制作人」列 */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `Music` ADD COLUMN `producer` TEXT NOT NULL DEFAULT ''")
            }
        }
    }
}