package com.example.sd_smart_parking_app.data.repository

import android.util.Log
import android.util.LruCache
import com.example.sd_smart_parking_app.BuildConfig
import com.example.sd_smart_parking_app.data.model.OccupancyHistory
import com.example.sd_smart_parking_app.data.model.OccupancyPrediction
import com.example.sd_smart_parking_app.data.model.HourlyOccupancyStats
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.concurrent.TimeUnit

class OccupancyRepository private constructor() {

    private val firestore = FirebaseFirestore.getInstance()
    private val occupancyCollectionPath = "parking_occupancy_history"
    private val vehicleRecordsPath = "vehicleRecords"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // -------------------------------------------------------------------------
    // LruCache de predicciones
    // maxSize = 24 (una entrada por cada hora del día como máximo)
    // Clave: hora del día (0-23)
    // Valor: Par de (OccupancyPrediction, timestamp de cuando fue cacheada)
    // -------------------------------------------------------------------------
    private val predictionCache = LruCache<Int, Pair<OccupancyPrediction, Long>>(24)
    private val cacheDurationMs = 30 * 60 * 1000L // 30 minutos

    private fun isCacheValid(targetHour: Int): Boolean {
        val cached = predictionCache.get(targetHour) ?: return false
        val now = System.currentTimeMillis()
        val isNotExpired = (now - cached.second) < cacheDurationMs
        return isNotExpired
    }

    private fun getCachedPrediction(targetHour: Int): OccupancyPrediction? {
        return predictionCache.get(targetHour)?.first
    }

    private fun savePredictionToCache(targetHour: Int, prediction: OccupancyPrediction) {
        predictionCache.put(targetHour, Pair(prediction, System.currentTimeMillis()))
        Log.d("OccupancyRepository", "Cache updated for hour $targetHour — cache size: ${predictionCache.size()}/${predictionCache.maxSize()}")
    }

