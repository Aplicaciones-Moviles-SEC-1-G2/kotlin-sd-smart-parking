package com.example.sd_smart_parking_app.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sd_smart_parking_app.data.model.OccupancyPrediction
import com.example.sd_smart_parking_app.data.repository.OccupancyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Estado UI del Predictor de Ocupación
 */
data class OccupancyPredictorUIState(
    val isLoading: Boolean = false,
    val currentPrediction: OccupancyPrediction? = null,
    val nextHoursPredictions: List<OccupancyPrediction> = emptyList(),
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class OccupancyPredictorViewModel : ViewModel() {

    private val repository = OccupancyRepository()

    private val _uiState = MutableStateFlow(OccupancyPredictorUIState())
    val uiState: StateFlow<OccupancyPredictorUIState> = _uiState

    init {
        Log.d("OccupancyPredictorViewModel", "Init - Loading current prediction")
        loadCurrentPrediction()
    }

    /**
     * Cargar predicción para la hora actual
     */
    fun loadCurrentPrediction() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            val result = repository.getCurrentHourPrediction()

            result.onSuccess { prediction ->
                Log.d("OccupancyPredictorViewModel", "Current prediction loaded: ${prediction.predictedOccupancy}%")
                _uiState.value = _uiState.value.copy(
                    currentPrediction = prediction,
                    isLoading = false
                )
            }.onFailure { error ->
                Log.e("OccupancyPredictorViewModel", "Error loading prediction: ${error.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = error.message ?: "Unknown error"
                )
            }
        }
    }

    /**
     * Cargar predicciones para las próximas N horas
     */
    fun loadNextHoursPredictions(hours: Int = 24) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            val result = repository.getNextHoursPredictions(hours)

            result.onSuccess { predictions ->
                Log.d("OccupancyPredictorViewModel", "Loaded ${predictions.size} predictions")
                _uiState.value = _uiState.value.copy(
                    nextHoursPredictions = predictions,
                    isLoading = false
                )
            }.onFailure { error ->
                Log.e("OccupancyPredictorViewModel", "Error loading predictions: ${error.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = error.message ?: "Unknown error"
                )
            }
        }
    }

    /**
     * Guardar datos de ocupación actual (para entrenar el modelo)
     */
    fun saveCurrentOccupancyData(
        occupancyPercentage: Float,
        availableSpots: Int,
        totalSpots: Int
    ) {
        viewModelScope.launch {
            val result = repository.saveOccupancyData(
                occupancyPercentage,
                availableSpots,
                totalSpots
            )

            result.onSuccess {
                Log.d("OccupancyPredictorViewModel", "Occupancy data saved")
                _uiState.value = _uiState.value.copy(
                    successMessage = "Datos de ocupación guardados"
                )
                // Recargar predicción después de guardar datos
                loadCurrentPrediction()
            }.onFailure { error ->
                Log.e("OccupancyPredictorViewModel", "Error saving data: ${error.message}")
                _uiState.value = _uiState.value.copy(
                    errorMessage = error.message ?: "Error saving data"
                )
            }
        }
    }

    /**
     * Limpiar mensaje de error
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    /**
     * Limpiar mensaje de éxito
     */
    fun clearSuccess() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }
}