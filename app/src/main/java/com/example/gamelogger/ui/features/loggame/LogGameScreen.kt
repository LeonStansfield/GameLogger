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
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.gamelogger.data.db.GameLoggerDatabase
import com.example.gamelogger.data.db.GameStatus
import com.example.gamelogger.ui.navigation.AppDestinations
import com.example.gamelogger.util.formatRelativeTime
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogGameScreen(
    gameId: String,
    navController: NavHostController,
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
    var selectedLocationName by remember { mutableStateOf<String?>(null) }

    var selectedPhotoUri by remember { mutableStateOf<String?>(null) }

    var showRatingDialog by remember { mutableStateOf(false) }
    var showLocationDialog by remember { mutableStateOf(false) }
    var showReviewDialog by remember { mutableStateOf(false) }
    var selectedReview by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()

    // Listen for result from CameraScreen
    // Watches the "photo_uri" key in navigation backstack
    val currentBackStackEntry = navController.currentBackStackEntry
    val savedStateHandle = currentBackStackEntry?.savedStateHandle
    val returnedPhotoUriState = savedStateHandle?.getLiveData<String>("photo_uri")?.observeAsState()

    // Update local state when photo returns
    LaunchedEffect(returnedPhotoUriState?.value) {
        val uri = returnedPhotoUriState?.value

        if (uri != null) {
            selectedPhotoUri = uri
            // Clear result so is not re-processed on rotation
            savedStateHandle?.remove<String>("photo_uri")
        }
    }

    LaunchedEffect(gameLog) {
        if (selectedStatus == null) {
            gameLog?.let {
                selectedStatus = it.status
                selectedRating = it.userRating
                selectedLatitude = it.latitude
                selectedLongitude = it.longitude
                selectedLocationName = it.locationName
                selectedReview = it.review
                selectedPhotoUri = it.photoUri
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Log Game") },
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
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

            // Status buttons
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

            // Review button (Dialog)
            OutlinedButton(
                onClick = { showReviewDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (!selectedReview.isNullOrBlank()) {
                        "Edit Review"
                    } else {
                        "Add Review"
                    }
                )
            }
            if (!selectedReview.isNullOrBlank()) {
                Text(
                    text = selectedReview!!,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // Location Button
            OutlinedButton(
                onClick = { showLocationDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (!selectedLocationName.isNullOrBlank()) {
                        "Location: $selectedLocationName"
                    } else if (selectedLatitude != null && selectedLongitude != null) {
                        "Location Selected"
                    } else {
                        "Add Location"
                    }
                )
            }

            // Camera Button
            OutlinedButton(
                onClick = {
                    // Navigate to Camera Screen
                    navController.navigate(AppDestinations.CAMERA)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (selectedPhotoUri != null) "Retake Photo" else "Take Photo")
            }

            // Display Photo
            if (selectedPhotoUri != null) {
                AsyncImage(
                    model = selectedPhotoUri,
                    contentDescription = "Memory",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Save button
            Button(
                onClick = {
                    coroutineScope.launch {
                        selectedStatus?.let { status ->
                            viewModel.saveGameLog(
                                status = status,
                                playTime = gameLog?.playTime ?: 0,
                                userRating = selectedRating,
                                review = selectedReview,
                                latitude = selectedLatitude,
                                longitude = selectedLongitude,
                                locationName = selectedLocationName,
                                photoUri = selectedPhotoUri
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

        // Location Dialog
        if (showLocationDialog) {
            LocationSelectionDialog(
                initialLocation = if (selectedLatitude != null && selectedLongitude != null) {
                    LatLng(selectedLatitude!!, selectedLongitude!!)
                } else null,
                onLocationSelected = { lat, lng, name ->
                    selectedLatitude = lat
                    selectedLongitude = lng
                    selectedLocationName = name
                    showLocationDialog = false
                },
                onDismiss = { showLocationDialog = false }
            )
        }

        // Review Dialog
        if (showReviewDialog) {
            ReviewDialog(
                initialReview = selectedReview,
                onReviewSubmitted = { review ->
                    selectedReview = review
                    showReviewDialog = false
                },
                onDismiss = { showReviewDialog = false }
            )
        }
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
