package com.example.gamelogger.ui.features.diary

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import com.example.gamelogger.data.db.GameStatus
import com.example.gamelogger.data.db.GameLog
import com.example.gamelogger.data.db.GameLoggerDatabase
import com.example.gamelogger.util.formatRelativeTime

@Composable
fun DiaryScreen(
    onPosterClick: (String) -> Unit,
    onEditClick: (String) -> Unit
) {
    val context = LocalContext.current
    val viewModel: DiaryViewModel = viewModel(
        factory = DiaryViewModelFactory(
            GameLoggerDatabase.getDatabase(context).gameLogDao()
        )
    )
    val gameLogs by viewModel.gameLogs.collectAsState()
    val currentFilter by viewModel.filter.collectAsState()
    var selectedLog by remember { mutableStateOf<GameLog?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Filter Chips
        ScrollableTabRow(
            selectedTabIndex = if (currentFilter == null) 0 else GameStatus.values().indexOf(currentFilter) + 1,
            edgePadding = 16.dp,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            indicator = { tabPositions ->
                // Custom indicator or default
                TabRowDefaults.Indicator(
                    Modifier.tabIndicatorOffset(tabPositions[if (currentFilter == null) 0 else GameStatus.values().indexOf(currentFilter) + 1])
                )
            },
            divider = {}
        ) {
            // "All" Tab
            Tab(
                selected = currentFilter == null,
                onClick = { viewModel.setFilter(null) },
                text = { Text("All") }
            )
            
            // Status Tabs
            GameStatus.values().forEach { status ->
                Tab(
                    selected = currentFilter == status,
                    onClick = { viewModel.setFilter(status) },
                    text = { 
                         val label = when(status) {
                             GameStatus.PLAYED -> "Played"
                             GameStatus.PLAYING -> "Playing"
                             GameStatus.BACKLOGGED -> "Backlog"
                             GameStatus.DROPPED -> "Dropped"
                             GameStatus.ON_HOLD -> "On Hold"
                         }
                         Text(label) 
                    }
                )
            }
        }

        if (gameLogs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (currentFilter == null) "No games logged yet." else "No games in this category.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(gameLogs) { log ->
                    GameLogItem(
                        gameLog = log,
                        onItemClick = { selectedLog = log },
                        onPosterClick = { onPosterClick(log.gameId) }
                    )
                }
            }
        }
    }

    if (selectedLog != null) {
        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text("Delete Log?") },
                text = { Text("Are you sure you want to delete this log? This cannot be undone.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteGameLog(selectedLog!!)
                            showDeleteConfirm = false
                            selectedLog = null
                        }
                    ) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) {
                        Text("Cancel")
                    }
                }
            )
        } else {
            GameLogDetailsDialog(
                gameLog = selectedLog!!,
                onDismiss = { selectedLog = null },
                onDelete = { showDeleteConfirm = true },
                onEdit = {
                     onEditClick(selectedLog!!.gameId)
                     selectedLog = null // Dismiss dialog when navigating to edit
                }
            )
        }
    }
}

@Composable
fun GameLogItem(
    gameLog: GameLog,
    onItemClick: () -> Unit,
    onPosterClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clickable(onClick = onItemClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Poster
            AsyncImage(
                model = gameLog.posterUrl,
                contentDescription = "Game Poster",
                modifier = Modifier
                    .width(70.dp)
                    .fillMaxHeight()
                    .clickable(onClick = onPosterClick),
                contentScale = ContentScale.Crop
            )

            // Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(8.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = gameLog.title ?: "Unknown Title",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Status Chip-like look
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = gameLog.status.name,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    if (gameLog.userRating != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Rating",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "${gameLog.userRating}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GameLogDetailsDialog(
    gameLog: GameLog,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    var showFullImage by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Box(contentAlignment = Alignment.Center) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = gameLog.title ?: "Game Details",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    DetailRow(label = "Status", value = gameLog.status.name)

                    val totalSeconds = gameLog.totalSecondsPlayed

                    val displayTime = if (totalSeconds > 0) {
                        if (totalSeconds < 3600) {
                            // Less than 1 hour: Show Minutes
                            val minutes = totalSeconds / 60
                            "$minutes mins (${gameLog.sessionCount} sessions)"
                        } else {
                            // 1 hour or more: Show Decimal Hours
                            val totalHours = totalSeconds / 3600f
                            String.format("%.1f hours (%d sessions)", totalHours, gameLog.sessionCount)
                        }
                    } else if (gameLog.playTime > 0) {
                        // Fallback for legacy manual entries
                        "${gameLog.playTime} hours (Manual)"
                    } else {
                        null
                    }

                    // Display the row if has a valid time string
                    displayTime?.let {
                        DetailRow(label = "Play Time", value = it)
                    }

                    gameLog.userRating?.let {
                        DetailRow(label = "Rating", value = "$it / 5.0")
                    }

                    gameLog.locationName?.let {
                        DetailRow(label = "Location", value = it)
                    }

                    if (!gameLog.review.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Review:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = gameLog.review,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    if (!gameLog.photoUri.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = "Memory:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(8.dp))

                        Card(
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showFullImage = true }
                        ){
                            AsyncImage(
                                model = gameLog.photoUri,
                                contentDescription = "Memory",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                    contentScale = ContentScale.Crop
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Logged: ${formatRelativeTime(gameLog.lastStatusDate)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = onDelete) {
                            Text("Delete", color = MaterialTheme.colorScheme.error)
                        }
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                             OutlinedButton(onClick = onEdit) {
                                Text("Edit")
                            }
                            Button(onClick = onDismiss) {
                                Text("Close")
                            }
                        }
                    }
                }
            }
            if (showFullImage && !gameLog.photoUri.isNullOrEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { showFullImage = false },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = gameLog.photoUri,
                        contentDescription = "Memory",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}