package com.example.sd_smart_parking_app.ui.screens.profile

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sd_smart_parking_app.ui.theme.BackgroundLightGray
import com.example.sd_smart_parking_app.ui.theme.BackgroundWhite
import com.example.sd_smart_parking_app.ui.theme.CornerRadius
import com.example.sd_smart_parking_app.ui.theme.DarkText
import com.example.sd_smart_parking_app.ui.theme.Elevation
import com.example.sd_smart_parking_app.ui.theme.ErrorRed
import com.example.sd_smart_parking_app.ui.theme.MediumGray
import com.example.sd_smart_parking_app.ui.theme.SmartParkingTheme
import com.example.sd_smart_parking_app.ui.theme.Spacing
import com.example.sd_smart_parking_app.ui.theme.Typography
import com.example.sd_smart_parking_app.ui.theme.WarningYellow
import com.example.sd_smart_parking_app.ui.theme.White
import com.example.sd_smart_parking_app.viewmodel.AuthViewModel
import com.example.sd_smart_parking_app.viewmodel.ProfileViewModel

@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel(),
    onNavigateToLogin: () -> Unit
) {
    val userProfile by viewModel.userProfile.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Dialog de confirmación de logout
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Log Out", style = Typography.headlineSmall) },
            text = { Text("You Sure you want log out?", style = Typography.bodyMedium) },
            confirmButton = {
                Button(
                    onClick = {
                        // Limpiar Remember Me y hacer logout
                        authViewModel.signOut()
                        showLogoutDialog = false
                        onNavigateToLogin()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) {
                    Text("Log Out", color = White)
                }
            },
            dismissButton = {
                Button(
                    onClick = { showLogoutDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MediumGray)
                ) {
                    Text("Cancel", color = White)
                }
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = WarningYellow)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BackgroundWhite)
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header con botón de logout mejorado
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.lg),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Columna de textos (Izquierda)
                    Column(
                        modifier = Modifier.weight(1f)
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

                    // Botón de Logout Mejorado (Derecha)
                    IconButton(
                        onClick = { showLogoutDialog = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Logout",
                            tint = ErrorRed,
                            modifier = Modifier.size(24.dp)
                        )
                    }
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
                                        text = userProfile.name.ifEmpty { "User Name" },
                                        style = Typography.headlineMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = userProfile.email.ifEmpty { "user@email.com" },
                                        style = Typography.bodySmall
                                    )
                                }
                            }

                            // Botón de editar
                            IconButton(onClick = { }) {
                                Text("✏️", fontSize = 20.sp)
                            }
                        }

                        // Estadísticas (Simuladas por ahora)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatisticItem(
                                label = "Total Visits",
                                value = "0"
                            )
                            StatisticItem(
                                label = "Avg Wait (min)",
                                value = "0"
                            )
                            StatisticItem(
                                label = "Avg Travel\nmin",
                                value = "0"
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
                        value = userProfile.email.ifEmpty { "Not set" }
                    )

                    // Card de Teléfono
                    AccountCard(
                        icon = "📱",
                        label = "Phone",
                        value = userProfile.phone.ifEmpty { "Not set" }
                    )

                    // Card de Vehículo (Muestra el primer carro si existe)
                    val vehicleInfo = if (userProfile.cars.isNotEmpty()) {
                        "${userProfile.cars[0].plate} • ${userProfile.cars[0].name}"
                    } else {
                        "No vehicle registered"
                    }

                    AccountCard(
                        icon = "🚗",
                        label = "Vehicle",
                        value = vehicleInfo
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
        ProfileScreen(
            onNavigateToLogin = {}
        )
    }
}