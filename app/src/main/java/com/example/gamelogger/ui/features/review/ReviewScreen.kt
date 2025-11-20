package com.example.gamelogger.ui.features.review

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gamelogger.data.db.GameLoggerDatabase
import com.example.gamelogger.ui.features.loggame.LogGameViewModel
import com.example.gamelogger.ui.features.loggame.LogGameViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    gameId: String,
    onBackClick: () -> Unit,
    viewModel: LogGameViewModel = viewModel(
        factory = LogGameViewModelFactory(
            GameLoggerDatabase.getDatabase(LocalContext.current).gameLogDao(),
            gameId
        )
    )
) {
    val gameLog by viewModel.gameLog.collectAsState()
    var reviewText by remember { mutableStateOf(gameLog?.review ?: "") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Write Review") },
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
        ) {
            OutlinedTextField(
                value = reviewText,
                onValueChange = { reviewText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                label = { Text("Write your review") },
                placeholder = { Text("Share your thoughts about the game...") },
                maxLines = 15
            )

            Button(
                onClick = {
                    viewModel.updateReview(reviewText)
                    onBackClick()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Text("Save Review")
            }
        }
    }
}

