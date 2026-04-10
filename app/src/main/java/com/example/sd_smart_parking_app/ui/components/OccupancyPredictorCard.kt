package com.example.sd_smart_parking_app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.sd_smart_parking_app.data.model.OccupancyPrediction
import com.example.sd_smart_parking_app.ui.theme.BackgroundWhite
import com.example.sd_smart_parking_app.ui.theme.NavigationBlue
import com.example.sd_smart_parking_app.ui.theme.SmartParkingTheme
import com.example.sd_smart_parking_app.ui.theme.Spacing
import com.example.sd_smart_parking_app.ui.theme.Typography

@Composable
fun OccupancyPredictorCard(
    prediction: OccupancyPrediction?,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (isLoading) {
        LoadingCard(modifier)
        return
    }

    if (prediction == null) {
        EmptyCard(modifier)
        return
    }

    val backgroundColor = if (prediction.isBusy) {
        Color(0xFFFFEBEE) // Rojo claro
    } else {
        Color(0xFFE8F5E9) // Verde claro
    }

    val statusColor = if (prediction.isBusy) {
        Color(0xFFC62828) // Rojo oscuro
    } else {
        Color(0xFF2E7D32) // Verde oscuro
    }

    val statusText = if (prediction.isBusy) {
        "🔴 Parqueadero Ocupado"
    } else {
        "🟢 Espacio Disponible"
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .padding(Spacing.md)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            // Header con icono y estado
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = "Occupancy",
                        tint = statusColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Predictor de Ocupación",
                        style = Typography.titleMedium,
                        color = statusColor,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Mostrar advertencia si está ocupado
                if (prediction.isBusy) {
                    Icon(
                        imageVector = Icons.Default.WarningAmber,
                        contentDescription = "Warning",
                        tint = Color(0xFFF57C00),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Línea separadora
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.Gray.copy(alpha = 0.3f))
            )

            // Estado actual
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Spacing.sm),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = statusText,
                    style = Typography.bodyMedium,
                    color = statusColor,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = "${prediction.predictedOccupancy.toInt()}%",
                    style = Typography.headlineSmall,
                    color = statusColor,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(Spacing.xs))

            // Barra de progreso visual
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Gray.copy(alpha = 0.2f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = prediction.predictedOccupancy / 100f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(statusColor)
                )
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            // Información adicional
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Hora actual:",
                        style = Typography.bodySmall,
                        color = Color.Gray
                    )
                    Text(
                        text = String.format("%02d:00", prediction.hour),
                        style = Typography.bodySmall,
                        color = Color.Gray,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Confianza:",
                        style = Typography.bodySmall,
                        color = Color.Gray
                    )
                    Text(
                        text = "${prediction.confidence.toInt()}%",
                        style = Typography.bodySmall,
                        color = Color.Gray,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (!prediction.isBusy) {
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Text(
                        text = "💡 Hora recomendada para estacionar: ${prediction.recommendedTime}",
                        style = Typography.bodySmall,
                        color = statusColor,
                        fontWeight = FontWeight.SemiBold
                    )
                } else {
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Text(
                        text = "⏰ Considera volver en ${prediction.recommendedTime}",
                        style = Typography.bodySmall,
                        color = statusColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingCard(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF5F5F5))
            .padding(Spacing.md),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            CircularProgressIndicator(
                color = NavigationBlue,
                modifier = Modifier.size(40.dp)
            )
            Text(
                text = "Cargando predicción...",
                style = Typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}

@Composable
private fun EmptyCard(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF5F5F5))
            .padding(Spacing.md),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "No hay datos de predicción disponibles",
            style = Typography.bodySmall,
            color = Color.Gray
        )
    }
}

@Preview(showBackground = true)
@Composable
fun OccupancyPredictorCardPreview() {
    SmartParkingTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(BackgroundWhite)
                .padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            // Card ocupado
            OccupancyPredictorCard(
                prediction = OccupancyPrediction(
                    hour = 14,
                    predictedOccupancy = 85f,
                    confidence = 95f,
                    recommendedTime = "10:00",
                    isBusy = true
                )
            )

            // Card con espacio
            OccupancyPredictorCard(
                prediction = OccupancyPrediction(
                    hour = 9,
                    predictedOccupancy = 30f,
                    confidence = 85f,
                    recommendedTime = "09:00",
                    isBusy = false
                )
            )

            // Card cargando
            OccupancyPredictorCard(
                prediction = null,
                isLoading = true
            )
        }
    }
}