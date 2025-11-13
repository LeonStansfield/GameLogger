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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogGameScreen(
    gameId: String,
    onBackClick: () -> Unit,
    viewModel: LogGameViewModel = viewModel(
        factory = LogGameViewModelFactory(
            GameLoggerDatabase.getDatabase(LocalContext.current).gameLogDao(),
            gameId
        )
    )
) {
    var selectedStatus by remember { mutableStateOf<GameStatus?>(null) }
    val coroutineScope = rememberCoroutineScope()

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

            // Rating button (placeholder)
            OutlinedButton(
                onClick = { /* TODO: Open rating dialog */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add Rating")
            }

            // Review button (placeholder)
            OutlinedButton(
                onClick = { /* TODO: Open review screen */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add Review")
            }

            Spacer(modifier = Modifier.weight(1f))

            // Save button
            Button(
                onClick = {
                    coroutineScope.launch {
                        selectedStatus?.let {
                            viewModel.saveGameLog(it, 0, null) // Play time and rating are not implemented yet
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
