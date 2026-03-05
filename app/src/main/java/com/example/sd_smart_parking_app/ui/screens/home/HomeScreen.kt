package com.example.sd_smart_parking_app.ui.screens.home

import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.sd_smart_parking_app.data.model.Floor
import com.example.sd_smart_parking_app.data.model.ParkingLot
import com.example.sd_smart_parking_app.ui.components.ParkingCard
import com.example.sd_smart_parking_app.ui.theme.BackgroundWhite
import com.example.sd_smart_parking_app.ui.theme.SmartParkingTheme
import com.example.sd_smart_parking_app.ui.theme.Spacing
import com.example.sd_smart_parking_app.ui.theme.Typography

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier
) {
    // Datos de ejemplo
    val parkingLot = ParkingLot(
        id = "sd-building-01",
        name = "SD Building Parking",
        location = "Universidad de los Andes",
        totalSpots = 120,
        availableSpots = 68,
        occupancyPercentage = 43,
        waitTimeMinutes = 0,
        floors = listOf(
            Floor(
                floorNumber = 1,
                totalSpots = 30,
                availableSpots = 13,
                occupiedSpots = 17,
                availabilityPercentage = 57,
                status = "Medium"
            ),
            Floor(
                floorNumber = 2,
                totalSpots = 30,
                availableSpots = 21,
                occupiedSpots = 9,
                availabilityPercentage = 30,
                status = "Medium"
            ),
            Floor(
                floorNumber = 3,
                totalSpots = 30,
                availableSpots = 19,
                occupiedSpots = 11,
                availabilityPercentage = 37,
                status = "Medium"
            )
        )
    )

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
            // Tarjeta principal de información del parqueadero
            ParkingCard(
                title = parkingLot.name,
                subtitle = parkingLot.location,
                availableSpots = parkingLot.availableSpots,
                totalSpots = parkingLot.totalSpots,
                availabilityPercentage = 57, // 68 de 120
                modifier = Modifier.padding(bottom = Spacing.lg)
            )

            // Timestamp de última actualización
            Text(
                text = "Last update: 3:01:56 PM",
                style = Typography.bodySmall,
                modifier = Modifier.padding(top = Spacing.md)
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HomeScreenPreview() {
    SmartParkingTheme {
        HomeScreen()
    }
}