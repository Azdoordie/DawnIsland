package com.yanrou.dawnisland.entities

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Cookie::class], version = 1, exportSchema = false)
abstract class DawnDatabase : RoomDatabase() {
    abstract fun cookieDao(): CookieDao

    companion object {
        // Singleton prevents multiple instances of database opening at the
        // same time.
        @Volatile
        private var INSTANCE: DawnDatabase? = null

        fun getDatabase(context: Context): DawnDatabase {
            val tempInstance =
                    INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }
            synchronized(this) {
                val instance = Room.databaseBuilder(
                        context.applicationContext,
                        DawnDatabase::class.java,
                        "dawn_database"
                ).build()
                INSTANCE = instance
                return instance
            }
        }
    }
}