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
    val userRating: Float?, // 0.5 to 5.0 stars in 0.5 increments
    val review: String? = null, // User's written review
    val lastStatusDate: Long = System.currentTimeMillis(), // Timestamp of last status update
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationName: String? = null,
    val title: String? = null,
    val posterUrl: String? = null
)

