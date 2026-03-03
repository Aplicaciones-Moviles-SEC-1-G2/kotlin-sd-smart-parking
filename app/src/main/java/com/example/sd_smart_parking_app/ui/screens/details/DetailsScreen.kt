package com.example.sd_smart_parking_app.ui.screens.details

import androidx.compose.ui.tooling.preview.Preview
import com.example.sd_smart_parking_app.ui.theme.SmartParkingTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sd_smart_parking_app.data.model.Floor
import com.example.sd_smart_parking_app.ui.components.FloorCard
import com.example.sd_smart_parking_app.ui.theme.BackgroundLightGray
import com.example.sd_smart_parking_app.ui.theme.BackgroundWhite
import com.example.sd_smart_parking_app.ui.theme.CornerRadius
import com.example.sd_smart_parking_app.ui.theme.DarkText
import com.example.sd_smart_parking_app.ui.theme.HighAvailabilityGreen
import com.example.sd_smart_parking_app.ui.theme.Spacing
import com.example.sd_smart_parking_app.ui.theme.Typography
import com.example.sd_smart_parking_app.ui.theme.WarningYellow

@Composable
fun DetailsScreen(
    modifier: Modifier = Modifier
) {
    // Datos de ejemplo de pisos
    val floors = listOf(
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.lg),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Floor Details",
                        style = Typography.headlineLarge
                    )
                    Text(
                        text = "Real-time availability",
                        style = Typography.bodySmall,
                        modifier = Modifier.padding(top = Spacing.xs)
                    )
                }
                // Botón de refrescar
                IconButton(onClick = { }) {
                    Text("🔄", fontSize = 20.sp)
                }
            }

            // Card de estado del sistema
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg)
                    .background(
                        color = HighAvailabilityGreen.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(CornerRadius.lg)
                    )
                    .padding(Spacing.md)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🟢", fontSize = 20.sp, modifier = Modifier.padding(end = Spacing.sm))
                    Column {
                        Text(
                            text = "System Operational",
                            style = Typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "Auto-update active",
                            style = Typography.bodySmall
                        )
                    }
                }
            }

            // Card de resumen (Total, Available, Occupied)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg, vertical = Spacing.lg)
                    .background(
                        color = WarningYellow,
                        shape = RoundedCornerShape(CornerRadius.lg)
                    )
                    .padding(Spacing.lg)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "120",
                            style = Typography.displaySmall
                        )
                        Text(
                            text = "Total",
                            style = Typography.bodySmall
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "69",
                            style = Typography.displaySmall
                        )
                        Text(
                            text = "Available",
                            style = Typography.bodySmall
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "51",
                            style = Typography.displaySmall
                        )
                        Text(
                            text = "Occupied",
                            style = Typography.bodySmall
                        )
                    }
                }
            }

            // Título de distribución por piso
            Text(
                text = "📊 Floor Distribution",
                style = Typography.headlineSmall,
                modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md)
            )

            // Lista de pisos
            Column(
                modifier = Modifier.padding(horizontal = Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                floors.forEach { floor ->
                    FloorCard(
                        floorNumber = floor.floorNumber,
                        availableSpots = floor.availableSpots,
                        totalSpots = floor.totalSpots,
                        availabilityPercentage = floor.availabilityPercentage,
                        availabilityStatus = floor.status
                    )
                }
            }

            // Espaciado inferior
            Box(modifier = Modifier.padding(bottom = Spacing.lg))
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DetailsScreenPreview() {
    SmartParkingTheme {
        DetailsScreen()
    }
}