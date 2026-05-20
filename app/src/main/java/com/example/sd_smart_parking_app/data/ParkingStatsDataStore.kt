package com.example.sd_smart_parking_app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// ── DataStore para metadata del caché de ParkingStats ────────────────────────
// Siguiendo el patrón del proyecto, esta clase usa el mismo `context.dataStore`
// que ya está definido en UserPreferencesDataStore.kt mediante la extensión:
//   val Context.dataStore by preferencesDataStore(name = "user_preferences")
//
// Decisión: reutilizamos el mismo DataStore ("user_preferences") en lugar de
// crear uno nuevo. DataStore es un singleton por nombre — crear dos instancias
// con el mismo nombre causa corrupción. Al compartir el mismo archivo de
// preferencias, mantenemos un único punto de verdad para todas las preferencias
// del usuario, consistente con cómo el proyecto ya gestiona DataStore.
//
// Responsabilidad de esta clase: guardar y leer METADATA del caché,
// NO los stats en sí (esos van en Room). Específicamente:
//   - lastSyncTimestamp: cuándo fue la última sincronización con Firestore
//   - cachedUserEmail:   de qué usuario son los datos cacheados en Room
class ParkingStatsDataStore(private val context: Context) {

    companion object {
        // Timestamp Unix (ms) de la última sincronización exitosa con Firestore
        val LAST_SYNC_TIMESTAMP = longPreferencesKey("parking_stats_last_sync")

        // Email del usuario cuyos stats están actualmente en Room.
        // Si el email actual != este valor, el caché de Room es de otro usuario
        // y debe ignorarse / limpiarse.
        val CACHED_USER_EMAIL = stringPreferencesKey("parking_stats_cached_email")

        // Tiempo máximo de vida del caché: 1 hora en milisegundos.
        // Pasado este tiempo, el ViewModel refrescará desde Firestore aunque
        // ya haya datos en Room.
        const val CACHE_TTL_MS = 60 * 60 * 1000L // 1 hora
    }

    // ── Leer timestamp de última sincronización ───────────────────────────
    // Retorna 0L si nunca se ha sincronizado (primera apertura).
    val lastSyncTimestamp: Flow<Long> = context.dataStore.data
        .map { preferences -> preferences[LAST_SYNC_TIMESTAMP] ?: 0L }

    // ── Leer email del usuario cacheado ───────────────────────────────────
    // Retorna string vacío si no hay caché previo.
    val cachedUserEmail: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[CACHED_USER_EMAIL] ?: "" }

    // ── Guardar timestamp de sincronización exitosa ───────────────────────
    // Se llama desde el ViewModel justo después de escribir en Room,
    // garantizando que timestamp y datos Room estén siempre en sincronía.
    suspend fun saveLastSyncTimestamp(timestamp: Long) {
        context.dataStore.edit { preferences ->
            preferences[LAST_SYNC_TIMESTAMP] = timestamp
        }
    }

    // ── Guardar email del usuario cacheado ────────────────────────────────
    // Se llama junto con saveLastSyncTimestamp al completar una sincronización.
    suspend fun saveCachedUserEmail(email: String) {
        context.dataStore.edit { preferences ->
            preferences[CACHED_USER_EMAIL] = email
        }
    }

    // ── Verificar si el caché está vigente ────────────────────────────────
    // Lógica: el caché es válido si fue guardado hace menos de CACHE_TTL_MS.
    // El ViewModel usa esto para decidir si mostrar Room directamente
    // o forzar un refresh desde Firestore.
    fun isCacheValid(lastSync: Long): Boolean {
        if (lastSync == 0L) return false
        val age = System.currentTimeMillis() - lastSync
        return age < CACHE_TTL_MS
    }

    // ── Limpiar metadata del caché ────────────────────────────────────────
    // Se llama cuando el usuario cierra sesión, junto con el borrado
    // del registro en Room. Garantiza que no quede metadata huérfana.
    suspend fun clearCacheMetadata() {
        context.dataStore.edit { preferences ->
            preferences.remove(LAST_SYNC_TIMESTAMP)
            preferences.remove(CACHED_USER_EMAIL)
        }
    }
}