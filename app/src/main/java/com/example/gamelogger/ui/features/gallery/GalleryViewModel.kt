package com.example.gamelogger.ui.features.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.gamelogger.data.db.GameLogDao
import com.example.gamelogger.data.db.GameLog
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class GalleryViewModel(gameLogDao: GameLogDao) : ViewModel() {

    // Hot flow of logs that have photos
    val photoLogs: StateFlow<List<GameLog>> = gameLogDao.getAllGamesWithPhotos()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
}

class GalleryViewModelFactory(private val gameLogDao: GameLogDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GalleryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GalleryViewModel(gameLogDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}