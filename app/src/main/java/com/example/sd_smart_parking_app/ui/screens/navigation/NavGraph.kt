package com.example.sd_smart_parking_app.ui.screens.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.sd_smart_parking_app.ui.screens.details.DetailsScreen
import com.example.sd_smart_parking_app.ui.screens.history.HistoryScreen
import com.example.sd_smart_parking_app.ui.screens.login.LoginScreen
import com.example.sd_smart_parking_app.ui.screens.register.RegisterScreen
import com.example.sd_smart_parking_app.ui.screens.home.HomeScreen
import com.example.sd_smart_parking_app.ui.screens.profile.ProfileScreen

// Rutas de navegación
object NavRoutes {
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
        startDestination = NavRoutes.LOGIN
    ) {
        composable(NavRoutes.LOGIN) {
            LoginScreen(
                onLoginClick = {
                    navController.navigate(NavRoutes.HOME) {
                        // Limpiamos el stack de login al entrar a la app
                        popUpTo(NavRoutes.LOGIN) { inclusive = true }
                    }
                },
                onRegisterClick = {
                    navController.navigate(NavRoutes.REGISTER)
                }
            )
        }
        composable(NavRoutes.REGISTER) {
            RegisterScreen(
                onRegisterClick = {
                    navController.navigate(NavRoutes.HOME) {
                        popUpTo(NavRoutes.LOGIN) { inclusive = true }
                    }
                },
                onLoginClick = {
                    navController.navigate(NavRoutes.LOGIN) {
                        popUpTo(NavRoutes.LOGIN) { inclusive = true }
                    }
                }
            )
        }
        composable(NavRoutes.HOME) {
            HomeScreen(
                onNavigationClick = {
                    // Navegamos a la pestaña de navegación usando la misma lógica que el bottom bar
                    // Esto evita que 'navigate' se apile sobre 'home' de forma incorrecta
                    navController.navigate(NavRoutes.NAVIGATE) {
                        popUpTo(navController.graph.findStartDestination().id) {
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
