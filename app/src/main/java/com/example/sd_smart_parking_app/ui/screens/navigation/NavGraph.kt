package com.example.sd_smart_parking_app.ui.screens.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.example.sd_smart_parking_app.data.model.UserProfile
import com.example.sd_smart_parking_app.data.model.UserCar
import com.example.sd_smart_parking_app.data.repository.ParkingRepository
import com.example.sd_smart_parking_app.ui.screens.details.DetailsScreen
import com.example.sd_smart_parking_app.ui.screens.history.HistoryScreen
import com.example.sd_smart_parking_app.ui.screens.login.LoginScreen
import com.example.sd_smart_parking_app.ui.screens.register.RegisterScreen
import com.example.sd_smart_parking_app.ui.screens.home.HomeScreen
import com.example.sd_smart_parking_app.ui.screens.profile.ProfileScreen
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth

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
    val repository = ParkingRepository()

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
                    onLoginSuccess = { _, _ ->
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
                    onRegisterSuccess = { name, email, phone, vehicleModel, vehiclePlate, role, _ ->
                        val uid = FirebaseAuth.getInstance().currentUser?.uid
                        if (uid != null) {
                            val profile = UserProfile(
                                name = name,
                                email = email,
                                role = role.lowercase(),
                                phone = phone,
                                cars = listOf(UserCar(name = vehicleModel, plate = vehiclePlate)),
                                createdAt = Timestamp.now()
                            )
                            repository.saveUserProfile(uid, profile) { success ->
                                if (success) {
                                    navController.navigate(NavRoutes.LOGIN) {
                                        popUpTo(NavRoutes.REGISTER) { inclusive = true }
                                    }
                                }
                            }
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
