package com.example.gamelogger.data.db

import android.util.Log
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GameLogModelTest {

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    @Test
    fun `GameLog creates with all fields`() {
        // Arrange & Act
        val gameLog = GameLog(
            gameId = "123",
            status = GameStatus.PLAYED,
            playTime = 25,
            userRating = 4.5f,
            review = "Great game!",
            lastStatusDate = 1000000L,
            latitude = 51.5074,
            longitude = -0.1278,
            locationName = "London",
            title = "The Witcher 3",
            posterUrl = "https://example.com/poster.jpg",
            photoUri = "file:///photo.jpg",
            totalSecondsPlayed = 90000,
            sessionCount = 5,
            timerStartTime = 2000000L
        )

        // Assert
        assertEquals("123", gameLog.gameId)
        assertEquals(GameStatus.PLAYED, gameLog.status)
        assertEquals(25L, gameLog.playTime)
        assertEquals(4.5f, gameLog.userRating)
        assertEquals("Great game!", gameLog.review)
        assertEquals(1000000L, gameLog.lastStatusDate)
        assertEquals(51.5074, gameLog.latitude!!, 0.0001)
        assertEquals(-0.1278, gameLog.longitude!!, 0.0001)
        assertEquals("London", gameLog.locationName)
        assertEquals("The Witcher 3", gameLog.title)
        assertEquals("https://example.com/poster.jpg", gameLog.posterUrl)
        assertEquals("file:///photo.jpg", gameLog.photoUri)
        assertEquals(90000L, gameLog.totalSecondsPlayed)
        assertEquals(5, gameLog.sessionCount)
        assertEquals(2000000L, gameLog.timerStartTime)
    }

    @Test
    fun `GameLog with minimal fields creates successfully`() {
        // Arrange & Act
        val gameLog = GameLog(
            gameId = "456",
            status = GameStatus.BACKLOGGED,
            playTime = 0,
            userRating = null
        )

        // Assert
        assertEquals("456", gameLog.gameId)
        assertEquals(GameStatus.BACKLOGGED, gameLog.status)
        assertEquals(0L, gameLog.playTime)
        assertNull(gameLog.userRating)
        assertNull(gameLog.review)
        assertNull(gameLog.latitude)
        assertNull(gameLog.longitude)
        assertNull(gameLog.locationName)
        assertNull(gameLog.title)
        assertNull(gameLog.posterUrl)
        assertNull(gameLog.photoUri)
        assertEquals(0L, gameLog.totalSecondsPlayed)
        assertEquals(0, gameLog.sessionCount)
        assertNull(gameLog.timerStartTime)
    }

    @Test
    fun `GameStatus enum has all expected values`() {
        // Assert
        val statuses = GameStatus.entries

        assertEquals(5, statuses.size)
        assertTrue(statuses.contains(GameStatus.PLAYED))
        assertTrue(statuses.contains(GameStatus.PLAYING))
        assertTrue(statuses.contains(GameStatus.BACKLOGGED))
        assertTrue(statuses.contains(GameStatus.DROPPED))
        assertTrue(statuses.contains(GameStatus.ON_HOLD))
    }

    @Test
    fun `GameStatus values are accessible`() {
        // Act & Assert
        assertNotNull(GameStatus.PLAYED)
        assertNotNull(GameStatus.PLAYING)
        assertNotNull(GameStatus.BACKLOGGED)
        assertNotNull(GameStatus.DROPPED)
        assertNotNull(GameStatus.ON_HOLD)
    }

    @Test
    fun `GameLog with null rating is valid`() {
        // Arrange & Act
        val gameLog = GameLog(
            gameId = "789",
            status = GameStatus.BACKLOGGED,
            playTime = 0,
            userRating = null
        )

        // Assert
        assertNull("Rating can be null for backlogged games", gameLog.userRating)
    }

    @Test
    fun `GameLog with rating bounds is valid`() {
        // Arrange & Act
        val minRating = GameLog(
            gameId = "1",
            status = GameStatus.PLAYED,
            playTime = 10,
            userRating = 0.5f
        )

        val maxRating = GameLog(
            gameId = "2",
            status = GameStatus.PLAYED,
            playTime = 10,
            userRating = 5.0f
        )

        // Assert
        assertEquals(0.5f, minRating.userRating)
        assertEquals(5.0f, maxRating.userRating)
    }

    @Test
    fun `GameLog default lastStatusDate is set`() {
        // Arrange
        val beforeTime = System.currentTimeMillis()

        // Act
        val gameLog = GameLog(
            gameId = "test",
            status = GameStatus.PLAYING,
            playTime = 5,
            userRating = null
        )

        val afterTime = System.currentTimeMillis()

        // Assert
        assertTrue("lastStatusDate should be >= creation time",
            gameLog.lastStatusDate >= beforeTime - 100) // Allow small margin
        assertTrue("lastStatusDate should be <= current time",
            gameLog.lastStatusDate <= afterTime + 100)
    }

    @Test
    fun `GameLog with location data stores correctly`() {
        // Arrange & Act
        val gameLog = GameLog(
            gameId = "loc_test",
            status = GameStatus.PLAYED,
            playTime = 10,
            userRating = 4.0f,
            latitude = 40.7128,
            longitude = -74.0060,
            locationName = "New York"
        )

        // Assert
        assertEquals(40.7128, gameLog.latitude!!, 0.0001)
        assertEquals(-74.0060, gameLog.longitude!!, 0.0001)
        assertEquals("New York", gameLog.locationName)
    }

    @Test
    fun `GameLog with photo URI stores correctly`() {
        // Arrange & Act
        val photoUri = "content://media/external/images/123"
        val gameLog = GameLog(
            gameId = "photo_test",
            status = GameStatus.PLAYED,
            playTime = 15,
            userRating = 5.0f,
            photoUri = photoUri
        )

        // Assert
        assertEquals(photoUri, gameLog.photoUri)
    }

    @Test
    fun `GameLog timer states work correctly`() {
        // Test timer not started
        val notStarted = GameLog(
            gameId = "1",
            status = GameStatus.PLAYING,
            playTime = 0,
            userRating = null,
            timerStartTime = null
        )
        assertNull("Timer not started", notStarted.timerStartTime)

        // Test timer running
        val running = GameLog(
            gameId = "2",
            status = GameStatus.PLAYING,
            playTime = 0,
            userRating = null,
            timerStartTime = System.currentTimeMillis()
        )
        assertNotNull("Timer is running", running.timerStartTime)
    }

    @Test
    fun `GameLog session tracking works`() {
        // Arrange & Act
        val gameLog = GameLog(
            gameId = "session_test",
            status = GameStatus.PLAYING,
            playTime = 0,
            userRating = null,
            totalSecondsPlayed = 7200, // 2 hours
            sessionCount = 3
        )

        // Assert
        assertEquals(7200L, gameLog.totalSecondsPlayed)
        assertEquals(3, gameLog.sessionCount)
    }

    @Test
    fun `GameLog copy preserves all fields`() {
        // Arrange
        val original = GameLog(
            gameId = "copy_test",
            status = GameStatus.PLAYED,
            playTime = 20,
            userRating = 4.5f,
            review = "Original review",
            lastStatusDate = 1000000L
        )

        // Act
        val copy = original.copy(review = "Updated review")

        // Assert
        assertEquals("copy_test", copy.gameId)
        assertEquals(GameStatus.PLAYED, copy.status)
        assertEquals(20L, copy.playTime)
        assertEquals(4.5f, copy.userRating)
        assertEquals("Updated review", copy.review)
        assertEquals(1000000L, copy.lastStatusDate) // Preserved
    }

    @Test
    fun `GameLog with empty review string is valid`() {
        // Arrange & Act
        val gameLog = GameLog(
            gameId = "empty_review",
            status = GameStatus.PLAYED,
            playTime = 10,
            userRating = 3.0f,
            review = ""
        )

        // Assert
        assertEquals("", gameLog.review)
    }

    @Test
    fun `GameLog with very long playtime is valid`() {
        // Arrange & Act - 1000 hours
        val gameLog = GameLog(
            gameId = "long_play",
            status = GameStatus.PLAYED,
            playTime = 1000,
            userRating = 5.0f,
            totalSecondsPlayed = 3600000 // 1000 hours in seconds
        )

        // Assert
        assertEquals(1000L, gameLog.playTime)
        assertEquals(3600000L, gameLog.totalSecondsPlayed)
    }

    @Test
    fun `GameStatus can be compared`() {
        // Arrange
        val status1 = GameStatus.PLAYED
        val status2 = GameStatus.PLAYED
        val status3 = GameStatus.PLAYING

        // Assert
        assertEquals(status1, status2)
        assertFalse("Different statuses should not be equal", status1 == status3)
    }
}

