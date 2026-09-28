package com.linnan.instadownloader.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [HistoryEntity::class], version = 1, exportSchema = false)
abstract class LinNanDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao

    companion object {
        @Volatile
        private var instance: LinNanDatabase? = null

        fun getInstance(context: Context): LinNanDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    LinNanDatabase::class.java,
                    "linnan_history.db"
                ).build().also { instance = it }
            }
    }
}
