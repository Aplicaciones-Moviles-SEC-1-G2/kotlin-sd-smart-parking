package com.example.sd_smart_parking_app.ui.screens.costbreakdown

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
import com.example.sd_smart_parking_app.ui.theme.MediumGray
import com.example.sd_smart_parking_app.ui.theme.NavigationBlue
import com.example.sd_smart_parking_app.ui.theme.SmartParkingTheme
import com.example.sd_smart_parking_app.ui.theme.Spacing
import com.example.sd_smart_parking_app.ui.theme.Typography
import com.example.sd_smart_parking_app.viewmodel.CostBreakdownViewModel

private val KpiGreen     = Color(0xFF43A047)
private val KpiBlue      = Color(0xFF1E88E5)
private val CapGreen     = Color(0xFF43A047)
private val CapOrange    = Color(0xFFFF8F00)
private val MonthlyGreen = Color(0xFF43A047)
private val ChartPurple  = Color(0xFF5E35B1)

@Composable
fun CostBreakdownScreen(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current

    val networkMonitor = remember { NetworkMonitor(context) }
    val isConnected by networkMonitor.isConnected.collectAsState()
    DisposableEffect(Unit) { onDispose { networkMonitor.unregister() } }

    val viewModel: CostBreakdownViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return CostBreakdownViewModel(context) as T
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
            Row(
                modifier = Modifier.fillMaxWidth().background(BackgroundWhite).padding(horizontal = Spacing.sm, vertical = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onNavigateBack) { Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                Text(text = "Cost Breakdown", style = Typography.headlineMedium, fontWeight = FontWeight.Bold)
                IconButton(onClick = { viewModel.loadCostBreakdown() }) { Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh") }
            }

            Spacer(modifier = Modifier.height(Spacing.lg))

            if (!isConnected) {
                OfflineBanner(
                    message = "Your cost breakdown is showing cached spending data. " +
                            "Daily cap calculations and monthly totals reflect your last connected session.",
                    subMessage = "Fresh cost data will be fetched as soon as connection is restored 💰",
                    modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm)
                )
                Spacer(modifier = Modifier.height(Spacing.sm))
            }

            when {
                uiState.isLoading -> {
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 80.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = NavigationBlue)
                    }
                }
                uiState.error != null && uiState.allTimeSpentCOP == 0.0 -> {
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 80.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("⚠️", fontSize = 40.sp)
                            Spacer(modifier = Modifier.height(Spacing.md))
                            Text(text = "Could not load cost breakdown", style = Typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(text = uiState.error ?: "", style = Typography.bodySmall, color = MediumGray, modifier = Modifier.padding(top = Spacing.xs))
                        }
                    }
                }
                uiState.allTimeSpentCOP == 0.0 -> {
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 80.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("💰", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(Spacing.md))
                            Text(text = "No spending data yet", style = Typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(text = "Your cost breakdown will appear after your first session.", style = Typography.bodySmall, color = MediumGray, modifier = Modifier.padding(top = Spacing.xs))
                        }
                    }
                }
                else -> CostBreakdownContent(viewModel = viewModel)
            }

            Spacer(modifier = Modifier.height(Spacing.lg))
        }
    }
}

@Composable
private fun CostBreakdownContent(viewModel: CostBreakdownViewModel) {
    val s by viewModel.uiState.collectAsState()

    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg), horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
        KpiCard(modifier = Modifier.weight(1f), icon = "¢", iconTint = KpiGreen, value = viewModel.formatCOP(s.allTimeSpentCOP), label = "All-time Spent")
        KpiCard(modifier = Modifier.weight(1f), icon = "📊", iconTint = KpiBlue, value = viewModel.formatCOP(s.avgPerSessionCOP), label = "Avg / Session")
        val capColor = if (s.hitDailyCapPercent < 20.0) CapGreen else CapOrange
        KpiCard(modifier = Modifier.weight(1f), icon = if (s.hitDailyCapPercent < 20.0) "✅" else "⚠️", iconTint = capColor, value = viewModel.formatPercent(s.hitDailyCapPercent), label = "Hit Daily Cap", valueColor = capColor)
    }

    Spacer(modifier = Modifier.height(Spacing.md))
    MonthlySpendingCard(monthLabel = s.monthLabel, spendingCOP = s.monthlySpendingCOP, maxSpendingCOP = s.maxMonthlySpendingCOP, formatCOP = { viewModel.formatCOP(it) })
    Spacer(modifier = Modifier.height(Spacing.md))

    if (s.avgCostByDay.isNotEmpty()) {
        AvgCostByDayChart(data = s.avgCostByDay, maxValue = viewModel.maxAvgCost(s.avgCostByDay), formatLabel = { viewModel.formatCOP(it) }, shortDay = { viewModel.shortDay(it) })
    }

    if (s.cachedAt > 0L) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = Spacing.sm), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Text("🗄", fontSize = 12.sp)
            Spacer(modifier = Modifier.width(4.dp))
            val dateStr = java.text.SimpleDateFormat("d 'de' MMMM 'de' yyyy", java.util.Locale("es", "CO")).format(java.util.Date(s.cachedAt))
            Text(text = if (s.isFromCache) "Cached $dateStr" else "Updated $dateStr", style = Typography.bodySmall, color = MediumGray)
        }
    }
}

