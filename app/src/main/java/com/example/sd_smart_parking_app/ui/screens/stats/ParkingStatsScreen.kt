package com.example.sd_smart_parking_app.ui.screens.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sd_smart_parking_app.data.NetworkMonitor
import com.example.sd_smart_parking_app.ui.components.OfflineBanner
import com.example.sd_smart_parking_app.ui.theme.BackgroundLightGray
import com.example.sd_smart_parking_app.ui.theme.BackgroundWhite
import com.example.sd_smart_parking_app.ui.theme.BorderGray
import com.example.sd_smart_parking_app.ui.theme.MediumGray
import com.example.sd_smart_parking_app.ui.theme.NavigationBlue
import com.example.sd_smart_parking_app.ui.theme.SmartParkingTheme
import com.example.sd_smart_parking_app.ui.theme.Spacing
import com.example.sd_smart_parking_app.ui.theme.Typography
import com.example.sd_smart_parking_app.viewmodel.ParkingStatsViewModel

private val KpiBlue      = Color(0xFF2979FF)
private val KpiPurple    = Color(0xFFAB47BC)
private val KpiGreen     = Color(0xFF43A047)
private val ChartBlue    = Color(0xFF1565C0)
private val InsightAmber = Color(0xFFFF8F00)
private val InsightBlue  = Color(0xFF5C6BC0)

@Composable
fun ParkingStatsScreen(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current

    // ── Network Monitor ───────────────────────────────────────────────────────
    val networkMonitor = remember { NetworkMonitor(context) }
    val isConnected by networkMonitor.isConnected.collectAsState()
    DisposableEffect(Unit) {
        onDispose { networkMonitor.unregister() }
    }

    val viewModel: ParkingStatsViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return ParkingStatsViewModel(context) as T
            }
        }
    )

    val uiState by viewModel.uiState.collectAsState()

    Scaffold(modifier = modifier.fillMaxSize()) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundLightGray)
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {

            // ── Top bar ───────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BackgroundWhite)
                    .padding(horizontal = Spacing.sm, vertical = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
                Text(
                    text = "My Parking Stats",
                    style = Typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { viewModel.loadStats() }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh"
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.lg))

            // ── Banner offline — ParkingStats ─────────────────────────────
            // Mensaje personalizado: informa que los stats vienen del caché
            // local (LRU + DataStore + JSON) y se actualizarán al recuperar
            // la conexión. No bloquea la UI — los datos cacheados se muestran.
            if (!isConnected) {
                OfflineBanner(
                    message = "Your parking stats are shown from your last sync. " +
                            "Sessions, time and cost data may not reflect the most recent activity.",
                    subMessage = "Stats will refresh automatically once you're back online 📶",
                    modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm)
                )
                Spacer(modifier = Modifier.height(Spacing.sm))
            }

            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = NavigationBlue)
                    }
                }

                uiState.error != null && uiState.totalSessions == 0 -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("⚠️", fontSize = 40.sp)
                            Spacer(modifier = Modifier.height(Spacing.md))
                            Text(
                                text = "Could not load stats",
                                style = Typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = uiState.error ?: "",
                                style = Typography.bodySmall,
                                color = MediumGray,
                                modifier = Modifier.padding(top = Spacing.xs)
                            )
                        }
                    }
                }

                uiState.totalSessions == 0 -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🅿️", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(Spacing.md))
                            Text(
                                text = "No parking sessions yet",
                                style = Typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Your stats will appear here after your first session.",
                                style = Typography.bodySmall,
                                color = MediumGray,
                                modifier = Modifier.padding(top = Spacing.xs)
                            )
                        }
                    }
                }

                else -> {
                    StatsContent(viewModel = viewModel)
                }
            }

            Spacer(modifier = Modifier.height(Spacing.lg))
        }
    }
}

