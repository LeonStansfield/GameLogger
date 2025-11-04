package com.example.gamelogger.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.gamelogger.ui.features.backlog.BacklogScreen
import com.example.gamelogger.ui.features.diary.DiaryScreen
import com.example.gamelogger.ui.features.discover.DiscoverScreen
import com.example.gamelogger.ui.features.search.SearchScreen
import com.example.gamelogger.ui.features.gameDetails.GameDetailsScreen

// Define navigation routes
object AppDestinations {
    const val DISCOVER = "discover"
    const val SEARCH = "search"
    const val DIARY = "diary"
    const val BACKLOG = "backlog"

    const val GAME_DETAILS = "gameDetails"
    const val GAME_ID_ARG = "gameID"
    val gameDetailsRoute = "$GAME_DETAILS/{$GAME_ID_ARG}"
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
            DiscoverScreen(navController = navController)
        }
        composable(AppDestinations.SEARCH) {
            SearchScreen(navController = navController)
        }
        composable(AppDestinations.DIARY) {
            DiaryScreen()
        }
        composable(AppDestinations.BACKLOG) {
            BacklogScreen()
        }
        composable(
            route = AppDestinations.gameDetailsRoute, // Use the path with argument
            arguments = listOf(navArgument(AppDestinations.GAME_ID_ARG) { // Define argument
                type = NavType.StringType // Or IntType if your ID is an Int
            })
        ) { backStackEntry ->
            // Extract the argument
            val gameId = backStackEntry.arguments?.getString(AppDestinations.GAME_ID_ARG)

            if (gameId != null) {
                GameDetailsScreen(
                    gameId = gameId,
                    onBackClick = { navController.popBackStack() } // Pass a back action
                )
            }
        }
    }
}