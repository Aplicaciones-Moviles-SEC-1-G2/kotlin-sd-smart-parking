package com.example.sd_smart_parking_app.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sd_smart_parking_app.data.NotificationManagerHelper
import com.example.sd_smart_parking_app.data.repository.NotificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import android.util.Log

data class NotificationPreferences(
    val enableHighAvailabilityNotifications: Boolean = true,
    val enableLowOccupancyNotifications: Boolean = true,
    val occupancyThreshold: Int = 30,
    val floorThreshold: Int = 50
)

class NotificationViewModel(context: Context) : ViewModel() {

    private val notificationRepository = NotificationRepository(
        NotificationManagerHelper(context)
    )

    private val _notificationPreferences = MutableStateFlow(NotificationPreferences())
    val notificationPreferences: StateFlow<NotificationPreferences> = _notificationPreferences

    private val _lastNotificationTime = MutableStateFlow<Long>(0)

    // Cooldown de 5 minutos para evitar notificaciones repetidas
    private val notificationCooldownMs = 5 * 60 * 1000L

    fun updateNotificationPreferences(preferences: NotificationPreferences) {
        _notificationPreferences.value = preferences
    }

    /**
     * Compara spots anteriores con actuales y notifica si se liberaron nuevos cupos
     * y la hora actual está dentro del rango de llegada habitual del usuario
     */
    fun checkAndNotifyNewSpots(
        previousSpots: List<Boolean>,
        currentSpots: List<Boolean>
    ) {
        // Contar cuántos spots pasaron de ocupado (false) a disponible (true)
        val newlyAvailable = currentSpots.zip(previousSpots).count { (current, previous) ->
            current && !previous
        }

        if (newlyAvailable <= 0) return

        // Respetar cooldown para no spamear notificaciones
        val now = System.currentTimeMillis()
        if (now - _lastNotificationTime.value < notificationCooldownMs) {
            Log.d("NotificationViewModel", "Notification skipped — cooldown active")
            return
        }

        _lastNotificationTime.value = now

        viewModelScope.launch {
            notificationRepository.checkAndNotifyIfInUserRange(newlyAvailable)
        }
    }

    fun sendAvailabilityNotification(
        floorNumber: Int,
        availableSpots: Int,
        occupancyPercentage: Int
    ) {
        viewModelScope.launch {
            notificationRepository.sendAvailabilityNotification(
                floorNumber = floorNumber,
                availableSpots = availableSpots,
                occupancyPercentage = occupancyPercentage
            )
        }
    }

    fun sendLowOccupancyNotification(averageOccupancy: Int) {
        viewModelScope.launch {
            notificationRepository.sendLowOccupancyNotification(averageOccupancy)
        }
    }
}