    // -------------------------------------------------------------------------
    // Singleton
    // -------------------------------------------------------------------------
    companion object {
        @Volatile
        private var INSTANCE: OccupancyRepository? = null

        fun getInstance(): OccupancyRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = OccupancyRepository()
                INSTANCE = instance
                instance
            }
        }
    }

    // -------------------------------------------------------------------------
    // Guardar datos de ocupación
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // Obtener estadísticas históricas por hora
    // -------------------------------------------------------------------------

    suspend fun getHourlyOccupancyStats(): Result<List<HourlyOccupancyStats>> {
        return try {
            val snapshot = firestore.collection(occupancyCollectionPath)
                .get()
                .await()

            val hourlyStats = mutableMapOf<Int, MutableList<Float>>()

            snapshot.documents.forEach { doc ->
                val occupancyPercentage = doc.getDouble("occupancyPercentage")?.toFloat() ?: 0f
                val hour = doc.getLong("hour")?.toInt() ?: 0
                hourlyStats.getOrPut(hour) { mutableListOf() }.add(occupancyPercentage)
            }

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

    // -------------------------------------------------------------------------
    // Obtener datos recientes de ocupación
    // -------------------------------------------------------------------------

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

            Log.d("OccupancyRepository", "Retrieved ${data.size} records from last $days days")
            Result.success(data)
        } catch (e: Exception) {
            Log.e("OccupancyRepository", "Error getting recent data: ${e.message}", e)
            Result.failure(e)
        }
    }

    // -------------------------------------------------------------------------
    // Obtener horas más comunes de llegada desde vehicleRecords
    // -------------------------------------------------------------------------

    private suspend fun getVehicleArrivalStats(): Map<Int, Int> {
        return try {
            val snapshot = firestore.collection(vehicleRecordsPath)
                .whereEqualTo("type", "entry")
                .get()
                .await()

            val hourCounts = mutableMapOf<Int, Int>()

            snapshot.documents.forEach { doc ->
                val timestamp = doc.getTimestamp("timestamp")
                if (timestamp != null) {
                    val cal = Calendar.getInstance()
                    cal.time = timestamp.toDate()
                    val hour = cal.get(Calendar.HOUR_OF_DAY)
                    hourCounts[hour] = (hourCounts[hour] ?: 0) + 1
                }
            }

            Log.d("OccupancyRepository", "Vehicle arrival stats: $hourCounts")
            hourCounts
        } catch (e: Exception) {
            Log.e("OccupancyRepository", "Error getting vehicle arrival stats: ${e.message}", e)
            emptyMap()
        }
    }

    // -------------------------------------------------------------------------
    // Predicción con IA (Claude)
    // -------------------------------------------------------------------------

    suspend fun predictWithAI(targetHour: Int): Result<OccupancyPrediction> {
        return try {

            // Verificar si hay caché válido en el LruCache
            if (isCacheValid(targetHour)) {
                val cached = getCachedPrediction(targetHour)!!
                Log.d("OccupancyRepository", "Cache hit — returning LruCache prediction for hour $targetHour")
                return Result.success(cached)
            }

            // -------------------------------------------------------------------------
            // MULTITHREADING: Dos corrutinas paralelas en Dispatchers.IO
            // Corrutina 1: obtiene datos recientes de ocupación (parking_occupancy_history)
            // Corrutina 2: obtiene estadísticas de llegada de vehículos (vehicleRecords)
            // Ambas corren simultáneamente en hilos separados del pool de IO
            // -------------------------------------------------------------------------
            val recentData: List<OccupancyHistory>
            val hourlyStats: List<HourlyOccupancyStats>
            val arrivalStats: Map<Int, Int>

            withContext(Dispatchers.IO) {
                Log.d("OccupancyRepository", "Starting parallel Firebase queries on thread: ${Thread.currentThread().name}")

                kotlinx.coroutines.coroutineScope {
                    // Corrutina anidada 1 — consulta parking_occupancy_history en Dispatchers.IO
                    val recentDataJob = async(Dispatchers.IO) {
                        Log.d("OccupancyRepository", "Coroutine 1 (recentData) running on thread: ${Thread.currentThread().name}")
                        getRecentOccupancyData(30).getOrNull() ?: emptyList<OccupancyHistory>()
                    }

                    // Corrutina anidada 2 — consulta vehicleRecords en Dispatchers.IO
                    val arrivalStatsJob = async(Dispatchers.IO) {
                        Log.d("OccupancyRepository", "Coroutine 2 (arrivalStats) running on thread: ${Thread.currentThread().name}")
                        getVehicleArrivalStats()
                    }

                    // Corrutina anidada 3 — consulta hourly stats en Dispatchers.IO
                    val hourlyStatsJob = async(Dispatchers.IO) {
                        Log.d("OccupancyRepository", "Coroutine 3 (hourlyStats) running on thread: ${Thread.currentThread().name}")
                        getHourlyOccupancyStats().getOrNull() ?: emptyList<HourlyOccupancyStats>()
                    }

                    Log.d("OccupancyRepository", "All parallel Firebase queries completed")

                    recentData = recentDataJob.await()
                    @Suppress("UNCHECKED_CAST")
                    arrivalStats = arrivalStatsJob.await() as Map<Int, Int>
                    hourlyStats = hourlyStatsJob.await()
                }
            }

            if (recentData.isEmpty() && hourlyStats.isEmpty()) {
                return Result.failure(Exception("No hay datos históricos disponibles para la predicción"))
            }

            val calendar = Calendar.getInstance()
            val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1
            val dayName = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")[dayOfWeek]

            val occupancySummary = buildString {
                appendLine("Historical occupancy by hour (last 30 days averages):")
                hourlyStats.forEach { stat ->
                    appendLine("  Hour ${String.format("%02d", stat.hour)}:00 → avg ${stat.avgOccupancy.toInt()}%, max ${stat.maxOccupancy.toInt()}%, min ${stat.minOccupancy.toInt()}% (${stat.dataPoints} data points)")
                }
            }

            val arrivalSummary = buildString {
                if (arrivalStats.isNotEmpty()) {
                    appendLine("Vehicle arrival frequency by hour (total entries recorded):")
                    arrivalStats.entries.sortedBy { it.key }.forEach { (hour, count) ->
                        appendLine("  Hour ${String.format("%02d", hour)}:00 → $count arrivals")
                    }
                    val peakHour = arrivalStats.maxByOrNull { it.value }
                    if (peakHour != null) {
                        appendLine("  Peak arrival hour: ${String.format("%02d", peakHour.key)}:00 (${peakHour.value} arrivals)")
                    }
                } else {
                    appendLine("No vehicle arrival data available.")
                }
            }

            val recentSummary = buildString {
                if (recentData.isNotEmpty()) {
                    appendLine("Last 5 recent occupancy records:")
                    recentData.takeLast(5).forEach { record ->
                        val recordDay = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")[record.dayOfWeek]
                        appendLine("  $recordDay ${String.format("%02d", record.hour)}:00 → ${record.occupancyPercentage.toInt()}% occupancy (${record.availableSpots}/${record.totalSpots} available)")
                    }
                }
            }

            val futureHours = (1..12).map { (currentHour + it) % 24 }
            val futureHoursStr = futureHours.joinToString(", ") { "${String.format("%02d", it)}:00" }

            val prompt = """
            You are an AI assistant for a smart parking app at a university. Analyze the data and predict parking conditions.

            Current context:
            - Current day: $dayName
            - Current hour: ${String.format("%02d", currentHour)}:00
            - Target hour for prediction: ${String.format("%02d", targetHour)}:00
            - Available future hours to recommend (next 12 hours only): $futureHoursStr

            $occupancySummary

            $arrivalSummary

            $recentSummary

            Based on this data:
            1. Predict the AVAILABILITY percentage (available spots / total spots) for hour ${String.format("%02d", targetHour)}:00. Higher value means more spots available.
            2. Recommend the BEST hour to park from the future hours list only ($futureHoursStr). Choose the hour with the HIGHEST expected availability that is NOT a peak arrival hour. If all future hours have similar availability, pick the earliest one.

            Respond ONLY with a valid JSON object, no explanation, no markdown, no extra text:
            {
              "predictedOccupancy": <number 0-100, represents AVAILABILITY percentage, higher is better>,
              "confidence": <number 0-100>,
              "isBusy": <true if predictedOccupancy < 30>,
              "recommendedTime": "<HH:00 format, chosen from future hours only>",
              "reasoning": "<one short sentence explaining both the prediction and the recommendation>"
            }
        """.trimIndent()

            val requestBody = JSONObject().apply {
                put("model", "claude-sonnet-4-20250514")
                put("max_tokens", 300)
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    })
                })
            }.toString()

            val request = Request.Builder()
                .url("https://api.anthropic.com/v1/messages")
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .header("x-api-key", BuildConfig.ANTHROPIC_API_KEY)
                .header("anthropic-version", "2023-06-01")
                .header("content-type", "application/json")
                .build()

            // -------------------------------------------------------------------------
            // MULTITHREADING: Llamada HTTP a Claude API en Dispatchers.IO
            // y actualización del estado en Dispatchers.Main
            // -------------------------------------------------------------------------
            val prediction: OccupancyPrediction = withContext(Dispatchers.IO) {
                Log.d("OccupancyRepository", "Calling Claude API on thread: ${Thread.currentThread().name}")
                val response = httpClient.newCall(request).execute()
                val responseBody = response.body?.string()
                    ?: throw Exception("Empty response from Claude API")

                if (!response.isSuccessful) {
                    Log.e("OccupancyRepository", "Claude API error: $responseBody")
                    throw Exception("Claude API error: ${response.code}")
                }

                val responseJson = JSONObject(responseBody)
                val content = responseJson
                    .getJSONArray("content")
                    .getJSONObject(0)
                    .getString("text")
                    .trim()

                Log.d("OccupancyRepository", "Claude response: $content")

                val predictionJson = JSONObject(content)
                OccupancyPrediction(
                    hour = targetHour,
                    predictedOccupancy = predictionJson.getDouble("predictedOccupancy").toFloat(),
                    confidence = predictionJson.getDouble("confidence").toFloat(),
                    isBusy = predictionJson.getBoolean("isBusy"),
                    recommendedTime = predictionJson.getString("recommendedTime"),
                    reasoning = predictionJson.optString("reasoning", "")
                )
            }

            // Guardar en caché y actualizar UI en Dispatchers.Main
            withContext(Dispatchers.Main) {
                Log.d("OccupancyRepository", "Updating cache on thread: ${Thread.currentThread().name}")
                savePredictionToCache(targetHour, prediction)
                Log.d("OccupancyRepository", "AI Prediction for hour $targetHour: ${prediction.predictedOccupancy}%")
            }

            Result.success(prediction)

        } catch (e: Exception) {
            Log.e("OccupancyRepository", "Error in AI prediction: ${e.message}", e)
            Result.failure(e)
        }
    }

    // -------------------------------------------------------------------------
    // Predicción hora actual y próximas horas
    // -------------------------------------------------------------------------

    suspend fun getCurrentHourPrediction(): Result<OccupancyPrediction> {
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return predictWithAI(currentHour)
    }

    suspend fun getNextHoursPredictions(hours: Int = 24): Result<List<OccupancyPrediction>> {
        return try {
            val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val predictions = mutableListOf<OccupancyPrediction>()

            for (i in 0 until hours) {
                val targetHour = (currentHour + i) % 24
                val prediction = predictWithAI(targetHour).getOrNull()
                if (prediction != null) {
                    predictions.add(prediction)
                }
            }

            Log.d("OccupancyRepository", "Retrieved ${predictions.size} AI predictions")
            Result.success(predictions)
        } catch (e: Exception) {
            Log.e("OccupancyRepository", "Error getting predictions: ${e.message}", e)
            Result.failure(e)
        }
    }
}