package com.example.sd_smart_parking_app.ui.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sd_smart_parking_app.ui.components.HistoryItem
import com.example.sd_smart_parking_app.ui.components.ParkingStatus
import com.example.sd_smart_parking_app.ui.theme.BackgroundWhite
import com.example.sd_smart_parking_app.ui.theme.NavigationBlue
import com.example.sd_smart_parking_app.ui.theme.SmartParkingTheme
import com.example.sd_smart_parking_app.ui.theme.Spacing
import com.example.sd_smart_parking_app.ui.theme.Typography
import com.example.sd_smart_parking_app.viewmodel.HistoryViewModel

@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundWhite)
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Column(modifier = Modifier.padding(Spacing.lg)) {
                Text(text = "Parking History", style = Typography.headlineLarge)
                Text(
                    text = "Your last 10 parking records",
                    style = Typography.bodySmall,
                    modifier = Modifier.padding(top = Spacing.xs)
                )
            }

            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = NavigationBlue)
                    }
                }
                uiState.error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Error loading history: ${uiState.error}",
                            style = Typography.bodyMedium
                        )
                    }
                }
                uiState.records.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No parking records found",
                            style = Typography.bodyMedium
                        )
                    }
                }
                else -> {
                    Column(
                        modifier = Modifier.padding(horizontal = Spacing.lg),
                        verticalArrangement = Arrangement.spacedBy(Spacing.md)
                    ) {
                        uiState.records.forEach { record ->
                            val status = if (record.type == "exit") {
                                ParkingStatus.COMPLETED
                            } else {
                                ParkingStatus.CANCELLED
                            }

                            val location = if (record.floor > 0 && record.spotNumber > 0) {
                                "Floor ${record.floor} - Space ${record.spotNumber}"
                            } else {
                                "Parking lot"
                            }

                            val duration = if (record.type == "exit" && record.durationHours > 0) {
                                viewModel.formatDuration(record.durationHours)
                            } else null

                            HistoryItem(
                                date = viewModel.formatDate(record.timestamp),
                                location = location,
                                status = status,
                                entryTime = viewModel.formatTime(record.timestamp),
                                waitTime = null,
                                duration = duration
                            )
                        }
                    }
                }
            }

            Box(modifier = Modifier.padding(bottom = Spacing.lg))
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HistoryScreenPreview() {
    SmartParkingTheme {
        HistoryScreen()
    }
}