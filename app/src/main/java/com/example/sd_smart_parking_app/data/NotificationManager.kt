package com.example.sd_smart_parking_app.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import kotlin.random.Random

class NotificationManagerHelper(private val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }
    fun sendAvailabilityNotification(
        floorNumber: Int,
        availableSpots: Int,
        occupancyPercentage: Int
    ) {
        val title = "¡Available spots!"
        val message = when {
            occupancyPercentage <= 20 -> {
                "Floor $floorNumber: $availableSpots available spots (High Availability)"
            }
            occupancyPercentage <= 50 -> {
                "Floor $floorNumber: $availableSpots available spots (Medium Availability)"
            }
            else -> {
                "Floor $floorNumber: $availableSpots available spots"
            }
        }

        sendNotification(title, message)
    }

    fun sendLowOccupancyNotification(averageOccupancy: Int) {
        val title = "Low Parking Occupancy"
        val message = "Average Occupancy: $averageOccupancy%. Is time to park"

        sendNotification(title, message)
    }

    fun sendReminderNotification(message: String) {
        val title = "Reminder"
        sendNotification(title, message)
    }

    private fun sendNotification(title: String, message: String) {
        createNotificationChannel()

        val notificationId = Random.nextInt(10000)
        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setVibrate(longArrayOf(0, 500, 250, 500))

        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Parking Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = "Parking Spots Availability Notifications"
            channel.enableVibration(true)

            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "parking_notifications_channel"
    }
}