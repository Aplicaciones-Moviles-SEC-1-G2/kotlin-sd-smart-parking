package com.example.sd_smart_parking_app.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

// config/parking
data class ParkingConfig(
    var parkingName: String = "",
    var numberOfFloors: Int = 0,
    var spotsPerFloor: Int = 0,
    var openingHour: Int = 0,
    var closingHour: Int = 0,
    var hourlyRate: Double = 0.0,
    var entryQueueLength: Int = 0
)

// parkingSpots/{id}
data class ParkingSpot(
    @get:PropertyName("id") @set:PropertyName("id") var id: String = "",
    @get:PropertyName("number") @set:PropertyName("number") var number: Int = 0,
    @get:PropertyName("floor") @set:PropertyName("floor") var floor: Int = 0,
    @get:PropertyName("isAvailable") @set:PropertyName("isAvailable") var isAvailable: Boolean = true,
    @get:PropertyName("currentPlate") @set:PropertyName("currentPlate") var currentPlate: String = ""
)

// users/{uid}
data class UserCar(
    var name: String = "",
    var plate: String = ""
)

data class UserProfile(
    var name: String = "",
    var email: String = "",
    var phone: String = "",
    var role: String = "driver",
    var cars: List<UserCar> = emptyList(),
    var createdAt: Timestamp? = null
)

data class Floor(
    val floorNumber: Int = 0,
    val totalSpots: Int = 0,
    val availableSpots: Int = 0,
    val occupiedSpots: Int = 0,
    val availabilityPercentage: Int = 0,
    val status: String = ""
)
