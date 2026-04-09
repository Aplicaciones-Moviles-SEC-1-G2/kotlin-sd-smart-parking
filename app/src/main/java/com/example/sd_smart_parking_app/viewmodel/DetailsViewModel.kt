package com.example.sd_smart_parking_app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sd_smart_parking_app.data.model.ParkingConfig
import com.example.sd_smart_parking_app.data.model.ParkingSpot
import com.example.sd_smart_parking_app.data.repository.ParkingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlin.random.Random

data class DetailsUIState(
    val parkingConfig: ParkingConfig = ParkingConfig(),
    val parkingSpots: List<ParkingSpot> = emptyList(),
    val isLoading: Boolean = false
)

class DetailsViewModel(private val repository: ParkingRepository = ParkingRepository()) : ViewModel() {

    private val _detailsState = MutableStateFlow(DetailsUIState())
    val detailsState: StateFlow<DetailsUIState> = _detailsState

    init {
        loadInitialData()
        startUpdatingSpots()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _detailsState.value = _detailsState.value.copy(isLoading = true)
            repository.getParkingConfig { config ->
                _detailsState.value = _detailsState.value.copy(parkingConfig = config)
            }
            repository.getParkingSpots { spots ->
                _detailsState.value = _detailsState.value.copy(
                    parkingSpots = spots,
                    isLoading = false
                )
            }
        }
    }

    private fun startUpdatingSpots() {
        viewModelScope.launch {
            while (true) {
                delay(10000) // Actualizar cada 10 segundos
                val currentSpots = _detailsState.value.parkingSpots
                val updatedSpots = currentSpots.map { spot ->
                    spot.copy(isAvailable = Random.nextBoolean())
                }
                _detailsState.value = _detailsState.value.copy(parkingSpots = updatedSpots)
            }
        }
    }
}