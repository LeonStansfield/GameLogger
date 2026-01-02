package com.example.gamelogger.ui.features.loggame

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gamelogger.data.db.GameLog
import com.example.gamelogger.data.db.GameLogDao
import com.example.gamelogger.data.db.GameStatus
import com.example.gamelogger.data.remote.IgdbService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LogGameViewModel(
    private val gameLogDao: GameLogDao,
    private val gameId: String
) : ViewModel() {

    val gameLog: StateFlow<GameLog?> = gameLogDao.getGameLog(gameId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val igdbService = IgdbService()

    suspend fun saveGameLog(
        status: GameStatus,
        playTime: Long,
        userRating: Float?,
        review: String? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        locationName: String? = null,
        photoUri: String? = null
    ): Boolean {
        return try {
            // Validate rating bounds
            val validatedRating = userRating?.coerceIn(0.5f, 5.0f)

            // Validate playTime
            val validatedPlayTime = playTime.coerceAtLeast(0)

            // Sanitize review text
            val sanitizedReview = review?.trim()?.take(5000) // Limit review length

            // Fetch game details - safely handle gameId conversion
            val details = try {
                val gameIdInt = gameId.toIntOrNull()
                if (gameIdInt != null) {
                    igdbService.getGameDetails(gameIdInt)
                } else {
                    Log.w("LogGameViewModel", "Invalid gameId format: $gameId")
                    null
                }
            } catch (e: Exception) {
                Log.e("LogGameViewModel", "Failed to fetch game details", e)
                null
            }

            val currentLog = gameLog.value
            val title = details?.name ?: currentLog?.title
            val posterUrl = details?.cover?.bigCoverUrl ?: currentLog?.posterUrl

            val gameLog = GameLog(
                gameId = gameId,
                status = status,
                playTime = validatedPlayTime,
                userRating = validatedRating,
                review = sanitizedReview,
                lastStatusDate = System.currentTimeMillis(),
                latitude = latitude,
                longitude = longitude,
                locationName = locationName?.trim()?.take(200),
                title = title,
                posterUrl = posterUrl,
                photoUri = photoUri
            )
            gameLogDao.insertOrUpdateGameLog(gameLog)
            true
        } catch (e: Exception) {
            Log.e("LogGameViewModel", "Failed to save game log", e)
            false
        }
    }

    fun updateReview(review: String): kotlinx.coroutines.Job {
        return viewModelScope.launch {
            try {
                val currentLog = gameLogDao.getGameLog(gameId).first()
                if (currentLog != null) {
                    val sanitizedReview = review.trim().take(5000)
                    val updatedLog = currentLog.copy(review = sanitizedReview)
                    gameLogDao.insertOrUpdateGameLog(updatedLog)
                }
            } catch (e: Exception) {
                Log.e("LogGameViewModel", "Failed to update review", e)
            }
        }
    }
}
