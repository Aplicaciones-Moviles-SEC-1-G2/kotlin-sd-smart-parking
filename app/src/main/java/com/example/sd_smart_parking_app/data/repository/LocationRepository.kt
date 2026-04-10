package com.example.sd_smart_parking_app.data.repository

import android.location.Location
import com.example.sd_smart_parking_app.data.LocationManager
import kotlinx.coroutines.flow.Flow

class LocationRepository(private val locationManager: LocationManager) {
    fun hasLocationPermission(): Boolean {
        return locationManager.hasLocationPermission()
    }
    suspend fun getCurrentLocation(): Location? {
        return locationManager.getCurrentLocation()
    }
    fun getLocationUpdates(): Flow<Location> {
        return locationManager.getLocationUpdates()
    }
    fun calculateDistance(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }
    fun calculateBearing(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Float {
        val dLon = Math.toRadians(lon2 - lon1)
        val lat1Rad = Math.toRadians(lat1)
        val lat2Rad = Math.toRadians(lat2)

        val y = kotlin.math.sin(dLon) * kotlin.math.cos(lat2Rad)
        val x = kotlin.math.cos(lat1Rad) * kotlin.math.sin(lat2Rad) -
                kotlin.math.sin(lat1Rad) * kotlin.math.cos(lat2Rad) * kotlin.math.cos(dLon)

        val bearingRad = kotlin.math.atan2(y, x)
        return (Math.toDegrees(bearingRad).toFloat() + 360) % 360
    }
}