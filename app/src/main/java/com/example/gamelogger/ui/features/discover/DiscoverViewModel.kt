package com.example.gamelogger.ui.features.discover // New package

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gamelogger.data.model.Game // Updated import
import com.example.gamelogger.data.remote.IgdbService // Updated import
import kotlinx.coroutines.launch

// Renamed from GameLoggerViewModel
class DiscoverViewModel : ViewModel() {

    private val igdbService = IgdbService()

    var games by mutableStateOf<List<Game>>(emptyList())
        private set
    var randomGame by mutableStateOf<Game?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set

    init {
        fetchTop20TrendingGames()
    }

    fun fetchTop20TrendingGames() {
        viewModelScope.launch {
            isLoading = true
            try {
                games = igdbService.getTop20TrendingGames()
                if (games.isEmpty()) {
                    Log.d("DiscoverViewModel", "Fetched games list is empty.")
                }
            } catch (e: Exception) {
                Log.e("DiscoverViewModel", "Exception fetching games", e)
            } finally {
                isLoading = false
            }
        }
    }

    fun fetchRandomGame() {
        viewModelScope.launch {
            isLoading = true
            try {
                // Ensure the getRandomGame() method exists in your IgdbService as discussed previously
                val game = igdbService.getRandomGame()
                if (game != null) {
                    randomGame = game
                } else {
                    Log.e("DiscoverViewModel", "Random game fetch returned null")
                }
            } catch (e: Exception) {
                Log.e("DiscoverViewModel", "Exception fetching random game", e)
            } finally {
                isLoading = false
            }
        }
    }

    // Call this after the UI successfully navigates to clear the state
    fun onRandomGameNavigated() {
        randomGame = null
    }
}