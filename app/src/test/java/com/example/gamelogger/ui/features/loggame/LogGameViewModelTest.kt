package com.example.gamelogger.ui.features.loggame

import android.util.Log
import com.example.gamelogger.data.db.GameLog
import com.example.gamelogger.data.db.GameLogDao
import com.example.gamelogger.data.db.GameStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class LogGameViewModelTest {

    private lateinit var viewModel: LogGameViewModel
    private lateinit var gameLogDao: GameLogDao
    private val testDispatcher = StandardTestDispatcher()
    private val testGameId = "123"

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        gameLogDao = mockk(relaxed = true)

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
    fun `gameLog flow emits current game log`() = runTest {
        // Arrange
        val expectedLog = GameLog(
            gameId = testGameId,
            status = GameStatus.PLAYED,
            playTime = 10,
            userRating = 4.5f,
            review = "Great game!"
        )
        coEvery { gameLogDao.getGameLog(testGameId) } returns flowOf(expectedLog)

        // Act
        viewModel = LogGameViewModel(gameLogDao, testGameId)

        // Collect from the StateFlow to trigger WhileSubscribed
        val job = launch {
            viewModel.gameLog.collect { }
        }
        advanceUntilIdle()

        val result = viewModel.gameLog.value
        job.cancel()

        // Assert
        assertEquals("Should emit the current game log", expectedLog, result)
    }

    @Test
    fun `saveGameLog creates new log with details`() = runTest {
        // Arrange
        coEvery { gameLogDao.getGameLog(testGameId) } returns flowOf(null)

        viewModel = LogGameViewModel(gameLogDao, testGameId)

        val gameLogSlot = slot<GameLog>()
        coEvery { gameLogDao.insertOrUpdateGameLog(capture(gameLogSlot)) } returns Unit

        // Act
        viewModel.saveGameLog(
            status = GameStatus.PLAYED,
            playTime = 15,
            userRating = 4.0f,
            review = "Amazing!"
        )
        advanceUntilIdle()

        // Assert
        coVerify { gameLogDao.insertOrUpdateGameLog(any()) }
        val capturedLog = gameLogSlot.captured
        assertEquals("Game ID should match", testGameId, capturedLog.gameId)
        assertEquals("Status should be PLAYED", GameStatus.PLAYED, capturedLog.status)
        assertEquals("Play time should match", 15L, capturedLog.playTime)
        assertEquals("Rating should match", 4.0f, capturedLog.userRating)
        assertEquals("Review should match", "Amazing!", capturedLog.review)
    }

    @Test
    fun `saveGameLog updates existing log`() = runTest {
        // Arrange
        val existingLog = GameLog(
            gameId = testGameId,
            status = GameStatus.PLAYING,
            playTime = 5,
            userRating = 3.0f,
            review = "Good so far",
            title = "Existing Title",
            posterUrl = "existing_url"
        )
        coEvery { gameLogDao.getGameLog(testGameId) } returns flowOf(existingLog)

        viewModel = LogGameViewModel(gameLogDao, testGameId)

        val gameLogSlot = slot<GameLog>()
        coEvery { gameLogDao.insertOrUpdateGameLog(capture(gameLogSlot)) } returns Unit

        // Act
        viewModel.saveGameLog(
            status = GameStatus.PLAYED,
            playTime = 20,
            userRating = 5.0f,
            review = "Finished it!"
        )
        advanceUntilIdle()

        // Assert
        val capturedLog = gameLogSlot.captured
        assertEquals("Status should be updated", GameStatus.PLAYED, capturedLog.status)
        assertEquals("Play time should be updated", 20L, capturedLog.playTime)
        assertEquals("Rating should be updated", 5.0f, capturedLog.userRating)
    }

    @Test
    fun `saveGameLog handles location data correctly`() = runTest {
        // Arrange
        coEvery { gameLogDao.getGameLog(testGameId) } returns flowOf(null)

        viewModel = LogGameViewModel(gameLogDao, testGameId)

        val gameLogSlot = slot<GameLog>()
        coEvery { gameLogDao.insertOrUpdateGameLog(capture(gameLogSlot)) } returns Unit

        // Act
        viewModel.saveGameLog(
            status = GameStatus.PLAYED,
            playTime = 10,
            userRating = 4.5f,
            latitude = 51.5074,
            longitude = -0.1278,
            locationName = "London"
        )
        advanceUntilIdle()

        // Assert
        val capturedLog = gameLogSlot.captured
        assertEquals("Latitude should match", 51.5074, capturedLog.latitude)
        assertEquals("Longitude should match", -0.1278, capturedLog.longitude)
        assertEquals("Location name should match", "London", capturedLog.locationName)
    }

    @Test
    fun `saveGameLog handles photo URI correctly`() = runTest {
        // Arrange
        coEvery { gameLogDao.getGameLog(testGameId) } returns flowOf(null)

        viewModel = LogGameViewModel(gameLogDao, testGameId)

        val gameLogSlot = slot<GameLog>()
        coEvery { gameLogDao.insertOrUpdateGameLog(capture(gameLogSlot)) } returns Unit

        // Act
        viewModel.saveGameLog(
            status = GameStatus.PLAYED,
            playTime = 10,
            userRating = 4.5f,
            photoUri = "file:///storage/photo.jpg"
        )
        advanceUntilIdle()

        // Assert
        val capturedLog = gameLogSlot.captured
        assertEquals("Photo URI should match", "file:///storage/photo.jpg", capturedLog.photoUri)
    }

    @Test
    fun `updateReview updates only review field`() = runTest {
        // Arrange
        val existingLog = GameLog(
            gameId = testGameId,
            status = GameStatus.PLAYED,
            playTime = 10,
            userRating = 4.5f,
            review = "Old review",
            lastStatusDate = 123456789L
        )
        coEvery { gameLogDao.getGameLog(testGameId) } returns flowOf(existingLog)

        viewModel = LogGameViewModel(gameLogDao, testGameId)

        // Start collecting to activate the StateFlow
        val job = launch {
            viewModel.gameLog.collect { }
        }
        advanceUntilIdle()

        val gameLogSlot = slot<GameLog>()
        coEvery { gameLogDao.insertOrUpdateGameLog(capture(gameLogSlot)) } returns Unit

        // Act
        viewModel.updateReview("New review!")
        advanceUntilIdle()
        job.cancel()

        // Assert
        val capturedLog = gameLogSlot.captured
        assertEquals("Review should be updated", "New review!", capturedLog.review)
        assertEquals("Status should be preserved", GameStatus.PLAYED, capturedLog.status)
        assertEquals("Play time should be preserved", 10L, capturedLog.playTime)
        assertEquals("Rating should be preserved", 4.5f, capturedLog.userRating)
        assertEquals("lastStatusDate should be preserved", 123456789L, capturedLog.lastStatusDate)
    }

    @Test
    fun `saveGameLog with null rating is allowed`() = runTest {
        // Arrange
        coEvery { gameLogDao.getGameLog(testGameId) } returns flowOf(null)

        viewModel = LogGameViewModel(gameLogDao, testGameId)

        val gameLogSlot = slot<GameLog>()
        coEvery { gameLogDao.insertOrUpdateGameLog(capture(gameLogSlot)) } returns Unit

        // Act
        viewModel.saveGameLog(
            status = GameStatus.BACKLOGGED,
            playTime = 0,
            userRating = null,
            review = null
        )
        advanceUntilIdle()

        // Assert
        val capturedLog = gameLogSlot.captured
        assertNull("Rating should be null", capturedLog.userRating)
        assertNull("Review should be null", capturedLog.review)
        assertEquals("Status should be BACKLOGGED", GameStatus.BACKLOGGED, capturedLog.status)
    }

    @Test
    fun `saveGameLog sets current timestamp for lastStatusDate`() = runTest {
        // Arrange
        val beforeTime = System.currentTimeMillis()
        coEvery { gameLogDao.getGameLog(testGameId) } returns flowOf(null)

        viewModel = LogGameViewModel(gameLogDao, testGameId)

        val gameLogSlot = slot<GameLog>()
        coEvery { gameLogDao.insertOrUpdateGameLog(capture(gameLogSlot)) } returns Unit

        // Act
        viewModel.saveGameLog(
            status = GameStatus.PLAYED,
            playTime = 10,
            userRating = 4.5f
        )
        advanceUntilIdle()

        val afterTime = System.currentTimeMillis()

        // Assert
        val capturedLog = gameLogSlot.captured
        assert(capturedLog.lastStatusDate >= beforeTime) {
            "lastStatusDate should be >= beforeTime"
        }
        assert(capturedLog.lastStatusDate <= afterTime) {
            "lastStatusDate should be <= afterTime"
        }
    }
}

