package com.example.gamelogger.ui.features.gameDetails

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.gamelogger.data.db.GameLoggerDatabase
import com.example.gamelogger.data.model.Game
import com.example.gamelogger.data.model.getReleaseDateString
import com.example.gamelogger.ui.features.timer.TimerViewModel
import com.example.gamelogger.ui.features.timer.TimerViewModelFactory
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.collectAsState
import com.example.gamelogger.ui.features.timer.EditTimeDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameDetailsScreen(
    gameId: Int,
    onBackClick: () -> Unit,
    onLogGameClick: () -> Unit,
    viewModel: GameDetailsViewModel = viewModel()
) {
    val context = LocalContext.current

    val timerViewModel: TimerViewModel = viewModel(
        factory = TimerViewModelFactory(
            GameLoggerDatabase.getDatabase(context).gameLogDao(),
            gameId.toString()
        )
    )

    var gameDetails by remember { mutableStateOf<Game?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(gameId) {
        isLoading = true
        errorMessage = null
        try {
            // Call ViewModel with the Int ID
            gameDetails = viewModel.fetchGameDetails(gameId)
            if (gameDetails == null) {
                errorMessage = "Game not found."
            }
        } catch (e: Exception) {
            errorMessage = "Failed to load game: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(gameDetails?.name ?: "Loading...") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        },
        floatingActionButton = {
            if (gameDetails != null) {
                ExtendedFloatingActionButton(
                    text = { Text("Log This Game") },
                    icon = { Icon(Icons.Filled.PostAdd, contentDescription = "Log Game") },
                    onClick = onLogGameClick
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator()
            } else if (errorMessage != null) {
                Text(
                    text = errorMessage ?: "",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center
                )
            } else if (gameDetails != null) {
                // Use the data-model-specific composable
                GameDetailsContent(
                    game = gameDetails!!,
                    timerViewModel = timerViewModel
                )
            }
        }
    }
}

@Composable
private fun GameDetailsContent(
    game: Game,
    timerViewModel: TimerViewModel
) {
    val scrollState = rememberScrollState()

    val gameLog by timerViewModel.gameLog.collectAsState(initial = null)
    val elapsedTime by timerViewModel.elapsedTimeSeconds.collectAsState()
    val isTimerRunning = gameLog?.timerStartTime != null
    var showEditTimeDialog by remember { mutableStateOf(false) }

    // Get the best available image URL
    // Prefer the first artwork, fall back to big cover
    val imageUrl = game.artworks?.firstOrNull()?.artworkUrl
        ?: game.cover?.bigCoverUrl

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // --- Image Header ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = "${game.name} artwork",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // Gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                            startY = 300f
                        )
                    )
            )
        }

        // --- Details Section ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = game.name,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
            )

            // Display comma-separated list of genres
            game.genres?.let { genres ->
                val genreText = genres.joinToString { it.name }
                if (genreText.isNotEmpty()) {
                    Text(
                        text = "Genres: $genreText",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Display formatted release date
            game.getReleaseDateString()?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Timer Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isTimerRunning) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = if (isTimerRunning) "Session Active" else "Track Session",
                            style = MaterialTheme.typography.labelMedium
                        )

                        // Timer Display + Edit Button
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = formatSecondsToTime(elapsedTime),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )

                            // Edit Icon
                            IconButton(
                                onClick = { showEditTimeDialog = true },
                                enabled = !isTimerRunning // Disable edit while running
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Time",
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (gameLog != null) {
                            Text(
                                text = "${gameLog!!.sessionCount} sessions logged",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    // Play / Stop Button
                    IconButton(
                        onClick = {
                            // Pass the Game object so the VM can create a log if one doesn't exist
                            timerViewModel.toggleTimer(game)
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                color = if (isTimerRunning) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.primary,
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = if (isTimerRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = if (isTimerRunning) "Stop Timer" else "Start Timer",
                            tint = Color.White
                        )
                    }
                }
            }

            // Summary
            game.summary?.let {
                Text(
                    text = "Summary",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = MaterialTheme.typography.bodyMedium.fontSize * 1.3
                )
            }

            Spacer(modifier = Modifier.height(80.dp)) // Padding for FAB
        }
    }
    if (showEditTimeDialog) {
        EditTimeDialog(
            initialSeconds = elapsedTime,
            onConfirm = { h, m ->
                timerViewModel.updateManualPlaytime(game, h, m)
                showEditTimeDialog = false
            },
            onDismiss = { showEditTimeDialog = false }
        )
    }
}
@SuppressLint("DefaultLocale")
fun formatSecondsToTime(totalSeconds: Long): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d:%02d", hours, minutes, seconds)
}