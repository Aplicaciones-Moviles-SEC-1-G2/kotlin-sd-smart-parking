package com.example.sd_smart_parking_app.ui.screens.details

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Business
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sd_smart_parking_app.data.model.ParkingConfig
import com.example.sd_smart_parking_app.data.model.ParkingSpot
import com.example.sd_smart_parking_app.data.repository.ParkingRepository
import com.example.sd_smart_parking_app.ui.components.FloorCard
import com.example.sd_smart_parking_app.ui.theme.*
import kotlinx.coroutines.delay
import com.example.sd_smart_parking_app.viewmodel.NotificationViewModel
import androidx.compose.material3.Button
import kotlin.random.Random
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DetailsScreen(
    modifier: Modifier = Modifier
) {
    val repository = remember { ParkingRepository() }
    val context = LocalContext.current
    var parkingConfig by remember { mutableStateOf(ParkingConfig()) }
    var parkingSpots by remember { mutableStateOf<List<ParkingSpot>>(emptyList()) }
    var lastUpdate by remember { mutableStateOf(SimpleDateFormat("h:mm:ss a", Locale.getDefault()).format(Date())) }

    LaunchedEffect(Unit) {
        repository.getParkingConfig { config ->
            parkingConfig = config
            lastUpdate = SimpleDateFormat("h:mm:ss a", Locale.getDefault()).format(Date())
        }
        repository.getParkingSpots { spots ->
            parkingSpots = spots
            lastUpdate = SimpleDateFormat("h:mm:ss a", Locale.getDefault()).format(Date())
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
    val occupiedSpots = totalSpots - availableSpots

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = BackgroundLightGray
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.lg, bottom = Spacing.md),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = "Floor Details",
                        style = Typography.headlineLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Real-time availability",
                        style = Typography.bodySmall.copy(color = MediumGray)
                    )
                }
                
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = White,
                    shadowElevation = 2.dp,
                    onClick = {
                        lastUpdate = SimpleDateFormat("h:mm:ss a", Locale.getDefault()).format(Date())
                    }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            modifier = Modifier.size(20.dp),
                            tint = Color.Black
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            // Card de estado del sistema
            Card(
                modifier = Modifier
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.05f)),
                shape = RoundedCornerShape(CornerRadius.lg),
                border = androidx.compose.foundation.BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(Spacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(SuccessGreen, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(Spacing.md))
                    Column {
                        Text(
                            text = "System Operational",
                            style = Typography.bodyLarge.copy(fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20))
                        )
                        Text(
                            text = "Auto-update active",
                            style = Typography.bodySmall.copy(color = SuccessGreen)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            // Card de resumen (Total, Available, Occupied)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = PrimaryYellow),
                shape = RoundedCornerShape(CornerRadius.lg)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Spacing.xl),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SummaryItem(value = totalSpots.toString(), label = "Total")
                    SummaryItem(value = availableSpots.toString(), label = "Available")
                    SummaryItem(value = occupiedSpots.toString(), label = "Occupied")
                }
            }

            Spacer(modifier = Modifier.height(Spacing.lg))

            // Card de distribución por piso
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = Spacing.md)
            ) {
                Icon(
                    imageVector = Icons.Default.Business,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text(
                    text = "Floor Distribution",
                    style = Typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )
            }

            // Lista de pisos
            Column(
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

            Spacer(modifier = Modifier.height(Spacing.lg))

            // Card de cola de entrada
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = QueueBackground),
                shape = RoundedCornerShape(CornerRadius.lg),
                border = androidx.compose.foundation.BorderStroke(1.dp, QueueBorder)
            ) {
                Column(
                    modifier = Modifier.padding(Spacing.lg)
                ) {
                    Text(
                        text = "Entry Queue",
                        style = Typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF8D6E63)
                        )
                    )
                    Spacer(modifier = Modifier.height(Spacing.xl))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = "Vehicles waiting",
                            style = Typography.bodyLarge.copy(color = Color(0xFF8D6E63))
                        )
                        Text(
                            text = parkingConfig.entryQueueLength.toString(),
                            style = Typography.displayMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF5D4037)
                            )
                        )
                    }
                }
            }

            // Footer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Spacing.xl),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Last update: $lastUpdate",
                    style = Typography.bodySmall.copy(color = LastUpdateGray)
                )
            }
        }
    }
}

@Composable
fun SummaryItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = Typography.displaySmall.copy(fontWeight = FontWeight.Bold, color = Color.Black)
        )
        Text(
            text = label,
            style = Typography.bodySmall.copy(color = Color.Black.copy(alpha = 0.7f))
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DetailsScreenPreview() {
    SmartParkingTheme {
        DetailsScreen()
    }
}
