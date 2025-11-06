package com.example.gamelogger.ui.features.gameDetails

import androidx.lifecycle.ViewModel
import com.example.gamelogger.data.model.Game
import com.example.gamelogger.data.remote.IgdbService
class GameDetailsViewModel : ViewModel() {

    // 2. Instantiate the repository
    private val igdbService = IgdbService()

    suspend fun fetchGameDetails(gameId: Int): Game? {
        // 3. Call the repository
        return igdbService.getGameDetails(gameId)
    }
}