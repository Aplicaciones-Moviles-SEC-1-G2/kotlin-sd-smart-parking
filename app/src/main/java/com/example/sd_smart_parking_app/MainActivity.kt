package com.example.sd_smart_parking_app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

// Icons
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person

import androidx.navigation.compose.rememberNavController
import com.example.sd_smart_parking_app.ui.components.BottomNavItem
import com.example.sd_smart_parking_app.ui.components.SmartParkingBottomNavigationBar
import com.example.sd_smart_parking_app.ui.navigation.NavRoutes
import com.example.sd_smart_parking_app.ui.navigation.SmartParkingNavGraph
import com.example.sd_smart_parking_app.ui.theme.SmartParkingTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SmartParkingTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainApp()
                }
            }
        }
    }
}

@Composable
fun MainApp() {
    val navController = rememberNavController()
    var currentRoute by remember { mutableStateOf(NavRoutes.HOME) }

    // Definir los items del bottom navigation
    val bottomNavItems = listOf(
        BottomNavItem(
            label = "Home",
            icon = Icons.Default.Home,
            route = NavRoutes.HOME
        ),
        BottomNavItem(
            label = "Details",
            icon = Icons.Default.Info,
            route = NavRoutes.DETAILS
        ),
        BottomNavItem(
            label = "Navigate",
            icon = Icons.Default.Navigation,
            route = NavRoutes.NAVIGATE
        ),
        BottomNavItem(
            label = "History",
            icon = Icons.Default.History,
            route = NavRoutes.HISTORY
        ),
        BottomNavItem(
            label = "Profile",
            icon = Icons.Default.Person,
            route = NavRoutes.PROFILE
        )
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            SmartParkingBottomNavigationBar(
                items = bottomNavItems,
                currentRoute = currentRoute,
                onItemSelected = { route ->
                    currentRoute = route
                    navController.navigate(route) {
                        // Pop hasta el start destination para evitar stack de navegación infinito
                        popUpTo(NavRoutes.HOME) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            SmartParkingNavGraph(navController = navController)
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun MainAppPreview() {
    SmartParkingTheme {
        MainApp()
    }
}