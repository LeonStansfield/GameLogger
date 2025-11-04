package com.example.gamelogger.ui.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.gamelogger.data.model.Game

@Composable
fun GameCard(game: Game,
             modifier: Modifier = Modifier,
             onClick: () -> Unit) {
    Card(
        modifier = modifier
            .clickable(onClick = onClick)
    ) {
        Column(
            // This Column will fill the height given by the modifier
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Image: Fills the available space, cropping to fit
            AsyncImage(
                model = game.cover?.bigCoverUrl,
                contentDescription = "${game.name} poster",
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f), // Takes up all remaining space
                contentScale = ContentScale.Crop // Scales image to fill
            )
            // Text: Has a fixed height and truncates long names
            Text(
                text = game.name,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .height(40.dp), // Fixed height for ~2 lines
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis // Add '...' for long text
            )
        }
    }
}