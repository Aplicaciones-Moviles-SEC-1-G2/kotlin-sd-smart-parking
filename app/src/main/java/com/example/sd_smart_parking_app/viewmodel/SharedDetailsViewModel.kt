package com.example.sd_smart_parking_app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sd_smart_parking_app.data.model.ParkingConfig
import com.example.sd_smart_parking_app.data.model.ParkingSpot
import com.example.sd_smart_parking_app.data.repository.ParkingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class FloorState(
    val floorNumber: Int,
    val availableSpots: Int,
    val totalSpots: Int,
    val availabilityPercentage: Int,
    val availabilityStatus: String
)

data class SharedDetailsUIState(
    val parkingConfig: ParkingConfig = ParkingConfig(),
    val parkingSpots: List<ParkingSpot> = emptyList(),
    val floorStates: List<FloorState> = emptyList(),
    val isLoading: Boolean = false,
    val totalSpots: Int = 0,
    val availableSpots: Int = 0,
    val occupiedSpots: Int = 0,
    val lastUpdate: String = ""
)

class SharedDetailsViewModel(private val repository: ParkingRepository = ParkingRepository.getInstance()) : ViewModel() {

    private val _detailsState = MutableStateFlow(SharedDetailsUIState())
    val detailsState: StateFlow<SharedDetailsUIState> = _detailsState

    private var isInitialized = false

    fun initializeIfNeeded() {
        if (!isInitialized) {
            loadInitialData()
            isInitialized = true
        }
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _detailsState.value = _detailsState.value.copy(isLoading = true)
            repository.getParkingConfig { config ->
                val currentState = _detailsState.value
                val total = config.numberOfFloors * config.spotsPerFloor
                _detailsState.value = currentState.copy(
                    parkingConfig = config,
                    totalSpots = total,
                    occupiedSpots = total - currentState.availableSpots
                )
                updateFloorStates()
            }
            repository.getParkingSpots { spots ->
                val currentState = _detailsState.value
                val total = spots.size
                val available = spots.count { it.isAvailable }
                val occupied = total - available
                _detailsState.value = currentState.copy(
                    parkingSpots = spots,
                    totalSpots = total,
                    availableSpots = available,
                    occupiedSpots = occupied,
                    isLoading = false,
                    lastUpdate = SimpleDateFormat("h:mm:ss a", Locale.getDefault()).format(Date())
                )
                updateFloorStates()
            }
        }
    }

    private fun updateFloorStates() {
        val config = _detailsState.value.parkingConfig
        val spots = _detailsState.value.parkingSpots

        val newFloorStates = (1..config.numberOfFloors).map { floorNum ->
            val spotsInFloor = spots.filter { it.floor == floorNum }
            val floorTotal = spotsInFloor.size
            val floorAvailable = spotsInFloor.count { it.isAvailable }
            val floorPercentage = if (floorTotal > 0) (floorAvailable * 100) / floorTotal else 0

            FloorState(
                floorNumber = floorNum,
                availableSpots = floorAvailable,
                totalSpots = floorTotal,
                availabilityPercentage = floorPercentage,
                availabilityStatus = when {
                    floorPercentage > 60 -> "High"
                    floorPercentage > 30 -> "Medium"
                    else -> "Low"
                }
            )
        }

        _detailsState.value = _detailsState.value.copy(floorStates = newFloorStates)
    }

    fun refreshData() {
        _detailsState.value = _detailsState.value.copy(
            lastUpdate = SimpleDateFormat("h:mm:ss a", Locale.getDefault()).format(Date())
        )
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