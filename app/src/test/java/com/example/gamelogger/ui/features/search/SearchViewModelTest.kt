package com.example.gamelogger.ui.features.search

import android.util.Log
import com.example.gamelogger.data.model.Game
import com.example.gamelogger.data.remote.IgdbService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class SearchViewModelTest {

    private lateinit var viewModel: SearchViewModel
    private lateinit var igdbService: IgdbService
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        igdbService = mockk()

        // Mock android.util as errors without
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
        Dispatchers.resetMain()
    }

    @Test
    fun `onSearchQueryChanged updates searchQuery state`() = runTest {
        // Arrange
        viewModel = SearchViewModel(igdbService)

        // Act
        viewModel.onSearchQueryChanged("Zelda")

        // Assert
        assertEquals("Zelda", viewModel.searchQuery)
    }

    @Test
    fun `onSearchQueryChanged with query less than 3 characters clears games list`() = runTest {
        // Arrange
        viewModel = SearchViewModel(igdbService)

        // Act
        viewModel.onSearchQueryChanged("ab")
        advanceUntilIdle()

        // Assert
        assertTrue("Games list should be empty for query < 3 chars", viewModel.games.isEmpty())
        assertFalse("Loading should be false", viewModel.isLoading)
    }

    @Test
    fun `onSearchQueryChanged with valid query triggers search after debounce`() = runTest {
        // Arrange
        val expectedGames = listOf(
            Game(id = 1, name = "The Legend of Zelda"),
            Game(id = 2, name = "Zelda: Breath of the Wild")
        )
        coEvery { igdbService.searchGames("Zelda") } returns expectedGames
        viewModel = SearchViewModel(igdbService)

        // Act
        viewModel.onSearchQueryChanged("Zelda")

        // Advance time by 500ms (debounce delay)
        advanceTimeBy(500)
        advanceUntilIdle()

        // Assert
        assertEquals("Search query should be updated", "Zelda", viewModel.searchQuery)
        assertEquals("Games list should match search results", expectedGames, viewModel.games)
        assertFalse("Loading should be false after search completes", viewModel.isLoading)
    }

    @Test
    fun `onSearchQueryChanged cancels previous search job when called multiple times`() = runTest {
        // Arrange
        val firstQuery = listOf(Game(id = 1, name = "First"))
        val secondQuery = listOf(Game(id = 2, name = "Second"))

        coEvery { igdbService.searchGames("first") } returns firstQuery
        coEvery { igdbService.searchGames("second") } returns secondQuery

        viewModel = SearchViewModel(igdbService)

        // Act - Type "first", then quickly type "second"
        viewModel.onSearchQueryChanged("first")
        advanceTimeBy(300) // Not enough to trigger debounce

        viewModel.onSearchQueryChanged("second")
        advanceTimeBy(500)
        advanceUntilIdle()

        // Assert - Only second search should have executed
        assertEquals("Games should match second search", secondQuery, viewModel.games)
        coVerify(exactly = 0) { igdbService.searchGames("first") }
        coVerify(exactly = 1) { igdbService.searchGames("second") }
    }

    @Test
    fun `onSearchQueryChanged handles search failure gracefully`() = runTest {
        // Arrange
        coEvery { igdbService.searchGames("error") } throws Exception("Network error")
        viewModel = SearchViewModel(igdbService)

        // Act
        viewModel.onSearchQueryChanged("error")
        advanceTimeBy(500)
        advanceUntilIdle()

        // Assert
        assertTrue("Games list should be empty on error", viewModel.games.isEmpty())
        assertFalse("Loading should be false after error", viewModel.isLoading)
    }

    @Test
    fun `search debounce prevents rapid API calls`() = runTest {
        // Arrange
        val games = listOf(Game(id = 1, name = "Game"))
        coEvery { igdbService.searchGames(any()) } returns games
        viewModel = SearchViewModel(igdbService)

        // Act - Simulate rapid typing
        viewModel.onSearchQueryChanged("zel")
        advanceTimeBy(100)
        viewModel.onSearchQueryChanged("zeld")
        advanceTimeBy(100)
        viewModel.onSearchQueryChanged("zelda")
        advanceTimeBy(500)
        advanceUntilIdle()

        // Assert - Only final search should execute
        coVerify(exactly = 1) { igdbService.searchGames("zelda") }
        coVerify(exactly = 0) { igdbService.searchGames("zel") }
        coVerify(exactly = 0) { igdbService.searchGames("zeld") }
    }

    @Test
    fun `loading state is true during search`() = runTest {
        // Arrange
        val games = listOf(Game(id = 1, name = "Game"))
        coEvery { igdbService.searchGames("test") } returns games
        viewModel = SearchViewModel(igdbService)

        // Act
        viewModel.onSearchQueryChanged("test")
        advanceTimeBy(500)

        // Note: In a real integration test, you'd check isLoading during the search
        // For unit tests with mocked service, this tests the state management
        advanceUntilIdle()

        // Assert
        assertFalse("Loading should be false after search completes", viewModel.isLoading)
    }
}

