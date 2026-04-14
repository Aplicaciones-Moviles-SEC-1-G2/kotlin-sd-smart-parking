package com.example.sd_smart_parking_app.data.repository

import android.util.Log
import com.example.sd_smart_parking_app.data.NotificationManagerHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Calendar

class NotificationRepository(private val notificationManager: NotificationManagerHelper) {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val vehicleRecordsPath = "vehicleRecords"

    // -------------------------------------------------------------------------
    // Métodos existentes
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // Nuevo método: validar rango horario y notificar si hay nuevos cupos
    // -------------------------------------------------------------------------

    suspend fun checkAndNotifyIfInUserRange(newlyAvailableSpots: Int) {
        try {
            val userEmail = auth.currentUser?.email
            if (userEmail.isNullOrEmpty()) {
                Log.d("NotificationRepository", "No authenticated user found")
                return
            }

            // Consultar vehicleRecords del usuario filtrando solo "entry"
            val snapshot = firestore.collection(vehicleRecordsPath)
                .whereEqualTo("ownerEmail", userEmail)
                .whereEqualTo("type", "entry")
                .get()
                .await()

            if (snapshot.isEmpty) {
                Log.d("NotificationRepository", "No entry records found for user $userEmail")
                return
            }

            // Contar frecuencia de llegadas por hora
            val hourCounts = mutableMapOf<Int, Int>()
            snapshot.documents.forEach { doc ->
                val timestamp = doc.getTimestamp("timestamp")
                if (timestamp != null) {
                    val cal = Calendar.getInstance(java.util.TimeZone.getTimeZone("America/Bogota"))
                    cal.time = timestamp.toDate()
                    val hour = cal.get(Calendar.HOUR_OF_DAY)
                    hourCounts[hour] = (hourCounts[hour] ?: 0) + 1
                }
            }

            if (hourCounts.isEmpty()) return

            Log.d("NotificationRepository", "Hour counts for $userEmail: $hourCounts")

            // Encontrar el rango de horas más recurrente
            val peakRange = findPeakHourRange(hourCounts)
            val rangeStart = peakRange.first
            val rangeEnd = peakRange.second

            Log.d("NotificationRepository", "Peak range: $rangeStart:00 - $rangeEnd:00")

            // Validar si la hora actual está dentro del rango
            val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val isInRange = if (rangeStart <= rangeEnd) {
                currentHour in rangeStart..rangeEnd
            } else {
                // Rango que cruza medianoche (ej. 23:00 - 01:00)
                currentHour >= rangeStart || currentHour <= rangeEnd
            }

            Log.d("NotificationRepository", "Current hour: $currentHour, isInRange: $isInRange")

            if (isInRange) {
                notificationManager.sendNewSpotsAvailableNotification(
                    availableSpots = newlyAvailableSpots,
                    rangeStart = rangeStart,
                    rangeEnd = rangeEnd
                )
                Log.d("NotificationRepository", "Notification sent for $newlyAvailableSpots new spots")
            }

        } catch (e: Exception) {
            Log.e("NotificationRepository", "Error checking user range: ${e.message}", e)
        }
    }

    // -------------------------------------------------------------------------
    // Encontrar el rango de 2 horas consecutivas con más llegadas
    // -------------------------------------------------------------------------

    private fun findPeakHourRange(hourCounts: Map<Int, Int>): Pair<Int, Int> {
        var maxCount = 0
        var peakStart = 0

        // Buscar ventana de 2 horas consecutivas con mayor frecuencia
        for (hour in 0..23) {
            val count = (hourCounts[hour] ?: 0) + (hourCounts[(hour + 1) % 24] ?: 0)
            if (count > maxCount) {
                maxCount = count
                peakStart = hour
            }
        }

        return Pair(peakStart, (peakStart + 1) % 24)
    }
}