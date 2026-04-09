package com.example.sd_smart_parking_app.data.repository

import com.example.sd_smart_parking_app.data.NotificationManagerHelper

class NotificationRepository(private val notificationManager: NotificationManagerHelper) {

    /**
     * Envía una notificación cuando hay espacios disponibles
     */
    fun sendAvailabilityNotification(
        floorNumber: Int,
        availableSpots: Int,
        occupancyPercentage: Int
    ) {
        notificationManager.sendAvailabilityNotification(
            floorNumber = floorNumber,
            availableSpots = availableSpots,
            occupancyPercentage = occupancyPercentage
        )
    }

    /**
     * Envía una notificación cuando la ocupación es baja
     */
    fun sendLowOccupancyNotification(averageOccupancy: Int) {
        notificationManager.sendLowOccupancyNotification(averageOccupancy)
    }

    /**
     * Envía una notificación de recordatorio
     */
    fun sendReminderNotification(message: String) {
        notificationManager.sendReminderNotification(message)
    }

    /**
     * Envía una notificación genérica de prueba
     */
    fun sendTestNotification(
        floorNumber: Int,
        availableSpots: Int,
        occupancyPercentage: Int
    ) {
        sendAvailabilityNotification(
            floorNumber = floorNumber,
            availableSpots = availableSpots,
            occupancyPercentage = occupancyPercentage
        )
    }
}