@Composable
private fun KpiCard(modifier: Modifier = Modifier, icon: String, iconTint: Color, value: String, label: String, valueColor: Color = Color.Black) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = BackgroundWhite), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.md, horizontal = Spacing.sm), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(iconTint.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) { Text(text = icon, fontSize = 16.sp) }
            Text(text = value, style = Typography.headlineSmall, fontWeight = FontWeight.Bold, color = valueColor)
            Text(text = label, style = Typography.bodySmall, color = MediumGray)
        }
    }
}

@Composable
private fun MonthlySpendingCard(monthLabel: String, spendingCOP: Double, maxSpendingCOP: Double, formatCOP: (Double) -> String) {
    val fraction = if (maxSpendingCOP > 0) (spendingCOP / maxSpendingCOP).coerceIn(0.0, 1.0).toFloat() else 0f
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg), colors = CardDefaults.cardColors(containerColor = BackgroundWhite), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(Spacing.lg)) {
            Text(text = "Monthly Spending", style = Typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = Color.Black)
            Spacer(modifier = Modifier.height(Spacing.md))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(text = monthLabel, style = Typography.bodySmall, color = MediumGray, modifier = Modifier.width(72.dp))
                Spacer(modifier = Modifier.width(Spacing.sm))
                Box(modifier = Modifier.weight(1f).height(20.dp).clip(RoundedCornerShape(10.dp)).background(BackgroundLightGray)) {
                    Box(modifier = Modifier.fillMaxWidth(fraction).height(20.dp).clip(RoundedCornerShape(10.dp)).background(MonthlyGreen))
                }
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text(text = formatCOP(spendingCOP), style = Typography.bodySmall, color = MediumGray, modifier = Modifier.width(44.dp))
            }
        }
    }
}

@Composable
private fun AvgCostByDayChart(data: Map<String, Double>, maxValue: Double, formatLabel: (Double) -> String, shortDay: (String) -> String) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg), colors = CardDefaults.cardColors(containerColor = BackgroundWhite), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(Spacing.lg)) {
            Text(text = "Avg Cost by Day of Week", style = Typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = Color.Black)
            Spacer(modifier = Modifier.height(Spacing.lg))

            // ── Optimization #4: Indexed loop over map entries ────────────────
            // data.forEach { } allocates an Iterator on every recomposition.
            // Converting entries to a list and using an indexed for loop
            // avoids that allocation on each chart render.
            val keys = data.keys.toList()
            for (i in 0 until keys.size) {
                val day     = keys[i]
                val avgCost = data[day] ?: 0.0
                val fraction = if (avgCost == 0.0) 0.04f else (avgCost / maxValue).coerceIn(0.0, 1.0).toFloat()
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                    Text(text = shortDay(day), style = Typography.bodySmall, color = MediumGray, modifier = Modifier.width(36.dp))
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Box(modifier = Modifier.weight(1f).height(20.dp).clip(RoundedCornerShape(10.dp)).background(BackgroundLightGray)) {
                        Box(modifier = Modifier.fillMaxWidth(fraction).height(20.dp).clip(RoundedCornerShape(10.dp)).background(ChartPurple))
                    }
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Text(text = formatLabel(avgCost), style = Typography.bodySmall, color = MediumGray, modifier = Modifier.width(44.dp))
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun CostBreakdownScreenPreview() {
    SmartParkingTheme { CostBreakdownScreen() }
}