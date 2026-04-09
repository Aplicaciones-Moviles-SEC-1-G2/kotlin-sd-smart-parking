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
import com.example.sd_smart_parking_app.viewmodel.WeatherViewModel
import com.example.sd_smart_parking_app.ui.components.WeatherCard
import androidx.compose.runtime.collectAsState
import com.example.sd_smart_parking_app.viewmodel.FloorRecommendationViewModel
import com.example.sd_smart_parking_app.ui.components.FloorRecommendationCard
import com.example.sd_smart_parking_app.data.model.Floor
import kotlinx.coroutines.delay
import com.example.sd_smart_parking_app.viewmodel.SharedDetailsViewModel

@Composable
fun HomeScreen(
    onNavigationClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sharedDetailsViewModel = remember { SharedDetailsViewModel.getInstance() }
    val detailsState by sharedDetailsViewModel.detailsState.collectAsState()
    LaunchedEffect(Unit) {
        sharedDetailsViewModel.initializeIfNeeded()
    }
    val floorRecommendationViewModel = remember { FloorRecommendationViewModel() }
    val recommendationState by floorRecommendationViewModel.recommendationState.collectAsState()
    val weatherViewModel = remember { WeatherViewModel() }
    val weatherState by weatherViewModel.weatherState.collectAsState()
    var lastUpdate by remember { mutableStateOf("") }

    // Actualizar recomendación cada vez que cambien los parkingSpots
    LaunchedEffect(detailsState.parkingSpots) {
        if (detailsState.parkingSpots.isNotEmpty() && detailsState.parkingConfig.numberOfFloors > 0) {
            val floors = (1..detailsState.parkingConfig.numberOfFloors).map { floorNum ->
                val spotsInFloor = detailsState.parkingSpots.filter { it.floor == floorNum }
                val floorTotal = detailsState.parkingConfig.spotsPerFloor
                val floorAvailable = spotsInFloor.count { it.isAvailable }
                val floorPercentage = if (floorTotal > 0) (floorAvailable * 100) / floorTotal else 0

                Floor(
                    floorNumber = floorNum,
                    totalSpots = floorTotal,
                    availableSpots = floorAvailable,
                    occupiedSpots = floorTotal - floorAvailable,
                    availabilityPercentage = floorPercentage,
                    status = when {
                        floorPercentage > 60 -> "High"
                        floorPercentage > 30 -> "Medium"
                        else -> "Low"
                    }
                )
            }
            floorRecommendationViewModel.updateRecommendation(floors)
            lastUpdate = SimpleDateFormat("h:mm:ss a", Locale.getDefault()).format(Date())
        }
    }

    val totalSpots = detailsState.parkingConfig.numberOfFloors * detailsState.parkingConfig.spotsPerFloor
    val availableSpots = detailsState.parkingSpots.count { it.isAvailable }
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
                title = detailsState.parkingConfig.parkingName.ifEmpty { "Loading..." },
                subtitle = "Universidad de los Andes",
                availableSpots = availableSpots,
                totalSpots = totalSpots,
                availabilityPercentage = availabilityPercentage,
                modifier = Modifier.padding(bottom = Spacing.lg),
                onNavigateClick = onNavigationClick
            )

            // Floor Recommendation Card
            FloorRecommendationCard(
                recommendation = recommendationState.recommendation,
                isLoading = recommendationState.isLoading,
                error = recommendationState.error,
                modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md)
            )

            // Weather Card
            WeatherCard(
                weatherData = weatherState.weatherData,
                recommendation = weatherState.recommendation,
                isLoading = weatherState.isLoading,
                error = weatherState.error,
                hasRainRisk = weatherState.hasRainRisk,
                onRefresh = { weatherViewModel.refreshWeather() },
                modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md)
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