package com.example.gamelogger.ui.features.discover

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gamelogger.data.model.Game
import com.example.gamelogger.data.remote.IgdbService
import kotlinx.coroutines.launch

class DiscoverViewModel(
    private val igdbService: IgdbService = IgdbService()
) : ViewModel() {

    var games by mutableStateOf<List<Game>>(emptyList())
        private set
    var randomGame by mutableStateOf<Game?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    init {
        fetchTop20TrendingGames()
    }

    fun fetchTop20TrendingGames() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                games = igdbService.getTop20TrendingGames()
                if (games.isEmpty()) {
                    Log.d("DiscoverViewModel", "Fetched games list is empty.")
                }
            } catch (e: Exception) {
                Log.e("DiscoverViewModel", "Exception fetching games", e)
                errorMessage = "Unable to load games. Please check your connection and try again."
            } finally {
                isLoading = false
            }
        }
    }

    fun fetchRandomGame() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val game = igdbService.getRandomGame()
                if (game != null) {
                    randomGame = game
                } else {
                    Log.e("DiscoverViewModel", "Random game fetch returned null")
                    errorMessage = "Couldn't find a random game. Please try again."
                }
            } catch (e: Exception) {
                Log.e("DiscoverViewModel", "Exception fetching random game", e)
                errorMessage = "Unable to load random game. Please check your connection."
            } finally {
                isLoading = false
            }
        }
    }

    fun clearError() {
        errorMessage = null
    }

    fun onRandomGameNavigated() {
        randomGame = null
    }
}