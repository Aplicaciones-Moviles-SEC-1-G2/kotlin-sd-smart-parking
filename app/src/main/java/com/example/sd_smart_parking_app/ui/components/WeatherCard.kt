package com.example.sd_smart_parking_app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sd_smart_parking_app.data.WeatherData
import com.example.sd_smart_parking_app.ui.theme.CornerRadius
import com.example.sd_smart_parking_app.ui.theme.Spacing
import com.example.sd_smart_parking_app.ui.theme.Typography
import com.example.sd_smart_parking_app.ui.theme.WarningYellow

@Composable
fun WeatherCard(
    weatherData: WeatherData?,
    recommendation: String,
    isLoading: Boolean,
    error: String?,
    hasRainRisk: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = if (hasRainRisk) Color(0xFFE3F2FD) else WarningYellow,
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
                    Button(
                        onClick = onRefresh,
                        modifier = Modifier.padding(top = Spacing.sm)
                    ) {
                        Text("Try Again")
                    }
                }
            }
            weatherData != null -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Weather in Bogotá",
                            style = Typography.headlineSmall
                        )
                        IconButton(onClick = onRefresh) {
                            Text("🔄", fontSize = 18.sp)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${weatherData.temperature.toInt()}°C",
                                style = Typography.displaySmall.copy(fontSize = 36.sp),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = weatherData.description.replaceFirstChar { it.uppercase() },
                                style = Typography.bodyMedium
                            )
                            Text(
                                text = "Sensation: ${weatherData.feelsLike.toInt()}°C",
                                style = Typography.bodySmall
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = Color.White.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(Spacing.md)
                            )
                            .padding(Spacing.md),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        WeatherDetailItem(
                            label = "Humidity",
                            value = "${weatherData.humidity}%",
                            icon = "💧"
                        )
                        WeatherDetailItem(
                            label = "Wind",
                            value = "${weatherData.windSpeed.toInt()} m/s",
                            icon = "💨"
                        )
                        WeatherDetailItem(
                            label = "Clouds",
                            value = "${weatherData.cloudiness}%",
                            icon = "☁️"
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = if (hasRainRisk) Color(0xFFFFCDD2) else Color(0xFFC8E6C9),
                                shape = RoundedCornerShape(Spacing.md)
                            )
                            .padding(Spacing.md)
                    ) {
                        Text(
                            text = recommendation,
                            style = Typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeatherDetailItem(
    label: String,
    value: String,
    icon: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = icon,
            fontSize = 24.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = label,
            style = Typography.bodySmall,
            fontSize = 10.sp
        )
        Text(
            text = value,
            style = Typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            fontSize = 12.sp
        )
    }
}