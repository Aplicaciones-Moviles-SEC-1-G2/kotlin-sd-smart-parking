package com.example.sd_smart_parking_app.data.repository

// Firestore collection: "nearbyParking"
// Each document has:
//   id: String (document ID, also stored as field)
//   name: String
//   address: String
//   lat: Double
//   lng: Double
//   approximateCapacity: Int
//   phone: String
//
// Sample documents to add manually in Firebase Console:
// Document ID: parking_calle_19
//   name: "Parqueadero Calle 19"
//   address: "Cl. 19 #3-16, Bogotá"
//   lat: 4.60098
//   lng: -74.06521
//   approximateCapacity: 80
//   phone: "+57 1 234 5678"
//
// Document ID: parking_la_candelaria
//   name: "Parqueadero La Candelaria"
//   address: "Cr. 2 #12b-24, Bogotá"
//   lat: 4.59843
//   lng: -74.07102
//   approximateCapacity: 45
//   phone: "+57 1 345 6789"
//
// Document ID: parking_eje_ambiental
//   name: "Parqueadero Eje Ambiental"
//   address: "Av. Jiménez #3-50, Bogotá"
//   lat: 4.60201
//   lng: -74.06874
//   approximateCapacity: 120
//   phone: "+57 1 456 7890"

import android.content.Context
import android.util.Log
import android.util.LruCache
import com.example.sd_smart_parking_app.data.model.NearbyParking
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

// ─────────────────────────────────────────────────────────────────────────────
// Sealed result type
// ─────────────────────────────────────────────────────────────────────────────
sealed class NearbyParkingResult {
    data class Fresh(val data: List<NearbyParking>) : NearbyParkingResult()
    data class FromCache(val data: List<NearbyParking>, val savedAtMs: Long) : NearbyParkingResult()
    data class FromLocalStorage(val data: List<NearbyParking>, val savedAtMs: Long) : NearbyParkingResult()
    object NoData : NearbyParkingResult()
}

class NearbyParkingRepository private constructor(private val context: Context) {

    private val firestore = FirebaseFirestore.getInstance()

    // SD building reference coordinates
    private val SD_LAT = 4.60145
    private val SD_LNG = -74.06617

    // ─────────────────────────────────────────────────────────────────────────
    // LruCache (in-memory cache)
    // maxSize = 1 — only one list at a time
    // Value: Pair(list, timestamp when cached)
    // ─────────────────────────────────────────────────────────────────────────
    private val nearbyParkingCache = LruCache<String, Pair<List<NearbyParking>, Long>>(1)
    private val cacheTtlMs = 10 * 60 * 1000L  // 10 minutes
    private val CACHE_KEY = "nearby_parking"

    private fun isCacheValid(): Boolean {
        val cached = nearbyParkingCache.get(CACHE_KEY) ?: return false
        val isNotExpired = (System.currentTimeMillis() - cached.second) < cacheTtlMs
        Log.d("NearbyParkingRepo", "isCacheValid=$isNotExpired — age=${System.currentTimeMillis() - cached.second}ms")
        return isNotExpired
    }

    private fun getCached(): Pair<List<NearbyParking>, Long>? = nearbyParkingCache.get(CACHE_KEY)

