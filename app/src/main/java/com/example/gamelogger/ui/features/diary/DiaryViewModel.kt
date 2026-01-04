package com.example.gamelogger.ui.features.diary

import android.util.Log
import androidx.lifecycle.SavedStateHandle
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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DiaryViewModel(
    private val gameLogDao: GameLogDao,
    private val savedStateHandle: SavedStateHandle = SavedStateHandle()
) : ViewModel() {

    private val _filter = MutableStateFlow<GameStatus?>(
        savedStateHandle.get<String>(KEY_FILTER)?.let {
            try { GameStatus.valueOf(it) } catch (e: Exception) { null }
        }
    )
    val filter: StateFlow<GameStatus?> = _filter.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val gameLogs: StateFlow<List<GameLog>> = gameLogDao.getAllGameLogs()
        .catch { e ->
            Log.e("DiaryViewModel", "Error loading game logs", e)
            _errorMessage.value = "Failed to load diary entries."
            emit(emptyList())
        }
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
        savedStateHandle[KEY_FILTER] = status?.name
    }

    fun deleteGameLog(gameLog: GameLog) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                gameLogDao.deleteGameLog(gameLog)
            } catch (e: Exception) {
                Log.e("DiaryViewModel", "Error deleting game log", e)
                _errorMessage.value = "Failed to delete entry. Please try again."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    private companion object {
        const val KEY_FILTER = "diary_filter"
    }
}

class DiaryViewModelFactory(
    private val gameLogDao: GameLogDao,
    private val savedStateHandle: SavedStateHandle = SavedStateHandle()
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DiaryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DiaryViewModel(gameLogDao, savedStateHandle) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
