package com.example.gamelogger.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [GameLog::class], version = 1, exportSchema = false)
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
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

