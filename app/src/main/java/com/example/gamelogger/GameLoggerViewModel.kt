package com.example.gamelogger

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

    init {
        fetchTop20TrendingGames()
    }

    private fun fetchTop20TrendingGames() {
        viewModelScope.launch {
            games = igdbService.getTop20TrendingGames()
        }
    }
}