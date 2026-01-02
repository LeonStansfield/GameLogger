package com.example.gamelogger.ui.features.timer

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.gamelogger.data.db.GameLog
import com.example.gamelogger.data.db.GameLoggerDatabase
import com.example.gamelogger.data.db.GameStatus
import com.example.gamelogger.data.model.Cover
import com.example.gamelogger.data.model.Game
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class TimerViewModelIntegrationTest {

    private lateinit var database: GameLoggerDatabase
    private lateinit var viewModel: TimerViewModel
    private val testGameId = "123"

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            GameLoggerDatabase::class.java
        ).allowMainThreadQueries()
            .build()

        viewModel = TimerViewModel(database.gameLogDao(), testGameId)
    }

    @After
    fun tearDown() {
        // Use runBlocking to ensure all pending database operations complete
        kotlinx.coroutines.runBlocking {
            // Small delay to allow any pending operations to complete
            kotlinx.coroutines.delay(500)
        }
        database.close()
    }

    @Test
    fun startTimerCreatesNewLog() = runTest {
        // Arrange
        val gameDetails = Game(
            id = 123,
            name = "Test Game",
            cover = Cover("cover123")
        )

        // Act
        viewModel.toggleTimer(gameDetails).join()

        // Assert
        val log = database.gameLogDao().getGameLogById(testGameId)
        assertNotNull(log)
        assertEquals(GameStatus.PLAYING, log?.status)
        assertNotNull(log?.timerStartTime)
        assertEquals(0, log?.sessionCount)
    }

    @Test
    fun stopTimerUpdatesLog() = runTest {
        // Arrange
        val gameDetails = Game(id = 123, name = "Test Game")
        viewModel.toggleTimer(gameDetails).join()

        // Act
        viewModel.toggleTimer(null).join()

        // Assert
        val log = database.gameLogDao().getGameLogById(testGameId)
        assertNull(log?.timerStartTime)
        assertEquals(1, log?.sessionCount)
        assertTrue("Total seconds should be >= 0", (log?.totalSecondsPlayed ?: -1) >= 0)
    }

    @Test
    fun manualPlaytimeUpdateCreatesLog() = runTest {
        // Arrange
        val gameDetails = Game(id = 123, name = "Test Game", cover = Cover("cover123"))

        // Act
        viewModel.updateManualPlaytime(gameDetails, 5, 30).join() // 5 hours 30 minutes

        // Assert
        val log = database.gameLogDao().getGameLogById(testGameId)
        val expectedSeconds = (5 * 3600) + (30 * 60)
        assertEquals(expectedSeconds.toLong(), log?.totalSecondsPlayed)
        assertEquals(1, log?.sessionCount)
    }

    @Test
    fun manualPlaytimeUpdateExistingLog() = runTest {
        // Arrange
        val existingLog = GameLog(
            gameId = testGameId,
            status = GameStatus.PLAYING,
            playTime = 0,
            userRating = null,
            totalSecondsPlayed = 3600,
            sessionCount = 2
        )
        database.gameLogDao().insertLog(existingLog)

        val gameDetails = Game(id = 123, name = "Test Game")

        // Act
        viewModel.updateManualPlaytime(gameDetails, 10, 0).join() // 10 hours

        // Assert
        val log = database.gameLogDao().getGameLogById(testGameId)
        assertEquals(36000L, log?.totalSecondsPlayed)
        assertEquals(3, log?.sessionCount) // Incremented for significant change
    }

    @Test
    fun elapsedTimeReflectsRunningTimer() = runTest {
        // Arrange
        val gameDetails = Game(id = 123, name = "Test Game")

        // Act
        viewModel.toggleTimer(gameDetails).join()

        // The timer heartbeat runs in viewModelScope with real delay(1000)
        // We need to wait for real time to pass for the heartbeat to tick
        // Using Thread.sleep for real-time delay since test coroutines use virtual time
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            kotlinx.coroutines.delay(2500) // Wait for heartbeat
        }

        // Assert
        val elapsed = viewModel.elapsedTimeSeconds.first()
        assertTrue("Elapsed time should be at least 1 second, was $elapsed", elapsed >= 1)
    }

    @Test
    fun elapsedTimeShowsTotalWhenStopped() = runTest {
        // Arrange - insert the log first
        val log = GameLog(
            gameId = testGameId,
            status = GameStatus.PLAYED,
            playTime = 0,
            userRating = null,
            timerStartTime = null,
            totalSecondsPlayed = 7200,
            sessionCount = 3
        )
        database.gameLogDao().insertLog(log)

        // Create a NEW ViewModel after data is inserted so it picks up the data
        val freshViewModel = TimerViewModel(database.gameLogDao(), testGameId)

        // Act - Wait for ViewModel's init flow collector to receive the data
        // Use real-time delay since the flow collection happens asynchronously
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            kotlinx.coroutines.delay(1000)
        }
        val elapsed = freshViewModel.elapsedTimeSeconds.first()

        // Assert
        assertEquals(7200L, elapsed)
    }

    @Test
    fun multipleTimerSessionsIncrementCount() = runTest {
        // Arrange
        val gameDetails = Game(id = 123, name = "Test Game")

        // Act - Session 1
        viewModel.toggleTimer(gameDetails).join()
        viewModel.toggleTimer(null).join()

        // Session 2
        viewModel.toggleTimer(gameDetails).join()
        viewModel.toggleTimer(null).join()

        // Assert
        val log = database.gameLogDao().getGameLogById(testGameId)
        assertEquals(2, log?.sessionCount)
    }

    @Test
    fun zeroPlaytimeIsValid() = runTest {
        // Arrange
        val gameDetails = Game(id = 123, name = "Test Game")

        // Act
        viewModel.updateManualPlaytime(gameDetails, 0, 0).join()

        // Assert
        val log = database.gameLogDao().getGameLogById(testGameId)
        assertEquals(0L, log?.totalSecondsPlayed)
    }

    @Test
    fun timerStatusChangesGameStatus() = runTest {
        // Arrange
        val existingLog = GameLog(
            gameId = testGameId,
            status = GameStatus.BACKLOGGED,
            playTime = 0,
            userRating = null
        )
        database.gameLogDao().insertLog(existingLog)

        val gameDetails = Game(id = 123, name = "Test Game")

        // Act
        viewModel.toggleTimer(gameDetails).join()

        // Assert
        val log = database.gameLogDao().getGameLogById(testGameId)
        assertEquals(GameStatus.PLAYING, log?.status)
    }

    @Test
    fun significantTimeChangeIncrementsSession() = runTest {
        // Arrange
        val existingLog = GameLog(
            gameId = testGameId,
            status = GameStatus.PLAYING,
            playTime = 0,
            userRating = null,
            totalSecondsPlayed = 1000,
            sessionCount = 1
        )
        database.gameLogDao().insertLog(existingLog)

        val gameDetails = Game(id = 123, name = "Test Game")

        // Act - Change by more than 60 seconds (1 minute)
        viewModel.updateManualPlaytime(gameDetails, 1, 0).join() // 3600 seconds (significant change)

        // Assert
        val log = database.gameLogDao().getGameLogById(testGameId)
        assertEquals(2, log?.sessionCount) // Incremented
    }

    @Test
    fun smallTimeChangeDoesNotIncrementSession() = runTest {
        // Arrange
        val existingLog = GameLog(
            gameId = testGameId,
            status = GameStatus.PLAYING,
            playTime = 0,
            userRating = null,
            totalSecondsPlayed = 3600,
            sessionCount = 2
        )
        database.gameLogDao().insertLog(existingLog)

        val gameDetails = Game(id = 123, name = "Test Game")

        // Act - Change by less than 60 seconds
        viewModel.updateManualPlaytime(gameDetails, 1, 0).join() // 3600 seconds (no change)

        // Assert
        val log = database.gameLogDao().getGameLogById(testGameId)
        assertEquals(2, log?.sessionCount) // Not incremented
    }
}

