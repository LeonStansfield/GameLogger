package com.example.gamelogger.ui.features.gameDetails

import android.util.Log
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class GameDetailsUtilsTest {

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
    fun `formatSecondsToTime formats zero seconds correctly`() {
        // Act
        val result = formatSecondsToTime(0)

        // Assert
        assertEquals("00:00:00", result)
    }

    @Test
    fun `formatSecondsToTime formats seconds only`() {
        // Act
        val result = formatSecondsToTime(45)

        // Assert
        assertEquals("00:00:45", result)
    }

    @Test
    fun `formatSecondsToTime formats minutes and seconds`() {
        // Act - 5 minutes 30 seconds
        val result = formatSecondsToTime(330)

        // Assert
        assertEquals("00:05:30", result)
    }

    @Test
    fun `formatSecondsToTime formats hours minutes and seconds`() {
        // Act - 2 hours 15 minutes 45 seconds
        val result = formatSecondsToTime(8145)

        // Assert
        assertEquals("02:15:45", result)
    }

    @Test
    fun `formatSecondsToTime formats exactly one hour`() {
        // Act
        val result = formatSecondsToTime(3600)

        // Assert
        assertEquals("01:00:00", result)
    }

    @Test
    fun `formatSecondsToTime formats exactly one minute`() {
        // Act
        val result = formatSecondsToTime(60)

        // Assert
        assertEquals("00:01:00", result)
    }

    @Test
    fun `formatSecondsToTime handles large hour values`() {
        // Act - 25 hours
        val result = formatSecondsToTime(90000)

        // Assert
        assertEquals("25:00:00", result)
    }

    @Test
    fun `formatSecondsToTime handles 99+ hours`() {
        // Act - 150 hours
        val result = formatSecondsToTime(540000)

        // Assert
        assertEquals("150:00:00", result)
    }

    @Test
    fun `formatSecondsToTime pads single digit values with zeros`() {
        // Act - 1 hour 1 minute 1 second
        val result = formatSecondsToTime(3661)

        // Assert
        assertEquals("01:01:01", result)
    }

    @Test
    fun `formatSecondsToTime handles 59 minutes 59 seconds`() {
        // Act
        val result = formatSecondsToTime(3599)

        // Assert
        assertEquals("00:59:59", result)
    }

    @Test
    fun `formatSecondsToTime handles typical gaming session`() {
        // Act - 3 hours 45 minutes 20 seconds
        val result = formatSecondsToTime(13520)

        // Assert
        assertEquals("03:45:20", result)
    }

    @Test
    fun `formatSecondsToTime handles maximum reasonable playtime`() {
        // Act - 999 hours 59 minutes 59 seconds
        val result = formatSecondsToTime(3599999)

        // Assert
        assertEquals("999:59:59", result)
    }
}

