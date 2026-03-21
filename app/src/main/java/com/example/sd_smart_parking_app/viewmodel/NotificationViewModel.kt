package com.example.sd_smart_parking_app.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sd_smart_parking_app.data.NotificationManagerHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

data class NotificationPreferences(
    val enableHighAvailabilityNotifications: Boolean = true,
    val enableLowOccupancyNotifications: Boolean = true,
    val occupancyThreshold: Int = 30,
    val floorThreshold: Int = 50
)

class NotificationViewModel(private val context: Context) : ViewModel() {

    private val notificationManager = NotificationManagerHelper(context)

    private val _notificationPreferences = MutableStateFlow(NotificationPreferences())
    val notificationPreferences: StateFlow<NotificationPreferences> = _notificationPreferences

    private val _lastNotificationTime = MutableStateFlow<Long>(0)

    fun updateNotificationPreferences(preferences: NotificationPreferences) {
        _notificationPreferences.value = preferences
    }

    fun sendTestNotification() {
        viewModelScope.launch {
            notificationManager.sendAvailabilityNotification(
                floorNumber = Random.nextInt(1, 4),
                availableSpots = Random.nextInt(5, 20),
                occupancyPercentage = Random.nextInt(10, 50)
            )
        }
    }
}