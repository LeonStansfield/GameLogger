package com.example.gamelogger.ui.features.search

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.gamelogger.GameLoggerApplication
import com.example.gamelogger.data.model.Game
import com.example.gamelogger.data.remote.IgdbService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SearchViewModel(
    private val igdbService: IgdbService = IgdbService(),
    private val savedStateHandle: SavedStateHandle = SavedStateHandle()
) : ViewModel() {

    // Restore search query from saved state
    var searchQuery by mutableStateOf(savedStateHandle.get<String>(KEY_SEARCH_QUERY) ?: "")
        private set

    var games by mutableStateOf<List<Game>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var isOffline by mutableStateOf(false)
        private set

    private var searchJob: Job? = null

    init {
        // Re-execute search if had a saved query
        val savedQuery = savedStateHandle.get<String>(KEY_SEARCH_QUERY)
        if (!savedQuery.isNullOrBlank() && savedQuery.length >= 3) {
            onSearchQueryChanged(savedQuery)
        }
    }

    fun onSearchQueryChanged(query: String) {
        searchQuery = query
        savedStateHandle[KEY_SEARCH_QUERY] = query
        searchJob?.cancel()
        errorMessage = null

        searchJob = viewModelScope.launch {
            // Sanitize input - remove potentially dangerous characters
            val sanitizedQuery = query.trim()
                .replace("\"", "")
                .replace(";", "")
                .replace("\\", "")
                .replace("'", "")
                .trim() // Trim again after removing characters
                .take(100) // Limit query length

            // Check length AFTER sanitization to prevent empty/short queries
            if (sanitizedQuery.length < 3) {
                games = emptyList()
                if (query.isNotBlank() && sanitizedQuery.isEmpty()) {
                    // User entered only special characters
                    errorMessage = "Please enter valid search characters."
                }
                return@launch
            }

            // Check network before making request
            val isConnected = try {
                GameLoggerApplication.instance.networkConnectivityManager.isCurrentlyConnected()
            } catch (e: Exception) {
                true // Assume connected if manager not available
            }

            if (!isConnected) {
                isOffline = true
                errorMessage = "No internet connection. Search requires network access."
                return@launch
            }

            isOffline = false
            isLoading = true
            delay(500) // Debounce
            try {
                games = igdbService.searchGames(sanitizedQuery)
                if (games.isEmpty() && sanitizedQuery.isNotEmpty()) {
                    // Not an error, just no results
                    errorMessage = null
                }
            } catch (e: Exception) {
                Log.e("SearchViewModel", "Exception searching games", e)
                games = emptyList()
                errorMessage = "Search failed. Please check your connection."
            } finally {
                isLoading = false
            }
        }
    }

    fun clearError() {
        errorMessage = null
    }

    fun clearSearch() {
        searchQuery = ""
        savedStateHandle[KEY_SEARCH_QUERY] = ""
        games = emptyList()
        errorMessage = null
        searchJob?.cancel()
    }

    override fun onCleared() {
        super.onCleared()
        searchJob?.cancel()
    }

    companion object {
        private const val KEY_SEARCH_QUERY = "search_query"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                SearchViewModel(
                    savedStateHandle = createSavedStateHandle()
                )
            }
        }
    }
}