    private fun saveToCache(list: List<NearbyParking>) {
        nearbyParkingCache.put(CACHE_KEY, Pair(list, System.currentTimeMillis()))
        Log.d("NearbyParkingRepo", "LruCache updated — ${list.size} items stored")
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SharedPreferences (local storage)
    // ─────────────────────────────────────────────────────────────────────────
    private val prefs by lazy {
        context.getSharedPreferences("nearby_parking_prefs", Context.MODE_PRIVATE)
    }

    private fun saveToSharedPreferences(list: List<NearbyParking>) {
        val array = JSONArray()
        list.forEach { parking ->
            val obj = JSONObject().apply {
                put("id", parking.id)
                put("name", parking.name)
                put("address", parking.address)
                put("lat", parking.lat)
                put("lng", parking.lng)
                put("approximateCapacity", parking.approximateCapacity)
                put("phone", parking.phone)
                // distanceMeters is NOT persisted — recalculated on load
            }
            array.put(obj)
        }
        prefs.edit()
            .putString("nearby_parking_json", array.toString())
            .putLong("nearby_parking_saved_at", System.currentTimeMillis())
            .apply()
        Log.d("NearbyParkingRepo", "SharedPreferences updated — ${list.size} items saved")
    }

    private fun loadFromSharedPreferences(): List<NearbyParking>? {
        val json = prefs.getString("nearby_parking_json", null) ?: run {
            Log.d("NearbyParkingRepo", "SharedPreferences — no data found")
            return null
        }
        return try {
            val array = JSONArray(json)
            val list = (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                NearbyParking(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    address = obj.getString("address"),
                    lat = obj.getDouble("lat"),
                    lng = obj.getDouble("lng"),
                    approximateCapacity = obj.getInt("approximateCapacity"),
                    phone = obj.getString("phone")
                    // distanceMeters left as default 0f — recalculated after loading
                )
            }
            Log.d("NearbyParkingRepo", "SharedPreferences loaded — ${list.size} items")
            list
        } catch (e: Exception) {
            Log.e("NearbyParkingRepo", "Error parsing SharedPreferences JSON: ${e.message}", e)
            null
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Distance helpers
    // ─────────────────────────────────────────────────────────────────────────
    private fun withDistances(list: List<NearbyParking>): List<NearbyParking> {
        return list.map { parking ->
            val results = FloatArray(1)
            android.location.Location.distanceBetween(SD_LAT, SD_LNG, parking.lat, parking.lng, results)
            parking.copy(distanceMeters = results[0])
        }.sortedBy { it.distanceMeters }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Main public function — "Network falling back to cache" strategy
    // Priority: Firestore → LruCache → SharedPreferences → NoData
    // ─────────────────────────────────────────────────────────────────────────
    suspend fun getNearbyParking(): NearbyParkingResult {
        // 1. Try Firestore fetch (multi-threaded with two parallel coroutines)
        return try {
            val freshList = withContext(Dispatchers.IO) {
                fetchFromFirestoreWithParallelCoroutines()
            }
            saveToCache(freshList)
            withContext(Dispatchers.IO) {
                saveToSharedPreferences(freshList)
            }
            Log.d("NearbyParkingRepo", "Returning Fresh data — ${freshList.size} items")
            NearbyParkingResult.Fresh(freshList)

        } catch (e: Exception) {
            Log.w("NearbyParkingRepo", "Firestore fetch failed: ${e.message} — falling back to cache")

            // 2. Check LruCache
            if (isCacheValid()) {
                val (cachedList, timestamp) = getCached()!!
                val listWithDistances = withDistances(cachedList)
                Log.d("NearbyParkingRepo", "Cache hit — returning ${listWithDistances.size} items from LruCache")
                return NearbyParkingResult.FromCache(listWithDistances, timestamp)
            }
            Log.d("NearbyParkingRepo", "Cache miss — checking SharedPreferences")

            // 3. Check SharedPreferences
            val localList = withContext(Dispatchers.IO) { loadFromSharedPreferences() }
            if (!localList.isNullOrEmpty()) {
                val savedAt = prefs.getLong("nearby_parking_saved_at", 0L)
                val listWithDistances = withDistances(localList)
                Log.d("NearbyParkingRepo", "SharedPreferences hit — returning ${listWithDistances.size} items")
                return NearbyParkingResult.FromLocalStorage(listWithDistances, savedAt)
            }
            Log.d("NearbyParkingRepo", "No data available in any source")

            // 4. NoData
            NearbyParkingResult.NoData
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Multi-threading: two parallel coroutines inside coroutineScope
    //   parkingJob  — Dispatchers.IO  — fetches documents from Firestore
    //   distanceJob — Dispatchers.Default — CPU-bound distance computation
    // ─────────────────────────────────────────────────────────────────────────
    private suspend fun fetchFromFirestoreWithParallelCoroutines(): List<NearbyParking> =
        coroutineScope {
            val parkingJob = async(Dispatchers.IO) {
                Log.d("NearbyParkingRepo", "Coroutine parkingJob running on: ${Thread.currentThread().name}")
                firestore.collection("nearbyParking")
                    .get()
                    .await()
                    .documents
                    .mapNotNull { doc ->
                        try {
                            NearbyParking(
                                id = doc.id,
                                name = doc.getString("name") ?: "",
                                address = doc.getString("address") ?: "",
                                lat = doc.getDouble("lat") ?: 0.0,
                                lng = doc.getDouble("lng") ?: 0.0,
                                approximateCapacity = doc.getLong("approximateCapacity")?.toInt() ?: 0,
                                phone = doc.getString("phone") ?: ""
                            )
                        } catch (e: Exception) {
                            Log.e("NearbyParkingRepo", "Error parsing document ${doc.id}: ${e.message}", e)
                            null
                        }
                    }
            }

            // distanceJob runs on Dispatchers.Default (CPU pool).
            // It waits for parkingJob to finish, then performs the Haversine
            // distance calculation — CPU-bound work on the computation dispatcher.
            val distanceJob = async(Dispatchers.Default) {
                Log.d("NearbyParkingRepo", "Coroutine distanceJob running on: ${Thread.currentThread().name}")
                parkingJob.await().associate { parking ->
                    val results = FloatArray(1)
                    android.location.Location.distanceBetween(
                        SD_LAT, SD_LNG,
                        parking.lat, parking.lng,
                        results
                    )
                    parking.id to results[0]
                }
            }

            val parkingList = parkingJob.await()
            val distances = distanceJob.await()

            parkingList
                .map { parking -> parking.copy(distanceMeters = distances[parking.id] ?: 0f) }
                .sortedBy { it.distanceMeters }
        }

    // ─────────────────────────────────────────────────────────────────────────
    // Singleton
    // ─────────────────────────────────────────────────────────────────────────
    companion object {
        @Volatile
        private var INSTANCE: NearbyParkingRepository? = null

        fun getInstance(context: Context): NearbyParkingRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = NearbyParkingRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
