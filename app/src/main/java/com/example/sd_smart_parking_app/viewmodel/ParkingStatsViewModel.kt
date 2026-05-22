package com.example.sd_smart_parking_app.viewmodel

import android.content.Context
import android.util.Log
import android.util.LruCache
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sd_smart_parking_app.data.ParkingStatsDataStore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.io.File
import java.util.Calendar
import java.util.TimeZone

data class ParkingStatsUIState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val totalSessions: Int = 0,
    val totalTimeHours: Double = 0.0,
    val totalPaidCOP: Double = 0.0,
    val avgSessionHours: Double = 0.0,
    val busiestDay: String = "",
    val busiestDaySessions: Int = 0,
    val favouriteFloor: Int = 0,
    val favouriteFloorVisits: Int = 0,
    val avgDurationByDay: Map<String, Double> = emptyMap(),
    val lastSyncTimestamp: Long = 0L
)

private data class ComputedStats(
    val totalSessions: Int,
    val totalTimeHours: Double,
    val totalPaidCOP: Double,
    val avgSessionHours: Double,
    val busiestDay: String,
    val busiestDaySessions: Int,
    val favouriteFloor: Int,
    val favouriteFloorVisits: Int,
    val avgDurationByDay: Map<String, Double>
)

class ParkingStatsViewModel(private val context: Context) : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // ── Capas de Local Storage ────────────────────────────────────────────────
    // 1. LRU Cache en memoria — acceso instantáneo, sin procesador de anotaciones
    //    MAX_ENTRIES = 3: soporta hasta 3 usuarios simultáneos en dispositivo compartido
    //    TTL = 1 hora: balance entre frescura y ahorro de llamadas a Firestore
    private val lruCache = object : LruCache<String, ComputedStats>(3) {
        override fun entryRemoved(
            evicted: Boolean, key: String,
            oldValue: ComputedStats, newValue: ComputedStats?
        ) {
            if (evicted) Log.d("ParkingStatsVM", "[LRU] Entrada eviccionada — key: $key")
        }
    }
    private val lruTimestamps = mutableMapOf<String, Long>()
    private val lruTtlMs = 60 * 60 * 1000L // 1 hora

    // 2. DataStore: metadata del caché (timestamp + email)
    private val dataStore = ParkingStatsDataStore(context)

    // 3. Archivo local JSON: respaldo offline de los stats
    private val statsFile = File(context.filesDir, "parking_stats_backup.json")

    private val _uiState = MutableStateFlow(ParkingStatsUIState())
    val uiState: StateFlow<ParkingStatsUIState> = _uiState

    private val hourlyRateCOP = 3_000.0
    private val dayOrder = listOf(
        "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
    )

    init {
        loadStats()
    }

    fun loadStats() {
        val userEmail = auth.currentUser?.email ?: return

        // ── CORRUTINA 1: Main — orquestadora ─────────────────────────────────
        viewModelScope.launch {

            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            Log.d("ParkingStatsVM", "[Main] Orquestadora iniciada — hilo: ${Thread.currentThread().name}")

            // ── PASO 1: Consultar LRU Cache en memoria ────────────────────────
            val cachedStats = lruCache.get(userEmail)
            val cacheTime = lruTimestamps[userEmail] ?: 0L
            val lruValid = cachedStats != null &&
                    (System.currentTimeMillis() - cacheTime) < lruTtlMs

            if (lruValid && cachedStats != null) {
                Log.d("ParkingStatsVM", "[Main] LRU Hit — mostrando datos desde caché en memoria")
                Log.d("ParkingStatsVM", "[LRU] hits: ${lruCache.hitCount()} | misses: ${lruCache.missCount()} | size: ${lruCache.size()}/3")
                withContext(Dispatchers.Main) {
                    _uiState.value = cachedStats.toUIState(
                        isRefreshing = false,
                        lastSync = cacheTime
                    )
                }
                return@launch
            }

            Log.d("ParkingStatsVM", "[LRU] Miss — consultando fuentes persistentes")
            Log.d("ParkingStatsVM", "[LRU] hits: ${lruCache.hitCount()} | misses: ${lruCache.missCount()} | size: ${lruCache.size()}/3")

            // ── PASO 2: Leer metadata del caché (DataStore en IO) ─────────────
            val lastSync = withContext(Dispatchers.IO) {
                dataStore.lastSyncTimestamp.first()
            }
            val cachedEmail = withContext(Dispatchers.IO) {
                dataStore.cachedUserEmail.first()
            }
            val datastoreValid = dataStore.isCacheValid(lastSync) && cachedEmail == userEmail

            // Si DataStore indica caché válido, intentar leer desde archivo JSON
            if (datastoreValid) {
                val fileStats = withContext(Dispatchers.IO) {
                    readStatsFromFile(userEmail)
                }
                if (fileStats != null) {
                    Log.d("ParkingStatsVM", "[Main] DataStore válido — mostrando datos desde archivo JSON")
                    withContext(Dispatchers.Main) {
                        _uiState.value = fileStats
                    }
                    // Poblar LRU con datos del archivo para próximas consultas
                    fileStats.toComputedStats()?.let { stats ->
                        lruCache.put(userEmail, stats)
                        lruTimestamps[userEmail] = lastSync
                    }
                    return@launch
                }
            }

            // ── PASO 3: Consultar Firestore (Dispatchers.IO) ──────────────────
            try {
                val recordsDeferred = async(Dispatchers.IO) {
                    Log.d("ParkingStatsVM", "[IO] Consultando Firestore — hilo: ${Thread.currentThread().name}")

                    val snapshot = withTimeoutOrNull(5_000L) {
                        firestore.collection("vehicleRecords")
                            .whereEqualTo("ownerEmail", userEmail)
                            .whereEqualTo("type", "exit")
                            .get()
                            .await()
                    } ?: throw Exception("No internet connection — showing cached data")

                    snapshot.documents.mapNotNull { doc ->
                        try {
                            VehicleRecord(
                                id = doc.id,
                                type = doc.getString("type") ?: "",
                                floor = doc.getLong("floor")?.toInt() ?: 0,
                                spotNumber = doc.getLong("spotNumber")?.toInt() ?: 0,
                                durationHours = doc.getDouble("durationHours") ?: 0.0,
                                timestamp = doc.getTimestamp("timestamp"),
                                plate = doc.getString("plate") ?: "",
                                ownerEmail = doc.getString("ownerEmail") ?: ""
                            )
                        } catch (e: Exception) {
                            Log.e("ParkingStatsVM", "[IO] Error parseando documento", e)
                            null
                        }
                    }.also {
                        Log.d("ParkingStatsVM", "[IO] ${it.size} registros obtenidos de Firestore")
                    }
                }

                val records = recordsDeferred.await()

                if (records.isEmpty()) {
                    Log.d("ParkingStatsVM", "[Main] Sin registros en Firestore")
                    // Intentar archivo JSON como último recurso
                    val fileStats = withContext(Dispatchers.IO) { readStatsFromFile(userEmail) }
                    withContext(Dispatchers.Main) {
                        _uiState.value = fileStats ?: ParkingStatsUIState(isLoading = false)
                    }
                    return@launch
                }

                // ── PASO 4: Calcular stats (Dispatchers.Default) ──────────────
                val statsDeferred = async(Dispatchers.Default) {
                    Log.d("ParkingStatsVM", "[Default] Calculando stats — hilo: ${Thread.currentThread().name}")

                    val bogota = TimeZone.getTimeZone("America/Bogota")
                    val totalSessions   = records.size
                    val totalTimeHours  = records.sumOf { it.durationHours }
                    val totalPaidCOP    = totalTimeHours * hourlyRateCOP
                    val avgSessionHours = totalTimeHours / totalSessions

                    val sessionsByDay = records
                        .mapNotNull { record ->
                            record.timestamp?.toDate()?.let { date ->
                                val cal = Calendar.getInstance(bogota)
                                cal.time = date
                                when (cal.get(Calendar.DAY_OF_WEEK)) {
                                    Calendar.MONDAY    -> "Monday"
                                    Calendar.TUESDAY   -> "Tuesday"
                                    Calendar.WEDNESDAY -> "Wednesday"
                                    Calendar.THURSDAY  -> "Thursday"
                                    Calendar.FRIDAY    -> "Friday"
                                    Calendar.SATURDAY  -> "Saturday"
                                    Calendar.SUNDAY    -> "Sunday"
                                    else               -> null
                                }
                            }
                        }
                        .groupingBy { it }
                        .eachCount()

                    val busiestEntry       = sessionsByDay.maxByOrNull { it.value }
                    val busiestDay         = busiestEntry?.key ?: ""
                    val busiestDaySessions = busiestEntry?.value ?: 0

                    val floorCounts = records
                        .filter { it.floor > 0 }
                        .groupingBy { it.floor }
                        .eachCount()

                    val favouriteEntry       = floorCounts.maxByOrNull { it.value }
                    val favouriteFloor       = favouriteEntry?.key ?: 0
                    val favouriteFloorVisits = favouriteEntry?.value ?: 0

                    val durationsByDay = records
                        .mapNotNull { record ->
                            record.timestamp?.toDate()?.let { date ->
                                val cal = Calendar.getInstance(bogota)
                                cal.time = date
                                val dayName = when (cal.get(Calendar.DAY_OF_WEEK)) {
                                    Calendar.MONDAY    -> "Monday"
                                    Calendar.TUESDAY   -> "Tuesday"
                                    Calendar.WEDNESDAY -> "Wednesday"
                                    Calendar.THURSDAY  -> "Thursday"
                                    Calendar.FRIDAY    -> "Friday"
                                    Calendar.SATURDAY  -> "Saturday"
                                    Calendar.SUNDAY    -> "Sunday"
                                    else               -> null
                                }
                                dayName?.let { it to record.durationHours }
                            }
                        }
                        .groupBy({ it.first }, { it.second })

                    val avgDurationByDay = dayOrder
                        .filter { durationsByDay.containsKey(it) }
                        .associateWith { day ->
                            val list = durationsByDay[day] ?: emptyList()
                            if (list.isEmpty()) 0.0 else list.average()
                        }

                    Log.d("ParkingStatsVM", "[Default] Cálculo completado — $totalSessions sesiones")

                    ComputedStats(
                        totalSessions        = totalSessions,
                        totalTimeHours       = totalTimeHours,
                        totalPaidCOP         = totalPaidCOP,
                        avgSessionHours      = avgSessionHours,
                        busiestDay           = busiestDay,
                        busiestDaySessions   = busiestDaySessions,
                        favouriteFloor       = favouriteFloor,
                        favouriteFloorVisits = favouriteFloorVisits,
                        avgDurationByDay     = avgDurationByDay
                    )
                }

                val stats = statsDeferred.await()
                val now = System.currentTimeMillis()

                // ── PASO 5: Persistir en las capas de storage (IO) ────────────
                async(Dispatchers.IO) {
                    Log.d("ParkingStatsVM", "[IO] Persistiendo en DataStore + archivo JSON")

                    // 1. DataStore — metadata del caché
                    dataStore.saveLastSyncTimestamp(now)
                    dataStore.saveCachedUserEmail(userEmail)
                    Log.d("ParkingStatsVM", "[IO] DataStore actualizado — timestamp: $now")

                    // 2. Archivo local JSON — respaldo offline
                    writeStatsToFile(stats, userEmail, now)
                    Log.d("ParkingStatsVM", "[IO] Archivo JSON actualizado: ${statsFile.absolutePath}")
                }.await()

                // 3. LRU Cache — acceso instantáneo para próxima apertura
                lruCache.put(userEmail, stats)
                lruTimestamps[userEmail] = now
                Log.d("ParkingStatsVM", "[LRU] Stats guardados — hits: ${lruCache.hitCount()} | misses: ${lruCache.missCount()} | size: ${lruCache.size()}/3")

                // ── PASO 6: Actualizar UI en Main ─────────────────────────────
                withContext(Dispatchers.Main) {
                    Log.d("ParkingStatsVM", "[Main] Actualizando UI con stats frescos de Firestore")
                    _uiState.value = stats.toUIState(isRefreshing = false, lastSync = now)
                }

            } catch (e: Exception) {
                Log.e("ParkingStatsVM", "[Main] Error consultando Firestore: ${e.message}", e)

                // Intentar archivo JSON como fallback offline
                val fileStats = withContext(Dispatchers.IO) { readStatsFromFile(userEmail) }
                withContext(Dispatchers.Main) {
                    if (fileStats != null) {
                        Log.d("ParkingStatsVM", "[Main] Mostrando datos desde archivo JSON (offline)")
                        _uiState.value = fileStats
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading    = false,
                            isRefreshing = false,
                            error        = e.message
                        )
                    }
                }
            }
        }
    }

    private fun writeStatsToFile(stats: ComputedStats, email: String, timestamp: Long) {
        try {
            val json = JSONObject().apply {
                put("ownerEmail", email)
                put("cachedAt", timestamp)
                put("totalSessions", stats.totalSessions)
                put("totalTimeHours", stats.totalTimeHours)
                put("totalPaidCOP", stats.totalPaidCOP)
                put("avgSessionHours", stats.avgSessionHours)
                put("busiestDay", stats.busiestDay)
                put("busiestDaySessions", stats.busiestDaySessions)
                put("favouriteFloor", stats.favouriteFloor)
                put("favouriteFloorVisits", stats.favouriteFloorVisits)
                val dayMap = JSONObject()
                stats.avgDurationByDay.forEach { (day, hours) -> dayMap.put(day, hours) }
                put("avgDurationByDay", dayMap)
            }
            statsFile.writeText(json.toString())
        } catch (e: Exception) {
            Log.e("ParkingStatsVM", "[IO] Error escribiendo archivo JSON", e)
        }
    }

    private fun readStatsFromFile(userEmail: String): ParkingStatsUIState? {
        return try {
            if (!statsFile.exists()) return null
            val json = JSONObject(statsFile.readText())
            if (json.getString("ownerEmail") != userEmail) return null

            val dayMapJson = json.getJSONObject("avgDurationByDay")
            val avgDurationByDay = dayOrder
                .filter { dayMapJson.has(it) }
                .associateWith { dayMapJson.getDouble(it) }

            ParkingStatsUIState(
                isLoading            = false,
                totalSessions        = json.getInt("totalSessions"),
                totalTimeHours       = json.getDouble("totalTimeHours"),
                totalPaidCOP         = json.getDouble("totalPaidCOP"),
                avgSessionHours      = json.getDouble("avgSessionHours"),
                busiestDay           = json.getString("busiestDay"),
                busiestDaySessions   = json.getInt("busiestDaySessions"),
                favouriteFloor       = json.getInt("favouriteFloor"),
                favouriteFloorVisits = json.getInt("favouriteFloorVisits"),
                avgDurationByDay     = avgDurationByDay,
                lastSyncTimestamp    = json.getLong("cachedAt")
            )
        } catch (e: Exception) {
            Log.e("ParkingStatsVM", "[IO] Error leyendo archivo JSON", e)
            null
        }
    }

    fun formatTotalTime(hours: Double): String {
        val h = hours.toInt()
        val m = ((hours - h) * 60).toInt()
        return when {
            h > 0 && m > 0 -> "${h}h ${m}m"
            h > 0           -> "${h}h"
            else            -> "${m}m"
        }
    }

    fun formatCOP(amount: Double): String {
        return when {
            amount >= 1_000_000 -> "$${(amount / 1_000_000).let { if (it % 1 == 0.0) it.toInt().toString() else String.format("%.1f", it) }}M"
            amount >= 1_000     -> "$${(amount / 1_000).let { if (it % 1 == 0.0) it.toInt().toString() else String.format("%.1f", it) }}K"
            else                -> "$${amount.toInt()}"
        }
    }

    fun formatAvgDuration(hours: Double): String {
        val h = hours.toInt()
        val m = ((hours - h) * 60).toInt()
        return when {
            h > 0 && m > 0 -> "${h}h ${m}m"
            h > 0           -> "${h}h"
            else            -> "${m}m"
        }
    }

    fun shortDay(day: String): String = day.take(3)

    fun maxAvgDuration(map: Map<String, Double>): Double =
        map.values.maxOrNull()?.takeIf { it > 0 } ?: 1.0
}

private fun ComputedStats.toUIState(isRefreshing: Boolean, lastSync: Long) = ParkingStatsUIState(
    isLoading            = false,
    isRefreshing         = isRefreshing,
    totalSessions        = totalSessions,
    totalTimeHours       = totalTimeHours,
    totalPaidCOP         = totalPaidCOP,
    avgSessionHours      = avgSessionHours,
    busiestDay           = busiestDay,
    busiestDaySessions   = busiestDaySessions,
    favouriteFloor       = favouriteFloor,
    favouriteFloorVisits = favouriteFloorVisits,
    avgDurationByDay     = avgDurationByDay,
    lastSyncTimestamp    = lastSync
)

private fun ParkingStatsUIState.toComputedStats(): ComputedStats? {
    if (totalSessions == 0) return null
    return ComputedStats(
        totalSessions        = totalSessions,
        totalTimeHours       = totalTimeHours,
        totalPaidCOP         = totalPaidCOP,
        avgSessionHours      = avgSessionHours,
        busiestDay           = busiestDay,
        busiestDaySessions   = busiestDaySessions,
        favouriteFloor       = favouriteFloor,
        favouriteFloorVisits = favouriteFloorVisits,
        avgDurationByDay     = avgDurationByDay
    )
}