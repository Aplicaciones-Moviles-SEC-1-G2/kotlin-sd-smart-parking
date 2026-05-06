package com.example.sd_smart_parking_app.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

data class VehicleRecord(
    val id: String = "",
    val type: String = "",
    val floor: Int = 0,
    val spotNumber: Int = 0,
    val durationHours: Double = 0.0,
    val timestamp: Timestamp? = null,
    val plate: String = "",
    val ownerEmail: String = ""
)

data class HistoryUIState(
    val records: List<VehicleRecord> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class HistoryViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow(HistoryUIState())
    val uiState: StateFlow<HistoryUIState> = _uiState

    init {
        loadHistory()
    }

    fun loadHistory() {
        val userEmail = auth.currentUser?.email ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val snapshot = firestore.collection("vehicleRecords")
                    .whereEqualTo("ownerEmail", userEmail)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(10)
                    .get()
                    .await()

                val records = snapshot.documents.mapNotNull { doc ->
                    try {
                        VehicleRecord(
                            id = doc.id,
                            type = doc.getString("type") ?: "",
                            floor = doc.getLong("floor")?.toInt() ?: 0,
                            spotNumber = doc.getLong("spotNumber")?.toInt() ?: 0,
                            durationHours = doc.getDouble("durationHours") ?: 0.0,
                            timestamp = doc.getTimestamp("timestamp"),
                            plate = doc.getString("plate") ?: "",
                            ownerEmail = doc.getString("ownerEmail") ?: ""
                        )
                    } catch (e: Exception) {
                        Log.e("HistoryViewModel", "Error parsing record", e)
                        null
                    }
                }

                _uiState.value = _uiState.value.copy(
                    records = records,
                    isLoading = false
                )
                Log.d("HistoryViewModel", "Loaded ${records.size} records")
            } catch (e: Exception) {
                Log.e("HistoryViewModel", "Error loading history: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun formatDate(timestamp: Timestamp?): String {
        if (timestamp == null) return "Unknown date"
        val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone("America/Bogota")
        return sdf.format(timestamp.toDate())
    }

    fun formatTime(timestamp: Timestamp?): String {
        if (timestamp == null) return "--:--"
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone("America/Bogota")
        return sdf.format(timestamp.toDate())
    }

    fun formatDuration(durationHours: Double): String {
        if (durationHours <= 0) return null.toString()
        val hours = durationHours.toInt()
        val minutes = ((durationHours - hours) * 60).toInt()
        return when {
            hours > 0 && minutes > 0 -> "${hours}h ${minutes}min"
            hours > 0 -> "${hours}h"
            else -> "${minutes}min"
        }
    }
}