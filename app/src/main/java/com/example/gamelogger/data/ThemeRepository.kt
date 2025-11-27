package com.example.gamelogger.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

enum class AppTheme(val value: Int) {
    System(0),
    Light(1),
    Dark(2);

    companion object {
        fun fromValue(value: Int): AppTheme = entries.find { it.value == value } ?: System
    }
}

class ThemeRepository(private val context: Context) {

    private val themeKey = intPreferencesKey("app_theme")

    val theme: Flow<AppTheme> = context.dataStore.data
        .map { preferences ->
            val themeValue = preferences[themeKey] ?: AppTheme.System.value
            AppTheme.fromValue(themeValue)
        }

    suspend fun setTheme(theme: AppTheme) {
        context.dataStore.edit { preferences ->
            preferences[themeKey] = theme.value
        }
    }
}
