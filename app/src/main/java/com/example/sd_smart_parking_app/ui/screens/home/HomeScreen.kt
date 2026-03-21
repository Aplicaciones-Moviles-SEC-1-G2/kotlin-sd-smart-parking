package com.example.sd_smart_parking_app.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.sd_smart_parking_app.data.model.ParkingConfig
import com.example.sd_smart_parking_app.data.model.ParkingSpot
import com.example.sd_smart_parking_app.data.repository.ParkingRepository
import com.example.sd_smart_parking_app.ui.components.ParkingCard
import com.example.sd_smart_parking_app.ui.theme.BackgroundWhite
import com.example.sd_smart_parking_app.ui.theme.SmartParkingTheme
import com.example.sd_smart_parking_app.ui.theme.Spacing
import com.example.sd_smart_parking_app.ui.theme.Typography
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    onNavigationClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val repository = remember { ParkingRepository() }
    var parkingConfig by remember { mutableStateOf(ParkingConfig()) }
    var parkingSpots by remember { mutableStateOf<List<ParkingSpot>>(emptyList()) }
    var lastUpdate by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        repository.getParkingConfig { config ->
            parkingConfig = config
        }
        repository.getParkingSpots { spots ->
            parkingSpots = spots
            lastUpdate = SimpleDateFormat("h:mm:ss a", Locale.getDefault()).format(Date())
        }
    }

    val totalSpots = parkingConfig.numberOfFloors * parkingConfig.spotsPerFloor
    val availableSpots = parkingSpots.count { it.isAvailable }
    val availabilityPercentage = if (totalSpots > 0) (availableSpots * 100) / totalSpots else 0

    Scaffold(
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundWhite)
                .padding(paddingValues)
                .padding(Spacing.lg)
                .verticalScroll(rememberScrollState())
        ) {
            // Tarjeta principal de información del parqueadero con datos reales
            ParkingCard(
                title = parkingConfig.parkingName.ifEmpty { "Loading..." },
                subtitle = "Universidad de los Andes",
                availableSpots = availableSpots,
                totalSpots = totalSpots,
                availabilityPercentage = availabilityPercentage,
                modifier = Modifier.padding(bottom = Spacing.lg),
                onNavigateClick = onNavigationClick
            )

            // Timestamp de última actualización real
            if (lastUpdate.isNotEmpty()) {
                Text(
                    text = "Last update: $lastUpdate",
                    style = Typography.bodySmall,
                    modifier = Modifier.padding(top = Spacing.md)
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HomeScreenPreview() {
    SmartParkingTheme {
        HomeScreen( onNavigationClick = {} )
    }
}
