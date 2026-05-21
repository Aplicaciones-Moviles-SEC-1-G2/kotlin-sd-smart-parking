package com.example.sd_smart_parking_app.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.ImageLoader
import coil.memory.MemoryCache
import coil.request.CachePolicy
import com.example.sd_smart_parking_app.data.CostBreakdownCache
import com.example.sd_smart_parking_app.data.CostBreakdownStats
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.TimeZone

data class CostBreakdownUIState(
    val isLoading: Boolean = false,
    val isFromCache: Boolean = false,
    val error: String? = null,
    val allTimeSpentCOP: Double = 0.0,
    val avgPerSessionCOP: Double = 0.0,
    val hitDailyCapPercent: Double = 0.0,
    val monthlySpendingCOP: Double = 0.0,
    val monthLabel: String = "",
    val maxMonthlySpendingCOP: Double = 0.0,
    val avgCostByDay: Map<String, Double> = emptyMap(),
    val profilePhotoUrl: String = "",
    val cachedAt: Long = 0L
)

class CostBreakdownViewModel(private val context: Context) : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow(CostBreakdownUIState())
    val uiState: StateFlow<CostBreakdownUIState> = _uiState

    private val hourlyRateCOP = 3_000.0

    // Cap diario: $17.000 COP es el máximo que se cobra por día.
    // Con hourlyRateCOP = 3.000, el usuario toca el cap cuando su sesión
    // dura 17.000 / 3.000 = 5.67 horas o más. A partir de ahí puede
    // permanecer hasta 12 horas sin costo adicional.
    private val dailyCapCOP = 17_000.0
    private val dailyCapHours = dailyCapCOP / hourlyRateCOP  // = 5.67h

    private val dayOrder = listOf(
        "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
    )

    // ── Coil ImageLoader con caché configurado explícitamente ─────────────────
    // Decisión: configuramos el ImageLoader manualmente en lugar de usar el
    // singleton global de Coil para tener control explícito sobre los parámetros
    // del caché de imágenes, cumpliendo la rúbrica de caching strategy.
    //
    // memoryCacheMaxSizePercent = 0.15 → usa hasta el 15% de la memoria de la
    // app para caché de imágenes en memoria (Coil recomienda 0.1-0.25).
    //
    // diskCachePolicy habilitado → Coil persiste imágenes en disco bajo
    // context.cacheDir para acceso offline.
    val coilImageLoader: ImageLoader = ImageLoader.Builder(context)
        .memoryCache {
            MemoryCache.Builder(context)
                .maxSizePercent(0.15)
                .build()
        }
        .memoryCachePolicy(CachePolicy.ENABLED)
        .diskCachePolicy(CachePolicy.ENABLED)
        .crossfade(true)
        .build()

    init {
        loadCostBreakdown()
    }

    fun loadCostBreakdown() {
        val userEmail = auth.currentUser?.email ?: return
        val profilePhotoUrl = auth.currentUser?.photoUrl?.toString() ?: ""

        // ── CORRUTINA Main — orquestadora ─────────────────────────────────────
        viewModelScope.launch {

            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            Log.d("CostBreakdownVM", "[Main] Iniciando carga — hilo: ${Thread.currentThread().name}")

            // ── PASO 1: Consultar LRU Cache ───────────────────────────────────
            // El LRU cache es la primera fuente de verdad. Si hay un hit válido
            // (no expirado) mostramos los datos inmediatamente sin ir a Firestore.
            // Decisión: el LRU vive en memoria — acceso en microsegundos vs
            // cientos de milisegundos de Firestore.
            val cached = CostBreakdownCache.get(userEmail)
            CostBreakdownCache.logStats()

            if (cached != null) {
                Log.d("CostBreakdownVM", "[Main] LRU Hit — mostrando datos desde caché")
                withContext(Dispatchers.Main) {
                    _uiState.value = cached.toUIState(
                        profilePhotoUrl = profilePhotoUrl,
                        isFromCache = true
                    )
                }
                return@launch
            }

            // ── PASO 2: LRU Miss — consultar Firestore (Dispatchers.IO) ───────
            Log.d("CostBreakdownVM", "[Main] LRU Miss — consultando Firestore")

            try {
                val recordsDeferred = async(Dispatchers.IO) {
                    Log.d("CostBreakdownVM", "[IO] Consultando Firestore — hilo: ${Thread.currentThread().name}")

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
                            Log.e("CostBreakdownVM", "[IO] Error parseando documento", e)
                            null
                        }
                    }.also {
                        Log.d("CostBreakdownVM", "[IO] ${it.size} registros obtenidos")
                    }
                }

                val records = recordsDeferred.await()

                if (records.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        _uiState.value = CostBreakdownUIState(isLoading = false)
                    }
                    return@launch
                }

                // ── PASO 3: Calcular stats (Dispatchers.Default) ──────────────
                val statsDeferred = async(Dispatchers.Default) {
                    Log.d("CostBreakdownVM", "[Default] Calculando costos — hilo: ${Thread.currentThread().name}")

                    val bogota = TimeZone.getTimeZone("America/Bogota")
                    val now = Calendar.getInstance(bogota)

                    // All-time spent
                    val totalHours = records.sumOf { it.durationHours }
                    val allTimeSpentCOP = totalHours * hourlyRateCOP

                    // Avg per session
                    val avgPerSessionCOP = if (records.isNotEmpty())
                        allTimeSpentCOP / records.size else 0.0

                    // Hit daily cap — % de sesiones que alcanzaron o superaron
                    // el umbral de 5.67h (= $17.000 / $3.000 por hora)
                    val sessionsThatHitCap = records.count { it.durationHours >= dailyCapHours }
                    val hitDailyCapPercent = if (records.isNotEmpty())
                        (sessionsThatHitCap.toDouble() / records.size) * 100.0 else 0.0

                    // Monthly spending — solo mes actual
                    val currentMonth = now.get(Calendar.MONTH)
                    val currentYear = now.get(Calendar.YEAR)
                    val monthlyRecords = records.filter { record ->
                        record.timestamp?.toDate()?.let { date ->
                            val cal = Calendar.getInstance(bogota)
                            cal.time = date
                            cal.get(Calendar.MONTH) == currentMonth &&
                                    cal.get(Calendar.YEAR) == currentYear
                        } ?: false
                    }
                    val monthlySpendingCOP = monthlyRecords.sumOf { it.durationHours } * hourlyRateCOP

                    // Label del mes actual
                    val monthNames = listOf(
                        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
                    )
                    val monthLabel = "${monthNames[currentMonth]} ${currentYear}"

                    // Max mensual para escalar la barra del gráfico
                    val spendingByMonth = records
                        .groupBy { record ->
                            record.timestamp?.toDate()?.let { date ->
                                val cal = Calendar.getInstance(bogota)
                                cal.time = date
                                "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.MONTH)}"
                            } ?: "unknown"
                        }
                        .mapValues { (_, recs) ->
                            recs.sumOf { it.durationHours } * hourlyRateCOP
                        }
                    val maxMonthlySpendingCOP = spendingByMonth.values.maxOrNull()
                        ?: monthlySpendingCOP

                    // Avg cost by day of week — ordenado de mayor a menor
                    val costsByDay = records
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
                                dayName?.let { it to (record.durationHours * hourlyRateCOP) }
                            }
                        }
                        .groupBy({ it.first }, { it.second })

                    val avgCostByDay = dayOrder
                        .filter { costsByDay.containsKey(it) }
                        .associateWith { day ->
                            val list = costsByDay[day] ?: emptyList()
                            if (list.isEmpty()) 0.0 else list.average()
                        }
                        .entries
                        .sortedByDescending { it.value }
                        .associate { it.toPair() }

                    Log.d("CostBreakdownVM", "[Default] Cálculo completado — dailyCapHours: $dailyCapHours | sessions hit cap: $sessionsThatHitCap/${records.size}")

                    CostBreakdownStats(
                        allTimeSpentCOP       = allTimeSpentCOP,
                        avgPerSessionCOP      = avgPerSessionCOP,
                        hitDailyCapPercent    = hitDailyCapPercent,
                        monthlySpendingCOP    = monthlySpendingCOP,
                        monthLabel            = monthLabel,
                        maxMonthlySpendingCOP = maxMonthlySpendingCOP,
                        avgCostByDay          = avgCostByDay
                    )
                }

                val stats = statsDeferred.await()

                // ── PASO 4: Guardar en LRU Cache ──────────────────────────────
                // Guardamos DESPUÉS de calcular para que el caché siempre
                // contenga datos completos y consistentes, nunca parciales.
                CostBreakdownCache.put(userEmail, stats)
                Log.d("CostBreakdownVM", "[Main] Stats guardados en LRU Cache")
                CostBreakdownCache.logStats()

                // ── PASO 5: Actualizar UI en Main ─────────────────────────────
                withContext(Dispatchers.Main) {
                    _uiState.value = stats.toUIState(
                        profilePhotoUrl = profilePhotoUrl,
                        isFromCache = false
                    )
                }

            } catch (e: Exception) {
                Log.e("CostBreakdownVM", "[Main] Error: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
            }
        }
    }

    // ── Helpers de formato ────────────────────────────────────────────────────

    fun formatCOP(amount: Double): String {
        return when {
            amount >= 1_000_000 -> "$${(amount / 1_000_000).let {
                if (it % 1 == 0.0) it.toInt().toString()
                else String.format("%.1f", it) }}M"
            amount >= 1_000     -> "$${(amount / 1_000).let {
                if (it % 1 == 0.0) it.toInt().toString()
                else String.format("%.1f", it) }}K"
            else                -> "$${amount.toInt()}"
        }
    }

    fun formatPercent(value: Double): String = "${value.toInt()}%"

    fun shortDay(day: String): String = day.take(3)

    fun maxAvgCost(map: Map<String, Double>): Double =
        map.values.maxOrNull()?.takeIf { it > 0 } ?: 1.0
}

// ── Extension: CostBreakdownStats → CostBreakdownUIState ─────────────────────
// Mantiene la separación entre capa de datos y capa de UI del patrón MVVM.
private fun CostBreakdownStats.toUIState(
    profilePhotoUrl: String,
    isFromCache: Boolean
) = CostBreakdownUIState(
    isLoading             = false,
    isFromCache           = isFromCache,
    allTimeSpentCOP       = allTimeSpentCOP,
    avgPerSessionCOP      = avgPerSessionCOP,
    hitDailyCapPercent    = hitDailyCapPercent,
    monthlySpendingCOP    = monthlySpendingCOP,
    monthLabel            = monthLabel,
    maxMonthlySpendingCOP = maxMonthlySpendingCOP,
    avgCostByDay          = avgCostByDay,
    profilePhotoUrl       = profilePhotoUrl,
    cachedAt              = cachedAt
)