package com.example.gamelogger.ui.features.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gamelogger.data.model.Game
import com.example.gamelogger.ui.composables.GameCard
import com.example.gamelogger.ui.navigation.AppDestinations
import androidx.navigation.NavHostController

@Composable
fun SearchScreen(
    navController: NavHostController
) {
    val viewModel: SearchViewModel = viewModel()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Search Bar
            OutlinedTextField(
                value = viewModel.searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                label = { Text("Search for a game...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Content Area
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (viewModel.isLoading) {
                    CircularProgressIndicator()
                } else {
                    SearchGameGrid(
                        games = viewModel.games,
                        onGameClick = { game ->
                            navController.navigate(
                                "${AppDestinations.GAME_DETAILS}/${game.id}"
                            )
                        }
                    )
                }
            }
        }
    }

@Composable
private fun SearchGameGrid(
    games: List<Game>,
    onGameClick: (Game) -> Unit
) {
    if (games.isEmpty()) {
        Text(
            text = "Search for games to see results.",
            textAlign = TextAlign.Center
        )
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(games) { game ->
                GameCard(
                    game = game,
                    modifier = Modifier.height(250.dp),
                    onClick = { onGameClick(game) }
                )
            }
        }
    }
}