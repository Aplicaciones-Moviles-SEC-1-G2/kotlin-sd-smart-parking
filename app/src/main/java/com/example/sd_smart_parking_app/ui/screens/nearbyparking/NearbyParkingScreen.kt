package com.example.sd_smart_parking_app.ui.screens.nearbyparking

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sd_smart_parking_app.data.model.NearbyParking
import com.example.sd_smart_parking_app.ui.components.PrimaryButton
import com.example.sd_smart_parking_app.ui.theme.CornerRadius
import com.example.sd_smart_parking_app.ui.theme.DarkText
import com.example.sd_smart_parking_app.ui.theme.Elevation
import com.example.sd_smart_parking_app.ui.theme.HighAvailabilityGreen
import com.example.sd_smart_parking_app.ui.theme.LowAvailabilityRed
import com.example.sd_smart_parking_app.ui.theme.MediumAvailabilityOrange
import com.example.sd_smart_parking_app.ui.theme.MediumGray
import com.example.sd_smart_parking_app.ui.theme.PrimaryYellow
import com.example.sd_smart_parking_app.ui.theme.SmartParkingTheme
import com.example.sd_smart_parking_app.ui.theme.Spacing
import com.example.sd_smart_parking_app.ui.theme.Typography
import com.example.sd_smart_parking_app.ui.theme.White
import com.example.sd_smart_parking_app.viewmodel.DataSource
import com.example.sd_smart_parking_app.viewmodel.NearbyParkingUiState
import com.example.sd_smart_parking_app.viewmodel.NearbyParkingViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NearbyParkingScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: NearbyParkingViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadNearbyParking() }

    NearbyParkingContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onRefresh = { viewModel.loadNearbyParking() },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NearbyParkingContent(
    uiState: NearbyParkingUiState,
    onNavigateBack: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Nearby Parking",
                        style = Typography.headlineMedium,
                        color = DarkText
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = DarkText
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = DarkText
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PrimaryYellow,
                    titleContentColor = DarkText,
                    navigationIconContentColor = DarkText,
                    actionIconContentColor = DarkText
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Data source banner — shown when data is not fresh
            if (uiState.dataSource != DataSource.FRESH && !uiState.isLoading) {
                DataSourceBanner(
                    dataSource = uiState.dataSource,
                    savedAtMs = uiState.savedAtMs
                )
            }

            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = PrimaryYellow)
                    }
                }

                uiState.parkingList.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(Spacing.md)
                        ) {
                            Text(text = "🅿️", fontSize = 48.sp)
                            Text(
                                text = "No nearby parking found",
                                style = Typography.bodyMedium,
                                color = MediumGray
                            )
                            PrimaryButton(
                                text = "Retry",
                                onClick = onRefresh,
                                modifier = Modifier
                                    .padding(horizontal = Spacing.xxl)
                                    .width(200.dp)
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(Spacing.md),
                        verticalArrangement = Arrangement.spacedBy(Spacing.md)
                    ) {
                        items(uiState.parkingList) { parking ->
                            NearbyParkingCard(parking = parking)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DataSourceBanner(
    dataSource: DataSource,
    savedAtMs: Long
) {
    val (backgroundColor, message) = when (dataSource) {
        DataSource.CACHE -> {
            val minutesAgo = ((System.currentTimeMillis() - savedAtMs) / 60000).toInt()
                .coerceAtLeast(0)
            Pair(
                Color(0xFFFFF8E1),
                "⏱ Showing recent data · Updated $minutesAgo min ago"
            )
        }
        DataSource.LOCAL_STORAGE -> {
            val formatted = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                .format(Date(savedAtMs))
            Pair(
                Color(0xFFFFF3E0),
                "📦 Offline · Last saved $formatted"
            )
        }
        DataSource.NONE -> Pair(
            Color(0xFFFFEBEE),
            "❌ No data available"
        )
        DataSource.FRESH -> return
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = Spacing.xs)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = backgroundColor),
            shape = RoundedCornerShape(CornerRadius.md)
        ) {
            Text(
                text = message,
                style = Typography.bodySmall,
                modifier = Modifier.padding(Spacing.md)
            )
        }
    }
}

@Composable
private fun NearbyParkingCard(parking: NearbyParking) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(CornerRadius.lg),
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.sm),
        colors = CardDefaults.cardColors(containerColor = White)
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = parking.name,
                        style = Typography.headlineSmall
                    )
                    Text(
                        text = parking.address,
                        style = Typography.bodySmall,
                        color = MediumGray
                    )
                }
                AvailabilityBadge(capacity = parking.approximateCapacity)
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MediumGray
                    )
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Text(
                        text = "${parking.distanceMeters.toInt()} m away",
                        style = Typography.bodySmall
                    )
                }
                if (parking.phone.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MediumGray
                        )
                        Spacer(modifier = Modifier.width(Spacing.xs))
                        Text(
                            text = parking.phone,
                            style = Typography.bodySmall
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            PrimaryButton(
                text = "How to get there",
                onClick = {
                    val uri = Uri.parse("google.navigation:q=${parking.lat},${parking.lng}")
                    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                        setPackage("com.google.android.apps.maps")
                    }
                    context.startActivity(intent)
                }
            )
        }
    }
}

@Composable
private fun AvailabilityBadge(capacity: Int) {
    val (backgroundColor, label) = when {
        capacity > 50 -> Pair(HighAvailabilityGreen, "High")
        capacity >= 20 -> Pair(MediumAvailabilityOrange, "Medium")
        else -> Pair(LowAvailabilityRed, "Low")
    }

    Box(
        modifier = Modifier.padding(start = Spacing.sm)
    ) {
        Card(
            shape = RoundedCornerShape(CornerRadius.sm),
            colors = CardDefaults.cardColors(containerColor = backgroundColor)
        ) {
            Text(
                text = label,
                style = Typography.labelSmall,
                color = White,
                modifier = Modifier.padding(
                    horizontal = Spacing.xs,
                    vertical = Spacing.xs
                )
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun NearbyParkingScreenPreview() {
    val mockParkingList = listOf(
        NearbyParking(
            id = "parking_calle_19",
            name = "Parqueadero Calle 19",
            address = "Cl. 19 #3-16, Bogotá",
            lat = 4.60098,
            lng = -74.06521,
            approximateCapacity = 80,
            phone = "+57 1 234 5678",
            distanceMeters = 560f
        ),
        NearbyParking(
            id = "parking_eje_ambiental",
            name = "Parqueadero Eje Ambiental",
            address = "Av. Jiménez #3-50, Bogotá",
            lat = 4.60201,
            lng = -74.06874,
            approximateCapacity = 35,
            phone = "+57 1 456 7890",
            distanceMeters = 280f
        )
    )
    SmartParkingTheme {
        NearbyParkingContent(
            uiState = NearbyParkingUiState(
                parkingList = mockParkingList,
                isLoading = false,
                dataSource = DataSource.FRESH
            ),
            onNavigateBack = {},
            onRefresh = {}
        )
    }
}
