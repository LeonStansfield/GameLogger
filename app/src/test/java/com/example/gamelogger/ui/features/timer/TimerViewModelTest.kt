package com.example.gamelogger.ui.features.timer

import android.util.Log
import com.example.gamelogger.data.db.GameLog
import com.example.gamelogger.data.db.GameLogDao
import com.example.gamelogger.data.db.GameStatus
import com.example.gamelogger.data.model.Cover
import com.example.gamelogger.data.model.Game
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
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class TimerViewModelTest {

    private lateinit var viewModel: TimerViewModel
    private lateinit var gameLogDao: GameLogDao
    private val testDispatcher = StandardTestDispatcher()
    private val testGameId = "123"

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        gameLogDao = mockk(relaxed = true)

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
    fun `elapsedTimeSeconds shows total when timer stopped`() = runTest {
        // Arrange
        val existingLog = GameLog(
            gameId = testGameId,
            status = GameStatus.PLAYED,
            playTime = 0,
            userRating = null,
            timerStartTime = null,
            totalSecondsPlayed = 3600, // 1 hour
            sessionCount = 3
        )

        coEvery { gameLogDao.getGameLog(testGameId) } returns flowOf(existingLog)

        viewModel = TimerViewModel(gameLogDao, testGameId)
        val job = launch { viewModel.gameLog.collect { } }
        advanceUntilIdle()

        // Act
        val elapsed = viewModel.elapsedTimeSeconds.value
        job.cancel()

        // Assert
        assertEquals("Should show total seconds when stopped", 3600L, elapsed)
    }

    @Test
    fun `updateManualPlaytime updates total seconds correctly`() = runTest {
        // Arrange
        val gameDetails = Game(id = 123, name = "Test Game")
        val existingLog = GameLog(
            gameId = testGameId,
            status = GameStatus.PLAYING,
            playTime = 0,
            userRating = null,
            totalSecondsPlayed = 1000,
            sessionCount = 2
        )

        coEvery { gameLogDao.getGameLog(testGameId) } returns flowOf(existingLog)
        coEvery { gameLogDao.getGameLogById(testGameId) } returns existingLog

        val updateSlot = slot<GameLog>()
        coEvery { gameLogDao.updateLog(capture(updateSlot)) } returns Unit

        viewModel = TimerViewModel(gameLogDao, testGameId)
        val job = launch { viewModel.gameLog.collect { } }
        advanceUntilIdle()

        // Act - Set to 2 hours 30 minutes
        viewModel.updateManualPlaytime(gameDetails, 2, 30)
        advanceUntilIdle()
        job.cancel()

        // Assert
        val expectedSeconds = (2 * 3600) + (30 * 60) // 9000 seconds
        coVerify { gameLogDao.updateLog(any()) }
        val capturedLog = updateSlot.captured
        assertEquals("Total seconds should be updated", expectedSeconds.toLong(), capturedLog.totalSecondsPlayed)
    }

    @Test
    fun `updateManualPlaytime creates new log if none exists`() = runTest {
        // Arrange
        val gameDetails = Game(id = 123, name = "Test Game", cover = Cover("cover123"))

        coEvery { gameLogDao.getGameLog(testGameId) } returns flowOf(null)
        coEvery { gameLogDao.getGameLogById(testGameId) } returns null

        val insertSlot = slot<GameLog>()
        coEvery { gameLogDao.insertLog(capture(insertSlot)) } returns Unit

        viewModel = TimerViewModel(gameLogDao, testGameId)
        val job = launch { viewModel.gameLog.collect { } }
        advanceUntilIdle()

        // Act - Set to 5 hours 15 minutes
        viewModel.updateManualPlaytime(gameDetails, 5, 15)
        advanceUntilIdle()
        job.cancel()

        // Assert
        val expectedSeconds = (5 * 3600) + (15 * 60) // 18900 seconds
        coVerify { gameLogDao.insertLog(any()) }
        val capturedLog = insertSlot.captured
        assertEquals("Total seconds should be set", expectedSeconds.toLong(), capturedLog.totalSecondsPlayed)
        assertEquals("Session count should be 1", 1, capturedLog.sessionCount)
        assertEquals("Status should be PLAYING", GameStatus.PLAYING, capturedLog.status)
    }

    @Test
    fun `toggleTimer handles null game details gracefully`() = runTest {
        // Arrange
        coEvery { gameLogDao.getGameLog(testGameId) } returns flowOf(null)
        coEvery { gameLogDao.getGameLogById(testGameId) } returns null

        viewModel = TimerViewModel(gameLogDao, testGameId)
        val job = launch { viewModel.gameLog.collect { } }
        advanceUntilIdle()

        // Act - Toggle with null game details
        viewModel.toggleTimer(null)
        advanceUntilIdle()
        job.cancel()

        // Assert - Should not crash, should not insert
        coVerify(exactly = 0) { gameLogDao.insertLog(any()) }
    }


    @Test
    fun `updateManualPlaytime with zero time is valid`() = runTest {
        // Arrange
        val gameDetails = Game(id = 123, name = "Test Game")
        val existingLog = GameLog(
            gameId = testGameId,
            status = GameStatus.BACKLOGGED,
            playTime = 0,
            userRating = null,
            totalSecondsPlayed = 1000,
            sessionCount = 0
        )

        coEvery { gameLogDao.getGameLog(testGameId) } returns flowOf(existingLog)
        coEvery { gameLogDao.getGameLogById(testGameId) } returns existingLog

        val updateSlot = slot<GameLog>()
        coEvery { gameLogDao.updateLog(capture(updateSlot)) } returns Unit

        viewModel = TimerViewModel(gameLogDao, testGameId)
        val job = launch { viewModel.gameLog.collect { } }
        advanceUntilIdle()

        // Act - Set to 0 hours 0 minutes
        viewModel.updateManualPlaytime(gameDetails, 0, 0)
        advanceUntilIdle()
        job.cancel()

        // Assert
        coVerify { gameLogDao.updateLog(any()) }
        val capturedLog = updateSlot.captured
        assertEquals("Total seconds should be 0", 0L, capturedLog.totalSecondsPlayed)
    }
}

