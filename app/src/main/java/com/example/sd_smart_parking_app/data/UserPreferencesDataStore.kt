package com.example.sd_smart_parking_app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferencesDataStore(private val context: Context) {

    companion object {
        val LAST_NOTE_TIMESTAMP = longPreferencesKey("last_note_timestamp")
        val PREFERRED_FLOOR_FILTER = stringPreferencesKey("preferred_floor_filter")
        val SHOW_ONLY_MY_NOTES = booleanPreferencesKey("show_only_my_notes")
        val NOTES_SORT_ORDER = stringPreferencesKey("notes_sort_order")
    }

    // Leer timestamp de la última nota escrita
    val lastNoteTimestamp: Flow<Long> = context.dataStore.data
        .map { preferences -> preferences[LAST_NOTE_TIMESTAMP] ?: 0L }

    // Leer filtro de piso preferido
    val preferredFloorFilter: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[PREFERRED_FLOOR_FILTER] ?: "all" }

    // Leer si el usuario prefiere ver solo sus notas
    val showOnlyMyNotes: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[SHOW_ONLY_MY_NOTES] ?: false }

    // Leer orden de las notas
    val notesSortOrder: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[NOTES_SORT_ORDER] ?: "newest" }

    // Guardar timestamp de la última nota
    suspend fun saveLastNoteTimestamp(timestamp: Long) {
        context.dataStore.edit { preferences ->
            preferences[LAST_NOTE_TIMESTAMP] = timestamp
        }
    }

    // Guardar filtro de piso preferido
    suspend fun savePreferredFloorFilter(floor: String) {
        context.dataStore.edit { preferences ->
            preferences[PREFERRED_FLOOR_FILTER] = floor
        }
    }

    // Guardar preferencia de mostrar solo mis notas
    suspend fun saveShowOnlyMyNotes(showOnly: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SHOW_ONLY_MY_NOTES] = showOnly
        }
    }

    // Guardar orden de las notas
    suspend fun saveNotesSortOrder(sortOrder: String) {
        context.dataStore.edit { preferences ->
            preferences[NOTES_SORT_ORDER] = sortOrder
        }
    }
}