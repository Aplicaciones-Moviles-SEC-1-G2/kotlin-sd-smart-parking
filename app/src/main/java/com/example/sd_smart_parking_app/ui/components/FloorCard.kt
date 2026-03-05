package com.example.sd_smart_parking_app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.sd_smart_parking_app.ui.theme.BackgroundLightGray
import com.example.sd_smart_parking_app.ui.theme.CornerRadius
import com.example.sd_smart_parking_app.ui.theme.Elevation
import com.example.sd_smart_parking_app.ui.theme.HighAvailabilityGreen
import com.example.sd_smart_parking_app.ui.theme.MediumGray
import com.example.sd_smart_parking_app.ui.theme.Spacing
import com.example.sd_smart_parking_app.ui.theme.Typography
import com.example.sd_smart_parking_app.ui.theme.White

@Composable
fun FloorCard(
    floorNumber: Int,
    availableSpots: Int,
    totalSpots: Int,
    availabilityPercentage: Int,
    availabilityStatus: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = Elevation.sm
        ),
        shape = RoundedCornerShape(CornerRadius.md)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    // Número de piso
                    Box(
                        modifier = Modifier
                            .background(
                                color = BackgroundLightGray,
                                shape = RoundedCornerShape(CornerRadius.md)
                            )
                            .padding(Spacing.md),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = floorNumber.toString(),
                            style = Typography.headlineLarge
                        )
                    }

                    // Información del piso
                    Column(
                        modifier = Modifier.padding(top = Spacing.md)
                    ) {
                        Text(
                            text = "Floor $floorNumber",
                            style = Typography.headlineMedium
                        )
                        Text(
                            text = "$availableSpots of $totalSpots available",
                            style = Typography.bodySmall,
                            modifier = Modifier.padding(top = Spacing.xs)
                        )
                    }
                }

                // Espacios disponibles (número grande a la derecha)
                Text(
                    text = availableSpots.toString(),
                    style = Typography.displayMedium,
                    color = HighAvailabilityGreen
                )
            }

            // Barra de disponibilidad
            AvailabilityProgressBarWithLabel(
                percentage = availabilityPercentage,
                label = "Availability",
                modifier = Modifier.padding(top = Spacing.lg)
            )

            // Status de disponibilidad
            Text(
                text = availabilityStatus,
                style = Typography.bodySmall.copy(color = MediumGray),
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = Spacing.sm)
            )
        }
    }
}