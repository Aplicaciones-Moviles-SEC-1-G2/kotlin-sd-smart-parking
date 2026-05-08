package com.example.sd_smart_parking_app.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sd_smart_parking_app.data.ParkingNotesDatabaseHelper
import com.example.sd_smart_parking_app.data.UserPreferencesDataStore
import com.example.sd_smart_parking_app.data.model.ParkingNote
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

data class ParkingNotesUIState(
    val notes: List<ParkingNote> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSubmitting: Boolean = false,
    val submitSuccess: Boolean = false,
    val showOnlyMyNotes: Boolean = false,
    val lastNoteTimestamp: Long = 0L
)

class ParkingNotesViewModel(private val context: Context) : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val dbHelper = ParkingNotesDatabaseHelper(context)
    private val dataStore = UserPreferencesDataStore(context)

    private val _uiState = MutableStateFlow(ParkingNotesUIState())
    val uiState: StateFlow<ParkingNotesUIState> = _uiState

    init {
        loadPreferences()
        loadNotes()
    }

    private fun loadPreferences() {
        viewModelScope.launch {
            val showOnlyMine = dataStore.showOnlyMyNotes.first()
            val lastTimestamp = dataStore.lastNoteTimestamp.first()
            _uiState.value = _uiState.value.copy(
                showOnlyMyNotes = showOnlyMine,
                lastNoteTimestamp = lastTimestamp
            )
            Log.d("ParkingNotesVM", "Preferences loaded — showOnlyMyNotes: $showOnlyMine, lastNoteTimestamp: $lastTimestamp")
        }
    }

    fun loadNotes() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            // Paso 1 — cargar desde SQLite inmediatamente
            val localNotes = withContext(Dispatchers.IO) {
                dbHelper.getLastTenNotes()
            }

            if (localNotes.isNotEmpty()) {
                _uiState.value = _uiState.value.copy(
                    notes = localNotes,
                    isLoading = false
                )
                Log.d("ParkingNotesVM", "Loaded ${localNotes.size} notes from SQLite")
            }

            // Paso 2 — sincronizar con Firestore
            try {
                val snapshot = withContext(Dispatchers.IO) {
                    firestore.collection("parkingNotes")
                        .orderBy("timestamp", Query.Direction.DESCENDING)
                        .limit(10)
                        .get()
                        .await()
                }

                val firestoreNotes = snapshot.documents.mapNotNull { doc ->
                    try {
                        ParkingNote(
                            id = doc.id,
                            userId = doc.getString("userId") ?: "",
                            userEmail = doc.getString("userEmail") ?: "",
                            userName = doc.getString("userName") ?: "",
                            message = doc.getString("message") ?: "",
                            floor = doc.getLong("floor")?.toInt() ?: 0,
                            timestamp = doc.getLong("timestamp") ?: 0L,
                            isLocal = false
                        )
                    } catch (e: Exception) {
                        null
                    }
                }

                // Guardar notas de Firestore en SQLite
                withContext(Dispatchers.IO) {
                    firestoreNotes.forEach { note ->
                        dbHelper.insertNote(note, isSynced = true)
                    }
                }

                // Sincronizar notas locales pendientes
                syncUnsyncedNotes()

                _uiState.value = _uiState.value.copy(
                    notes = firestoreNotes,
                    isLoading = false
                )
                Log.d("ParkingNotesVM", "Loaded ${firestoreNotes.size} notes from Firestore")

            } catch (e: Exception) {
                Log.e("ParkingNotesVM", "Error loading from Firestore: ${e.message}")
                if (_uiState.value.notes.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Could not load notes. Showing cached data."
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            }
        }
    }

    fun addNote(message: String, floor: Int = 0) {
        val user = auth.currentUser ?: return
        if (message.isBlank()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true)

            val note = ParkingNote(
                id = UUID.randomUUID().toString(),
                userId = user.uid,
                userEmail = user.email ?: "",
                userName = user.displayName ?: user.email?.substringBefore("@") ?: "Anonymous",
                message = message.trim(),
                floor = floor,
                timestamp = System.currentTimeMillis(),
                isLocal = true
            )

            // Guardar en SQLite inmediatamente
            withContext(Dispatchers.IO) {
                dbHelper.insertNote(note, isSynced = false)
            }

            // Guardar timestamp en DataStore
            dataStore.saveLastNoteTimestamp(note.timestamp)

            Log.d("ParkingNotesVM", "Note saved to SQLite: ${note.id}")

            // Intentar sincronizar con Firestore
            try {
                withContext(Dispatchers.IO) {
                    firestore.collection("parkingNotes")
                        .document(note.id)
                        .set(mapOf(
                            "userId" to note.userId,
                            "userEmail" to note.userEmail,
                            "userName" to note.userName,
                            "message" to note.message,
                            "floor" to note.floor,
                            "timestamp" to note.timestamp
                        ))
                        .await()
                    dbHelper.markNoteAsSynced(note.id)
                }
                Log.d("ParkingNotesVM", "Note synced to Firestore: ${note.id}")
            } catch (e: Exception) {
                Log.e("ParkingNotesVM", "Note saved locally, sync failed: ${e.message}")
            }

            _uiState.value = _uiState.value.copy(
                isSubmitting = false,
                submitSuccess = true
            )

            // Recargar notas
            loadNotes()
        }
    }

    private suspend fun syncUnsyncedNotes() {
        val unsyncedNotes = withContext(Dispatchers.IO) {
            dbHelper.getUnsyncedNotes()
        }

        unsyncedNotes.forEach { note ->
            try {
                withContext(Dispatchers.IO) {
                    firestore.collection("parkingNotes")
                        .document(note.id)
                        .set(mapOf(
                            "userId" to note.userId,
                            "userEmail" to note.userEmail,
                            "userName" to note.userName,
                            "message" to note.message,
                            "floor" to note.floor,
                            "timestamp" to note.timestamp
                        ))
                        .await()
                    dbHelper.markNoteAsSynced(note.id)
                }
                Log.d("ParkingNotesVM", "Unsynced note synced: ${note.id}")
            } catch (e: Exception) {
                Log.e("ParkingNotesVM", "Failed to sync note: ${note.id}")
            }
        }
    }

    fun toggleShowOnlyMyNotes() {
        viewModelScope.launch {
            val newValue = !_uiState.value.showOnlyMyNotes
            dataStore.saveShowOnlyMyNotes(newValue)
            _uiState.value = _uiState.value.copy(showOnlyMyNotes = newValue)
            Log.d("ParkingNotesVM", "Show only my notes: $newValue")
        }
    }

    fun resetSubmitSuccess() {
        _uiState.value = _uiState.value.copy(submitSuccess = false)
    }

    fun getCurrentUserEmail(): String {
        return auth.currentUser?.email ?: ""
    }

    fun formatTimestamp(timestamp: Long): String {
        val diff = System.currentTimeMillis() - timestamp
        val minutes = diff / 60000
        val hours = minutes / 60
        val days = hours / 24
        return when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            else -> "${days}d ago"
        }
    }
}