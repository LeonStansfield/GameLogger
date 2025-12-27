package com.example.gamelogger.ui.features.diary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.gamelogger.data.db.GameLog
import com.example.gamelogger.data.db.GameLogDao
import com.example.gamelogger.data.db.GameStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DiaryViewModel(
    private val gameLogDao: GameLogDao
) : ViewModel() {

    private val _filter = MutableStateFlow<GameStatus?>(null)
    val filter: StateFlow<GameStatus?> = _filter.asStateFlow()

    val gameLogs: StateFlow<List<GameLog>> = gameLogDao.getAllGameLogs()
        .combine(_filter) { logs, currentFilter ->
            if (currentFilter == null) {
                logs
            } else {
                logs.filter { it.status == currentFilter }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setFilter(status: GameStatus?) {
        _filter.value = status
    }

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
