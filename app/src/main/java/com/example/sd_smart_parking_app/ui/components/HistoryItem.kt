package com.example.sd_smart_parking_app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.example.sd_smart_parking_app.ui.theme.ErrorRed
import com.example.sd_smart_parking_app.ui.theme.HighAvailabilityGreen
import com.example.sd_smart_parking_app.ui.theme.Spacing
import com.example.sd_smart_parking_app.ui.theme.Typography
import com.example.sd_smart_parking_app.ui.theme.White

enum class ParkingStatus {
    COMPLETED,
    CANCELLED
}

@Composable
fun HistoryItem(
    date: String,
    location: String,
    status: ParkingStatus,
    entryTime: String?,
    waitTime: String?,
    duration: String?,
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
            // Encabezado con ícono, fecha y status
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    // Ícono de estado
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                color = when (status) {
                                    ParkingStatus.COMPLETED -> HighAvailabilityGreen.copy(alpha = 0.2f)
                                    ParkingStatus.CANCELLED -> ErrorRed.copy(alpha = 0.2f)
                                },
                                shape = RoundedCornerShape(50.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (status) {
                                ParkingStatus.COMPLETED -> "✓"
                                ParkingStatus.CANCELLED -> "✕"
                            },
                            style = Typography.headlineLarge.copy(
                                color = when (status) {
                                    ParkingStatus.COMPLETED -> HighAvailabilityGreen
                                    ParkingStatus.CANCELLED -> ErrorRed
                                }
                            )
                        )
                    }

                    Column {
                        Text(
                            text = "📅 $date",
                            style = Typography.bodyMedium
                        )
                        Text(
                            text = location,
                            style = Typography.bodySmall,
                            modifier = Modifier.padding(top = Spacing.xs)
                        )
                    }
                }

                // Badge de estado
                StatusBadge(
                    text = when (status) {
                        ParkingStatus.COMPLETED -> "Completed"
                        ParkingStatus.CANCELLED -> "Cancelled"
                    },
                    status = when (status) {
                        ParkingStatus.COMPLETED -> BadgeStatus.SUCCESS
                        ParkingStatus.CANCELLED -> BadgeStatus.ERROR
                    }
                )
            }

            // Información de tiempos (solo si está disponible)
            if (entryTime != null || waitTime != null || duration != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Spacing.lg),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (entryTime != null) {
                        TimeInfo(
                            label = "Entry Time",
                            value = entryTime,
                            icon = "⏰"
                        )
                    }
                    if (waitTime != null) {
                        TimeInfo(
                            label = "Wait Time",
                            value = waitTime,
                            icon = "⏱️"
                        )
                    }
                    if (duration != null) {
                        TimeInfo(
                            label = "Duration",
                            value = duration,
                            icon = "⏳"
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TimeInfo(
    label: String,
    value: String,
    icon: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = icon,
            style = Typography.headlineSmall
        )
        Text(
            text = label,
            style = Typography.bodySmall,
            modifier = Modifier.padding(top = Spacing.xs)
        )
        Text(
            text = value,
            style = Typography.bodyMedium,
            modifier = Modifier.padding(top = Spacing.xs)
        )
    }
}