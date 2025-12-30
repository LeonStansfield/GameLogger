package com.example.gamelogger.ui.features.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.gamelogger.data.db.GameLog
import com.example.gamelogger.data.db.GameLoggerDatabase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen() {
    val context = LocalContext.current
    val viewModel: GalleryViewModel = viewModel(
        factory = GalleryViewModelFactory(
            GameLoggerDatabase.getDatabase(context).gameLogDao()
        )
    )
    val photoLogs by viewModel.photoLogs.collectAsState()

    // State for viewing a photo in full screen
    var selectedLog by remember { mutableStateOf<GameLog?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Photo Gallery") })
        }
    ) { innerPadding ->
        if (photoLogs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("No photos taken yet.", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 120.dp), // Auto-sizing columns
                contentPadding = PaddingValues(
                    start = 8.dp,
                    end = 8.dp,
                    top = innerPadding.calculateTopPadding() + 8.dp,
                    bottom = 8.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(photoLogs) { log ->
                    GalleryItem(
                        gameLog = log,
                        onClick = { selectedLog = log }
                    )
                }
            }
        }
    }

    // Full Screen Overlay (Simple version)
    if (selectedLog != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.9f))
                .clickable { selectedLog = null },
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = selectedLog!!.photoUri,
                contentDescription = "Full Screen Photo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
            // Optional: Show Game Title at bottom
            Text(
                text = selectedLog!!.title ?: "",
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(24.dp)
            )
        }
    }
}

@Composable
fun GalleryItem(
    gameLog: GameLog,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f) // Makes the item square
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
    ) {
        // Main Photo
        AsyncImage(
            model = gameLog.photoUri,
            contentDescription = "Captured Photo",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Small Game Icon in Corner
        // We use a small circular version of the poster
        if (gameLog.posterUrl != null) {
            AsyncImage(
                model = gameLog.posterUrl,
                contentDescription = "Game Icon",
                modifier = Modifier
                    .align(Alignment.BottomEnd) // Position in bottom right
                    .padding(6.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .border(1.dp, Color.White, CircleShape), // White border for visibility
                contentScale = ContentScale.Crop
            )
        }
    }
}