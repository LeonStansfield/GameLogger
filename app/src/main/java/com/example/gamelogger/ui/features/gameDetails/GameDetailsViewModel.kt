package com.example.gamelogger.ui.features.gameDetails

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gamelogger.GameLoggerApplication
import com.example.gamelogger.data.model.Game
import com.example.gamelogger.data.remote.IgdbService
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class GameDetailsViewModel(
    private val igdbService: IgdbService = IgdbService()
) : ViewModel() {

    var gameDetails by mutableStateOf<Game?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var isOffline by mutableStateOf(false)
        private set

    private var currentJob: Job? = null

    fun loadGameDetails(gameId: Int) {
        currentJob?.cancel()

        currentJob = viewModelScope.launch {
            isLoading = true
            errorMessage = null

            // Check network before making request
            val isConnected = try {
                GameLoggerApplication.instance.networkConnectivityManager.isCurrentlyConnected()
            } catch (e: Exception) {
                true // Assume connected if manager not available
            }

            if (!isConnected) {
                isOffline = true
                errorMessage = "No internet connection. Please check your network."
                isLoading = false
                return@launch
            }

            isOffline = false

            try {
                val details = igdbService.getGameDetails(gameId)
                if (details != null) {
                    gameDetails = details
                } else {
                    errorMessage = "Game details not found."
                }
            } catch (e: Exception) {
                Log.e("GameDetailsViewModel", "Exception fetching game details", e)
                errorMessage = "Unable to load game details. Please try again."
            } finally {
                isLoading = false
            }
        }
    }

    suspend fun fetchGameDetails(gameId: Int): Game? {
        // Check network before making request
        val isConnected = try {
            GameLoggerApplication.instance.networkConnectivityManager.isCurrentlyConnected()
        } catch (e: Exception) {
            true
        }

        if (!isConnected) {
            isOffline = true
            return null
        }

        return try {
            igdbService.getGameDetails(gameId)
        } catch (e: Exception) {
            Log.e("GameDetailsViewModel", "Exception fetching game details", e)
            null
        }
    }

    fun clearError() {
        errorMessage = null
    }

    fun retry(gameId: Int) {
        loadGameDetails(gameId)
    }

    override fun onCleared() {
        super.onCleared()
        currentJob?.cancel()
    }
}