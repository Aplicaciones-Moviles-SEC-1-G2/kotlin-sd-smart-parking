package com.example.sd_smart_parking_app.data.repository

import android.util.Log
import com.example.sd_smart_parking_app.data.model.OccupancyHistory
import com.example.sd_smart_parking_app.data.model.OccupancyPrediction
import com.example.sd_smart_parking_app.data.model.HourlyOccupancyStats
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Calendar

class OccupancyRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val occupancyCollectionPath = "parking_occupancy_history"

    /**
     * Guardar datos de ocupación en Firestore
     */
    suspend fun saveOccupancyData(
        occupancyPercentage: Float,
        availableSpots: Int,
        totalSpots: Int
    ): Result<Unit> {
        return try {
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1

            val occupancyData = OccupancyHistory(
                hour = hour,
                dayOfWeek = dayOfWeek,
                occupancyPercentage = occupancyPercentage,
                availableSpots = availableSpots,
                totalSpots = totalSpots,
                timestamp = System.currentTimeMillis()
            )

            firestore.collection(occupancyCollectionPath)
                .add(occupancyData)
                .await()

            Log.d("OccupancyRepository", "Occupancy data saved: $occupancyPercentage%")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("OccupancyRepository", "Error saving occupancy data: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Obtener estadísticas históricas de ocupación por hora
     */
    suspend fun getHourlyOccupancyStats(): Result<List<HourlyOccupancyStats>> {
        return try {
            val snapshot = firestore.collection(occupancyCollectionPath)
                .get()
                .await()

            val hourlyStats = mutableMapOf<Int, MutableList<Float>>()

            // Agrupar datos por hora
            snapshot.documents.forEach { doc ->
                val occupancyPercentage = doc.getDouble("occupancyPercentage")?.toFloat() ?: 0f
                val hour = doc.getLong("hour")?.toInt() ?: 0

                hourlyStats.getOrPut(hour) { mutableListOf() }.add(occupancyPercentage)
            }

            // Calcular estadísticas
            val stats = hourlyStats.map { (hour, percentages) ->
                HourlyOccupancyStats(
                    hour = hour,
                    avgOccupancy = percentages.average().toFloat(),
                    maxOccupancy = percentages.maxOrNull() ?: 0f,
                    minOccupancy = percentages.minOrNull() ?: 0f,
                    dataPoints = percentages.size
                )
            }.sortedBy { it.hour }

            Log.d("OccupancyRepository", "Retrieved ${stats.size} hourly stats")
            Result.success(stats)
        } catch (e: Exception) {
            Log.e("OccupancyRepository", "Error getting hourly stats: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Obtener datos históricos del último mes
     */
    suspend fun getRecentOccupancyData(days: Int = 30): Result<List<OccupancyHistory>> {
        return try {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_MONTH, -days)
            val startTime = calendar.timeInMillis

            val snapshot = firestore.collection(occupancyCollectionPath)
                .whereGreaterThan("timestamp", startTime)
                .get()
                .await()

            val data = snapshot.documents.mapNotNull { doc ->
                try {
                    OccupancyHistory(
                        hour = doc.getLong("hour")?.toInt() ?: 0,
                        dayOfWeek = doc.getLong("dayOfWeek")?.toInt() ?: 0,
                        occupancyPercentage = doc.getDouble("occupancyPercentage")?.toFloat() ?: 0f,
                        availableSpots = doc.getLong("availableSpots")?.toInt() ?: 0,
                        totalSpots = doc.getLong("totalSpots")?.toInt() ?: 0,
                        timestamp = doc.getLong("timestamp") ?: 0
                    )
                } catch (e: Exception) {
                    Log.e("OccupancyRepository", "Error parsing document", e)
                    null
                }
            }

            Log.d("OccupancyRepository", "Retrieved ${data.size} occupancy records from last $days days")
            Result.success(data)
        } catch (e: Exception) {
            Log.e("OccupancyRepository", "Error getting recent data: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Predecir ocupación para una hora específica
     */
    suspend fun predictOccupancy(targetHour: Int): Result<OccupancyPrediction> {
        return try {
            val stats = getHourlyOccupancyStats().getOrNull()
                ?: return Result.failure(Exception("No historical data available"))

            val hourStats = stats.find { it.hour == targetHour }
                ?: return Result.failure(Exception("No data for hour $targetHour"))

            // Predicción simple basada en promedio histórico
            val predictedOccupancy = hourStats.avgOccupancy

            // Calcular confianza basada en cantidad de datos (más datos = más confianza)
            val confidence = minOf(100f, (hourStats.dataPoints * 10f))

            // Determinar si está ocupado (>75%)
            val isBusy = predictedOccupancy > 75f

            // Recomendar mejor hora
            val recommendedHour = stats.minByOrNull { it.avgOccupancy }?.hour ?: 9

            val prediction = OccupancyPrediction(
                hour = targetHour,
                predictedOccupancy = predictedOccupancy,
                confidence = confidence,
                recommendedTime = String.format("%02d:00", recommendedHour),
                isBusy = isBusy
            )

            Log.d("OccupancyRepository", "Prediction for hour $targetHour: $predictedOccupancy%")
            Result.success(prediction)
        } catch (e: Exception) {
            Log.e("OccupancyRepository", "Error predicting occupancy: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Obtener predicción para la hora actual
     */
    suspend fun getCurrentHourPrediction(): Result<OccupancyPrediction> {
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return predictOccupancy(currentHour)
    }

    /**
     * Obtener predicciones para las próximas N horas
     */
    suspend fun getNextHoursPredictions(hours: Int = 24): Result<List<OccupancyPrediction>> {
        return try {
            val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val predictions = mutableListOf<OccupancyPrediction>()

            for (i in 0 until hours) {
                val targetHour = (currentHour + i) % 24
                val prediction = predictOccupancy(targetHour).getOrNull()
                if (prediction != null) {
                    predictions.add(prediction)
                }
            }

            Log.d("OccupancyRepository", "Retrieved ${predictions.size} predictions")
            Result.success(predictions)
        } catch (e: Exception) {
            Log.e("OccupancyRepository", "Error getting predictions: ${e.message}", e)
            Result.failure(e)
        }
    }
}