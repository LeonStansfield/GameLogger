package com.example.gamelogger.ui.features.discover

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
import com.example.gamelogger.util.NetworkStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class DiscoverViewModel(
    private val igdbService: IgdbService = IgdbService(),
    private val savedStateHandle: SavedStateHandle = SavedStateHandle()
) : ViewModel() {

    var games by mutableStateOf<List<Game>>(emptyList())
        private set
    var randomGame by mutableStateOf<Game?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var isOffline by mutableStateOf(false)
        private set

    private var currentJob: Job? = null
    private var hasLoadedOnce = false

    init {
        hasLoadedOnce = savedStateHandle.get<Boolean>(KEY_HAS_LOADED) ?: false
        observeNetworkChanges()
        observeLifecycleChanges()

        if (!hasLoadedOnce) {
            fetchTop20TrendingGames()
        }
    }

    private fun observeNetworkChanges() {
        try {
            val networkManager = GameLoggerApplication.instance.networkConnectivityManager
            networkManager.networkStatus
                .onEach { status ->
                    when (status) {
                        NetworkStatus.Available -> {
                            isOffline = false
                            // Auto-retry if we had an error and network is back
                            if (errorMessage != null && games.isEmpty()) {
                                fetchTop20TrendingGames()
                            }
                        }
                        NetworkStatus.Lost, NetworkStatus.Unavailable -> {
                            isOffline = true
                        }
                    }
                }
                .launchIn(viewModelScope)
        } catch (e: Exception) {
            Log.w("DiscoverViewModel", "Network manager not available", e)
        }
    }

    private fun observeLifecycleChanges() {
        try {
            val lifecycleObserver = GameLoggerApplication.instance.appLifecycleObserver
            lifecycleObserver.wasBackgroundedLongEnough
                .onEach { shouldRefresh ->
                    if (shouldRefresh && !isOffline) {
                        fetchTop20TrendingGames()
                        lifecycleObserver.onRefreshHandled()
                    }
                }
                .launchIn(viewModelScope)
        } catch (e: Exception) {
            Log.w("DiscoverViewModel", "Lifecycle observer not available", e)
        }
    }

    fun fetchTop20TrendingGames() {
        // Cancel any existing job
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

            try {
                games = igdbService.getTop20TrendingGames()
                if (games.isEmpty()) {
                    Log.d("DiscoverViewModel", "Fetched games list is empty.")
                } else {
                    hasLoadedOnce = true
                    savedStateHandle[KEY_HAS_LOADED] = true
                    savedStateHandle[KEY_LAST_FETCH_TIME] = System.currentTimeMillis()
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
        currentJob?.cancel()

        currentJob = viewModelScope.launch {
            isLoading = true
            errorMessage = null

            // Check network before making request
            val isConnected = try {
                GameLoggerApplication.instance.networkConnectivityManager.isCurrentlyConnected()
            } catch (e: Exception) {
                true
            }

            if (!isConnected) {
                isOffline = true
                errorMessage = "No internet connection. Please check your network."
                isLoading = false
                return@launch
            }

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

    override fun onCleared() {
        super.onCleared()
        currentJob?.cancel()
    }

    companion object {
        private const val KEY_HAS_LOADED = "has_loaded"
        private const val KEY_LAST_FETCH_TIME = "last_fetch_time"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                DiscoverViewModel(
                    savedStateHandle = createSavedStateHandle()
                )
            }
        }
    }
}