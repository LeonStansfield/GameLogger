package com.example.gamelogger.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class GameLogDaoIntegrationTest {

    private lateinit var database: GameLoggerDatabase
    private lateinit var gameLogDao: GameLogDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Use in-memory database for testing
        database = Room.inMemoryDatabaseBuilder(
            context,
            GameLoggerDatabase::class.java
        ).allowMainThreadQueries() // Only for testing
            .build()
        gameLogDao = database.gameLogDao()
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
    fun insertAndRetrieveGameLog() = runTest {
        // Arrange
        val gameLog = GameLog(
            gameId = "123",
            status = GameStatus.PLAYED,
            playTime = 10,
            userRating = 4.5f,
            review = "Great game!",
            title = "Test Game"
        )

        // Act
        gameLogDao.insertOrUpdateGameLog(gameLog)
        val retrieved = gameLogDao.getGameLog("123").first()

        // Assert
        assertNotNull(retrieved)
        assertEquals("123", retrieved?.gameId)
        assertEquals(GameStatus.PLAYED, retrieved?.status)
        assertEquals(10L, retrieved?.playTime)
        assertEquals(4.5f, retrieved?.userRating)
        assertEquals("Great game!", retrieved?.review)
        assertEquals("Test Game", retrieved?.title)
    }

    @Test
    fun updateExistingGameLog() = runTest {
        // Arrange
        val original = GameLog(
            gameId = "456",
            status = GameStatus.PLAYING,
            playTime = 5,
            userRating = 3.0f,
            review = "Good so far"
        )
        gameLogDao.insertOrUpdateGameLog(original)

        // Act
        val updated = original.copy(
            status = GameStatus.PLAYED,
            playTime = 20,
            userRating = 5.0f,
            review = "Finished! Amazing!"
        )
        gameLogDao.insertOrUpdateGameLog(updated)
        val retrieved = gameLogDao.getGameLog("456").first()

        // Assert
        assertEquals(GameStatus.PLAYED, retrieved?.status)
        assertEquals(20L, retrieved?.playTime)
        assertEquals(5.0f, retrieved?.userRating)
        assertEquals("Finished! Amazing!", retrieved?.review)
    }

    @Test
    fun deleteGameLog() = runTest {
        // Arrange
        val gameLog = GameLog(
            gameId = "789",
            status = GameStatus.BACKLOGGED,
            playTime = 0,
            userRating = null
        )
        gameLogDao.insertOrUpdateGameLog(gameLog)

        // Act
        gameLogDao.deleteGameLog(gameLog)
        val retrieved = gameLogDao.getGameLog("789").first()

        // Assert
        assertNull(retrieved)
    }

    @Test
    fun getAllGameLogs() = runTest {
        // Arrange
        val log1 = GameLog("1", GameStatus.PLAYED, 10, 4.5f)
        val log2 = GameLog("2", GameStatus.PLAYING, 5, null)
        val log3 = GameLog("3", GameStatus.BACKLOGGED, 0, null)

        // Act
        gameLogDao.insertOrUpdateGameLog(log1)
        gameLogDao.insertOrUpdateGameLog(log2)
        gameLogDao.insertOrUpdateGameLog(log3)
        val allLogs = gameLogDao.getAllGameLogs().first()

        // Assert
        assertEquals(3, allLogs.size)
        assertTrue(allLogs.any { it.gameId == "1" })
        assertTrue(allLogs.any { it.gameId == "2" })
        assertTrue(allLogs.any { it.gameId == "3" })
    }

    @Test
    fun deleteAllGameLogs() = runTest {
        // Arrange
        gameLogDao.insertOrUpdateGameLog(GameLog("1", GameStatus.PLAYED, 10, 4.5f))
        gameLogDao.insertOrUpdateGameLog(GameLog("2", GameStatus.PLAYING, 5, null))

        // Act
        gameLogDao.deleteAllGameLogs()
        val allLogs = gameLogDao.getAllGameLogs().first()

        // Assert
        assertTrue(allLogs.isEmpty())
    }

    @Test
    fun getAllGamesWithPhotos() = runTest {
        // Arrange
        val withPhoto1 = GameLog(
            gameId = "1",
            status = GameStatus.PLAYED,
            playTime = 10,
            userRating = 4.5f,
            photoUri = "file:///photo1.jpg"
        )
        val withPhoto2 = GameLog(
            gameId = "2",
            status = GameStatus.PLAYED,
            playTime = 5,
            userRating = 4.0f,
            photoUri = "file:///photo2.jpg"
        )
        val withoutPhoto = GameLog(
            gameId = "3",
            status = GameStatus.PLAYED,
            playTime = 15,
            userRating = 5.0f,
            photoUri = null
        )

        // Act
        gameLogDao.insertOrUpdateGameLog(withPhoto1)
        gameLogDao.insertOrUpdateGameLog(withPhoto2)
        gameLogDao.insertOrUpdateGameLog(withoutPhoto)
        val photosOnly = gameLogDao.getAllGamesWithPhotos().first()

        // Assert
        assertEquals(2, photosOnly.size)
        assertTrue(photosOnly.all { !it.photoUri.isNullOrEmpty() })
    }

    @Test
    fun getGameLogById() = runTest {
        // Arrange
        val gameLog = GameLog(
            gameId = "999",
            status = GameStatus.ON_HOLD,
            playTime = 8,
            userRating = 3.5f
        )
        gameLogDao.insertOrUpdateGameLog(gameLog)

        // Act
        val retrieved = gameLogDao.getGameLogById("999")

        // Assert
        assertNotNull(retrieved)
        assertEquals("999", retrieved?.gameId)
        assertEquals(GameStatus.ON_HOLD, retrieved?.status)
    }

    @Test
    fun insertLogWithIgnoreConflict() = runTest {
        // Arrange
        val original = GameLog(
            gameId = "conflict",
            status = GameStatus.PLAYED,
            playTime = 10,
            userRating = 4.5f
        )
        gameLogDao.insertLog(original)

        // Act - Try to insert again with different data
        val duplicate = GameLog(
            gameId = "conflict",
            status = GameStatus.BACKLOGGED,
            playTime = 0,
            userRating = null
        )
        gameLogDao.insertLog(duplicate)
        val retrieved = gameLogDao.getGameLog("conflict").first()

        // Assert - Original should be preserved due to IGNORE strategy
        assertEquals(GameStatus.PLAYED, retrieved?.status)
        assertEquals(10L, retrieved?.playTime)
    }

    @Test
    fun updateLog() = runTest {
        // Arrange
        val gameLog = GameLog(
            gameId = "update_test",
            status = GameStatus.PLAYING,
            playTime = 5,
            userRating = null,
            totalSecondsPlayed = 1800
        )
        gameLogDao.insertLog(gameLog)

        // Act
        val updated = gameLog.copy(
            totalSecondsPlayed = 3600,
            sessionCount = 2
        )
        gameLogDao.updateLog(updated)
        val retrieved = gameLogDao.getGameLog("update_test").first()

        // Assert
        assertEquals(3600L, retrieved?.totalSecondsPlayed)
        assertEquals(2, retrieved?.sessionCount)
    }

    @Test
    fun gameLogWithLocationData() = runTest {
        // Arrange & Act
        val gameLog = GameLog(
            gameId = "location_test",
            status = GameStatus.PLAYED,
            playTime = 12,
            userRating = 4.0f,
            latitude = 51.5074,
            longitude = -0.1278,
            locationName = "London"
        )
        gameLogDao.insertOrUpdateGameLog(gameLog)
        val retrieved = gameLogDao.getGameLog("location_test").first()

        // Assert
        assertEquals(51.5074, retrieved?.latitude!!, 0.0001)
        assertEquals(-0.1278, retrieved?.longitude!!, 0.0001)
        assertEquals("London", retrieved?.locationName)
    }

    @Test
    fun gameLogWithTimerData() = runTest {
        // Arrange & Act
        val currentTime = System.currentTimeMillis()
        val gameLog = GameLog(
            gameId = "timer_test",
            status = GameStatus.PLAYING,
            playTime = 0,
            userRating = null,
            timerStartTime = currentTime,
            totalSecondsPlayed = 7200,
            sessionCount = 3
        )
        gameLogDao.insertOrUpdateGameLog(gameLog)
        val retrieved = gameLogDao.getGameLog("timer_test").first()

        // Assert
        assertEquals(currentTime, retrieved?.timerStartTime)
        assertEquals(7200L, retrieved?.totalSecondsPlayed)
        assertEquals(3, retrieved?.sessionCount)
    }

    @Test
    fun multipleLogsWithDifferentStatuses() = runTest {
        // Arrange
        val played = GameLog("1", GameStatus.PLAYED, 10, 5.0f)
        val playing = GameLog("2", GameStatus.PLAYING, 5, null)
        val backlogged = GameLog("3", GameStatus.BACKLOGGED, 0, null)
        val dropped = GameLog("4", GameStatus.DROPPED, 2, 1.5f)
        val onHold = GameLog("5", GameStatus.ON_HOLD, 8, 3.5f)

        // Act
        gameLogDao.insertOrUpdateGameLog(played)
        gameLogDao.insertOrUpdateGameLog(playing)
        gameLogDao.insertOrUpdateGameLog(backlogged)
        gameLogDao.insertOrUpdateGameLog(dropped)
        gameLogDao.insertOrUpdateGameLog(onHold)
        val allLogs = gameLogDao.getAllGameLogs().first()

        // Assert
        assertEquals(5, allLogs.size)
        assertEquals(1, allLogs.count { it.status == GameStatus.PLAYED })
        assertEquals(1, allLogs.count { it.status == GameStatus.PLAYING })
        assertEquals(1, allLogs.count { it.status == GameStatus.BACKLOGGED })
        assertEquals(1, allLogs.count { it.status == GameStatus.DROPPED })
        assertEquals(1, allLogs.count { it.status == GameStatus.ON_HOLD })
    }

    @Test
    fun gameLogWithAllOptionalFields() = runTest {
        // Arrange & Act
        val completeLog = GameLog(
            gameId = "complete",
            status = GameStatus.PLAYED,
            playTime = 25,
            userRating = 5.0f,
            review = "Perfect game!",
            lastStatusDate = System.currentTimeMillis(),
            latitude = 40.7128,
            longitude = -74.0060,
            locationName = "New York",
            title = "Complete Game",
            posterUrl = "https://example.com/poster.jpg",
            photoUri = "file:///photo.jpg",
            totalSecondsPlayed = 90000,
            sessionCount = 10,
            timerStartTime = null
        )
        gameLogDao.insertOrUpdateGameLog(completeLog)
        val retrieved = gameLogDao.getGameLog("complete").first()

        // Assert
        assertNotNull(retrieved)
        assertEquals("complete", retrieved?.gameId)
        assertEquals("Perfect game!", retrieved?.review)
        assertEquals("Complete Game", retrieved?.title)
        assertEquals("https://example.com/poster.jpg", retrieved?.posterUrl)
        assertEquals("file:///photo.jpg", retrieved?.photoUri)
        assertEquals(90000L, retrieved?.totalSecondsPlayed)
        assertEquals(10, retrieved?.sessionCount)
    }

    @Test
    fun queryNonExistentGameLog() = runTest {
        // Act
        val retrieved = gameLogDao.getGameLog("nonexistent").first()

        // Assert
        assertNull(retrieved)
    }

    @Test
    fun emptyPhotoListWhenNoPhotos() = runTest {
        // Arrange
        gameLogDao.insertOrUpdateGameLog(GameLog("1", GameStatus.PLAYED, 10, 4.5f, photoUri = null))
        gameLogDao.insertOrUpdateGameLog(GameLog("2", GameStatus.PLAYED, 5, 4.0f, photoUri = ""))

        // Act
        val photosOnly = gameLogDao.getAllGamesWithPhotos().first()

        // Assert
        assertTrue(photosOnly.isEmpty())
    }
}

