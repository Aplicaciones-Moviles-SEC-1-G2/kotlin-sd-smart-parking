package com.example.sd_smart_parking_app.ui.screens.profile

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sd_smart_parking_app.ui.theme.BackgroundLightGray
import com.example.sd_smart_parking_app.ui.theme.BackgroundWhite
import com.example.sd_smart_parking_app.ui.theme.CornerRadius
import com.example.sd_smart_parking_app.ui.theme.Elevation
import com.example.sd_smart_parking_app.ui.theme.Spacing
import com.example.sd_smart_parking_app.ui.theme.Typography
import com.example.sd_smart_parking_app.ui.theme.WarningYellow
import com.example.sd_smart_parking_app.ui.theme.White

@Composable
fun ProfileScreen(
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
            // Header
            Column(
                modifier = Modifier.padding(Spacing.lg)
            ) {
                Text(
                    text = "Profile",
                    style = Typography.headlineLarge
                )
                Text(
                    text = "Manage your account and preferences",
                    style = Typography.bodySmall,
                    modifier = Modifier.padding(top = Spacing.xs)
                )
            }

            // Card de perfil del usuario (Amarillo)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg),
                colors = CardDefaults.cardColors(
                    containerColor = WarningYellow
                ),
                shape = RoundedCornerShape(CornerRadius.lg),
                elevation = CardDefaults.cardElevation(defaultElevation = Elevation.md)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.lg)
                ) {
                    // Nombre de usuario y avatar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = Spacing.lg),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                        ) {
                            // Avatar
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(
                                        color = White,
                                        shape = RoundedCornerShape(50.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("👤", fontSize = 28.sp)
                            }

                            Column {
                                Text(
                                    text = "Student User",
                                    style = Typography.headlineMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "student@uniandes.edu.co",
                                    style = Typography.bodySmall
                                )
                            }
                        }

                        // Botón de editar
                        IconButton(onClick = { }) {
                            Text("✏️", fontSize = 20.sp)
                        }
                    }

                    // Estadísticas
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatisticItem(
                            label = "Total Visits",
                            value = "47"
                        )
                        StatisticItem(
                            label = "Avg Wait (min)",
                            value = "8.5"
                        )
                        StatisticItem(
                            label = "Avg Travel time\nmin",
                            value = "25"
                        )
                    }
                }
            }

            // Sección Account Information
            Column(
                modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.lg)
            ) {
                Text(
                    text = "Account Information",
                    style = Typography.headlineSmall,
                    modifier = Modifier.padding(bottom = Spacing.md)
                )

                // Card de Email
                AccountCard(
                    icon = "📧",
                    label = "Email",
                    value = "student@uniandes.edu.co"
                )

                // Card de Teléfono
                AccountCard(
                    icon = "📱",
                    label = "Phone",
                    value = "+57 300 123 4567"
                )

                // Card de Vehículo
                AccountCard(
                    icon = "🚗",
                    label = "Vehicle",
                    value = "ABC-123 • Toyota Corolla"
                )
            }

            // Sección Preferences
            Column(
                modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.lg)
            ) {
                Text(
                    text = "Preferences",
                    style = Typography.headlineSmall,
                    modifier = Modifier.padding(bottom = Spacing.md)
                )

                PreferenceCard(
                    icon = "🔔",
                    label = "Notifications",
                    isEnabled = true
                )
            }

            // Espaciado inferior
            Box(modifier = Modifier.padding(bottom = Spacing.lg))
        }
    }
}

@Composable
fun StatisticItem(
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = Typography.displaySmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = Typography.bodySmall,
            modifier = Modifier.padding(top = Spacing.xs)
        )
    }
}

@Composable
fun AccountCard(
    icon: String,
    label: String,
    value: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = Spacing.md),
        colors = CardDefaults.cardColors(
            containerColor = BackgroundLightGray
        ),
        shape = RoundedCornerShape(CornerRadius.md)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = White,
                            shape = RoundedCornerShape(50.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(icon, fontSize = 18.sp)
                }

                Column {
                    Text(
                        text = label,
                        style = Typography.bodySmall
                    )
                    Text(
                        text = value,
                        style = Typography.bodyMedium,
                        modifier = Modifier.padding(top = Spacing.xs)
                    )
                }
            }

            Text("→", fontSize = 20.sp)
        }
    }
}

@Composable
fun PreferenceCard(
    icon: String,
    label: String,
    isEnabled: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = Spacing.md),
        colors = CardDefaults.cardColors(
            containerColor = BackgroundLightGray
        ),
        shape = RoundedCornerShape(CornerRadius.md)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = White,
                            shape = RoundedCornerShape(50.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(icon, fontSize = 18.sp)
                }

                Text(
                    text = label,
                    style = Typography.bodyMedium
                )
            }

            // Toggle simple (podría ser mejorado con un Switch real)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = if (isEnabled) com.example.sd_smart_parking_app.ui.theme.HighAvailabilityGreen else BackgroundLightGray,
                        shape = RoundedCornerShape(50.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(if (isEnabled) "✓" else "○", fontSize = 18.sp)
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ProfileScreenPreview() {
    SmartParkingTheme {
        ProfileScreen()
    }
}