@Composable
private fun StatsContent(viewModel: ParkingStatsViewModel) {
    val s by viewModel.uiState.collectAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        KpiCard(modifier = Modifier.weight(1f), icon = "🚗", iconTint = KpiBlue, value = s.totalSessions.toString(), label = "Sessions")
        KpiCard(modifier = Modifier.weight(1f), icon = "🕐", iconTint = KpiPurple, value = viewModel.formatTotalTime(s.totalTimeHours), label = "Total Time")
        KpiCard(modifier = Modifier.weight(1f), icon = "¢", iconTint = KpiGreen, value = viewModel.formatCOP(s.totalPaidCOP), label = "Total Paid")
    }

    Spacer(modifier = Modifier.height(Spacing.md))

    AvgSessionCard(label = "Average Session", value = viewModel.formatTotalTime(s.avgSessionHours))

    Spacer(modifier = Modifier.height(Spacing.md))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        InsightCard(
            modifier = Modifier.weight(1f),
            icon = "📅", iconTint = InsightAmber, title = "Busiest Day",
            mainValue = s.busiestDay,
            subValue = "${s.busiestDaySessions} session${if (s.busiestDaySessions != 1) "s" else ""}"
        )
        InsightCard(
            modifier = Modifier.weight(1f),
            icon = "🏢", iconTint = InsightBlue, title = "Favourite Floor",
            mainValue = if (s.favouriteFloor > 0) "Floor ${s.favouriteFloor}" else "—",
            subValue = "${s.favouriteFloorVisits} visit${if (s.favouriteFloorVisits != 1) "s" else ""}"
        )
    }

    Spacer(modifier = Modifier.height(Spacing.md))

    val chartData = if (s.avgDurationByDay.isNotEmpty()) s.avgDurationByDay
    else listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday").associateWith { 0.0 }

    AvgDurationBarChart(
        data = chartData,
        maxValue = viewModel.maxAvgDuration(chartData),
        formatLabel = { viewModel.formatAvgDuration(it) },
        shortDay = { viewModel.shortDay(it) }
    )
}

@Composable
private fun KpiCard(modifier: Modifier = Modifier, icon: String, iconTint: Color, value: String, label: String) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = BackgroundWhite), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.md, horizontal = Spacing.sm), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(iconTint.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) { Text(text = icon, fontSize = 18.sp) }
            Text(text = value, style = Typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.Black)
            Text(text = label, style = Typography.bodySmall, color = MediumGray)
        }
    }
}

@Composable
private fun AvgSessionCard(label: String, value: String) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg), colors = CardDefaults.cardColors(containerColor = BackgroundWhite), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = Spacing.lg), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(text = label, style = Typography.bodySmall, color = MediumGray)
                Text(text = value, fontSize = 36.sp, fontWeight = FontWeight.Bold, color = NavigationBlue)
            }
            Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(NavigationBlue.copy(alpha = 0.08f)), contentAlignment = Alignment.Center) { Text("⏱", fontSize = 26.sp) }
        }
    }
}

@Composable
private fun InsightCard(modifier: Modifier = Modifier, icon: String, iconTint: Color, title: String, mainValue: String, subValue: String) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = BackgroundWhite), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Box(modifier = Modifier.size(28.dp).clip(RoundedCornerShape(6.dp)).background(iconTint.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) { Text(text = icon, fontSize = 14.sp) }
                Text(text = title, style = Typography.bodySmall, color = MediumGray)
            }
            Text(text = mainValue, style = Typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.Black)
            Text(text = subValue, style = Typography.bodySmall, color = MediumGray)
        }
    }
}

@Composable
private fun AvgDurationBarChart(data: Map<String, Double>, maxValue: Double, formatLabel: (Double) -> String, shortDay: (String) -> String) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg), colors = CardDefaults.cardColors(containerColor = BackgroundWhite), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(Spacing.lg)) {
            Text(text = "Avg Duration by Day of Week", style = Typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = Color.Black)
            Spacer(modifier = Modifier.height(Spacing.lg))
            data.forEach { (day, avgHours) ->
                val fraction = if (avgHours == 0.0) 0.04f else (avgHours / maxValue).coerceIn(0.0, 1.0).toFloat()
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                    Text(text = shortDay(day), style = Typography.bodySmall, color = MediumGray, modifier = Modifier.width(36.dp))
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Box(modifier = Modifier.weight(1f).height(20.dp).clip(RoundedCornerShape(10.dp)).background(BorderGray)) {
                        Box(modifier = Modifier.fillMaxWidth(fraction).height(20.dp).clip(RoundedCornerShape(10.dp)).background(ChartBlue))
                    }
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Text(text = formatLabel(avgHours), style = Typography.bodySmall, color = MediumGray, modifier = Modifier.width(44.dp))
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ParkingStatsScreenPreview() {
    SmartParkingTheme { ParkingStatsScreen() }
}