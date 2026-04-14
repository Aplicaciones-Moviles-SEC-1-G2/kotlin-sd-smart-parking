package com.example.sd_smart_parking_app.data.repository

import android.util.Log
import com.example.sd_smart_parking_app.data.model.Floor
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.TimeZone

data class FloorRecommendation(
    val floorNumber: Int,
    val availableSpots: Int,
    val availabilityPercentage: Int,
    val reason: String,
    val emoji: String,
    val score: Float,
    val userAvgDuration: Double = 0.0,
    val floorAvgDuration: Double = 0.0
)

class FloorRecommendationRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val vehicleRecordsPath = "vehicleRecords"

    // -------------------------------------------------------------------------
    // Consultas a Firebase
    // -------------------------------------------------------------------------

    suspend fun getFloorAvgDurationsToday(floorNumbers: List<Int>): Map<Int, Double> {
        return try {
            // Obtener inicio y fin del día actual en America/Bogota
            val cal = Calendar.getInstance(TimeZone.getTimeZone("America/Bogota"))
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val startOfDay = cal.time

            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            val endOfDay = cal.time

            val snapshot = firestore.collection(vehicleRecordsPath)
                .whereEqualTo("type", "exit")
                .whereGreaterThanOrEqualTo("timestamp", com.google.firebase.Timestamp(startOfDay))
                .whereLessThanOrEqualTo("timestamp", com.google.firebase.Timestamp(endOfDay))
                .get()
                .await()

            // Agrupar duraciones por piso
            val floorDurations = mutableMapOf<Int, MutableList<Double>>()
            snapshot.documents.forEach { doc ->
                val floor = doc.getLong("floor")?.toInt() ?: return@forEach
                val duration = doc.getDouble("durationHours") ?: return@forEach
                if (duration > 0) {
                    floorDurations.getOrPut(floor) { mutableListOf() }.add(duration)
                }
            }

            // Calcular promedio por piso
            val result = floorDurations.mapValues { (_, durations) ->
                durations.average()
            }

            Log.d("FloorRecommendationRepository", "Floor avg durations today: $result")
            result
        } catch (e: Exception) {
            Log.e("FloorRecommendationRepository", "Error getting floor durations: ${e.message}", e)
            emptyMap()
        }
    }

    suspend fun getUserAvgDuration(): Double {
        return try {
            val userEmail = auth.currentUser?.email
            if (userEmail.isNullOrEmpty()) return 0.0

            val snapshot = firestore.collection(vehicleRecordsPath)
                .whereEqualTo("ownerEmail", userEmail)
                .whereEqualTo("type", "exit")
                .get()
                .await()

            val durations = snapshot.documents.mapNotNull { doc ->
                doc.getDouble("durationHours")?.takeIf { it > 0 }
            }

            if (durations.isEmpty()) return 0.0

            val avg = durations.average()
            Log.d("FloorRecommendationRepository", "User avg duration: $avg hours")
            avg
        } catch (e: Exception) {
            Log.e("FloorRecommendationRepository", "Error getting user avg duration: ${e.message}", e)
            0.0
        }
    }

    // -------------------------------------------------------------------------
    // Lógica de recomendación con context aware
    // -------------------------------------------------------------------------

    suspend fun getFloorRecommendationContextAware(floors: List<Floor>): FloorRecommendation? {
        if (floors.isEmpty()) return null

        val availableFloors = floors.filter { it.availableSpots > 0 }

        if (availableFloors.isEmpty()) {
            return FloorRecommendation(
                floorNumber = 0,
                availableSpots = 0,
                availabilityPercentage = 0,
                reason = "No available spots in the parking",
                emoji = "❌",
                score = 0f
            )
        }

        // Obtener datos de Firebase
        val floorNumbers = availableFloors.map { it.floorNumber }
        val floorAvgDurations = getFloorAvgDurationsToday(floorNumbers)
        val userAvgDuration = getUserAvgDuration()

        Log.d("FloorRecommendationRepository", "User avg: $userAvgDuration, Floor avgs: $floorAvgDurations")

        // Criterio principal: mayor disponibilidad
        // Criterio de desempate: menor duración promedio hoy
        val maxAvailableSpots = availableFloors.maxOf { it.availableSpots }
        val floorsWithMaxAvailability = availableFloors.filter { it.availableSpots == maxAvailableSpots }

        val bestFloor = if (floorsWithMaxAvailability.size > 1 && floorAvgDurations.isNotEmpty()) {
            // Desempate por menor duración promedio hoy
            floorsWithMaxAvailability.minByOrNull { floor ->
                floorAvgDurations[floor.floorNumber] ?: Double.MAX_VALUE
            }
        } else {
            // Un solo piso con mayor disponibilidad o sin datos de duración
            floorsWithMaxAvailability.first()
        } ?: return null

        val floorAvgDuration = floorAvgDurations[bestFloor.floorNumber] ?: 0.0

        return FloorRecommendation(
            floorNumber = bestFloor.floorNumber,
            availableSpots = bestFloor.availableSpots,
            availabilityPercentage = bestFloor.availabilityPercentage,
            reason = generateReason(bestFloor, floorAvgDuration, floorAvgDurations.isNotEmpty()),
            emoji = getEmojiForFloor(bestFloor),
            score = bestFloor.availabilityPercentage / 100f,
            userAvgDuration = userAvgDuration,
            floorAvgDuration = floorAvgDuration
        )
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun generateReason(
        floor: Floor,
        floorAvgDuration: Double,
        hasDurationData: Boolean
    ): String {
        return if (hasDurationData && floorAvgDuration > 0) {
            val hours = floorAvgDuration.toInt()
            val minutes = ((floorAvgDuration - hours) * 60).toInt()
            val durationStr = if (hours > 0) "${hours}h ${minutes}min" else "${minutes}min"
            "Floor ${floor.floorNumber} has the shortest avg stay today ($durationStr), " +
                    "with ${floor.availableSpots} available spots"
        } else {
            when {
                floor.availabilityPercentage >= 75 ->
                    "Floor ${floor.floorNumber} has excellent availability with ${floor.availableSpots} free spots"
                floor.availabilityPercentage >= 50 ->
                    "Floor ${floor.floorNumber} is a good option with ${floor.availableSpots} free spots"
                floor.availabilityPercentage >= 25 ->
                    "Floor ${floor.floorNumber} has limited availability (${floor.availableSpots} free spots)"
                else ->
                    "Floor ${floor.floorNumber} is the best available option (${floor.availableSpots} free spots)"
            }
        }
    }

    private fun getEmojiForFloor(floor: Floor): String {
        return when {
            floor.availabilityPercentage >= 75 -> "🟢"
            floor.availabilityPercentage >= 50 -> "🟡"
            floor.availabilityPercentage >= 25 -> "🟠"
            else -> "🔴"
        }
    }

    // Mantener métodos anteriores para compatibilidad
    fun getFloorRecommendation(floors: List<Floor>): FloorRecommendation? {
        if (floors.isEmpty()) return null
        val availableFloors = floors.filter { it.availableSpots > 0 }
        if (availableFloors.isEmpty()) {
            return FloorRecommendation(
                floorNumber = 0,
                availableSpots = 0,
                availabilityPercentage = 0,
                reason = "No available spots in the parking",
                emoji = "❌",
                score = 0f
            )
        }
        val bestFloor = availableFloors.maxByOrNull { it.availabilityPercentage } ?: return null
        return FloorRecommendation(
            floorNumber = bestFloor.floorNumber,
            availableSpots = bestFloor.availableSpots,
            availabilityPercentage = bestFloor.availabilityPercentage,
            reason = generateReason(bestFloor, 0.0, false),
            emoji = getEmojiForFloor(bestFloor),
            score = bestFloor.availabilityPercentage / 100f
        )
    }

    fun getRankedFloors(floors: List<Floor>): List<FloorRecommendation> {
        return floors.filter { it.availableSpots > 0 }
            .map { floor ->
                FloorRecommendation(
                    floorNumber = floor.floorNumber,
                    availableSpots = floor.availableSpots,
                    availabilityPercentage = floor.availabilityPercentage,
                    reason = generateReason(floor, 0.0, false),
                    emoji = getEmojiForFloor(floor),
                    score = floor.availabilityPercentage / 100f
                )
            }
            .sortedByDescending { it.score }
    }
}