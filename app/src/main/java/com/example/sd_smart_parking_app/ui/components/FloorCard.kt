package com.example.sd_smart_parking_app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sd_smart_parking_app.ui.theme.BackgroundLightGray
import com.example.sd_smart_parking_app.ui.theme.CornerRadius
import com.example.sd_smart_parking_app.ui.theme.Elevation
import com.example.sd_smart_parking_app.ui.theme.HighAvailabilityGreen
import com.example.sd_smart_parking_app.ui.theme.LowAvailabilityRed
import com.example.sd_smart_parking_app.ui.theme.MediumAvailabilityOrange
import com.example.sd_smart_parking_app.ui.theme.MediumGray
import com.example.sd_smart_parking_app.ui.theme.Spacing
import com.example.sd_smart_parking_app.ui.theme.Typography
import com.example.sd_smart_parking_app.ui.theme.White
import com.example.sd_smart_parking_app.ui.theme.DeepBlack
import com.example.sd_smart_parking_app.ui.theme.BorderGray

@Composable
fun FloorCard(
    floorNumber: Int,
    availableSpots: Int,
    totalSpots: Int,
    availabilityPercentage: Int,
    availabilityStatus: String,
    modifier: Modifier = Modifier
) {
    val statusColor = when {
        availabilityPercentage > 60 -> HighAvailabilityGreen
        availabilityPercentage > 30 -> MediumAvailabilityOrange
        else -> LowAvailabilityRed
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.sm),
        shape = RoundedCornerShape(CornerRadius.lg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Número de piso
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .background(
                                color = BackgroundLightGray,
                                shape = RoundedCornerShape(CornerRadius.md)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = floorNumber.toString(),
                            style = Typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    // Información del piso
                    Column(modifier = Modifier.padding(start = Spacing.md)) {
                        Text(
                            text = "Floor $floorNumber",
                            style = Typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "$availableSpots of $totalSpots available",
                            style = Typography.bodySmall.copy(color = MediumGray)
                        )
                    }
                }

                // Espacios disponibles
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = availableSpots.toString(),
                        style = Typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                        color = statusColor
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (availabilityPercentage > 50) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                            contentDescription = null,
                            tint = statusColor.copy(alpha = 0.7f),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "$availabilityPercentage%",
                            style = Typography.bodySmall.copy(fontSize = 10.sp, color = MediumGray)
                        )
                    }
                }
            }

            // Barra de disponibilidad
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.lg)
                    .height(8.dp)
                    .background(color = BorderGray, shape = RoundedCornerShape(4.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = availabilityPercentage / 100f)
                        .height(8.dp)
                        .background(color = DeepBlack, shape = RoundedCornerShape(4.dp))
                )
            }

            // Etiquetas debajo de la barra
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.sm),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Availability",
                    style = Typography.bodySmall.copy(color = MediumGray)
                )
                Text(
                    text = availabilityStatus,
                    style = Typography.bodySmall.copy(color = statusColor, fontWeight = FontWeight.Medium)
                )
            }
        }
    }
}
