package com.example.gamelogger.util

import android.util.Log
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Locale

class DateUtilsTest {

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0

        // Set default locale for consistent test results
        Locale.setDefault(Locale.US)
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    @Test
    fun `formatTimestamp returns correctly formatted date`() {
        // Arrange - January 15, 2024, 12:00:00 UTC
        val timestamp = 1705320000000L

        // Act
        val result = formatTimestamp(timestamp, "MMM dd, yyyy")

        // Assert - Result will depend on timezone, but should contain year
        assertTrue("Should contain year 2024", result.contains("2024"))
    }

    @Test
    fun `formatTimestamp with custom format works correctly`() {
        // Arrange - January 15, 2024
        val timestamp = 1705320000000L

        // Act
        val result = formatTimestamp(timestamp, "yyyy-MM-dd")

        // Assert
        assertTrue("Should start with 2024", result.startsWith("2024"))
    }

    @Test
    fun `formatTimestamp handles zero timestamp`() {
        // Arrange - Epoch time
        val timestamp = 0L

        // Act
        val result = formatTimestamp(timestamp)

        // Assert - Should format without error
        assertTrue("Should return a valid date string", result.isNotEmpty())
    }

}

