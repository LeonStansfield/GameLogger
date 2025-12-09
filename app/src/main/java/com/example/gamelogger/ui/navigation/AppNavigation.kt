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
import com.example.gamelogger.ui.features.loggame.LogGameScreen
import com.example.gamelogger.ui.features.review.ReviewScreen
import com.example.gamelogger.ui.features.settings.SettingsScreen
import com.example.gamelogger.ui.features.settings.SettingsViewModel

// Define navigation routes
object AppDestinations {
    const val DISCOVER = "discover"
    const val SEARCH = "search"
    const val DIARY = "diary"
    const val BACKLOG = "backlog"

    const val GAME_DETAILS = "gameDetails"
    const val GAME_ID_ARG = "gameID"
    val gameDetailsRoute = "$GAME_DETAILS/{$GAME_ID_ARG}"

    const val LOG_GAME = "logGame"
    val logGameRoute = "$LOG_GAME/{$GAME_ID_ARG}"

    const val REVIEW = "review"
    val reviewRoute = "$REVIEW/{$GAME_ID_ARG}"

    const val SETTINGS = "settings"
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    settingsViewModel: SettingsViewModel
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
            DiaryScreen(
                onPosterClick = { gameId ->
                    navController.navigate("${AppDestinations.GAME_DETAILS}/$gameId")
                }
            )
        }
        composable(AppDestinations.BACKLOG) {
            BacklogScreen()
        }
        composable(AppDestinations.SETTINGS) {
            SettingsScreen(
                viewModel = settingsViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(
            route = AppDestinations.gameDetailsRoute, // Use the path with argument
            arguments = listOf(navArgument(AppDestinations.GAME_ID_ARG) { // Define argument
                type = NavType.IntType // Or IntType if your ID is an Int
            })
        ) { backStackEntry ->
            // Extract the argument
            val gameId = backStackEntry.arguments?.getInt(AppDestinations.GAME_ID_ARG)

            if (gameId != null) {
                GameDetailsScreen(
                    gameId = gameId,
                    onBackClick = { navController.popBackStack() }, // Pass a back action
                    onLogGameClick = {
                        navController.navigate("${AppDestinations.LOG_GAME}/$gameId")
                    }
                )
            }
        }
        // Log game route - after navigating from game details
        composable(
            route = AppDestinations.logGameRoute,
            arguments = listOf(navArgument(AppDestinations.GAME_ID_ARG) {
                type = NavType.StringType
            })
        ) { backStackEntry ->
            val gameId = backStackEntry.arguments?.getString(AppDestinations.GAME_ID_ARG)

            if (gameId != null) {
                LogGameScreen(
                    gameId = gameId,
                    onBackClick = { navController.popBackStack() },
                    onNavigateToReview = { id, _ ->
                        navController.navigate("${AppDestinations.REVIEW}/$id")
                    }
                )
            }
        }

        // Review screen route
        composable(
            route = AppDestinations.reviewRoute,
            arguments = listOf(
                navArgument(AppDestinations.GAME_ID_ARG) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val gameId = backStackEntry.arguments?.getString(AppDestinations.GAME_ID_ARG)

            if (gameId != null) {
                ReviewScreen(
                    gameId = gameId,
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
    }
}