package com.example.gamelogger.ui.features.loggame

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gamelogger.data.db.GameLog
import com.example.gamelogger.data.db.GameLogDao
import com.example.gamelogger.data.db.GameStatus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LogGameViewModel(
    private val gameLogDao: GameLogDao,
    private val gameId: String
) : ViewModel() {

    val gameLog: StateFlow<GameLog?> = gameLogDao.getGameLog(gameId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun saveGameLog(status: GameStatus, playTime: Long, userRating: Float?) {
        viewModelScope.launch {
            val gameLog = GameLog(
                gameId = gameId,
                status = status,
                playTime = playTime,
                userRating = userRating
            )
            gameLogDao.insertOrUpdateGameLog(gameLog)
        }
    }
}
