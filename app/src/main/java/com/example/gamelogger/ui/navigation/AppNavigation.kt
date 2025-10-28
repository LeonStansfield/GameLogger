package com.example.gamelogger.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.gamelogger.ui.features.discover.DiscoverScreen
import com.example.gamelogger.ui.features.search.SearchScreen

// Define navigation routes
object AppDestinations {
    const val DISCOVER = "discover"
    const val SEARCH = "search"
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = AppDestinations.DISCOVER, // Start on Discover screen
        modifier = modifier
    ) {
        composable(AppDestinations.DISCOVER) {
            DiscoverScreen()
        }
        composable(AppDestinations.SEARCH) {
            SearchScreen()
        }
        // Future screens go here
        // composable(AppDestinations.DIARY) { DiaryScreen() }
    }
}