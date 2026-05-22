package com.example.sd_smart_parking_app.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.sd_smart_parking_app.data.model.NearbyParking
import com.example.sd_smart_parking_app.data.repository.NearbyParkingRepository
import com.example.sd_smart_parking_app.data.repository.NearbyParkingResult
import com.example.sd_smart_parking_app.utils.NetworkMonitor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NearbyParkingUiState(
    val parkingList: List<NearbyParking> = emptyList(),
    val isLoading: Boolean = false,
    val dataSource: DataSource = DataSource.NONE,
    val savedAtMs: Long = 0L,
    val errorMessage: String? = null
)

enum class DataSource { FRESH, CACHE, LOCAL_STORAGE, NONE }

class NearbyParkingViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = NearbyParkingRepository.getInstance(application)
    private val networkMonitor = NetworkMonitor(application)

    private val _uiState = MutableStateFlow(NearbyParkingUiState())
    val uiState: StateFlow<NearbyParkingUiState> = _uiState.asStateFlow()

    init {
        // Collect network state — auto-refresh when connectivity is restored
        viewModelScope.launch {
            var previousState: Boolean? = null
            networkMonitor.networkStateFlow.collect { isOnline ->
                Log.d("NearbyParkingVM", "Network state changed: $isOnline (previous: $previousState)")
                if (previousState == false && isOnline) {
                    Log.d("NearbyParkingVM", "Connectivity restored — refreshing nearby parking")
                    loadNearbyParking()
                }
                previousState = isOnline
            }
        }
    }

    fun loadNearbyParking() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            Log.d("NearbyParkingVM", "loadNearbyParking started on: ${Thread.currentThread().name}")

            when (val result = repository.getNearbyParking()) {
                is NearbyParkingResult.Fresh -> {
                    Log.d("NearbyParkingVM", "Result: Fresh — ${result.data.size} items")
                    _uiState.update {
                        it.copy(
                            parkingList = result.data,
                            isLoading = false,
                            dataSource = DataSource.FRESH,
                            savedAtMs = 0L,
                            errorMessage = null
                        )
                    }
                }
                is NearbyParkingResult.FromCache -> {
                    Log.d("NearbyParkingVM", "Result: FromCache — ${result.data.size} items, savedAt=${result.savedAtMs}")
                    _uiState.update {
                        it.copy(
                            parkingList = result.data,
                            isLoading = false,
                            dataSource = DataSource.CACHE,
                            savedAtMs = result.savedAtMs,
                            errorMessage = null
                        )
                    }
                }
                is NearbyParkingResult.FromLocalStorage -> {
                    Log.d("NearbyParkingVM", "Result: FromLocalStorage — ${result.data.size} items, savedAt=${result.savedAtMs}")
                    _uiState.update {
                        it.copy(
                            parkingList = result.data,
                            isLoading = false,
                            dataSource = DataSource.LOCAL_STORAGE,
                            savedAtMs = result.savedAtMs,
                            errorMessage = null
                        )
                    }
                }
                is NearbyParkingResult.NoData -> {
                    Log.d("NearbyParkingVM", "Result: NoData")
                    _uiState.update {
                        it.copy(
                            parkingList = emptyList(),
                            isLoading = false,
                            dataSource = DataSource.NONE,
                            savedAtMs = 0L,
                            errorMessage = "No nearby parking data available"
                        )
                    }
                }
            }
        }
    }
}
