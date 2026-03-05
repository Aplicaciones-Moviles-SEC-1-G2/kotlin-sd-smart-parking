package com.example.sd_smart_parking_app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.sd_smart_parking_app.ui.screens.details.DetailsScreen
import com.example.sd_smart_parking_app.ui.screens.history.HistoryScreen
import com.example.sd_smart_parking_app.ui.screens.home.HomeScreen
import com.example.sd_smart_parking_app.ui.screens.navigation.NavigationScreen
import com.example.sd_smart_parking_app.ui.screens.profile.ProfileScreen

// Rutas de navegación
object NavRoutes {
    const val HOME = "home"
    const val DETAILS = "details"
    const val NAVIGATE = "navigate"
    const val HISTORY = "history"
    const val PROFILE = "profile"
}

@Composable
fun SmartParkingNavGraph(
    navController: NavHostController
) {
    NavHost(
        navController = navController,
        startDestination = NavRoutes.HOME
    ) {
        composable(NavRoutes.HOME) {
            HomeScreen()
        }
        composable(NavRoutes.DETAILS) {
            DetailsScreen()
        }
        composable(NavRoutes.NAVIGATE) {
            NavigationScreen()
        }
        composable(NavRoutes.HISTORY) {
            HistoryScreen()
        }
        composable(NavRoutes.PROFILE) {
            ProfileScreen()
        }
    }
}