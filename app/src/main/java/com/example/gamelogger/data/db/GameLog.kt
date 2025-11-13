package com.example.gamelogger.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class GameStatus {
    PLAYED,
    PLAYING,
    BACKLOGGED,
    DROPPED,
    ON_HOLD
}

@Entity(tableName = "game_logs")
data class GameLog(
    @PrimaryKey
    val gameId: String,
    val status: GameStatus,
    val playTime: Long, // in hours
    val userRating: Float?
)

