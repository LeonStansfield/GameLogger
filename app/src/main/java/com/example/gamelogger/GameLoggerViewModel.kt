package com.example.gamelogger

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class GameLoggerViewModel : ViewModel() {

    private val igdbService = IgdbService()

    var games by mutableStateOf<List<Game>>(emptyList())
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
                    Log.d("GameLoggerViewModel", "Fetched games list is empty.")
                }
            } catch (e: Exception) {
                Log.e("GameLoggerViewModel", "Exception fetching games", e)
            } finally {
                isLoading = false
            }
        }
    }
}