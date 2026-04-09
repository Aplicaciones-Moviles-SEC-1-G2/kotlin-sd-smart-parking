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
                        text = "🎯 Recomendación Inteligente",
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
                                // Emoji del piso
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

                                // Información del piso
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "Piso ${recommendation.floorNumber}",
                                        style = Typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "${recommendation.availableSpots} espacios disponibles",
                                        style = Typography.bodyMedium
                                    )
                                    Text(
                                        text = "Disponibilidad: ${recommendation.availabilityPercentage}%",
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
                                text = "💡 ${recommendation.reason}",
                                style = Typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        // Sin espacios disponibles
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
                    text = "Sin datos disponibles",
                    style = Typography.bodyMedium,
                    modifier = Modifier.padding(Spacing.md)
                )
            }
        }
    }
}

/**
 * Retorna el color basado en el emoji del piso
 */
private fun getFloorColor(emoji: String): Color {
    return when (emoji) {
        "🟢" -> Color(0xFFC8E6C9)  // Verde
        "🟡" -> Color(0xFFFFF9C4)  // Amarillo
        "🟠" -> Color(0xFFFFE0B2)  // Naranja
        "🔴" -> Color(0xFFFFCDD2)  // Rojo
        else -> Color(0xFFF5F5F5)   // Gris
    }
}