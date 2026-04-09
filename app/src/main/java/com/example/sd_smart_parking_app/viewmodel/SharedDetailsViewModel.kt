package com.example.sd_smart_parking_app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.sd_smart_parking_app.data.model.ParkingConfig
import com.example.sd_smart_parking_app.data.model.ParkingSpot
import com.example.sd_smart_parking_app.data.repository.ParkingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlin.random.Random

data class SharedDetailsUIState(
    val parkingConfig: ParkingConfig = ParkingConfig(),
    val parkingSpots: List<ParkingSpot> = emptyList(),
    val isLoading: Boolean = false
)

class SharedDetailsViewModel(private val repository: ParkingRepository = ParkingRepository()) : ViewModel() {

    private val _detailsState = MutableStateFlow(SharedDetailsUIState())
    val detailsState: StateFlow<SharedDetailsUIState> = _detailsState

    private var isInitialized = false

    fun initializeIfNeeded() {
        if (!isInitialized) {
            loadInitialData()
            startUpdatingSpots()
            isInitialized = true
        }
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

    companion object {
        private var instance: SharedDetailsViewModel? = null

        fun getInstance(): SharedDetailsViewModel {
            if (instance == null) {
                instance = SharedDetailsViewModel()
            }
            return instance!!
        }
    }
}