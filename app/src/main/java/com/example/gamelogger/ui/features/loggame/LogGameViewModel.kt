package com.example.gamelogger.ui.features.loggame

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
    ) {
        // Fetch game details to get title and poster
        val details = try {
            igdbService.getGameDetails(gameId.toInt())
        } catch (e: Exception) {
            null
        }

        val currentLog = gameLog.value
        val title = details?.name ?: currentLog?.title
        val posterUrl = details?.cover?.bigCoverUrl ?: currentLog?.posterUrl

        val gameLog = GameLog(
            gameId = gameId,
            status = status,
            playTime = playTime,
            userRating = userRating,
            review = review,
            lastStatusDate = System.currentTimeMillis(), // Set current timestamp
            latitude = latitude,
            longitude = longitude,
            locationName = locationName,
            title = title,
            posterUrl = posterUrl,
            photoUri = photoUri
        )
        gameLogDao.insertOrUpdateGameLog(gameLog)
    }

    fun updateReview(review: String): kotlinx.coroutines.Job {
        return viewModelScope.launch {
            // Query database directly instead of relying on StateFlow value
            val currentLog = gameLogDao.getGameLog(gameId).first()
            if (currentLog != null) {
                // Preserve the lastStatusDate when only updating review
                val updatedLog = currentLog.copy(review = review)
                gameLogDao.insertOrUpdateGameLog(updatedLog)
            }
        }
    }
}
