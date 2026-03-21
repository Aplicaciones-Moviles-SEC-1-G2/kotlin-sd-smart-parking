package com.example.sd_smart_parking_app.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.sd_smart_parking_app.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class NotificationService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Extraer datos del mensaje
        val title = remoteMessage.notification?.title ?: "SD Parking"
        val body = remoteMessage.notification?.body ?: "Nueva notificación"

        // Mostrar notificación
        sendNotification(title, body)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Aquí puedes enviar el token a tu servidor
        // para mantener un registro de dispositivos registrados
    }

    private fun sendNotification(title: String, messageBody: String) {
        createNotificationChannel()

        val notificationId = 1001
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_notification_clear_all)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setStyle(NotificationCompat.BigTextStyle().bigText(messageBody))

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Parking Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = "Notificaciones de disponibilidad de espacios"

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "parking_notifications"
    }
}