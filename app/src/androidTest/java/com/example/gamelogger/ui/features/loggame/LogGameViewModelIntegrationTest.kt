package com.example.gamelogger.ui.features.loggame

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.gamelogger.data.db.GameLog
import com.example.gamelogger.data.db.GameLoggerDatabase
import com.example.gamelogger.data.db.GameStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class LogGameViewModelIntegrationTest {

    private lateinit var database: GameLoggerDatabase
    private lateinit var viewModel: LogGameViewModel
    private val testGameId = "123"

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            GameLoggerDatabase::class.java
        ).allowMainThreadQueries()
            .build()

        viewModel = LogGameViewModel(database.gameLogDao(), testGameId)
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
    fun saveNewGameLog() = runTest {
        // Act
        viewModel.saveGameLog(
            status = GameStatus.PLAYED,
            playTime = 15,
            userRating = 4.5f,
            review = "Excellent game!"
        )

        // Assert
        val saved = database.gameLogDao().getGameLog(testGameId).first()
        assertNotNull(saved)
        assertEquals(GameStatus.PLAYED, saved?.status)
        assertEquals(15L, saved?.playTime)
        assertEquals(4.5f, saved?.userRating)
        assertEquals("Excellent game!", saved?.review)
    }

    @Test
    fun updateExistingGameLog() = runTest {
        // Arrange
        viewModel.saveGameLog(
            status = GameStatus.PLAYING,
            playTime = 5,
            userRating = 3.0f,
            review = "Good so far"
        )

        // Act
        viewModel.saveGameLog(
            status = GameStatus.PLAYED,
            playTime = 20,
            userRating = 5.0f,
            review = "Finished! Amazing!"
        )

        // Assert
        val updated = database.gameLogDao().getGameLog(testGameId).first()
        assertEquals(GameStatus.PLAYED, updated?.status)
        assertEquals(20L, updated?.playTime)
        assertEquals(5.0f, updated?.userRating)
        assertEquals("Finished! Amazing!", updated?.review)
    }

    @Test
    fun saveGameLogWithLocation() = runTest {
        // Act
        viewModel.saveGameLog(
            status = GameStatus.PLAYED,
            playTime = 10,
            userRating = 4.0f,
            latitude = 51.5074,
            longitude = -0.1278,
            locationName = "London"
        )

        // Assert
        val saved = database.gameLogDao().getGameLog(testGameId).first()
        assertEquals(51.5074, saved?.latitude!!, 0.0001)
        assertEquals(-0.1278, saved?.longitude!!, 0.0001)
        assertEquals("London", saved?.locationName)
    }

    @Test
    fun saveGameLogWithPhoto() = runTest {
        // Act
        viewModel.saveGameLog(
            status = GameStatus.PLAYED,
            playTime = 12,
            userRating = 4.5f,
            photoUri = "file:///storage/photo.jpg"
        )

        // Assert
        val saved = database.gameLogDao().getGameLog(testGameId).first()
        assertEquals("file:///storage/photo.jpg", saved?.photoUri)
    }

    @Test
    fun updateReviewOnly() = runTest {
        // Arrange
        viewModel.saveGameLog(
            status = GameStatus.PLAYED,
            playTime = 10,
            userRating = 4.5f,
            review = "Original review"
        )
        val original = database.gameLogDao().getGameLog(testGameId).first()
        val originalTimestamp = original?.lastStatusDate

        // Act
        viewModel.updateReview("Updated review!").join() // Wait for coroutine to complete

        // Assert
        val updated = database.gameLogDao().getGameLog(testGameId).first()
        assertEquals("Updated review!", updated?.review)
        assertEquals(GameStatus.PLAYED, updated?.status) // Preserved
        assertEquals(10L, updated?.playTime) // Preserved
        assertEquals(4.5f, updated?.userRating) // Preserved
        assertEquals(originalTimestamp, updated?.lastStatusDate) // Preserved
    }

    @Test
    fun gameLogFlowEmitsUpdates() = runTest {
        // Arrange - collect initial value
        val initial = viewModel.gameLog.first()
        assertNull(initial)

        // Act
        viewModel.saveGameLog(
            status = GameStatus.PLAYED,
            playTime = 10,
            userRating = 4.5f
        )

        // Assert - collect updated value
        val updated = viewModel.gameLog.drop(1).first()
        assertNotNull(updated)
        assertEquals(testGameId, updated?.gameId)
    }

    @Test
    fun saveGameLogWithNullRating() = runTest {
        // Act
        viewModel.saveGameLog(
            status = GameStatus.BACKLOGGED,
            playTime = 0,
            userRating = null,
            review = null
        )

        // Assert
        val saved = database.gameLogDao().getGameLog(testGameId).first()
        assertEquals(GameStatus.BACKLOGGED, saved?.status)
        assertNull(saved?.userRating)
        assertNull(saved?.review)
    }

    @Test
    fun saveGameLogSetsTimestamp() = runTest {
        // Arrange
        val beforeTime = System.currentTimeMillis()

        // Act
        viewModel.saveGameLog(
            status = GameStatus.PLAYED,
            playTime = 10,
            userRating = 4.5f
        )

        val afterTime = System.currentTimeMillis()

        // Assert
        val saved = database.gameLogDao().getGameLog(testGameId).first()
        assertNotNull(saved?.lastStatusDate)
        assert(saved!!.lastStatusDate >= beforeTime - 100)
        assert(saved.lastStatusDate <= afterTime + 100)
    }

    @Test
    fun multipleGameLogsSavedIndependently() = runTest {
        // Arrange
        val viewModel1 = LogGameViewModel(database.gameLogDao(), "game1")
        val viewModel2 = LogGameViewModel(database.gameLogDao(), "game2")

        // Act
        viewModel1.saveGameLog(GameStatus.PLAYED, 10, 4.5f, "Game 1 review")
        viewModel2.saveGameLog(GameStatus.PLAYING, 5, null, null)

        // Assert
        val game1 = database.gameLogDao().getGameLog("game1").first()
        val game2 = database.gameLogDao().getGameLog("game2").first()

        assertEquals(GameStatus.PLAYED, game1?.status)
        assertEquals("Game 1 review", game1?.review)
        assertEquals(GameStatus.PLAYING, game2?.status)
        assertNull(game2?.review)
    }

    @Test
    fun updateReviewOnNonExistentLogDoesNothing() = runTest {
        // Act
        viewModel.updateReview("New review")

        // Assert
        val result = database.gameLogDao().getGameLog(testGameId).first()
        assertNull(result) // No log should be created
    }
}

