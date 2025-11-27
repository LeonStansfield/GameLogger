package com.example.gamelogger.ui.features.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gamelogger.data.AppTheme
import com.example.gamelogger.data.ThemeRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val themeRepository: ThemeRepository
) : ViewModel() {

    val currentTheme: StateFlow<AppTheme> = themeRepository.theme
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppTheme.System
        )

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch {
            themeRepository.setTheme(theme)
        }
    }
}
