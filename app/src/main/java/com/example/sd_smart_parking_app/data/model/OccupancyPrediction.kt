package com.example.sd_smart_parking_app.data.model

data class OccupancyPrediction(
    val hour: Int,
    val predictedOccupancy: Float,
    val confidence: Float,
    val recommendedTime: String,
    val isBusy: Boolean,
    val reasoning: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class OccupancyHistory(
    val hour: Int,
    val dayOfWeek: Int,
    val occupancyPercentage: Float,
    val availableSpots: Int,
    val totalSpots: Int,
    val timestamp: Long
)

data class HourlyOccupancyStats(
    val hour: Int,
    val avgOccupancy: Float,
    val maxOccupancy: Float,
    val minOccupancy: Float,
    val dataPoints: Int
)