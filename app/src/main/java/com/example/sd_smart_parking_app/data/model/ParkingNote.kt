package com.example.sd_smart_parking_app.data.model

data class ParkingNote(
    val id: String = "",
    val userId: String = "",
    val userEmail: String = "",
    val userName: String = "",
    val message: String = "",
    val floor: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val isLocal: Boolean = false
)