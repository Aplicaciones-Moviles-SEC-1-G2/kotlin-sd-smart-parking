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

    /**
     * Calcula la recomendación del mejor piso basándose en:
     * 1. Ocupación (disponibilidad)
     * 2. Proximidad al destino (pisos más bajos = más cercanos a entrada)
     * 3. Disponibilidad mínima (al menos 1 espacio libre)
     */
    fun getFloorRecommendation(floors: List<Floor>): FloorRecommendation? {
        if (floors.isEmpty()) return null

        // Filtrar pisos que tengan al menos 1 espacio disponible
        val availableFloors = floors.filter { it.availableSpots > 0 }

        if (availableFloors.isEmpty()) {
            return FloorRecommendation(
                floorNumber = 0,
                availableSpots = 0,
                availabilityPercentage = 0,
                reason = "No hay espacios disponibles en el parqueadero",
                emoji = "❌",
                score = 0f
            )
        }

        // Calcular score para cada piso
        val floorScores = availableFloors.map { floor ->
            val proximityScore = calculateProximityScore(floor.floorNumber)
            val occupancyScore = calculateOccupancyScore(floor.availabilityPercentage)
            val totalScore = (occupancyScore * 0.6f) + (proximityScore * 0.4f)

            Pair(floor, totalScore)
        }

        // Obtener el piso con mayor score
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

    /**
     * Score de proximidad: pisos más bajos tienen mejor score (más cercanos a entrada)
     * Piso 1 = 1.0, Piso 2 = 0.7, Piso 3 = 0.4
     */
    private fun calculateProximityScore(floorNumber: Int): Float {
        return when (floorNumber) {
            1 -> 1.0f
            2 -> 0.7f
            3 -> 0.4f
            else -> 0.2f
        }
    }

    /**
     * Score de ocupación basado en disponibilidad
     * Mayor disponibilidad = mayor score
     */
    private fun calculateOccupancyScore(availabilityPercentage: Int): Float {
        return availabilityPercentage / 100f
    }

    /**
     * Genera un mensaje de recomendación personalizado
     */
    private fun generateRecommendationReason(floor: Floor): String {
        return when {
            floor.availabilityPercentage >= 75 -> {
                "Piso ${floor.floorNumber} tiene excelente disponibilidad con ${floor.availableSpots} espacios libres"
            }
            floor.availabilityPercentage >= 50 -> {
                "Piso ${floor.floorNumber} es una buena opción con ${floor.availableSpots} espacios disponibles"
            }
            floor.availabilityPercentage >= 25 -> {
                "Piso ${floor.floorNumber} tiene disponibilidad limitada (${floor.availableSpots} espacios)"
            }
            else -> {
                "Piso ${floor.floorNumber} es la mejor opción disponible (${floor.availableSpots} espacio)"
            }
        }
    }

    /**
     * Retorna emoji basado en el estado del piso
     */
    private fun getEmojiForFloor(floor: Floor): String {
        return when {
            floor.availabilityPercentage >= 75 -> "🟢"
            floor.availabilityPercentage >= 50 -> "🟡"
            floor.availabilityPercentage >= 25 -> "🟠"
            else -> "🔴"
        }
    }

    /**
     * Retorna lista de pisos ordenados por recomendación
     */
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