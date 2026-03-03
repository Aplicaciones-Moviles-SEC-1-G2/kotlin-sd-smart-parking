package com.example.sd_smart_parking_app.ui.screens.history

import androidx.compose.ui.tooling.preview.Preview
import com.example.sd_smart_parking_app.ui.theme.SmartParkingTheme
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
import com.example.sd_smart_parking_app.ui.components.HistoryItem
import com.example.sd_smart_parking_app.ui.components.ParkingStatus
import com.example.sd_smart_parking_app.ui.theme.BackgroundWhite
import com.example.sd_smart_parking_app.ui.theme.Spacing
import com.example.sd_smart_parking_app.ui.theme.Typography

@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier
) {
    // Datos de ejemplo del historial
    val parkingHistory = listOf(
        Triple(
            "Mar 3, 2026",
            "Floor 4 - Space 1-0",
            ParkingStatus.COMPLETED
        ) to Triple("12:56", "2 min", "89 min"),
        Triple(
            "Mar 2, 2026",
            "No space available",
            ParkingStatus.CANCELLED
        ) to Triple("12:32", "20 min", null),
        Triple(
            "Mar 1, 2026",
            "No space available",
            ParkingStatus.CANCELLED
        ) to Triple("10:30", "44 min", null),
        Triple(
            "Feb 28, 2026",
            "Floor 2 - Space 1-3",
            ParkingStatus.COMPLETED
        ) to Triple("11:45", "5 min", "120 min")
    )

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
            Column(
                modifier = Modifier.padding(Spacing.lg)
            ) {
                Text(
                    text = "Parking History",
                    style = Typography.headlineLarge
                )
                Text(
                    text = "Your recent parking activity",
                    style = Typography.bodySmall,
                    modifier = Modifier.padding(top = Spacing.xs)
                )
            }

            // Lista de historial
            Column(
                modifier = Modifier.padding(horizontal = Spacing.lg),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(Spacing.md)
            ) {
                parkingHistory.forEach { (history, times) ->
                    HistoryItem(
                        date = history.first,
                        location = history.second,
                        status = history.third,
                        entryTime = times.first,
                        waitTime = times.second,
                        duration = times.third
                    )
                }
            }

            // Espaciado inferior
            androidx.compose.foundation.layout.Box(modifier = Modifier.padding(bottom = Spacing.lg))
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