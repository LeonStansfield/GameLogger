package com.example.gamelogger.ui.features.diary

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.gamelogger.data.db.GameLog
import com.example.gamelogger.data.db.GameLoggerDatabase
import com.example.gamelogger.data.db.GameStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class DiaryViewModelIntegrationTest {

    private lateinit var database: GameLoggerDatabase
    private lateinit var viewModel: DiaryViewModel
    private lateinit var collectorJob: Job

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            GameLoggerDatabase::class.java
        ).allowMainThreadQueries()
            .build()

        viewModel = DiaryViewModel(database.gameLogDao())
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
    fun diaryDisplaysAllLogsWithoutFilter() = runTest {
        // Arrange
        val log1 = GameLog("1", GameStatus.PLAYED, 10, 4.5f, review = "Great!")
        val log2 = GameLog("2", GameStatus.PLAYING, 5, null)
        val log3 = GameLog("3", GameStatus.BACKLOGGED, 0, null)

        database.gameLogDao().insertOrUpdateGameLog(log1)
        database.gameLogDao().insertOrUpdateGameLog(log2)
        database.gameLogDao().insertOrUpdateGameLog(log3)

        // Act - drop initial empty value, get first real emission
        val logs = viewModel.gameLogs.drop(1).first()

        // Assert
        assertEquals(3, logs.size)
    }

    @Test
    fun diaryFiltersByPlayedStatus() = runTest {
        // Arrange
        val played1 = GameLog("1", GameStatus.PLAYED, 10, 4.5f)
        val played2 = GameLog("2", GameStatus.PLAYED, 15, 5.0f)
        val playing = GameLog("3", GameStatus.PLAYING, 5, null)

        database.gameLogDao().insertOrUpdateGameLog(played1)
        database.gameLogDao().insertOrUpdateGameLog(played2)
        database.gameLogDao().insertOrUpdateGameLog(playing)

        // Act
        viewModel.setFilter(GameStatus.PLAYED)
        val logs = viewModel.gameLogs.drop(1).first()

        // Assert
        assertEquals(2, logs.size)
        assertTrue(logs.all { it.status == GameStatus.PLAYED })
    }

    @Test
    fun diaryDeletesGameLog() = runTest {
        // Arrange
        val log1 = GameLog("1", GameStatus.PLAYED, 10, 4.5f)
        val log2 = GameLog("2", GameStatus.PLAYING, 5, null)

        database.gameLogDao().insertOrUpdateGameLog(log1)
        database.gameLogDao().insertOrUpdateGameLog(log2)

        // Act
        viewModel.deleteGameLog(log1)
        val logs = viewModel.gameLogs.drop(1).first()

        // Assert
        assertEquals(1, logs.size)
        assertEquals("2", logs[0].gameId)
    }

    @Test
    fun diaryClearFilterShowsAllLogs() = runTest {
        // Arrange
        database.gameLogDao().insertOrUpdateGameLog(GameLog("1", GameStatus.PLAYED, 10, 4.5f))
        database.gameLogDao().insertOrUpdateGameLog(GameLog("2", GameStatus.PLAYING, 5, null))

        viewModel.setFilter(GameStatus.PLAYED)
        val filteredLogs = viewModel.gameLogs.drop(1).first()
        assertEquals(1, filteredLogs.size)

        // Act
        viewModel.setFilter(null)
        val allLogs = viewModel.gameLogs.drop(1).first()

        // Assert
        assertEquals(2, allLogs.size)
    }

    @Test
    fun diaryHandlesEmptyDatabase() = runTest {
        // Act
        val logs = viewModel.gameLogs.first()

        // Assert
        assertTrue(logs.isEmpty())
    }

    @Test
    fun diaryUpdatesWhenNewLogAdded() = runTest {
        // Arrange - initial state should be empty
        val initialLogs = viewModel.gameLogs.first()
        assertEquals(0, initialLogs.size)

        // Act
        database.gameLogDao().insertOrUpdateGameLog(
            GameLog("1", GameStatus.PLAYED, 10, 4.5f)
        )
        val updatedLogs = viewModel.gameLogs.drop(1).first()

        // Assert
        assertEquals(1, updatedLogs.size)
    }

    @Test
    fun diaryFiltersAllStatuses() = runTest {
        // Arrange
        database.gameLogDao().insertOrUpdateGameLog(GameLog("1", GameStatus.PLAYED, 10, 5.0f))
        database.gameLogDao().insertOrUpdateGameLog(GameLog("2", GameStatus.PLAYING, 5, null))
        database.gameLogDao().insertOrUpdateGameLog(GameLog("3", GameStatus.BACKLOGGED, 0, null))
        database.gameLogDao().insertOrUpdateGameLog(GameLog("4", GameStatus.DROPPED, 2, 1.5f))
        database.gameLogDao().insertOrUpdateGameLog(GameLog("5", GameStatus.ON_HOLD, 8, 3.5f))

        // Test each status filter
        val statuses = listOf(
            GameStatus.PLAYED,
            GameStatus.PLAYING,
            GameStatus.BACKLOGGED,
            GameStatus.DROPPED,
            GameStatus.ON_HOLD
        )

        for (status in statuses) {
            // Act
            viewModel.setFilter(status)
            val logs = viewModel.gameLogs.drop(1).first()

            // Assert
            assertEquals(1, logs.size)
            assertEquals(status, logs[0].status)
        }
    }
}

