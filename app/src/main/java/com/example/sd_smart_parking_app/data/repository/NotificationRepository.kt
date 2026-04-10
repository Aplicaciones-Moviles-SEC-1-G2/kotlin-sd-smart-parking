package com.example.sd_smart_parking_app.data.repository

import com.example.sd_smart_parking_app.data.NotificationManagerHelper

class NotificationRepository(private val notificationManager: NotificationManagerHelper) {
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
    fun sendLowOccupancyNotification(averageOccupancy: Int) {
        notificationManager.sendLowOccupancyNotification(averageOccupancy)
    }
    fun sendReminderNotification(message: String) {
        notificationManager.sendReminderNotification(message)
    }
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