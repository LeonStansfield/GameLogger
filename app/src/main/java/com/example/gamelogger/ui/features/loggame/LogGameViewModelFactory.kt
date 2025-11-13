package com.example.gamelogger.ui.features.loggame

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.gamelogger.data.db.GameLogDao

class LogGameViewModelFactory(
    private val gameLogDao: GameLogDao,
    private val gameId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LogGameViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LogGameViewModel(gameLogDao, gameId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

