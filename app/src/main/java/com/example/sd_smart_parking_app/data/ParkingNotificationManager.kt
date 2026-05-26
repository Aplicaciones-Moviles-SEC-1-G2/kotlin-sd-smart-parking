package com.example.sd_smart_parking_app.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.sd_smart_parking_app.data.model.ParkingSpot
import kotlinx.coroutines.*
import java.util.Collections
import java.util.concurrent.atomic.AtomicBoolean

class ParkingNotificationManager(private val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val sharedPrefs = context.getSharedPreferences("parking_prefs", Context.MODE_PRIVATE)
    
    private val backgroundScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    private val spotCache = Collections.synchronizedList(mutableListOf<ParkingSpot>())
    
    private val isNetworkAvailable = AtomicBoolean(true)
    private var lastThresholdAlertCount = -1

    init {
        createNotificationChannels()
        setupNetworkMonitoring()
    }

    var spotThreshold: Int
        get() = sharedPrefs.getInt("parking_spot_threshold", 5)
        set(value) = sharedPrefs.edit().putInt("parking_spot_threshold", value).apply()

    fun updateCache(newSpots: List<ParkingSpot>) {
        backgroundScope.launch {
            synchronized(spotCache) {
                newSpots.forEach { spot ->
                    val index = spotCache.binarySearch { it.id.compareTo(spot.id) }
                    if (index >= 0) {
                        spotCache[index] = spot
                    } else {
                        spotCache.add(-(index + 1), spot)
                    }
                }
            }
            checkThreshold(newSpots.count { it.isAvailable })
        }
    }

    private fun checkThreshold(availableCount: Int) {
        if (availableCount < spotThreshold && availableCount != lastThresholdAlertCount) {
            lastThresholdAlertCount = availableCount
            sendNotification(
                "Critical Capacity",
                "Alert! Only $availableCount spots available in the SD Building."
            )
        }
    }

    private fun setupNetworkMonitoring() {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
            override fun onLost(network: Network) {
                isNetworkAvailable.set(false)
                backgroundScope.launch {
                    val lastKnownAvailable = synchronized(spotCache) {
                        spotCache.count { it.isAvailable }
                    }
                    if (!isNetworkAvailable.get()) {
                        sendNotification(
                            "Basement Mode: Connection Lost",
                            "You've entered a zone with no coverage. Last known availability: $lastKnownAvailable free spots."
                        )
                    }
                }
            }

            override fun onAvailable(network: Network) {
                isNetworkAvailable.set(true)
            }
        })
    }

    private fun sendNotification(title: String, message: String) {
        val builder = NotificationCompat.Builder(context, "parking_alerts")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 500, 200, 500))

        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "parking_alerts",
                "Parking Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Proactive availability and connection notifications"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun simulateNotification(title: String, message: String) {
        sendNotification(title, message)
    }
}
