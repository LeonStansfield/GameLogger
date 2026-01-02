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
    fun `formatRelativeTime returns 'Just now' for recent timestamps`() {
        // Arrange - Mock current time
        mockkStatic(System::class)
        val now = 1705320000000L
        val timestamp = now - 30000 // 30 seconds ago
        every { System.currentTimeMillis() } returns now

        // Act
        val result = formatRelativeTime(timestamp)

        // Assert
        assertEquals("Should return 'Just now' for recent time", "Just now", result)

        unmockkStatic(System::class)
    }

    @Test
    fun `formatRelativeTime returns minutes for timestamps within an hour`() {
        // Arrange
        mockkStatic(System::class)
        val now = 1705320000000L
        val timestamp = now - (15 * 60 * 1000) // 15 minutes ago
        every { System.currentTimeMillis() } returns now

        // Act
        val result = formatRelativeTime(timestamp)

        // Assert
        assertEquals("Should return '15 minutes ago'", "15 minutes ago", result)

        unmockkStatic(System::class)
    }

    @Test
    fun `formatRelativeTime returns singular minute for 1 minute ago`() {
        // Arrange
        mockkStatic(System::class)
        val now = 1705320000000L
        val timestamp = now - (60 * 1000) // 1 minute ago
        every { System.currentTimeMillis() } returns now

        // Act
        val result = formatRelativeTime(timestamp)

        // Assert
        assertEquals("Should return '1 minute ago' (singular)", "1 minute ago", result)

        unmockkStatic(System::class)
    }

    @Test
    fun `formatRelativeTime returns hours for timestamps within a day`() {
        // Arrange
        mockkStatic(System::class)
        val now = 1705320000000L
        val timestamp = now - (5 * 60 * 60 * 1000) // 5 hours ago
        every { System.currentTimeMillis() } returns now

        // Act
        val result = formatRelativeTime(timestamp)

        // Assert
        assertEquals("Should return '5 hours ago'", "5 hours ago", result)

        unmockkStatic(System::class)
    }

    @Test
    fun `formatRelativeTime returns days for timestamps within a week`() {
        // Arrange
        mockkStatic(System::class)
        val now = 1705320000000L
        val timestamp = now - (3 * 24 * 60 * 60 * 1000L) // 3 days ago
        every { System.currentTimeMillis() } returns now

        // Act
        val result = formatRelativeTime(timestamp)

        // Assert
        assertEquals("Should return '3 days ago'", "3 days ago", result)

        unmockkStatic(System::class)
    }

    @Test
    fun `formatRelativeTime returns weeks for timestamps within a month`() {
        // Arrange
        mockkStatic(System::class)
        val now = 1705320000000L
        val timestamp = now - (2 * 7 * 24 * 60 * 60 * 1000L) // 2 weeks ago
        every { System.currentTimeMillis() } returns now

        // Act
        val result = formatRelativeTime(timestamp)

        // Assert
        assertEquals("Should return '2 weeks ago'", "2 weeks ago", result)

        unmockkStatic(System::class)
    }

    @Test
    fun `formatRelativeTime returns months for timestamps within a year`() {
        // Arrange
        mockkStatic(System::class)
        val now = 1705320000000L
        val timestamp = now - (6 * 30 * 24 * 60 * 60 * 1000L) // ~6 months ago
        every { System.currentTimeMillis() } returns now

        // Act
        val result = formatRelativeTime(timestamp)

        // Assert
        assertEquals("Should return '6 months ago'", "6 months ago", result)

        unmockkStatic(System::class)
    }

    @Test
    fun `formatRelativeTime returns years for old timestamps`() {
        // Arrange
        mockkStatic(System::class)
        val now = 1705320000000L
        val timestamp = now - (2 * 365 * 24 * 60 * 60 * 1000L) // 2 years ago
        every { System.currentTimeMillis() } returns now

        // Act
        val result = formatRelativeTime(timestamp)

        // Assert
        assertEquals("Should return '2 years ago'", "2 years ago", result)

        unmockkStatic(System::class)
    }

    @Test
    fun `formatRelativeTime handles singular forms correctly`() {
        mockkStatic(System::class)
        val now = 1705320000000L

        // Test 1 hour
        every { System.currentTimeMillis() } returns now
        var timestamp = now - (60 * 60 * 1000L)
        assertEquals("1 hour ago", formatRelativeTime(timestamp))

        // Test 1 day
        timestamp = now - (24 * 60 * 60 * 1000L)
        assertEquals("1 day ago", formatRelativeTime(timestamp))

        // Test 1 week
        timestamp = now - (7 * 24 * 60 * 60 * 1000L)
        assertEquals("1 week ago", formatRelativeTime(timestamp))

        // Test 1 month
        timestamp = now - (30 * 24 * 60 * 60 * 1000L)
        assertEquals("1 month ago", formatRelativeTime(timestamp))

        // Test 1 year
        timestamp = now - (365 * 24 * 60 * 60 * 1000L)
        assertEquals("1 year ago", formatRelativeTime(timestamp))

        unmockkStatic(System::class)
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

    @Test
    fun `formatRelativeTime handles future timestamps gracefully`() {
        // Arrange - Future timestamp
        mockkStatic(System::class)
        val now = 1705320000000L
        val timestamp = now + (60 * 60 * 1000L) // 1 hour in future
        every { System.currentTimeMillis() } returns now

        // Act
        val result = formatRelativeTime(timestamp)

        // Assert - Negative diff, should return "Just now"
        assertEquals("Should handle future time", "Just now", result)

        unmockkStatic(System::class)
    }
}

