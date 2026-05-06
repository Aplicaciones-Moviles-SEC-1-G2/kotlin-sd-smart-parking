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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.example.sd_smart_parking_app.data.NetworkMonitor
import com.example.sd_smart_parking_app.data.model.Floor
import com.example.sd_smart_parking_app.ui.components.FloorRecommendationCard
import com.example.sd_smart_parking_app.ui.components.OccupancyPredictorCard
import com.example.sd_smart_parking_app.ui.components.OfflineBanner
import com.example.sd_smart_parking_app.ui.components.ParkingCard
import com.example.sd_smart_parking_app.ui.components.WeatherCard
import com.example.sd_smart_parking_app.ui.theme.BackgroundWhite
import com.example.sd_smart_parking_app.ui.theme.SmartParkingTheme
import com.example.sd_smart_parking_app.ui.theme.Spacing
import com.example.sd_smart_parking_app.ui.theme.Typography
import com.example.sd_smart_parking_app.viewmodel.FloorRecommendationViewModel
import com.example.sd_smart_parking_app.viewmodel.OccupancyPredictorViewModel
import com.example.sd_smart_parking_app.viewmodel.SharedDetailsViewModel
import com.example.sd_smart_parking_app.viewmodel.WeatherViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    onNavigationClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val networkMonitor = remember { NetworkMonitor(context) }
    val isConnected by networkMonitor.isConnected.collectAsState()

    val sharedDetailsViewModel = remember { SharedDetailsViewModel.getInstance() }
    val detailsState by sharedDetailsViewModel.detailsState.collectAsState()
    LaunchedEffect(Unit) {
        sharedDetailsViewModel.initializeIfNeeded()
    }
    val floorRecommendationViewModel = remember { FloorRecommendationViewModel() }
    val recommendationState by floorRecommendationViewModel.recommendationState.collectAsState()
    val weatherViewModel = remember { WeatherViewModel() }
    val weatherState by weatherViewModel.weatherState.collectAsState()
    val occupancyPredictorViewModel = remember { OccupancyPredictorViewModel() }
    val occupancyState by occupancyPredictorViewModel.uiState.collectAsState()
    var lastUpdate by remember { mutableStateOf("") }

    DisposableEffect(Unit) {
        onDispose { networkMonitor.unregister() }
    }

    LaunchedEffect(detailsState.parkingSpots) {
        if (detailsState.parkingSpots.isNotEmpty() && detailsState.parkingConfig.numberOfFloors > 0) {
            val floors = (1..detailsState.parkingConfig.numberOfFloors).map { floorNum ->
                val spotsInFloor = detailsState.parkingSpots.filter { it.floor == floorNum }
                val floorTotal = spotsInFloor.size
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

            val totalSpotsForOccupancy = detailsState.parkingSpots.size
            val availableSpotsForOccupancy = detailsState.parkingSpots.count { it.isAvailable }
            val occupancyPercentage = if (totalSpotsForOccupancy > 0)
                ((totalSpotsForOccupancy - availableSpotsForOccupancy) * 100f) / totalSpotsForOccupancy
            else 0f
            occupancyPredictorViewModel.saveCurrentOccupancyData(
                occupancyPercentage = occupancyPercentage,
                availableSpots = availableSpotsForOccupancy,
                totalSpots = totalSpotsForOccupancy
            )

            lastUpdate = SimpleDateFormat("h:mm:ss a", Locale.getDefault()).format(Date())
        }
    }

    val totalSpots = detailsState.parkingSpots.size
    val availableSpots = detailsState.parkingSpots.count { it.isAvailable }
    val occupiedSpots = totalSpots - availableSpots
    val availabilityPercentage = if (totalSpots > 0) (availableSpots * 100) / totalSpots else 0
    val occupancyPercentageDisplay = if (totalSpots > 0) (occupiedSpots * 100) / totalSpots else 0

    Scaffold(modifier = modifier.fillMaxSize()) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundWhite)
                .padding(paddingValues)
                .padding(Spacing.lg)
                .verticalScroll(rememberScrollState())
        ) {
            // Banner offline — solo visible cuando no hay conexión
            if (!isConnected) {
                OfflineBanner(
                    message = "You lost your connection. Real-time parking updates are paused.",
                    subMessage = "Showing last known availability — AI predictions still available from cache 🤖",
                    modifier = Modifier.padding(bottom = Spacing.md)
                )
            }

            ParkingCard(
                title = detailsState.parkingConfig.parkingName.ifEmpty { "Loading..." },
                subtitle = "Andes University",
                availableSpots = availableSpots,
                totalSpots = totalSpots,
                availabilityPercentage = availabilityPercentage,
                occupancyPercentage = occupancyPercentageDisplay,
                modifier = Modifier.padding(bottom = Spacing.lg),
                onNavigateClick = onNavigationClick
            )

            OccupancyPredictorCard(
                prediction = occupancyState.currentPrediction,
                isLoading = occupancyState.isLoading,
                modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md)
            )

            FloorRecommendationCard(
                recommendation = recommendationState.recommendation,
                isLoading = recommendationState.isLoading,
                error = recommendationState.error,
                modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md)
            )

            WeatherCard(
                weatherData = weatherState.weatherData,
                recommendation = weatherState.recommendation,
                isLoading = weatherState.isLoading,
                error = weatherState.error,
                hasRainRisk = weatherState.hasRainRisk,
                onRefresh = { weatherViewModel.refreshWeather() },
                modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md)
            )

            if (lastUpdate.isNotEmpty()) {
                Text(
                    text = if (isConnected) "Last update: $lastUpdate" else "⚠️ Last update: $lastUpdate (offline)",
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
        HomeScreen(onNavigationClick = {})
    }
}