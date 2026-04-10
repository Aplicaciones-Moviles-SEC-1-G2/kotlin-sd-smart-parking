package com.example.sd_smart_parking_app.data.repository

import com.example.sd_smart_parking_app.data.model.Floor

data class FloorRecommendation(
    val floorNumber: Int,
    val availableSpots: Int,
    val availabilityPercentage: Int,
    val reason: String,
    val emoji: String,
    val score: Float
)

class FloorRecommendationRepository {

    fun getFloorRecommendation(floors: List<Floor>): FloorRecommendation? {
        if (floors.isEmpty()) return null

        val availableFloors = floors.filter { it.availableSpots > 0 }

        if (availableFloors.isEmpty()) {
            return FloorRecommendation(
                floorNumber = 0,
                availableSpots = 0,
                availabilityPercentage = 0,
                reason = "No available spots in the parking",
                emoji = "❌",
                score = 0f
            )
        }

        val floorScores = availableFloors.map { floor ->
            val proximityScore = calculateProximityScore(floor.floorNumber)
            val occupancyScore = calculateOccupancyScore(floor.availabilityPercentage)
            val totalScore = (occupancyScore * 0.6f) + (proximityScore * 0.4f)

            Pair(floor, totalScore)
        }

        val bestFloor = floorScores.maxByOrNull { it.second }?.first
            ?: return null

        return FloorRecommendation(
            floorNumber = bestFloor.floorNumber,
            availableSpots = bestFloor.availableSpots,
            availabilityPercentage = bestFloor.availabilityPercentage,
            reason = generateRecommendationReason(bestFloor),
            emoji = getEmojiForFloor(bestFloor),
            score = floorScores.find { it.first.floorNumber == bestFloor.floorNumber }?.second ?: 0f
        )
    }

    private fun calculateProximityScore(floorNumber: Int): Float {
        return when (floorNumber) {
            1 -> 1.0f
            2 -> 0.7f
            3 -> 0.4f
            else -> 0.2f
        }
    }

    private fun calculateOccupancyScore(availabilityPercentage: Int): Float {
        return availabilityPercentage / 100f
    }

    private fun generateRecommendationReason(floor: Floor): String {
        return when {
            floor.availabilityPercentage >= 75 -> {
                "Floor ${floor.floorNumber} has excellent availability with ${floor.availableSpots} free spots"
            }
            floor.availabilityPercentage >= 50 -> {
                "Floor ${floor.floorNumber} is a good option with ${floor.availableSpots} free spots"
            }
            floor.availabilityPercentage >= 25 -> {
                "Floor ${floor.floorNumber} has a limited availability with (${floor.availableSpots} free spots)"
            }
            else -> {
                "Floor ${floor.floorNumber} is the best available option (${floor.availableSpots} free spots)"
            }
        }
    }

    private fun getEmojiForFloor(floor: Floor): String {
        return when {
            floor.availabilityPercentage >= 75 -> "🟢"
            floor.availabilityPercentage >= 50 -> "🟡"
            floor.availabilityPercentage >= 25 -> "🟠"
            else -> "🔴"
        }
    }

    fun getRankedFloors(floors: List<Floor>): List<FloorRecommendation> {
        return floors.filter { it.availableSpots > 0 }
            .map { floor ->
                val proximityScore = calculateProximityScore(floor.floorNumber)
                val occupancyScore = calculateOccupancyScore(floor.availabilityPercentage)
                val totalScore = (occupancyScore * 0.6f) + (proximityScore * 0.4f)

                FloorRecommendation(
                    floorNumber = floor.floorNumber,
                    availableSpots = floor.availableSpots,
                    availabilityPercentage = floor.availabilityPercentage,
                    reason = generateRecommendationReason(floor),
                    emoji = getEmojiForFloor(floor),
                    score = totalScore
                )
            }
            .sortedByDescending { it.score }
    }
}