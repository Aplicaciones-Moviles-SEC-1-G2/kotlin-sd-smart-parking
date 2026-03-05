package com.example.sd_smart_parking_app.ui.screens.navigation

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sd_smart_parking_app.ui.components.PrimaryButton
import com.example.sd_smart_parking_app.ui.theme.BackgroundWhite
import com.example.sd_smart_parking_app.ui.theme.CornerRadius
import com.example.sd_smart_parking_app.ui.theme.DarkText
import com.example.sd_smart_parking_app.ui.theme.Elevation
import com.example.sd_smart_parking_app.ui.theme.NavigationBlue
import com.example.sd_smart_parking_app.ui.theme.Spacing
import com.example.sd_smart_parking_app.ui.theme.Typography
import com.example.sd_smart_parking_app.ui.theme.White

@Composable
fun NavigationScreen(
    modifier: Modifier = Modifier
) {
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
            // Header con título
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.lg),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            color = com.example.sd_smart_parking_app.ui.theme.WarningYellow,
                            shape = RoundedCornerShape(50.dp)
                        )
                        .padding(Spacing.md),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "A",
                        style = Typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                        color = DarkText
                    )
                }

                Column(
                    modifier = Modifier.padding(start = Spacing.md)
                ) {
                    Text(
                        text = "Navigation",
                        style = Typography.headlineLarge
                    )
                    Text(
                        text = "To SD Building Parking",
                        style = Typography.bodySmall,
                        modifier = Modifier.padding(top = Spacing.xs)
                    )
                }
            }

            // Card de información de ruta (azul)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                colors = CardDefaults.cardColors(
                    containerColor = NavigationBlue
                ),
                shape = RoundedCornerShape(CornerRadius.lg),
                elevation = CardDefaults.cardElevation(defaultElevation = Elevation.md)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.lg),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    InfoColumn(
                        icon = "⏱️",
                        value = "20",
                        label = "minutes"
                    )

                    Box(
                        modifier = Modifier
                            .padding(horizontal = Spacing.sm)
                            .background(
                                color = White.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(2.dp)
                            )
                            .padding(horizontal = 1.dp, vertical = Spacing.lg)
                    )

                    InfoColumn(
                        icon = "🛣️",
                        value = "5.2",
                        label = "kilometers"
                    )

                    Box(
                        modifier = Modifier
                            .padding(horizontal = Spacing.sm)
                            .background(
                                color = White.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(2.dp)
                            )
                            .padding(horizontal = 1.dp, vertical = Spacing.lg)
                    )

                    InfoColumn(
                        icon = "📅",
                        value = "03:22",
                        label = "PM\narrival"
                    )
                }
            }

            // Título de ruta
            Text(
                text = "📍 Route Overview",
                style = Typography.headlineSmall,
                modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md)
            )

            // Lista de instrucciones de ruta
            Column(
                modifier = Modifier.padding(horizontal = Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                RouteStep(
                    icon = "⬆️",
                    instruction = "Head north on Carrera 1ª",
                    distance = "1.2 km"
                )
                RouteStep(
                    icon = "➡️",
                    instruction = "Turn right onto Calle 19A",
                    distance = "0.8 km"
                )
                RouteStep(
                    icon = "⬆️",
                    instruction = "Continue straight for 2.5 km",
                    distance = "2.5 km"
                )
                RouteStep(
                    icon = "➡️",
                    instruction = "Turn right to SD Building",
                    distance = "0.7 km"
                )
                RouteStep(
                    icon = "📍",
                    instruction = "Arrive at SD Parking",
                    distance = ""
                )
            }

            // Espaciado inferior
            Box(modifier = Modifier.padding(bottom = Spacing.lg))
        }
    }
}

@Composable
fun InfoColumn(
    icon: String,
    value: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(icon, fontSize = 20.sp)
        Text(
            text = value,
            style = Typography.headlineLarge.copy(
                color = White,
                fontWeight = FontWeight.Bold
            ),
            fontSize = 24.sp
        )
        Text(
            text = label,
            style = Typography.bodySmall.copy(color = White),
            fontSize = 10.sp
        )
    }
}

@Composable
fun RouteStep(
    icon: String,
    instruction: String,
    distance: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Spacing.md)
            .background(
                color = com.example.sd_smart_parking_app.ui.theme.BackgroundLightGray,
                shape = RoundedCornerShape(CornerRadius.md)
            )
            .padding(Spacing.md),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = White,
                    shape = RoundedCornerShape(50.dp)
                )
                .padding(Spacing.sm),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, fontSize = 18.sp)
        }

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = instruction,
                style = Typography.bodyMedium
            )
            if (distance.isNotEmpty()) {
                Text(
                    text = distance,
                    style = Typography.bodySmall,
                    modifier = Modifier.padding(top = Spacing.xs)
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun NavigationScreenPreview() {
    SmartParkingTheme {
        NavigationScreen()
    }
}