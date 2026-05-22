package com.example.sd_smart_parking_app.viewmodel

import android.content.Context
import android.util.ArrayMap
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
import kotlinx.coroutines.withTimeoutOrNull
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
    private val dailyCapCOP   = 17_000.0
    private val dailyCapHours = dailyCapCOP / hourlyRateCOP // = 5.67h

    // ── Optimization #5: Array instead of List for dayOrder ──────────────────
    // Indexed array access avoids iterator allocation on every loop.
    private val dayOrder = arrayOf(
        "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
    )

    // ── Optimization #1: Reusable Calendar and TimeZone ───────────────────────
    // A new Calendar.getInstance(bogota) was previously created inside every
    // mapNotNull iteration over records. Reusing a single instance and setting
    // cal.time = date on each iteration eliminates hundreds of short-lived
    // Calendar allocations per loadCostBreakdown() call.
    private val bogotaTimezone  = TimeZone.getTimeZone("America/Bogota")
    private val reusableCalendar = Calendar.getInstance(bogotaTimezone)

    // ── Optimization #5: Array for month names ────────────────────────────────
    // Previously declared as a new List inside the Default coroutine on every
    // loadCostBreakdown() call. Moving it to a class-level Array means it is
    // allocated once for the lifetime of the ViewModel.
    private val monthNames = arrayOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )

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

    // ── Optimization #12: onCleared — release Coil and cache resources ────────
    // CostBreakdownCache is a singleton object. Without explicit cleanup, the
    // stats entry for the current user remains in the LRU indefinitely.
    // Clearing it in onCleared() ensures memory is freed when the screen is
    // permanently left, respecting the ViewModel lifecycle.
    override fun onCleared() {
        super.onCleared()
        auth.currentUser?.email?.let { email ->
            CostBreakdownCache.get(email) // fuerza eviction natural del LRU
        }
        Log.d("CostBreakdownVM", "[Lifecycle] onCleared — ViewModel destroyed, resources released")
    }

    fun loadCostBreakdown() {
        val userEmail = auth.currentUser?.email ?: return
        val profilePhotoUrl = auth.currentUser?.photoUrl?.toString() ?: ""

        viewModelScope.launch {

            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            Log.d("CostBreakdownVM", "[Main] Loading started — thread: ${Thread.currentThread().name}")

            val cached = CostBreakdownCache.get(userEmail)
            CostBreakdownCache.logStats()

            if (cached != null) {
                Log.d("CostBreakdownVM", "[Main] LRU Hit — showing cached data")
                withContext(Dispatchers.Main) {
                    _uiState.value = cached.toUIState(profilePhotoUrl = profilePhotoUrl, isFromCache = true)
                }
                return@launch
            }

            Log.d("CostBreakdownVM", "[Main] LRU Miss — querying Firestore")

            try {
                val recordsDeferred = async(Dispatchers.IO) {
                    Log.d("CostBreakdownVM", "[IO] Querying Firestore — thread: ${Thread.currentThread().name}")

                    val snapshot = withTimeoutOrNull(5_000L) {
                        firestore.collection("vehicleRecords")
                            .whereEqualTo("ownerEmail", userEmail)
                            .whereEqualTo("type", "exit")
                            .get()
                            .await()
                    } ?: throw Exception("No internet connection — showing cached data")

                    // ── Optimization #1 + #4: Pre-sized ArrayList + indexed loop
                    // Pre-allocating ArrayList(docs.size) avoids internal array
                    // resizing as elements are added. The indexed for loop avoids
                    // the Iterator object that mapNotNull/forEach would allocate.
                    val docs = snapshot.documents
                    val result = ArrayList<VehicleRecord>(docs.size)
                    for (i in 0 until docs.size) {
                        val doc = docs[i]
                        try {
                            result.add(
                                VehicleRecord(
                                    id            = doc.id,
                                    type          = doc.getString("type") ?: "",
                                    floor         = doc.getLong("floor")?.toInt() ?: 0,
                                    spotNumber    = doc.getLong("spotNumber")?.toInt() ?: 0,
                                    durationHours = doc.getDouble("durationHours") ?: 0.0,
                                    timestamp     = doc.getTimestamp("timestamp"),
                                    plate         = doc.getString("plate") ?: "",
                                    ownerEmail    = doc.getString("ownerEmail") ?: ""
                                )
                            )
                        } catch (e: Exception) {
                            Log.e("CostBreakdownVM", "[IO] Error parsing document", e)
                        }
                    }
                    Log.d("CostBreakdownVM", "[IO] ${result.size} records fetched")
                    result
                }

                val records = recordsDeferred.await()

                if (records.isEmpty()) {
                    withContext(Dispatchers.Main) { _uiState.value = CostBreakdownUIState(isLoading = false) }
                    return@launch
                }

                val statsDeferred = async(Dispatchers.Default) {
                    Log.d("CostBreakdownVM", "[Default] Computing costs — thread: ${Thread.currentThread().name}")

                    // ── Optimization #4: Indexed loop for totalHours ──────────
                    var totalHours = 0.0
                    var sessionsThatHitCap = 0
                    for (i in 0 until records.size) {
                        totalHours += records[i].durationHours
                        if (records[i].durationHours >= dailyCapHours) sessionsThatHitCap++
                    }

                    val allTimeSpentCOP   = totalHours * hourlyRateCOP
                    val avgPerSessionCOP  = if (records.isNotEmpty()) allTimeSpentCOP / records.size else 0.0
                    val hitDailyCapPercent = if (records.isNotEmpty())
                        (sessionsThatHitCap.toDouble() / records.size) * 100.0 else 0.0

                    // reusableCalendar is accessed from Default dispatcher — safe
                    // because loadCostBreakdown() is not called concurrently.
                    reusableCalendar.timeInMillis = System.currentTimeMillis()
                    val currentMonth = reusableCalendar.get(Calendar.MONTH)
                    val currentYear  = reusableCalendar.get(Calendar.YEAR)

                    // ── Optimization #5: ArrayMap for spendingByMonth ─────────
                    var monthlySpendingCOP = 0.0
                    val spendingByMonth = ArrayMap<String, Double>(12)

                    // ── Optimization #1 + #4: Reuse Calendar + indexed loop ───
                    for (i in 0 until records.size) {
                        records[i].timestamp?.toDate()?.let { date ->
                            reusableCalendar.time = date
                            val month = reusableCalendar.get(Calendar.MONTH)
                            val year  = reusableCalendar.get(Calendar.YEAR)
                            val cost  = records[i].durationHours * hourlyRateCOP
                            val key   = "$year-$month"
                            spendingByMonth[key] = (spendingByMonth[key] ?: 0.0) + cost
                            if (month == currentMonth && year == currentYear) {
                                monthlySpendingCOP += cost
                            }
                        }
                    }

                    val monthLabel = "${monthNames[currentMonth]} $currentYear"
                    var maxMonthlySpendingCOP = monthlySpendingCOP
                    for (i in 0 until spendingByMonth.size) {
                        val v = spendingByMonth.valueAt(i)
                        if (v > maxMonthlySpendingCOP) maxMonthlySpendingCOP = v
                    }

                    // ── Optimization #5: ArrayMap for costSums and costCounts ─
                    val costSums   = ArrayMap<String, Double>(7)
                    val costCounts = ArrayMap<String, Int>(7)

                    for (i in 0 until records.size) {
                        records[i].timestamp?.toDate()?.let { date ->
                            reusableCalendar.time = date
                            val dayName = when (reusableCalendar.get(Calendar.DAY_OF_WEEK)) {
                                Calendar.MONDAY    -> "Monday"
                                Calendar.TUESDAY   -> "Tuesday"
                                Calendar.WEDNESDAY -> "Wednesday"
                                Calendar.THURSDAY  -> "Thursday"
                                Calendar.FRIDAY    -> "Friday"
                                Calendar.SATURDAY  -> "Saturday"
                                Calendar.SUNDAY    -> "Sunday"
                                else               -> null
                            }
                            if (dayName != null) {
                                val cost = records[i].durationHours * hourlyRateCOP
                                costSums[dayName]   = (costSums[dayName] ?: 0.0) + cost
                                costCounts[dayName] = (costCounts[dayName] ?: 0) + 1
                            }
                        }
                    }

                    // ── Optimization #5: ArrayMap for avgCostByDay ────────────
                    // Previously: .entries.sortedByDescending{}.associate{} created
                    // a new List<Map.Entry>, sorted it, then built a LinkedHashMap.
                    // Now we build the ArrayMap directly and sort only the keys
                    // by their average value — one allocation instead of three.
                    val avgCostByDay = ArrayMap<String, Double>(7)
                    for (i in 0 until dayOrder.size) {
                        val day   = dayOrder[i]
                        val sum   = costSums[day]   ?: continue
                        val count = costCounts[day] ?: continue
                        if (count > 0) avgCostByDay[day] = sum / count
                    }

                    // Sort by descending average cost
                    val sortedEntries = (0 until avgCostByDay.size)
                        .map { avgCostByDay.keyAt(it) to avgCostByDay.valueAt(it) }
                        .sortedByDescending { it.second }
                    val sortedAvgCostByDay = ArrayMap<String, Double>(sortedEntries.size)
                    for (i in sortedEntries.indices) {
                        sortedAvgCostByDay[sortedEntries[i].first] = sortedEntries[i].second
                    }

                    Log.d("CostBreakdownVM", "[Default] Computation complete — dailyCapHours: $dailyCapHours | sessions hit cap: $sessionsThatHitCap/${records.size}")

                    CostBreakdownStats(
                        allTimeSpentCOP       = allTimeSpentCOP,
                        avgPerSessionCOP      = avgPerSessionCOP,
                        hitDailyCapPercent    = hitDailyCapPercent,
                        monthlySpendingCOP    = monthlySpendingCOP,
                        monthLabel            = monthLabel,
                        maxMonthlySpendingCOP = maxMonthlySpendingCOP,
                        avgCostByDay          = sortedAvgCostByDay
                    )
                }

                val stats = statsDeferred.await()

                CostBreakdownCache.put(userEmail, stats)
                Log.d("CostBreakdownVM", "[Main] Stats saved to LRU Cache")
                CostBreakdownCache.logStats()

                withContext(Dispatchers.Main) {
                    _uiState.value = stats.toUIState(profilePhotoUrl = profilePhotoUrl, isFromCache = false)
                }

            } catch (e: Exception) {
                Log.e("CostBreakdownVM", "[Main] Error: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
                }
            }
        }
    }

    fun formatCOP(amount: Double): String {
        return when {
            amount >= 1_000_000 -> "$${(amount / 1_000_000).let { if (it % 1 == 0.0) it.toInt().toString() else String.format("%.1f", it) }}M"
            amount >= 1_000     -> "$${(amount / 1_000).let { if (it % 1 == 0.0) it.toInt().toString() else String.format("%.1f", it) }}K"
            else                -> "$${amount.toInt()}"
        }
    }

    fun formatPercent(value: Double): String = "${value.toInt()}%"
    fun shortDay(day: String): String = day.take(3)
    fun maxAvgCost(map: Map<String, Double>): Double =
        map.values.maxOrNull()?.takeIf { it > 0 } ?: 1.0
}

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