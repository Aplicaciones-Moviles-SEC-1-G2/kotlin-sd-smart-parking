package com.example.sd_smart_parking_app.data.model

data class NearbyParking(
    val id: String = "",
    val name: String = "",
    val address: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val approximateCapacity: Int = 0,
    val phone: String = "",
    val distanceMeters: Float = 0f
)
