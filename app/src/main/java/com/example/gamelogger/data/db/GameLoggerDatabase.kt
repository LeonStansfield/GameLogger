package com.example.gamelogger.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

import androidx.room.AutoMigration

@Database(
    entities = [GameLog::class],
    version = 6,
    exportSchema = true,
    autoMigrations = [
        AutoMigration (from = 5, to = 6)
    ]
)
abstract class GameLoggerDatabase : RoomDatabase() {

    abstract fun gameLogDao(): GameLogDao

    companion object {
        @Volatile
        private var INSTANCE: GameLoggerDatabase? = null

        fun getDatabase(context: Context): GameLoggerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                                context.applicationContext,
                                GameLoggerDatabase::class.java,
                                "gamelogger_database"
                            ).fallbackToDestructiveMigration(true)
                 .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

