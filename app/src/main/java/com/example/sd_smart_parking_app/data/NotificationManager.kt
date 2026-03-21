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

    /**
     * Envía una notificación cuando hay espacios disponibles
     */
    fun sendAvailabilityNotification(
        floorNumber: Int,
        availableSpots: Int,
        occupancyPercentage: Int
    ) {
        val title = "¡Espacios disponibles!"
        val message = when {
            occupancyPercentage <= 20 -> {
                "Piso $floorNumber: $availableSpots espacios libres (Alta disponibilidad) 🎉"
            }
            occupancyPercentage <= 50 -> {
                "Piso $floorNumber: $availableSpots espacios libres (Disponibilidad media) 👍"
            }
            else -> {
                "Piso $floorNumber: $availableSpots espacios libres"
            }
        }

        sendNotification(title, message)
    }

    /**
     * Envía una notificación cuando la ocupación es baja
     */
    fun sendLowOccupancyNotification(averageOccupancy: Int) {
        val title = "Baja ocupación en el parqueadero"
        val message = "Ocupación promedio: $averageOccupancy%. Es un buen momento para estacionar 🚗"

        sendNotification(title, message)
    }

    /**
     * Envía una notificación de recordatorio
     */
    fun sendReminderNotification(message: String) {
        val title = "Recordatorio"
        sendNotification(title, message)
    }

    /**
     * Envía una notificación genérica
     */
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

    /**
     * Crea el canal de notificaciones
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Parking Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = "Notificaciones de disponibilidad de espacios en el parqueadero"
            channel.enableVibration(true)

            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "parking_notifications_channel"
    }
}