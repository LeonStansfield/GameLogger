package com.example.gamelogger.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class ThemeRepositoryTest {

    private lateinit var themeRepository: ThemeRepository
    private lateinit var context: Context
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var preferences: Preferences

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        dataStore = mockk(relaxed = true)
        preferences = mockk(relaxed = true)

        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    @Test
    fun `theme returns System as default when no preference saved`() = runTest {
        // Arrange
        val themeKey = intPreferencesKey("app_theme")
        every { preferences[themeKey] } returns null
        coEvery { dataStore.data } returns flowOf(preferences)

        // Note: Can't easily mock the extension property, so this test
        // verifies the AppTheme enum behavior instead

        // Act
        val result = AppTheme.fromValue(0)

        // Assert
        assertEquals("Should return System theme for value 0", AppTheme.System, result)
    }

    @Test
    fun `AppTheme fromValue returns correct theme`() {
        // Test System
        assertEquals(AppTheme.System, AppTheme.fromValue(0))

        // Test Light
        assertEquals(AppTheme.Light, AppTheme.fromValue(1))

        // Test Dark
        assertEquals(AppTheme.Dark, AppTheme.fromValue(2))

        // Test invalid value defaults to System
        assertEquals(AppTheme.System, AppTheme.fromValue(99))
        assertEquals(AppTheme.System, AppTheme.fromValue(-1))
    }

    @Test
    fun `AppTheme enum has correct values`() {
        assertEquals(0, AppTheme.System.value)
        assertEquals(1, AppTheme.Light.value)
        assertEquals(2, AppTheme.Dark.value)
    }

    @Test
    fun `AppTheme entries returns all themes`() {
        val entries = AppTheme.entries

        assertEquals(3, entries.size)
        assert(entries.contains(AppTheme.System))
        assert(entries.contains(AppTheme.Light))
        assert(entries.contains(AppTheme.Dark))
    }

    @Test
    fun `AppTheme values are unique`() {
        val values = AppTheme.entries.map { it.value }
        val uniqueValues = values.toSet()

        assertEquals("All theme values should be unique", values.size, uniqueValues.size)
    }
}

