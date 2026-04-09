package com.example.sd_smart_parking_app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sd_smart_parking_app.data.model.Floor
import com.example.sd_smart_parking_app.data.repository.FloorRecommendation
import com.example.sd_smart_parking_app.data.repository.FloorRecommendationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class FloorRecommendationUIState(
    val recommendation: FloorRecommendation? = null,
    val rankedFloors: List<FloorRecommendation> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class FloorRecommendationViewModel : ViewModel() {

    private val repository = FloorRecommendationRepository()

    private val _recommendationState = MutableStateFlow(FloorRecommendationUIState())
    val recommendationState: StateFlow<FloorRecommendationUIState> = _recommendationState

    /**
     * Calcula la recomendación del mejor piso basándose en los pisos disponibles
     */
    fun calculateRecommendation(floors: List<Floor>) {
        viewModelScope.launch {
            try {
                _recommendationState.value = _recommendationState.value.copy(isLoading = true)

                // Obtener la mejor recomendación
                val recommendation = repository.getFloorRecommendation(floors)

                // Obtener el ranking de pisos
                val rankedFloors = repository.getRankedFloors(floors)

                _recommendationState.value = FloorRecommendationUIState(
                    recommendation = recommendation,
                    rankedFloors = rankedFloors,
                    isLoading = false,
                    error = null
                )
            } catch (e: Exception) {
                _recommendationState.value = _recommendationState.value.copy(
                    isLoading = false,
                    error = "Error al calcular recomendación: ${e.message}"
                )
            }
        }
    }

    /**
     * Actualiza la recomendación cuando cambien los pisos
     */
    fun updateRecommendation(floors: List<Floor>) {
        calculateRecommendation(floors)
    }
}