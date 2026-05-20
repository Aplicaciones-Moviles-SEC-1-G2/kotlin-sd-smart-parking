package com.example.sd_smart_parking_app.data.repository

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.sd_smart_parking_app.data.model.ParkingStatsEntity

// ── DAO (Data Access Object) ──────────────────────────────────────────────────
// Siguiendo el patrón MVVM del proyecto:
//   UI (Screen) → ViewModel → Repository → DAO → Room DB
//
// El DAO es la capa de acceso a la BD. Define las operaciones SQL disponibles
// sobre la tabla "parking_stats". El ViewModel nunca toca Room directamente —
// siempre pasa por aquí.
//
// Decisión: todas las funciones son suspend para ejecutarse en Dispatchers.IO
// sin bloquear el hilo principal, igual que las demás operaciones de datos
// del proyecto.
@Dao
interface ParkingStatsDao {

    // ── Leer stats cacheados para un usuario ──────────────────────────────
    // Retorna null si no hay caché para ese email (primera vez que abre la app
    // o si se limpió la BD local).
    @Query("SELECT * FROM parking_stats WHERE ownerEmail = :email LIMIT 1")
    suspend fun getStatsByEmail(email: String): ParkingStatsEntity?

    // ── Guardar o actualizar stats ────────────────────────────────────────
    // OnConflictStrategy.REPLACE: si ya existe un registro para ese email,
    // lo sobreescribe completamente con los datos frescos de Firestore.
    // Decisión: REPLACE en lugar de UPDATE para simplificar — siempre
    // guardamos el objeto completo, nunca parcialmente.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(entity: ParkingStatsEntity)

    // ── Eliminar stats de un usuario ──────────────────────────────────────
    // Usado cuando el usuario cierra sesión — limpiamos su caché local
    // para que otro usuario no vea datos ajenos.
    @Query("DELETE FROM parking_stats WHERE ownerEmail = :email")
    suspend fun deleteByEmail(email: String)

    // ── Verificar si existe caché para un usuario ─────────────────────────
    // Permite al ViewModel saber rápidamente si hay datos locales
    // sin traer todo el objeto.
    @Query("SELECT COUNT(*) FROM parking_stats WHERE ownerEmail = :email")
    suspend fun hasCache(email: String): Int
}