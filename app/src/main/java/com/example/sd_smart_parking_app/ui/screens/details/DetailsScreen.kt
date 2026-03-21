package com.example.sd_smart_parking_app.ui.screens.details

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
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.example.sd_smart_parking_app.data.model.ParkingConfig
import com.example.sd_smart_parking_app.data.model.ParkingSpot
import com.example.sd_smart_parking_app.data.repository.ParkingRepository
import com.example.sd_smart_parking_app.ui.components.FloorCard
import com.example.sd_smart_parking_app.ui.theme.BackgroundWhite
import com.example.sd_smart_parking_app.ui.theme.CornerRadius
import com.example.sd_smart_parking_app.ui.theme.HighAvailabilityGreen
import com.example.sd_smart_parking_app.ui.theme.SmartParkingTheme
import com.example.sd_smart_parking_app.ui.theme.Spacing
import com.example.sd_smart_parking_app.ui.theme.Typography
import com.example.sd_smart_parking_app.ui.theme.WarningYellow
import kotlinx.coroutines.delay
import com.example.sd_smart_parking_app.viewmodel.NotificationViewModel
import androidx.compose.material3.Button
import kotlin.random.Random

@Composable
fun DetailsScreen(
    modifier: Modifier = Modifier
) {
    val repository = remember { ParkingRepository() }
    val context = LocalContext.current
    var parkingConfig by remember { mutableStateOf(ParkingConfig()) }
    var parkingSpots by remember { mutableStateOf<List<ParkingSpot>>(emptyList()) }

    LaunchedEffect(Unit) {
        repository.getParkingConfig { config ->
            parkingConfig = config
        }
        repository.getParkingSpots { spots ->
            parkingSpots = spots
        }
    }

    // Actualizar disponibilidad cada 10 segundos con datos simulados (para pruebas)
    LaunchedEffect(Unit) {
        while (true) {
            delay(10000) // Actualizar cada 10 segundos para pruebas
            // Simular cambios en parkingSpots
            val updatedSpots = parkingSpots.map { spot ->
                // Cambiar aleatoriamente si un espacio está disponible
                spot.copy(isAvailable = Random.nextBoolean())
            }
            parkingSpots = updatedSpots
        }
    }

    // ViewModel para notificaciones
    val notificationViewModel = remember { NotificationViewModel(context = context) }

    // Verificar disponibilidad y enviar notificaciones
    LaunchedEffect(parkingSpots) {
        if (parkingSpots.isNotEmpty()) {
            try {
                notificationViewModel.sendTestNotification()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val totalSpots = parkingConfig.numberOfFloors * parkingConfig.spotsPerFloor
    val availableSpots = parkingSpots.count { it.isAvailable }
    val occupiedSpots = parkingSpots.count { !it.isAvailable && it.floor > 0 }

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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.lg),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Floor Details",
                        style = Typography.headlineLarge
                    )
                    Text(
                        text = "Real-time availability",
                        style = Typography.bodySmall,
                        modifier = Modifier.padding(top = Spacing.xs)
                    )
                }
                // Botón de refrescar
                IconButton(onClick = { }) {
                    Text("🔄", fontSize = 20.sp)
                }
            }

            // Card de estado del sistema
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg)
                    .background(
                        color = HighAvailabilityGreen.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(CornerRadius.lg)
                    )
                    .padding(Spacing.md)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🟢", fontSize = 20.sp, modifier = Modifier.padding(end = Spacing.sm))
                    Column {
                        Text(
                            text = "System Operational",
                            style = Typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "Auto-update active",
                            style = Typography.bodySmall
                        )
                    }
                }
            }

            // Card de resumen (Total, Available, Occupied)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg, vertical = Spacing.lg)
                    .background(
                        color = WarningYellow,
                        shape = RoundedCornerShape(CornerRadius.lg)
                    )
                    .padding(Spacing.lg)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = totalSpots.toString(),
                            style = Typography.displaySmall
                        )
                        Text(
                            text = "Total",
                            style = Typography.bodySmall
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = availableSpots.toString(),
                            style = Typography.displaySmall
                        )
                        Text(
                            text = "Available",
                            style = Typography.bodySmall
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = (totalSpots - availableSpots).toString(),
                            style = Typography.displaySmall
                        )
                        Text(
                            text = "Occupied",
                            style = Typography.bodySmall
                        )
                    }
                }
            }

            // Título de distribución por piso
            Text(
                text = "📊 Floor Distribution",
                style = Typography.headlineSmall,
                modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md)
            )

            // Lista de pisos
            Column(
                modifier = Modifier.padding(horizontal = Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                for (floorNum in 1..parkingConfig.numberOfFloors) {
                    val spotsInFloor = parkingSpots.filter { it.floor == floorNum }
                    val floorTotal = parkingConfig.spotsPerFloor
                    val floorAvailable = spotsInFloor.count { it.isAvailable }
                    val floorPercentage = if (floorTotal > 0) (floorAvailable * 100) / floorTotal else 0

                    FloorCard(
                        floorNumber = floorNum,
                        availableSpots = floorAvailable,
                        totalSpots = floorTotal,
                        availabilityPercentage = floorPercentage,
                        availabilityStatus = when {
                            floorPercentage > 60 -> "High"
                            floorPercentage > 30 -> "Medium"
                            else -> "Low"
                        }
                    )
                }
            }

            // Espaciado inferior
            Box(modifier = Modifier.padding(bottom = Spacing.lg))
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DetailsScreenPreview() {
    SmartParkingTheme {
        DetailsScreen()
    }
}