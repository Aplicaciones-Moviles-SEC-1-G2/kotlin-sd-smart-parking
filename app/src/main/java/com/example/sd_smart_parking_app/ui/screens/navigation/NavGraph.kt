package com.example.sd_smart_parking_app.ui.screens.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.example.sd_smart_parking_app.ui.screens.details.DetailsScreen
import com.example.sd_smart_parking_app.ui.screens.history.HistoryScreen
import com.example.sd_smart_parking_app.ui.screens.login.LoginScreen
import com.example.sd_smart_parking_app.ui.screens.register.RegisterScreen
import com.example.sd_smart_parking_app.ui.screens.home.HomeScreen
import com.example.sd_smart_parking_app.ui.screens.profile.ProfileScreen

// Rutas de navegación
object NavRoutes {
    const val AUTH_GRAPH = "auth_graph"
    const val LOGIN = "login"
    const val REGISTER = "register"
    
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
        startDestination = NavRoutes.AUTH_GRAPH
    ) {
        // Auth Graph
        navigation(
            startDestination = NavRoutes.LOGIN,
            route = NavRoutes.AUTH_GRAPH
        ) {
            composable(NavRoutes.LOGIN) {
                LoginScreen(
                    onLoginClick = { email, password ->
                        navController.navigate(NavRoutes.HOME) {
                            popUpTo(NavRoutes.AUTH_GRAPH) { inclusive = true }
                        }
                    },
                    onRegisterClick = {
                        navController.navigate(NavRoutes.REGISTER)
                    }
                )
            }
            composable(NavRoutes.REGISTER) {
                RegisterScreen(
                    onRegisterClick = { name, email, phone, vehicleModel, vehiclePlate, role, password ->
                        // Registration success: navigate back to login
                        navController.navigate(NavRoutes.LOGIN) {
                            popUpTo(NavRoutes.REGISTER) { inclusive = true }
                        }
                    },
                    onLoginClick = {
                        navController.navigate(NavRoutes.LOGIN) {
                            popUpTo(NavRoutes.LOGIN) { inclusive = true }
                        }
                    }
                )
            }
        }

        // Main App flow
        composable(NavRoutes.HOME) {
            HomeScreen(
                onNavigationClick = {
                    navController.navigate(NavRoutes.NAVIGATE) {
                        popUpTo(NavRoutes.HOME) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
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
