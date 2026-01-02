package com.example.gamelogger.ui.features.gameDetails

import androidx.lifecycle.ViewModel
import com.example.gamelogger.data.model.Game
import com.example.gamelogger.data.remote.IgdbService
class GameDetailsViewModel(
    private val igdbService: IgdbService = IgdbService()
) : ViewModel() {

    suspend fun fetchGameDetails(gameId: Int): Game? {
        return igdbService.getGameDetails(gameId)
    }
}