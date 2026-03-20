package com.example.sd_smart_parking_app.data.model

import com.google.firebase.Timestamp

// config/parking
data class ParkingConfig(
    var parkingName: String = "",
    var numberOfFloors: Int = 0,
    var spotsPerFloor: Int = 0,
    var openingHour: Int = 0,
    var closingHour: Int = 0,
    var hourlyRate: Double = 0.0
)

// parkingSpots/{id}
data class ParkingSpot(
    var id: String = "",
    var number: Int = 0,
    var floor: Int = 0,
    var isAvailable: Boolean = true,
    var currentPlate: String = ""
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
