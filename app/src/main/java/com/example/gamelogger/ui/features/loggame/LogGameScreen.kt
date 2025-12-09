package com.example.gamelogger.ui.features.loggame

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gamelogger.data.db.GameLoggerDatabase
import com.example.gamelogger.data.db.GameStatus
import com.example.gamelogger.util.formatRelativeTime
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogGameScreen(
    gameId: String,
    onBackClick: () -> Unit,
    onNavigateToReview: (String, String?) -> Unit,
    viewModel: LogGameViewModel = viewModel(
        factory = LogGameViewModelFactory(
            GameLoggerDatabase.getDatabase(LocalContext.current).gameLogDao(),
            gameId
        )
    )
) {
    val gameLog by viewModel.gameLog.collectAsState()
    var selectedStatus by remember { mutableStateOf<GameStatus?>(null) }
    var selectedRating by remember { mutableStateOf<Float?>(null) }
    var selectedLatitude by remember { mutableStateOf<Double?>(null) }
    var selectedLongitude by remember { mutableStateOf<Double?>(null) }
    var showRatingDialog by remember { mutableStateOf(false) }
    var showLocationDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(gameLog) {
        gameLog?.let {
            selectedStatus = it.status
            selectedRating = it.userRating
            selectedLatitude = it.latitude
            selectedLongitude = it.longitude
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Log Game") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Log game ID: $gameId",
                fontSize = 18.sp
            )

            // Display last status update date if available
            gameLog?.let {
                Text(
                    text = "Last updated: ${formatRelativeTime(it.lastStatusDate)}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(text = "Status", fontSize = 16.sp)

            // Status buttons - similar to Backloggd
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusButton(
                    text = "Played",
                    isSelected = selectedStatus == GameStatus.PLAYED,
                    onClick = { selectedStatus = GameStatus.PLAYED },
                    modifier = Modifier.weight(1f)
                )
                StatusButton(
                    text = "Playing",
                    isSelected = selectedStatus == GameStatus.PLAYING,
                    onClick = { selectedStatus = GameStatus.PLAYING },
                    modifier = Modifier.weight(1f)
                )
                StatusButton(
                    text = "Backlog",
                    isSelected = selectedStatus == GameStatus.BACKLOGGED,
                    onClick = { selectedStatus = GameStatus.BACKLOGGED },
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusButton(
                    text = "Dropped",
                    isSelected = selectedStatus == GameStatus.DROPPED,
                    onClick = { selectedStatus = GameStatus.DROPPED },
                    modifier = Modifier.weight(1f)
                )
                StatusButton(
                    text = "On Hold",
                    isSelected = selectedStatus == GameStatus.ON_HOLD,
                    onClick = { selectedStatus = GameStatus.ON_HOLD },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Rating button
            OutlinedButton(
                onClick = { showRatingDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (selectedRating != null) {
                        "Rating: $selectedRating ★"
                    } else {
                        "Add Rating"
                    }
                )
            }

            // Review button
            OutlinedButton(
                onClick = {
                    onNavigateToReview(gameId, gameLog?.review)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (gameLog?.review?.isNotEmpty() == true) {
                        "Edit Review"
                    } else {
                        "Add Review"
                    }
                )
            }
            
            // Location Button
            OutlinedButton(
                onClick = { showLocationDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (selectedLatitude != null && selectedLongitude != null) {
                        "Location Selected"
                    } else {
                        "Add Location"
                    }
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Save button
            Button(
                onClick = {
                    coroutineScope.launch {
                        selectedStatus?.let {
                            viewModel.saveGameLog(
                                it,
                                gameLog?.playTime ?: 0,
                                selectedRating,
                                gameLog?.review,
                                selectedLatitude,
                                selectedLongitude
                            )
                        }
                        onBackClick()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedStatus != null
            ) {
                Text("Save")
            }
        }

        // Rating Dialog
        if (showRatingDialog) {
            RatingDialog(
                currentRating = selectedRating,
                onRatingSelected = { rating ->
                    selectedRating = rating
                    showRatingDialog = false
                },
                onDismiss = { showRatingDialog = false }
            )
        }
    }

    if (showLocationDialog) {
        LocationSelectionDialog(
            initialLocation = if (selectedLatitude != null && selectedLongitude != null) {
                LatLng(selectedLatitude!!, selectedLongitude!!)
            } else null,
            onLocationSelected = { lat, lng ->
                selectedLatitude = lat
                selectedLongitude = lng
                showLocationDialog = false
            },
            onDismiss = { showLocationDialog = false }
        )
    }
}

@Composable
private fun StatusButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isSelected) {
        Button(
            onClick = onClick,
            modifier = modifier
        ) {
            Text(text)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier
        ) {
            Text(text)
        }
    }
}
