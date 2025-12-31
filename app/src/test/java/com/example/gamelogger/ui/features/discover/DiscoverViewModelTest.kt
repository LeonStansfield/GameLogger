package com.example.gamelogger.ui.features.discover

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
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class DiscoverViewModelTest {

    private lateinit var viewModel: DiscoverViewModel
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
        // Clean up static mocks to prevent leaks
        unmockkStatic(Log::class)
        Dispatchers.resetMain()
    }

    @Test
    fun `fetchTop20TrendingGames success updates games list`() = runTest {
        // Arrange
        val expectedGames = listOf(
            Game(id = 1, name = "Game 1"),
            Game(id = 2, name = "Game 2")
        )
        coEvery { igdbService.getTop20TrendingGames() } returns expectedGames

        // Act
        viewModel = DiscoverViewModel(igdbService)
        advanceUntilIdle() // Wait for coroutines to finish

        // Assert
        assertEquals("Games list should match the mocked response", expectedGames, viewModel.games)
        assertFalse("Loading should be false after fetch completes", viewModel.isLoading)
        coVerify(exactly = 1) { igdbService.getTop20TrendingGames() }
    }

    @Test
    fun `fetchTop20TrendingGames failure keeps games list empty and handles error`() = runTest {
        // Arrange
        coEvery { igdbService.getTop20TrendingGames() } throws Exception("Network error")

        // Act
        viewModel = DiscoverViewModel(igdbService)
        advanceUntilIdle()

        // Assert
        assertTrue("Games list should be empty on failure", viewModel.games.isEmpty())
        assertFalse("Loading should be false even after error", viewModel.isLoading)
        // Verify we attempted to fetch
        coVerify(exactly = 1) { igdbService.getTop20TrendingGames() }
    }

    @Test
    fun `fetchRandomGame success updates randomGame state`() = runTest {
        // Arrange
        coEvery { igdbService.getTop20TrendingGames() } returns emptyList()
        viewModel = DiscoverViewModel(igdbService)
        
        val randomGame = Game(id = 99, name = "Random Game")
        coEvery { igdbService.getRandomGame() } returns randomGame

        // Act
        viewModel.fetchRandomGame()
        advanceUntilIdle()

        // Assert
        assertEquals("Random game state should be updated", randomGame, viewModel.randomGame)
        assertFalse("Loading should be false after fetch", viewModel.isLoading)
    }

    @Test
    fun `fetchRandomGame failure handles exception gracefully`() = runTest {
        // Arrange
        coEvery { igdbService.getTop20TrendingGames() } returns emptyList()
        viewModel = DiscoverViewModel(igdbService)

        coEvery { igdbService.getRandomGame() } throws Exception("API Error")

        // Act
        viewModel.fetchRandomGame()
        advanceUntilIdle()

        // Assert
        assertNull("Random game should remain null on error", viewModel.randomGame)
        assertFalse("Loading should be reset to false after error", viewModel.isLoading)
    }

    @Test
    fun `onRandomGameNavigated clears randomGame state`() = runTest {
        // Arrange
        coEvery { igdbService.getTop20TrendingGames() } returns emptyList()
        viewModel = DiscoverViewModel(igdbService)
        
        // Simulate a loaded game
        val randomGame = Game(id = 99, name = "Random Game")
        coEvery { igdbService.getRandomGame() } returns randomGame
        viewModel.fetchRandomGame()
        advanceUntilIdle()
        
        // Verify it is set
        assertEquals(randomGame, viewModel.randomGame)

        // Act
        viewModel.onRandomGameNavigated()

        // Assert
        assertNull("Random game state should be cleared after navigation", viewModel.randomGame)
    }
}
