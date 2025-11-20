package com.example.gamelogger.ui.features.loggame

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.StarHalf
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RatingDialog(
    currentRating: Float?,
    onRatingSelected: (Float?) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedRating by remember { mutableStateOf(currentRating ?: 0f) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rate this game") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (selectedRating > 0f) "$selectedRating stars" else "No rating",
                    fontSize = 18.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                RatingBar(
                    rating = selectedRating,
                    onRatingChanged = { selectedRating = it }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onRatingSelected(if (selectedRating > 0f) selectedRating else null) }) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = {
                onRatingSelected(null)
                onDismiss()
            }) {
                Text("Clear Rating")
            }
        }
    )
}

@Composable
fun RatingBar(
    rating: Float,
    onRatingChanged: (Float) -> Unit,
    modifier: Modifier = Modifier,
    maxStars: Int = 5
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
    ) {
        for (i in 1..maxStars) {
            StarIcon(
                position = i,
                rating = rating,
                onStarClick = { clickedPosition ->
                    // If clicking the same star, toggle between full and half
                    val newRating = when {
                        rating == clickedPosition.toFloat() -> clickedPosition - 0.5f
                        rating == clickedPosition - 0.5f -> clickedPosition.toFloat()
                        else -> clickedPosition.toFloat()
                    }
                    onRatingChanged(newRating)
                }
            )
        }
    }
}

@Composable
private fun StarIcon(
    position: Int,
    rating: Float,
    onStarClick: (Int) -> Unit
) {
    val icon = when {
        rating >= position -> Icons.Filled.Star
        rating >= position - 0.5f -> Icons.AutoMirrored.Filled.StarHalf
        else -> Icons.Outlined.StarOutline
    }

    val tint = if (rating >= position - 0.5f) Color(0xFFFFD700) else Color.Gray

    Icon(
        imageVector = icon,
        contentDescription = "Star $position",
        tint = tint,
        modifier = Modifier
            .size(48.dp)
            .clickable { onStarClick(position) }
            .padding(4.dp)
    )
}

