package com.example.gamelogger.ui.features.search

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gamelogger.data.model.Game
import com.example.gamelogger.data.remote.IgdbService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SearchViewModel(
    private val igdbService: IgdbService = IgdbService()
) : ViewModel() {

    var searchQuery by mutableStateOf("")
        private set

    var games by mutableStateOf<List<Game>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    private var searchJob: Job? = null

    fun onSearchQueryChanged(query: String) {
        searchQuery = query
        searchJob?.cancel() // Cancel previous job
        searchJob = viewModelScope.launch {
            if (query.length < 3) {
                games = emptyList() // Clear results if query is too short
                return@launch
            }

            isLoading = true
            delay(500) // Debounce: wait 500ms after user stops typing
            try {
                games = igdbService.searchGames(query)
            } catch (e: Exception) {
                Log.e("SearchViewModel", "Exception searching games", e)
            } finally {
                isLoading = false
            }
        }
    }
}