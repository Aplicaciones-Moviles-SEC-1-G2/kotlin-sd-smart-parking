package com.example.sd_smart_parking_app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import com.example.sd_smart_parking_app.data.ParkingNotificationManager
import com.example.sd_smart_parking_app.data.model.ParkingConfig
import com.example.sd_smart_parking_app.data.model.ParkingSpot
import com.example.sd_smart_parking_app.data.repository.ParkingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import java.text.SimpleDateFormat
import java.util.Calendar
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
    val lastUpdate: String = "",
    val isFromCache: Boolean = false
)

class SharedDetailsViewModel(
    private val repository: ParkingRepository = ParkingRepository.getInstance()
) : ViewModel() {

    private val _detailsState = MutableStateFlow(SharedDetailsUIState())
    val detailsState: StateFlow<SharedDetailsUIState> = _detailsState

    private var isInitialized = false
    private var notificationManager: ParkingNotificationManager? = null
    
    private val _threshold = MutableStateFlow(5)
    val thresholdFlow: StateFlow<Int> = _threshold

    fun initNotificationManager(context: Context) {
        if (notificationManager == null) {
            notificationManager = ParkingNotificationManager(context.applicationContext)
            _threshold.value = notificationManager?.spotThreshold ?: 5
            
            // Sync cache if spots were already loaded
            val currentSpots = _detailsState.value.parkingSpots
            if (currentSpots.isNotEmpty()) {
                notificationManager?.updateCache(currentSpots)
            }
        }
    }

    var threshold: Int
        get() = _threshold.value
        set(value) {
            _threshold.value = value
            notificationManager?.spotThreshold = value
        }

    init {
        val savedTimestamp = repository.getLastServerUpdate()
        if (savedTimestamp > 0L) {
            _detailsState.value = _detailsState.value.copy(
                lastUpdate = formatTimestamp(savedTimestamp),
                isFromCache = true
            )
        }
    }

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

            repository.getParkingSpotsWithSource { spots, isFromCache ->
                val currentState = _detailsState.value
                val total = spots.size
                val available = spots.count { it.isAvailable }
                val occupied = total - available

                val displayTime = if (isFromCache) {
                    val savedTs = repository.getLastServerUpdate()
                    // Use saved server timestamp if available; fall back to now so the label is never blank
                    if (savedTs > 0L) formatTimestamp(savedTs) else formatTimestamp(System.currentTimeMillis())
                } else {
                    formatTimestamp(System.currentTimeMillis())
                }

                notificationManager?.updateCache(spots)

                _detailsState.value = currentState.copy(
                    parkingSpots = spots,
                    totalSpots = total,
                    availableSpots = available,
                    occupiedSpots = occupied,
                    isLoading = false,
                    lastUpdate = displayTime,
                    isFromCache = isFromCache
                )
                updateFloorStates()
            }
        }
    }

    private fun updateFloorStates() {
        viewModelScope.launch(Dispatchers.Default) {
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
    }

    fun refreshData() {
        repository.forceRefreshParkingSpots(
            onResult = { spots, timestamp ->
                val total = spots.size
                val available = spots.count { it.isAvailable }
                val occupied = total - available
                
                notificationManager?.updateCache(spots)

                _detailsState.value = _detailsState.value.copy(
                    parkingSpots = spots,
                    totalSpots = total,
                    availableSpots = available,
                    occupiedSpots = occupied,
                    lastUpdate = formatTimestamp(timestamp),
                    isFromCache = false
                )
                updateFloorStates()
            },
            onError = { }
        )
    }

    fun simulateNotification() {
        val available = _detailsState.value.availableSpots
        notificationManager?.simulateNotification(
            "Critical Capacity",
            "Alert! Only $available spots available in the SD Building."
        )
    }

    fun simulateLostConnection() {
        val available = _detailsState.value.availableSpots
        notificationManager?.simulateNotification(
            "Basement Mode: Connection Lost",
            "You've entered a zone with no coverage. Last known availability: $available free spots."
        )
    }

    private fun formatTimestamp(timestamp: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val today = Calendar.getInstance()
        return if (cal.get(Calendar.DATE) == today.get(Calendar.DATE) &&
            cal.get(Calendar.MONTH) == today.get(Calendar.MONTH) &&
            cal.get(Calendar.YEAR) == today.get(Calendar.YEAR)
        ) {
            SimpleDateFormat("h:mm:ss a", Locale.ENGLISH).format(Date(timestamp))
        } else {
            SimpleDateFormat("MMM d, h:mm a", Locale.ENGLISH).format(Date(timestamp))
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
