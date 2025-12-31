package com.example.gamelogger.ui.features.timer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.gamelogger.data.db.GameLogDao
import com.example.gamelogger.data.db.GameLog
import com.example.gamelogger.data.db.GameStatus
import com.example.gamelogger.data.model.Game
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class TimerViewModel(
    private val gameLogDao: GameLogDao,
    private val gameId: String
) : ViewModel() {

    // Observe the log from DB
    val gameLog: Flow<GameLog?> = gameLogDao.getGameLog(gameId)

    // Local state for the UI ticker (update every second)
    private val _elapsedTimeSeconds = MutableStateFlow(0L)
    val elapsedTimeSeconds: StateFlow<Long> = _elapsedTimeSeconds.asStateFlow()

    private var timerJob: Job? = null


    init {
        // Start a heartbeat to update the UI every second if the timer is running
        viewModelScope.launch {
            gameLog.collect { log ->
                timerJob?.cancel()

                if (log?.timerStartTime != null) {
                    timerJob = launch {
                        while (isActive && log.timerStartTime != null) {
                            val now = System.currentTimeMillis()
                            val currentSessionSeconds = (now - log.timerStartTime) / 1000
                            _elapsedTimeSeconds.value =
                                log.totalSecondsPlayed + currentSessionSeconds
                            delay(1000)
                        }
                    }
                } else {
                    // Timer is stopped, just show total
                    _elapsedTimeSeconds.value = log?.totalSecondsPlayed ?: 0L
                }
            }
        }
    }

    fun toggleTimer(gameDetails: Game?) {
        viewModelScope.launch {
            val existingLog = gameLogDao.getGameLogById(gameId)
            val currentTime = System.currentTimeMillis()

            if (existingLog == null) {
                // Create new log if it doesn't exist
                if (gameDetails != null) {
                    val newLog = GameLog(
                        gameId = gameId,
                        title = gameDetails.name,
                        posterUrl = gameDetails.cover?.bigCoverUrl,
                        status = GameStatus.PLAYING,
                        playTime = 0,
                        userRating = null,
                        review = null,
                        latitude = null,
                        longitude = null,
                        locationName = null,
                        lastStatusDate = currentTime,
                        // Start the timer immediately
                        timerStartTime = currentTime,
                        totalSecondsPlayed = 0,
                        sessionCount = 0
                    )
                    gameLogDao.insertLog(newLog)
                }
            } else {
                // Toggle existing log
                if (existingLog.timerStartTime == null) {
                    // START TIMER
                    val updatedLog = existingLog.copy(
                        timerStartTime = currentTime,
                        status = GameStatus.PLAYING, // Auto-switch to Playing
                        lastStatusDate = currentTime
                    )
                    gameLogDao.updateLog(updatedLog)
                } else {
                    // STOP TIMER
                    val sessionDurationSeconds = (currentTime - existingLog.timerStartTime) / 1000
                    val newTotal = existingLog.totalSecondsPlayed + sessionDurationSeconds

                    val updatedLog = existingLog.copy(
                        timerStartTime = null, // Stop it
                        totalSecondsPlayed = newTotal,
                        sessionCount = existingLog.sessionCount + 1,
                        lastStatusDate = currentTime
                    )
                    gameLogDao.updateLog(updatedLog)
                }
            }
        }
    }
    fun updateManualPlaytime(gameDetails: Game?, hours: Int, minutes: Int) {
        viewModelScope.launch {
            // Calculate the target total seconds from user input
            val targetTotalSeconds = (hours * 3600L) + (minutes * 60L)

            val existingLog = gameLogDao.getGameLogById(gameId)
            val currentTime = System.currentTimeMillis()

            if (existingLog != null) {
                // Check if the time has actually changed
                if (targetTotalSeconds != existingLog.totalSecondsPlayed) {

                    val isSignificantChange = kotlin.math.abs(targetTotalSeconds - existingLog.totalSecondsPlayed) > 60
                    val newSessionCount = if (isSignificantChange) existingLog.sessionCount + 1 else existingLog.sessionCount

                    val updatedLog = existingLog.copy(
                        totalSecondsPlayed = targetTotalSeconds,
                        sessionCount = newSessionCount,
                        lastStatusDate = currentTime
                    )
                    gameLogDao.updateLog(updatedLog)
                }
            } else if (gameDetails != null) {
                // If creating a NEW log manually, it counts as the first session
                val newLog = GameLog(
                    gameId = gameId,
                    title = gameDetails.name,
                    posterUrl = gameDetails.cover?.bigCoverUrl,
                    status = GameStatus.PLAYING,
                    playTime = 0,
                    userRating = null,
                    review = null,
                    latitude = null,
                    longitude = null,
                    locationName = null,
                    lastStatusDate = currentTime,
                    timerStartTime = null,
                    totalSecondsPlayed = targetTotalSeconds,
                    sessionCount = 1 // First session
                )
                gameLogDao.insertLog(newLog)
            }

            // Force UI update
            _elapsedTimeSeconds.value = targetTotalSeconds
        }
    }
}

class TimerViewModelFactory(
    private val gameLogDao: GameLogDao,
    private val gameId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return TimerViewModel(gameLogDao, gameId) as T
    }
}