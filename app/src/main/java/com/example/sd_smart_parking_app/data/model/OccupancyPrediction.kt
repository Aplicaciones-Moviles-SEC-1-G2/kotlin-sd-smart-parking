package com.example.sd_smart_parking_app.data.model

/**
 * Modelo para predicciones de ocupación
 */
data class OccupancyPrediction(
    val hour: Int,                    // Hora del día (0-23)
    val predictedOccupancy: Float,    // Ocupación predicha (0-100%)
    val confidence: Float,             // Confianza de la predicción (0-100%)
    val recommendedTime: String,      // Hora recomendada para estacionar
    val isBusy: Boolean,              // ¿Está ocupado ahora?
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Datos históricos de ocupación
 */
data class OccupancyHistory(
    val hour: Int,
    val dayOfWeek: Int,               // 0 = Sunday, 1 = Monday, etc
    val occupancyPercentage: Float,   // Ocupación registrada
    val availableSpots: Int,
    val totalSpots: Int,
    val timestamp: Long
)

/**
 * Estadísticas de ocupación por hora
 */
data class HourlyOccupancyStats(
    val hour: Int,
    val avgOccupancy: Float,
    val maxOccupancy: Float,
    val minOccupancy: Float,
    val dataPoints: Int              // Cuántos datos se usaron para calcular
)