package com.example.gamelogger.ui.features.diary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.gamelogger.data.db.GameLog
import com.example.gamelogger.data.db.GameLogDao
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DiaryViewModel(
    private val gameLogDao: GameLogDao
) : ViewModel() {

    val gameLogs: StateFlow<List<GameLog>> = gameLogDao.getAllGameLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteGameLog(gameLog: GameLog) {
        viewModelScope.launch {
            gameLogDao.deleteGameLog(gameLog)
        }
    }
}

class DiaryViewModelFactory(private val gameLogDao: GameLogDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DiaryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DiaryViewModel(gameLogDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
