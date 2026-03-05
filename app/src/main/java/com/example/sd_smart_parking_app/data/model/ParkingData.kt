package com.example.sd_smart_parking_app.data.model

data class ParkingLot(
    val id: String,
    val name: String,
    val location: String,
    val totalSpots: Int,
    val availableSpots: Int,
    val occupancyPercentage: Int,
    val waitTimeMinutes: Int,
    val floors: List<Floor>
)

data class Floor(
    val floorNumber: Int,
    val totalSpots: Int,
    val availableSpots: Int,
    val occupiedSpots: Int,
    val availabilityPercentage: Int,
    val status: String // "High", "Medium", "Low"
)

data class ParkingHistory(
    val id: String,
    val date: String,
    val location: String,
    val floorNumber: Int,
    val spaceNumber: String,
    val status: String, // "Completed", "Cancelled"
    val entryTime: String?,
    val waitTimeMinutes: Int?,
    val durationMinutes: Int?
)

data class UserProfile(
    val name: String,
    val email: String,
    val phone: String,
    val vehiclePlate: String,
    val vehicleModel: String,
    val totalVisits: Int,
    val averageWaitTime: Double,
    val averageTravelTime: Double
)