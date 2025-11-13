package com.example.gamelogger.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GameLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateGameLog(gameLog: GameLog)

    @Query("SELECT * FROM game_logs WHERE gameId = :gameId")
    fun getGameLog(gameId: String): Flow<GameLog?>

    @Query("SELECT * FROM game_logs")
    fun getAllGameLogs(): Flow<List<GameLog>>
}

