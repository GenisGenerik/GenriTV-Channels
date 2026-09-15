package com.example.genritv.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.genritv.model.TvChannel
import com.example.genritv.model.VodMovie
import com.example.genritv.model.Series
import com.example.genritv.ui.HomeScreen

@Composable
fun TVNavigation(
    navController: NavHostController,
    onNavigateToLiveTv: (TvChannel) -> Unit,
    onNavigateToMovies: (VodMovie) -> Unit,
    onNavigateToSeries: (Series) -> Unit,
    playerScreen: @Composable () -> Unit
) {
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onNavigateToLiveTv = onNavigateToLiveTv,
                onNavigateToMovies = onNavigateToMovies,
                onNavigateToSeries = onNavigateToSeries,
                onNavigateToSettings = { /* ... */ }
            )
        }
        composable("player") {
            playerScreen()
        }
    }
}
