package com.example.gamelogger.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Formats a timestamp (milliseconds since epoch) to a human-readable date string
 * @param timestamp The timestamp in milliseconds
 * @param format The desired date format (default: "MMM dd, yyyy")
 * @return Formatted date string
 */
@Suppress("unused")
fun formatTimestamp(timestamp: Long, format: String = "MMM dd, yyyy"): String {
    val sdf = SimpleDateFormat(format, Locale.getDefault())
    return sdf.format(Date(timestamp))
}

/**
 * Formats a timestamp to a relative time string (e.g., "2 days ago")
 * @param timestamp The timestamp in milliseconds
 * @return Relative time string
 */
@Suppress("unused")
fun formatRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24
    val weeks = days / 7
    val months = days / 30
    val years = days / 365

    return when {
        seconds < 60 -> "Just now"
        minutes < 60 -> "$minutes minute${if (minutes != 1L) "s" else ""} ago"
        hours < 24 -> "$hours hour${if (hours != 1L) "s" else ""} ago"
        days < 7 -> "$days day${if (days != 1L) "s" else ""} ago"
        weeks < 4 -> "$weeks week${if (weeks != 1L) "s" else ""} ago"
        months < 12 -> "$months month${if (months != 1L) "s" else ""} ago"
        else -> "$years year${if (years != 1L) "s" else ""} ago"
    }
}

