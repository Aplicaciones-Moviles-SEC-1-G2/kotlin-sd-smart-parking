package com.example.sd_smart_parking_app.ui.screens.details

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.sd_smart_parking_app.ui.components.FloorCard
import com.example.sd_smart_parking_app.ui.theme.*
import com.example.sd_smart_parking_app.utils.NetworkMonitor
import com.example.sd_smart_parking_app.viewmodel.NotificationViewModel
import com.example.sd_smart_parking_app.viewmodel.SharedDetailsViewModel

@Composable
fun DetailsScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel = remember { SharedDetailsViewModel.getInstance() }
    val networkMonitor = remember { NetworkMonitor(context) }
    val isOnline by networkMonitor.networkStateFlow.collectAsState(initial = networkMonitor.isOnline())

    LaunchedEffect(Unit) {
        viewModel.initializeIfNeeded()
    }

    val detailsState by viewModel.detailsState.collectAsState()
    val notificationViewModel = remember { NotificationViewModel(context = context) }

    var previousSpotsAvailability by remember { mutableStateOf<List<Boolean>>(emptyList()) }

    LaunchedEffect(detailsState.parkingSpots) {
        if (detailsState.parkingSpots.isNotEmpty()) {
            val currentAvailability = detailsState.parkingSpots.map { it.isAvailable }
            if (previousSpotsAvailability.isNotEmpty()) {
                notificationViewModel.checkAndNotifyNewSpots(
                    previousSpots = previousSpotsAvailability,
                    currentSpots = currentAvailability
                )
            }
            previousSpotsAvailability = currentAvailability
        }
    }

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
                    modifier = Modifier
                        .size(40.dp)
                        .alpha(if (isOnline) 1f else 0.4f),
                    shape = CircleShape,
                    color = White,
                    shadowElevation = if (isOnline) 2.dp else 0.dp,
                    onClick = { if (isOnline) viewModel.refreshData() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            modifier = Modifier.size(20.dp),
                            tint = if (isOnline) Color.Black else Color.Gray
                        )
                    }
                }
            }

            // Offline banner
            if (!isOnline) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(CornerRadius.lg),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(Spacing.md),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.WifiOff,
                            contentDescription = null,
                            tint = ErrorRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(Spacing.sm))
                        Column {
                            Text(
                                text = "Offline mode",
                                style = Typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = ErrorRed
                                )
                            )
                            if (detailsState.lastUpdate.isNotEmpty()) {
                                Text(
                                    text = "Showing data from ${detailsState.lastUpdate}",
                                    style = Typography.bodySmall.copy(color = ErrorRed.copy(alpha = 0.8f))
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(Spacing.md))
            }

            // System status card
            Card(
                modifier = Modifier.fillMaxWidth(),
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
                            style = Typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1B5E20)
                            )
                        )
                        Text(
                            text = "Auto-update active",
                            style = Typography.bodySmall.copy(color = SuccessGreen)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            // Summary card
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
                    SummaryItem(value = detailsState.totalSpots.toString(), label = "Total")
                    SummaryItem(value = detailsState.availableSpots.toString(), label = "Available")
                    SummaryItem(value = detailsState.occupiedSpots.toString(), label = "Occupied")
                }
            }

            Spacer(modifier = Modifier.height(Spacing.lg))

            // Floor distribution header
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

            // Floor list
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                detailsState.floorStates.forEach { floor ->
                    FloorCard(
                        floorNumber = floor.floorNumber,
                        availableSpots = floor.availableSpots,
                        totalSpots = floor.totalSpots,
                        availabilityPercentage = floor.availabilityPercentage,
                        availabilityStatus = floor.availabilityStatus
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.lg))

            // Entry queue card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = QueueBackground),
                shape = RoundedCornerShape(CornerRadius.lg),
                border = androidx.compose.foundation.BorderStroke(1.dp, QueueBorder)
            ) {
                Column(modifier = Modifier.padding(Spacing.lg)) {
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
                            text = detailsState.parkingConfig.entryQueueLength.toString(),
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
                    text = "Last update: ${detailsState.lastUpdate}",
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
