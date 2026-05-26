package com.example.sd_smart_parking_app.ui.screens.notifications

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.SignalWifiOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sd_smart_parking_app.data.NetworkMonitor
import com.example.sd_smart_parking_app.ui.theme.*
import com.example.sd_smart_parking_app.viewmodel.SharedDetailsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val viewModel = remember { SharedDetailsViewModel.getInstance() }
    val detailsState by viewModel.detailsState.collectAsState()
    val threshold by viewModel.thresholdFlow.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.initNotificationManager(context)
        }
    }

    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val permission = android.Manifest.permission.POST_NOTIFICATIONS
            if (androidx.core.content.ContextCompat.checkSelfPermission(context, permission) != 
                android.content.pm.PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(permission)
            } else {
                viewModel.initNotificationManager(context)
            }
        } else {
            viewModel.initNotificationManager(context)
        }
    }
    
    val networkMonitor = remember { NetworkMonitor(context) }
    val isConnected by networkMonitor.isConnected.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications SD", style = Typography.headlineMedium.copy(fontWeight = FontWeight.Bold)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundWhite)
            )
        },
        containerColor = BackgroundWhite
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            Text("Notifications", style = Typography.titleMedium.copy(color = Color.Gray))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = BackgroundLightGray),
                shape = RoundedCornerShape(CornerRadius.lg)
            ) {
                Column(modifier = Modifier.padding(Spacing.lg)) {
                    Text(
                        text = "Alert when availability is less than:",
                        style = Typography.bodyMedium.copy(color = Color.Gray)
                    )
                    Spacer(modifier = Modifier.height(Spacing.md))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = WarningYellow,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(Spacing.sm))
                            Text(
                                text = "$threshold empty spots",
                                style = Typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                onClick = { if (threshold > 1) viewModel.threshold-- },
                                shape = RoundedCornerShape(4.dp),
                                color = Color.LightGray.copy(alpha = 0.3f)
                            ) {
                                Text("-", modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), fontSize = 20.sp)
                            }
                            Spacer(modifier = Modifier.width(Spacing.md))
                            Surface(
                                onClick = { if (threshold < 100) viewModel.threshold++ },
                                shape = RoundedCornerShape(4.dp),
                                color = Color.LightGray.copy(alpha = 0.3f)
                            ) {
                                Text("+", modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), fontSize = 20.sp)
                            }
                        }
                    }
                }
            }

            Text("Current state (SD Building)", style = Typography.titleMedium.copy(color = Color.Gray))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = BackgroundLightGray),
                shape = RoundedCornerShape(CornerRadius.lg)
            ) {
                Column(modifier = Modifier.padding(Spacing.lg)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isConnected) Icons.Default.Wifi else Icons.Default.WifiOff,
                                contentDescription = null,
                                tint = NavigationBlue
                            )
                            Spacer(modifier = Modifier.width(Spacing.sm))
                            Text("Connectivity:")
                        }
                        Text(
                            text = if (isConnected) "Online" else "Offline",
                            color = if (isConnected) SuccessGreen else ErrorRed,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(Spacing.md))
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(Spacing.md))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🚗", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(Spacing.sm))
                            Text("Live availability:")
                        }
                        Text(
                            text = "${detailsState.availableSpots} free",
                            color = NavigationBlue,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Text("Offline Simulator", style = Typography.titleMedium.copy(color = Color.Gray))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = BackgroundLightGray),
                shape = RoundedCornerShape(CornerRadius.lg)
            ) {
                Column(modifier = Modifier.padding(Spacing.md)) {
                    TextButton(
                        onClick = { viewModel.simulateNotification() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = null, tint = NavigationBlue)
                            Spacer(modifier = Modifier.width(Spacing.sm))
                            Text("Simulate Threshold Trigger", color = NavigationBlue)
                        }
                    }
                    
                    TextButton(
                        onClick = { viewModel.simulateLostConnection() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("💥", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(Spacing.sm))
                            Text("Simulate lost internet", color = Color(0xFFD35400))
                        }
                    }

                    Spacer(modifier = Modifier.height(Spacing.sm))
                    
                    Button(
                        onClick = { viewModel.refreshData() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = NavigationBlue.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(CornerRadius.md)
                    ) {
                        Text("Refresh Data", color = NavigationBlue)
                    }
                }
            }
        }
    }
}
