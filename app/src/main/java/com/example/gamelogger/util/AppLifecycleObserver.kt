package com.example.gamelogger.util

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Observes app lifecycle events and provides state updates.
 * This helps ViewModels react to app backgrounding/foregrounding.
 */
class AppLifecycleObserver : DefaultLifecycleObserver {

    private val _appState = MutableStateFlow(AppState.FOREGROUND)
    val appState: StateFlow<AppState> = _appState.asStateFlow()

    private var lastBackgroundTime: Long = 0L
    private val _wasBackgroundedLongEnough = MutableStateFlow(false)
    val wasBackgroundedLongEnough: StateFlow<Boolean> = _wasBackgroundedLongEnough.asStateFlow()

    /**
     * Threshold in milliseconds to consider "long enough" for data refresh.
     * Default: 5 minutes
     */
    var refreshThresholdMs: Long = 5 * 60 * 1000L

    override fun onStart(owner: LifecycleOwner) {
        val wasBackground = _appState.value == AppState.BACKGROUND
        _appState.value = AppState.FOREGROUND

        if (wasBackground) {
            val timeInBackground = System.currentTimeMillis() - lastBackgroundTime
            _wasBackgroundedLongEnough.value = timeInBackground > refreshThresholdMs
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        _appState.value = AppState.BACKGROUND
        lastBackgroundTime = System.currentTimeMillis()
        _wasBackgroundedLongEnough.value = false
    }

    override fun onDestroy(owner: LifecycleOwner) {
        _appState.value = AppState.DESTROYED
    }

    /**
     * Reset the background flag after handling it.
     */
    fun onRefreshHandled() {
        _wasBackgroundedLongEnough.value = false
    }

    /**
     * Check if app is currently in foreground.
     */
    fun isInForeground(): Boolean = _appState.value == AppState.FOREGROUND
}

/**
 * Represents the current app lifecycle state.
 */
enum class AppState {
    FOREGROUND,
    BACKGROUND,
    DESTROYED
}

