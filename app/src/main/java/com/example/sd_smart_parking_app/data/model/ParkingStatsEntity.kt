package com.example.sd_smart_parking_app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

// ── Type Converter: Map<String, Double> ↔ String (JSON) ──────────────────────
// Room solo puede persistir tipos primitivos. El mapa de duraciones por día
// se serializa a JSON para guardarlo en la columna y se deserializa al leerlo.
class MapTypeConverter {
    private val gson = Gson()

    @TypeConverter
    fun fromMap(map: Map<String, Double>): String =
        gson.toJson(map)

    @TypeConverter
    fun toMap(json: String): Map<String, Double> {
        val type = object : TypeToken<Map<String, Double>>() {}.type
        return gson.fromJson(json, type) ?: emptyMap()
    }
}

// ── Entidad Room ──────────────────────────────────────────────────────────────
// Representa una fila en la tabla "parking_stats".
// Decisión: usamos el email del usuario como PrimaryKey porque cada usuario
// tiene exactamente un set de stats cacheados. Si el usuario cambia, se
// sobreescribe el registro existente (onConflict = REPLACE en el DAO).
@Entity(tableName = "parking_stats")
@TypeConverters(MapTypeConverter::class)
data class ParkingStatsEntity(

    @PrimaryKey
    val ownerEmail: String,

    // ── KPIs principales ──────────────────────────────────────────────────
    val totalSessions: Int,
    val totalTimeHours: Double,
    val totalPaidCOP: Double,
    val avgSessionHours: Double,

    // ── Insights ──────────────────────────────────────────────────────────
    val busiestDay: String,
    val busiestDaySessions: Int,
    val favouriteFloor: Int,
    val favouriteFloorVisits: Int,

    // ── Gráfica (serializada como JSON) ───────────────────────────────────
    val avgDurationByDay: Map<String, Double>,

    // ── Metadata del caché ────────────────────────────────────────────────
    // Timestamp Unix (ms) de cuándo se guardaron estos datos.
    // Usado por el ViewModel para decidir si el caché está vigente.
    val cachedAt: Long = System.currentTimeMillis()
)