package com.example.gamelogger.ui.features.diary

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gamelogger.ui.features.search.SearchViewModel

@Composable
fun DiaryScreen() {
    val viewModel: SearchViewModel = viewModel()

    Text("DiaryScreen")
}