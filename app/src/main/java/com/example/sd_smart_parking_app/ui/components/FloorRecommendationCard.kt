package com.example.sd_smart_parking_app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.sd_smart_parking_app.data.repository.FloorRecommendation
import com.example.sd_smart_parking_app.ui.theme.CornerRadius
import com.example.sd_smart_parking_app.ui.theme.HighAvailabilityGreen
import com.example.sd_smart_parking_app.ui.theme.Spacing
import com.example.sd_smart_parking_app.ui.theme.Typography
import com.example.sd_smart_parking_app.ui.theme.WarningYellow

@Composable
fun FloorRecommendationCard(
    recommendation: FloorRecommendation?,
    isLoading: Boolean,
    error: String?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = WarningYellow,
                shape = RoundedCornerShape(CornerRadius.lg)
            )
            .padding(Spacing.lg)
    ) {
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.lg),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            error != null -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "⚠️ Error",
                        style = Typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        modifier = Modifier.padding(bottom = Spacing.sm)
                    )
                    Text(
                        text = error,
                        style = Typography.bodySmall,
                        modifier = Modifier.padding(bottom = Spacing.md)
                    )
                }
            }
            recommendation != null -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    // Título
                    Text(
                        text = "Smart Recommendation",
                        style = Typography.headlineSmall
                    )

                    // Piso recomendado
                    if (recommendation.floorNumber > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = Color.White.copy(alpha = 0.8f),
                                    shape = RoundedCornerShape(Spacing.md)
                                )
                                .padding(Spacing.lg)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = getFloorColor(recommendation.emoji),
                                            shape = RoundedCornerShape(50)
                                        )
                                        .padding(Spacing.md),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = recommendation.emoji,
                                        fontSize = 24.sp
                                    )
                                }

                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "Floor ${recommendation.floorNumber}",
                                        style = Typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "${recommendation.availableSpots} available spots",
                                        style = Typography.bodyMedium
                                    )
                                    Text(
                                        text = "Availability: ${recommendation.availabilityPercentage}%",
                                        style = Typography.bodySmall,
                                        modifier = Modifier.padding(top = Spacing.xs)
                                    )
                                }
                            }
                        }

                        // Razón de la recomendación
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = HighAvailabilityGreen.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(Spacing.md)
                                )
                                .padding(Spacing.md)
                        ) {
                            Text(
                                text = recommendation.reason,
                                style = Typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Tiempo promedio histórico del usuario
                        if (recommendation.userAvgDuration > 0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = Color.White.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(Spacing.md)
                                    )
                                    .padding(Spacing.md)
                            ) {
                                Text(
                                    text = "⏱️ Your historical avg parking time: ${
                                        formatDuration(recommendation.userAvgDuration)
                                    }",
                                    style = Typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Tiempo promedio del piso ese mismo día
                        // Tiempo promedio del piso hoy
                        if (recommendation.floorAvgDuration > 0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = Color.White.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(Spacing.md)
                                    )
                                    .padding(Spacing.md)
                            ) {
                                Text(
                                    text = "🅿️ Avg parking time on this floor today: ${
                                        formatDuration(recommendation.floorAvgDuration)
                                    }",
                                    style = Typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = Color(0xFFFFCDD2),
                                    shape = RoundedCornerShape(Spacing.md)
                                )
                                .padding(Spacing.md)
                        ) {
                            Text(
                                text = recommendation.reason,
                                style = Typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }
                }
            }
            else -> {
                Text(
                    text = "No Available Data",
                    style = Typography.bodyMedium,
                    modifier = Modifier.padding(Spacing.md)
                )
            }
        }
    }
}

private fun formatDuration(hours: Double): String {
    val h = hours.toInt()
    val m = ((hours - h) * 60).toInt()
    return when {
        h > 0 && m > 0 -> "${h}h ${m}min"
        h > 0 -> "${h}h"
        else -> "${m}min"
    }
}

private fun getFloorColor(emoji: String): Color {
    return when (emoji) {
        "🟢" -> Color(0xFFC8E6C9)
        "🟡" -> Color(0xFFFFF9C4)
        "🟠" -> Color(0xFFFFE0B2)
        "🔴" -> Color(0xFFFFCDD2)
        else -> Color(0xFFF5F5F5)
    }
}