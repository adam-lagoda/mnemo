package com.mnemo.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.mnemo.data.db.entities.GraphEdgeEntity
import com.mnemo.data.db.entities.ScreenshotEntity

@Database(
    entities = [ScreenshotEntity::class, GraphEdgeEntity::class],
    version = 1,
    exportSchema = true
)
abstract class MnemoDatabase : RoomDatabase() {
    abstract fun screenshotDao(): ScreenshotDao
    abstract fun graphEdgeDao(): GraphEdgeDao

    companion object {
        private const val DB_NAME = "mnemo.db"

        @Volatile private var INSTANCE: MnemoDatabase? = null

        fun getInstance(context: Context): MnemoDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MnemoDatabase::class.java,
                    DB_NAME
                ).build().also { INSTANCE = it }
            }
        }
    }
}
