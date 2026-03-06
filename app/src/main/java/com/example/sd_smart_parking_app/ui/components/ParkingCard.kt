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
import com.example.sd_smart_parking_app.ui.theme.DarkText
import com.example.sd_smart_parking_app.ui.theme.Elevation
import com.example.sd_smart_parking_app.ui.theme.HighAvailabilityGreen
import com.example.sd_smart_parking_app.ui.theme.Spacing
import com.example.sd_smart_parking_app.ui.theme.Typography
import com.example.sd_smart_parking_app.ui.theme.White

@Composable
fun ParkingCard(
    title: String,
    subtitle: String,
    availableSpots: Int,
    totalSpots: Int,
    availabilityPercentage: Int,
    onNavigateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = Elevation.md
        ),
        shape = RoundedCornerShape(CornerRadius.lg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg)
        ) {
            // Título y Subtítulo
            Text(
                text = title,
                style = Typography.headlineLarge
            )
            Text(
                text = subtitle,
                style = Typography.bodySmall,
                modifier = Modifier.padding(top = Spacing.xs)
            )

            // Número de espacios disponibles
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = Spacing.lg)
                    .background(
                        color = BackgroundLightGray,
                        shape = RoundedCornerShape(50.dp)
                    )
                    .padding(Spacing.lg)
            ) {
                Text(
                    text = availableSpots.toString(),
                    style = Typography.displaySmall
                )
            }

            // Label de disponibilidad
            Text(
                text = "Available Spots",
                style = Typography.bodySmall,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            // Badge de alta disponibilidad
            StatusBadge(
                text = "High Availability",
                status = BadgeStatus.SUCCESS,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = Spacing.md)
            )

            // Barra de disponibilidad
            AvailabilityProgressBarWithLabel(
                percentage = availabilityPercentage,
                label = "Availability",
                modifier = Modifier.padding(vertical = Spacing.md)
            )

            // Info cards (Occupancy y Wait)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Spacing.md),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                InfoCard(
                    label = "Occupancy",
                    value = "43%",
                    icon = "🚗",
                    modifier = Modifier.weight(1f)
                )
                InfoCard(
                    label = "Wait",
                    value = "0 min",
                    icon = "⏱️",
                    modifier = Modifier.weight(1f)
                )
            }

            // Botón de navegación
            PrimaryButton(
                text = "Navigate to Parking",
                onClick = onNavigateClick,
                modifier = Modifier.padding(top = Spacing.md)
            )
        }
    }
}

@Composable
fun InfoCard(
    label: String,
    value: String,
    icon: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = BackgroundLightGray
        ),
        shape = RoundedCornerShape(CornerRadius.md)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = icon,
                style = Typography.headlineLarge
            )
            Text(
                text = label,
                style = Typography.bodySmall,
                modifier = Modifier.padding(top = Spacing.sm)
            )
            Text(
                text = value,
                style = Typography.headlineMedium,
                modifier = Modifier.padding(top = Spacing.xs)
            )
        }
    }
}