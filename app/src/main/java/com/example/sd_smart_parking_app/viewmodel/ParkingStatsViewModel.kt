package com.example.sd_smart_parking_app.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sd_smart_parking_app.data.ParkingStatsDatabase
import com.example.sd_smart_parking_app.data.ParkingStatsDataStore
import com.example.sd_smart_parking_app.data.model.ParkingStatsEntity
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

// Modelo de resultado de cálculo — separa responsabilidades entre IO y Default
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
    // 1. Room: BD relacional para los stats calculados
    private val db = ParkingStatsDatabase.getInstance(context)
    private val dao = db.parkingStatsDao()

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
        // Coordina las 3 capas de local storage + Firestore.
        // Decisión: viewModelScope garantiza cancelación automática al destruir
        // el ViewModel, evitando memory leaks.
        viewModelScope.launch {

            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            Log.d("ParkingStatsVM", "[Main] Orquestadora iniciada — hilo: ${Thread.currentThread().name}")

            // ── PASO 1: Leer caché de Room (Dispatchers.IO) ───────────────────
            // Intentamos mostrar datos locales inmediatamente mientras
            // decidimos si necesitamos ir a Firestore.
            val cachedEntity = async(Dispatchers.IO) {
                Log.d("ParkingStatsVM", "[IO] Leyendo Room — hilo: ${Thread.currentThread().name}")
                dao.getStatsByEmail(userEmail)
            }.await()

            // ── PASO 2: Leer metadata del caché (DataStore) ───────────────────
            val lastSync = dataStore.lastSyncTimestamp.first()
            val cachedEmail = dataStore.cachedUserEmail.first()
            val cacheValid = dataStore.isCacheValid(lastSync) && cachedEmail == userEmail

            Log.d("ParkingStatsVM", "[Main] Caché válido: $cacheValid | lastSync: $lastSync | email match: ${cachedEmail == userEmail}")

            // Si hay caché de Room para este usuario, mostrarlo inmediatamente
            if (cachedEntity != null && cachedEmail == userEmail) {
                Log.d("ParkingStatsVM", "[Main] Mostrando datos desde Room")
                withContext(Dispatchers.Main) {
                    _uiState.value = cachedEntity.toUIState(isRefreshing = !cacheValid)
                }
            }

            // Si el caché es válido y los datos ya están en pantalla, no vamos a Firestore
            if (cacheValid && cachedEntity != null) {
                Log.d("ParkingStatsVM", "[Main] Caché vigente — omitiendo Firestore")
                _uiState.value = _uiState.value.copy(isLoading = false, isRefreshing = false)
                return@launch
            }

            // ── PASO 3: Consultar Firestore (Dispatchers.IO) ──────────────────
            // Solo llegamos aquí si el caché expiró o no existe.
            try {
                val recordsDeferred = async(Dispatchers.IO) {
                    Log.d("ParkingStatsVM", "[IO] Consultando Firestore — hilo: ${Thread.currentThread().name}")

                    val snapshot = firestore.collection("vehicleRecords")
                        .whereEqualTo("ownerEmail", userEmail)
                        .whereEqualTo("type", "exit")
                        .get()
                        .await()

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
                    withContext(Dispatchers.Main) {
                        _uiState.value = ParkingStatsUIState(isLoading = false)
                    }
                    return@launch
                }

                // ── PASO 4: Calcular stats (Dispatchers.Default) ──────────────
                // Operaciones CPU-intensivas en el pool de cómputo.
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

                // ── PASO 5: Persistir en las 3 capas de storage (IO) ──────────
                async(Dispatchers.IO) {
                    Log.d("ParkingStatsVM", "[IO] Persistiendo en Room + DataStore + archivo")

                    // 1. Room — BD relacional
                    val entity = ParkingStatsEntity(
                        ownerEmail           = userEmail,
                        totalSessions        = stats.totalSessions,
                        totalTimeHours       = stats.totalTimeHours,
                        totalPaidCOP         = stats.totalPaidCOP,
                        avgSessionHours      = stats.avgSessionHours,
                        busiestDay           = stats.busiestDay,
                        busiestDaySessions   = stats.busiestDaySessions,
                        favouriteFloor       = stats.favouriteFloor,
                        favouriteFloorVisits = stats.favouriteFloorVisits,
                        avgDurationByDay     = stats.avgDurationByDay,
                        cachedAt             = now
                    )
                    dao.insertOrReplace(entity)
                    Log.d("ParkingStatsVM", "[IO] Room actualizado")

                    // 2. DataStore — metadata del caché
                    dataStore.saveLastSyncTimestamp(now)
                    dataStore.saveCachedUserEmail(userEmail)
                    Log.d("ParkingStatsVM", "[IO] DataStore actualizado — timestamp: $now")

                    // 3. Archivo local JSON — respaldo offline
                    writeStatsToFile(stats, userEmail, now)
                    Log.d("ParkingStatsVM", "[IO] Archivo JSON actualizado: ${statsFile.absolutePath}")

                }.await()

                // ── PASO 6: Actualizar UI en Main ─────────────────────────────
                withContext(Dispatchers.Main) {
                    Log.d("ParkingStatsVM", "[Main] Actualizando UI con stats frescos de Firestore")
                    _uiState.value = ParkingStatsUIState(
                        isLoading            = false,
                        isRefreshing         = false,
                        totalSessions        = stats.totalSessions,
                        totalTimeHours       = stats.totalTimeHours,
                        totalPaidCOP         = stats.totalPaidCOP,
                        avgSessionHours      = stats.avgSessionHours,
                        busiestDay           = stats.busiestDay,
                        busiestDaySessions   = stats.busiestDaySessions,
                        favouriteFloor       = stats.favouriteFloor,
                        favouriteFloorVisits = stats.favouriteFloorVisits,
                        avgDurationByDay     = stats.avgDurationByDay,
                        lastSyncTimestamp    = now
                    )
                }

            } catch (e: Exception) {
                Log.e("ParkingStatsVM", "[Main] Error consultando Firestore: ${e.message}", e)

                // Si Firestore falla pero tenemos caché, no mostramos error
                if (cachedEntity != null) {
                    Log.d("ParkingStatsVM", "[Main] Firestore falló — manteniendo datos de Room")
                    withContext(Dispatchers.Main) {
                        _uiState.value = cachedEntity.toUIState(isRefreshing = false)
                    }
                } else {
                    // Sin caché y sin red — intentar leer archivo JSON
                    val fileStats = readStatsFromFile(userEmail)
                    withContext(Dispatchers.Main) {
                        if (fileStats != null) {
                            Log.d("ParkingStatsVM", "[Main] Mostrando datos desde archivo JSON")
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
    }

    // ── Escribir stats a archivo JSON local ───────────────────────────────────
    // Dispatchers.IO — operación de disco
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

    // ── Leer stats desde archivo JSON local ───────────────────────────────────
    // Último recurso: sin Room y sin Firestore
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

    // ── Helpers de formato ────────────────────────────────────────────────────

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

// ── Extension: ParkingStatsEntity → ParkingStatsUIState ──────────────────────
// Convierte la entidad de Room al estado de UI sin pasar por el ViewModel.
// Decisión: extension function en lugar de método en la entidad para respetar
// la separación entre capa de datos y capa de UI del patrón MVVM.
private fun ParkingStatsEntity.toUIState(isRefreshing: Boolean = false) = ParkingStatsUIState(
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
    lastSyncTimestamp    = cachedAt
)