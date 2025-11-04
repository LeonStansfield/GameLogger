package com.example.gamelogger.ui.features.backlog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gamelogger.ui.features.search.SearchViewModel

@Composable
fun BacklogScreen() {
    val viewModel: SearchViewModel = viewModel()

    Text("BacklogScreen")
}