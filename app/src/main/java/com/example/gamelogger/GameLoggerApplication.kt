package com.example.gamelogger

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.gamelogger.util.AppLifecycleObserver
import com.example.gamelogger.util.NetworkConnectivityManager

/**
 * Application class that manages global app state including:
 * - Network connectivity monitoring
 * - App lifecycle observation
 * - Memory management callbacks
 */
class GameLoggerApplication : Application() {

    lateinit var networkConnectivityManager: NetworkConnectivityManager
        private set

    lateinit var appLifecycleObserver: AppLifecycleObserver
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize network connectivity manager
        networkConnectivityManager = NetworkConnectivityManager(this)

        // Initialize and register app lifecycle observer
        appLifecycleObserver = AppLifecycleObserver()
        ProcessLifecycleOwner.get().lifecycle.addObserver(appLifecycleObserver)
    }

    @Suppress("DEPRECATION")
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)

        // Handle memory pressure events
        when (level) {
            TRIM_MEMORY_UI_HIDDEN -> {
                // App UI is hidden, good time to release UI resources
            }
            TRIM_MEMORY_RUNNING_MODERATE,
            TRIM_MEMORY_RUNNING_LOW,
            TRIM_MEMORY_RUNNING_CRITICAL -> {
                // System is running low on memory while app is running
                // Consider releasing caches
            }
            TRIM_MEMORY_BACKGROUND,
            TRIM_MEMORY_MODERATE,
            TRIM_MEMORY_COMPLETE -> {
                // App is backgrounded and system needs memory
                // Release as much as possible
            }
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        // Critical memory situation - release everything possible
    }

    companion object {
        lateinit var instance: GameLoggerApplication
            private set
    }
}

