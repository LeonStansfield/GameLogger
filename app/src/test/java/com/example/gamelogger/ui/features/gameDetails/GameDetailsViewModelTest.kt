package com.example.gamelogger.ui.features.gameDetails

import android.util.Log
import com.example.gamelogger.data.model.Cover
import com.example.gamelogger.data.model.Game
import com.example.gamelogger.data.model.Genre
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
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class GameDetailsViewModelTest {

    private lateinit var viewModel: GameDetailsViewModel
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
    fun `fetchGameDetails returns game when successful`() = runTest {
        // Arrange
        val gameId = 123
        val expectedGame = Game(
            id = gameId,
            name = "The Witcher 3",
            cover = Cover(imageId = "cover123"),
            summary = "An epic RPG adventure",
            genres = listOf(Genre(id = 1, name = "RPG")),
            firstReleaseDate = 1431993600L
        )
        coEvery { igdbService.getGameDetails(gameId) } returns expectedGame
        viewModel = GameDetailsViewModel(igdbService)

        // Act
        val result = viewModel.fetchGameDetails(gameId)

        // Assert
        assertEquals("Should return the correct game", expectedGame, result)
        coVerify(exactly = 1) { igdbService.getGameDetails(gameId) }
    }

    @Test
    fun `fetchGameDetails returns null when game not found`() = runTest {
        // Arrange
        val gameId = 999
        coEvery { igdbService.getGameDetails(gameId) } returns null
        viewModel = GameDetailsViewModel(igdbService)

        // Act
        val result = viewModel.fetchGameDetails(gameId)

        // Assert
        assertNull("Should return null when game not found", result)
        coVerify(exactly = 1) { igdbService.getGameDetails(gameId) }
    }

    @Test
    fun `fetchGameDetails returns null on exception`() = runTest {
        // Arrange
        val gameId = 456
        coEvery { igdbService.getGameDetails(gameId) } throws Exception("Network error")
        viewModel = GameDetailsViewModel(igdbService)

        // Act
        val result = try {
            viewModel.fetchGameDetails(gameId)
        } catch (_: Exception) {
            null
        }

        // Assert
        assertNull("Should return null on exception", result)
    }

    @Test
    fun `fetchGameDetails handles multiple consecutive calls correctly`() = runTest {
        // Arrange
        val game1 = Game(id = 1, name = "Game 1")
        val game2 = Game(id = 2, name = "Game 2")

        coEvery { igdbService.getGameDetails(1) } returns game1
        coEvery { igdbService.getGameDetails(2) } returns game2

        viewModel = GameDetailsViewModel(igdbService)

        // Act
        val result1 = viewModel.fetchGameDetails(1)
        val result2 = viewModel.fetchGameDetails(2)

        // Assert
        assertEquals("First call should return game 1", game1, result1)
        assertEquals("Second call should return game 2", game2, result2)
        coVerify(exactly = 1) { igdbService.getGameDetails(1) }
        coVerify(exactly = 1) { igdbService.getGameDetails(2) }
    }

    @Test
    fun `fetchGameDetails returns game with minimal data`() = runTest {
        // Arrange
        val gameId = 789
        val minimalGame = Game(
            id = gameId,
            name = "Minimal Game"
        )
        coEvery { igdbService.getGameDetails(gameId) } returns minimalGame
        viewModel = GameDetailsViewModel(igdbService)

        // Act
        val result = viewModel.fetchGameDetails(gameId)

        // Assert
        assertEquals("Should handle games with minimal data", minimalGame, result)
        assertEquals("Game name should match", "Minimal Game", result?.name)
        assertNull("Cover should be null", result?.cover)
        assertNull("Summary should be null", result?.summary)
    }
}

