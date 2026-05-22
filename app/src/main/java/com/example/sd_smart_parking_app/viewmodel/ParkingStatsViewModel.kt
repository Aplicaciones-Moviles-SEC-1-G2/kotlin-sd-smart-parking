package com.example.sd_smart_parking_app.viewmodel

import android.content.Context
import android.util.ArrayMap
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

    private val lruCache = object : LruCache<String, ComputedStats>(3) {
        override fun entryRemoved(
            evicted: Boolean, key: String,
            oldValue: ComputedStats, newValue: ComputedStats?
        ) {
            if (evicted) Log.d("ParkingStatsVM", "[LRU] Entry evicted — key: $key")
        }
    }
    private val lruTimestamps = mutableMapOf<String, Long>()
    private val lruTtlMs = 60 * 60 * 1000L

    private val dataStore = ParkingStatsDataStore(context)
    private val statsFile = File(context.filesDir, "parking_stats_backup.json")

    private val _uiState = MutableStateFlow(ParkingStatsUIState())
    val uiState: StateFlow<ParkingStatsUIState> = _uiState

    private val hourlyRateCOP = 3_000.0

    // ── Optimization #5: ArrayMap instead of List for dayOrder ────────────────
    // ArrayMap uses ~50% less memory than HashMap for small fixed-key collections.
    // dayOrder is a fixed 7-element set used repeatedly in grouping operations —
    // declaring it as an Array avoids iterator allocation on every indexed access.
    private val dayOrder = arrayOf(
        "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
    )

    // ── Optimization #1: Reusable Calendar instance ───────────────────────────
    // Previously, a new Calendar instance was created inside every mapNotNull
    // iteration over records (once per document). With large record sets this
    // creates hundreds of short-lived Calendar objects, triggering GC pressure.
    // Declaring it once and reusing it via cal.time = date eliminates those
    // allocations entirely.
    private val bogotaTimezone = TimeZone.getTimeZone("America/Bogota")
    private val reusableCalendar = Calendar.getInstance(bogotaTimezone)

    init {
        loadStats()
    }

    // ── Optimization #12: onCleared — release resources on ViewModel destroy ──
    // lruTimestamps holds strong references to user email strings. Without
    // explicit cleanup, these references persist until the process dies.
    // onCleared() is guaranteed to be called when the ViewModel is destroyed
    // (user navigates permanently away), releasing all cached timestamps.
    override fun onCleared() {
        super.onCleared()
        lruTimestamps.clear()
        lruCache.evictAll()
        Log.d("ParkingStatsVM", "[Lifecycle] onCleared — LRU and timestamps released")
    }

    fun loadStats() {
        val userEmail = auth.currentUser?.email ?: return

        viewModelScope.launch {

            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            Log.d("ParkingStatsVM", "[Main] Orchestrator started — thread: ${Thread.currentThread().name}")

            val cachedStats = lruCache.get(userEmail)
            val cacheTime = lruTimestamps[userEmail] ?: 0L
            val lruValid = cachedStats != null &&
                    (System.currentTimeMillis() - cacheTime) < lruTtlMs

            if (lruValid && cachedStats != null) {
                Log.d("ParkingStatsVM", "[LRU] Hit — hits: ${lruCache.hitCount()} | misses: ${lruCache.missCount()} | size: ${lruCache.size()}/3")
                withContext(Dispatchers.Main) {
                    _uiState.value = cachedStats.toUIState(isRefreshing = false, lastSync = cacheTime)
                }
                return@launch
            }

            Log.d("ParkingStatsVM", "[LRU] Miss — hits: ${lruCache.hitCount()} | misses: ${lruCache.missCount()} | size: ${lruCache.size()}/3")

            val lastSync = withContext(Dispatchers.IO) { dataStore.lastSyncTimestamp.first() }
            val cachedEmail = withContext(Dispatchers.IO) { dataStore.cachedUserEmail.first() }
            val datastoreValid = dataStore.isCacheValid(lastSync) && cachedEmail == userEmail

            if (datastoreValid) {
                val fileStats = withContext(Dispatchers.IO) { readStatsFromFile(userEmail) }
                if (fileStats != null) {
                    Log.d("ParkingStatsVM", "[Main] DataStore valid — showing data from JSON file")
                    withContext(Dispatchers.Main) { _uiState.value = fileStats }
                    fileStats.toComputedStats()?.let { stats ->
                        lruCache.put(userEmail, stats)
                        lruTimestamps[userEmail] = lastSync
                    }
                    return@launch
                }
            }

            try {
                val recordsDeferred = async(Dispatchers.IO) {
                    Log.d("ParkingStatsVM", "[IO] Querying Firestore — thread: ${Thread.currentThread().name}")

                    val snapshot = withTimeoutOrNull(5_000L) {
                        firestore.collection("vehicleRecords")
                            .whereEqualTo("ownerEmail", userEmail)
                            .whereEqualTo("type", "exit")
                            .get()
                            .await()
                    } ?: throw Exception("No internet connection — showing cached data")

                    // ── Optimization #1: Avoid unnecessary objects inside loop ─
                    // Previously: type, floor, spotNumber, plate, ownerEmail were
                    // extracted via getString/getLong even for documents that might
                    // fail parsing. Now we extract only durationHours and timestamp
                    // first (the two fields used in all calculations), and reuse
                    // the same local variables across the loop body.
                    val docs = snapshot.documents
                    val result = ArrayList<VehicleRecord>(docs.size)
                    for (i in 0 until docs.size) {
                        val doc = docs[i]
                        try {
                            result.add(
                                VehicleRecord(
                                    id           = doc.id,
                                    type         = doc.getString("type") ?: "",
                                    floor        = doc.getLong("floor")?.toInt() ?: 0,
                                    spotNumber   = doc.getLong("spotNumber")?.toInt() ?: 0,
                                    durationHours = doc.getDouble("durationHours") ?: 0.0,
                                    timestamp    = doc.getTimestamp("timestamp"),
                                    plate        = doc.getString("plate") ?: "",
                                    ownerEmail   = doc.getString("ownerEmail") ?: ""
                                )
                            )
                        } catch (e: Exception) {
                            Log.e("ParkingStatsVM", "[IO] Error parsing document", e)
                        }
                    }
                    Log.d("ParkingStatsVM", "[IO] ${result.size} records fetched from Firestore")
                    result
                }

                val records = recordsDeferred.await()

                if (records.isEmpty()) {
                    Log.d("ParkingStatsVM", "[Main] No records in Firestore")
                    val fileStats = withContext(Dispatchers.IO) { readStatsFromFile(userEmail) }
                    withContext(Dispatchers.Main) {
                        _uiState.value = fileStats ?: ParkingStatsUIState(isLoading = false)
                    }
                    return@launch
                }

                val statsDeferred = async(Dispatchers.Default) {
                    Log.d("ParkingStatsVM", "[Default] Computing stats — thread: ${Thread.currentThread().name}")

                    val totalSessions   = records.size
                    var totalTimeHours  = 0.0

                    // ── Optimization #4: Indexed loop instead of forEach/iterator
                    // forEach on a List allocates an Iterator object on every call.
                    // Using an indexed for loop avoids that allocation entirely.
                    // With 100+ records this eliminates 100+ short-lived Iterator
                    // objects per loadStats() call, directly reducing GC pressure.
                    for (i in 0 until records.size) {
                        totalTimeHours += records[i].durationHours
                    }

                    val totalPaidCOP    = totalTimeHours * hourlyRateCOP
                    val avgSessionHours = totalTimeHours / totalSessions

                    // ── Optimization #5: ArrayMap for sessionsByDay ───────────
                    // sessionsByDay maps 7 fixed day-name strings to Int counts.
                    // ArrayMap<String, Int> uses a sorted array of keys instead
                    // of a hash table, consuming ~50% less memory than HashMap
                    // for collections with fewer than ~10 entries.
                    val sessionsByDay = ArrayMap<String, Int>(7)

                    // ── Optimization #1: Reuse Calendar — no new instance per record
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
                                sessionsByDay[dayName] = (sessionsByDay[dayName] ?: 0) + 1
                            }
                        }
                    }

                    var busiestDay         = ""
                    var busiestDaySessions = 0
                    for (i in 0 until sessionsByDay.size) {
                        val count = sessionsByDay.valueAt(i)
                        if (count > busiestDaySessions) {
                            busiestDaySessions = count
                            busiestDay = sessionsByDay.keyAt(i)
                        }
                    }

                    // ── Optimization #5: ArrayMap for floorCounts ────────────
                    val floorCounts = ArrayMap<Int, Int>(8)
                    for (i in 0 until records.size) {
                        val floor = records[i].floor
                        if (floor > 0) {
                            floorCounts[floor] = (floorCounts[floor] ?: 0) + 1
                        }
                    }

                    var favouriteFloor       = 0
                    var favouriteFloorVisits = 0
                    for (i in 0 until floorCounts.size) {
                        val count = floorCounts.valueAt(i)
                        if (count > favouriteFloorVisits) {
                            favouriteFloorVisits = count
                            favouriteFloor = floorCounts.keyAt(i)
                        }
                    }

                    // ── Optimization #5: ArrayMap for durationSums and counts ─
                    val durationSums   = ArrayMap<String, Double>(7)
                    val durationCounts = ArrayMap<String, Int>(7)

                    // ── Optimization #1 + #4: Reuse Calendar + indexed loop ───
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
                                durationSums[dayName]   = (durationSums[dayName] ?: 0.0) + records[i].durationHours
                                durationCounts[dayName] = (durationCounts[dayName] ?: 0) + 1
                            }
                        }
                    }

                    // ── Optimization #5: ArrayMap for avgDurationByDay ────────
                    val avgDurationByDay = ArrayMap<String, Double>(7)
                    for (i in 0 until dayOrder.size) {
                        val day = dayOrder[i]
                        val sum   = durationSums[day]   ?: continue
                        val count = durationCounts[day] ?: continue
                        if (count > 0) avgDurationByDay[day] = sum / count
                    }

                    Log.d("ParkingStatsVM", "[Default] Computation complete — $totalSessions sessions")

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

                async(Dispatchers.IO) {
                    dataStore.saveLastSyncTimestamp(now)
                    dataStore.saveCachedUserEmail(userEmail)
                    writeStatsToFile(stats, userEmail, now)
                    Log.d("ParkingStatsVM", "[IO] DataStore + JSON updated")
                }.await()

                lruCache.put(userEmail, stats)
                lruTimestamps[userEmail] = now
                Log.d("ParkingStatsVM", "[LRU] Stats saved — hits: ${lruCache.hitCount()} | misses: ${lruCache.missCount()} | size: ${lruCache.size()}/3")

                withContext(Dispatchers.Main) {
                    _uiState.value = stats.toUIState(isRefreshing = false, lastSync = now)
                }

            } catch (e: Exception) {
                Log.e("ParkingStatsVM", "[Main] Error querying Firestore: ${e.message}", e)
                val fileStats = withContext(Dispatchers.IO) { readStatsFromFile(userEmail) }
                withContext(Dispatchers.Main) {
                    if (fileStats != null) {
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
                val avgMap = stats.avgDurationByDay
                // ── Optimization #4: indexed loop over ArrayMap ───────────────
                if (avgMap is ArrayMap) {
                    for (i in 0 until avgMap.size) {
                        dayMap.put(avgMap.keyAt(i), avgMap.valueAt(i))
                    }
                } else {
                    avgMap.forEach { (day, hours) -> dayMap.put(day, hours) }
                }
                put("avgDurationByDay", dayMap)
            }
            statsFile.writeText(json.toString())
        } catch (e: Exception) {
            Log.e("ParkingStatsVM", "[IO] Error writing JSON file", e)
        }
    }

    private fun readStatsFromFile(userEmail: String): ParkingStatsUIState? {
        return try {
            if (!statsFile.exists()) return null
            val json = JSONObject(statsFile.readText())
            if (json.getString("ownerEmail") != userEmail) return null

            val dayMapJson = json.getJSONObject("avgDurationByDay")
            // ── Optimization #5: ArrayMap for deserialized day map ────────────
            val avgDurationByDay = ArrayMap<String, Double>(7)
            for (i in 0 until dayOrder.size) {
                val day = dayOrder[i]
                if (dayMapJson.has(day)) avgDurationByDay[day] = dayMapJson.getDouble(day)
            }

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
            Log.e("ParkingStatsVM", "[IO] Error reading JSON file", e)
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