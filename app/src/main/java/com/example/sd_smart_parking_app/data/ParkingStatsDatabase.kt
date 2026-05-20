package com.example.sd_smart_parking_app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.sd_smart_parking_app.data.model.MapTypeConverter
import com.example.sd_smart_parking_app.data.model.ParkingStatsEntity
import com.example.sd_smart_parking_app.data.repository.ParkingStatsDao

// ── Base de datos Room ────────────────────────────────────────────────────────
// Siguiendo el patrón MVVM del proyecto, esta clase vive en la capa `data/`
// junto a otros servicios de infraestructura como NetworkMonitor,
// NotificationManager y ParkingNotesDatabaseHelper.
//
// Decisión de diseño: Singleton con double-checked locking.
// Solo existe una instancia de la BD en toda la app. Crear múltiples
// instancias de RoomDatabase es costoso y puede causar corrupción de datos
// si dos instancias escriben al mismo tiempo.
//
// version = 1: primera versión del esquema. Si en el futuro se agregan
// columnas, se incrementa la versión y se provee una Migration.
@Database(
    entities = [ParkingStatsEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(MapTypeConverter::class)
abstract class ParkingStatsDatabase : RoomDatabase() {

    // Room genera la implementación concreta de este DAO en tiempo de compilación
    abstract fun parkingStatsDao(): ParkingStatsDao

    companion object {

        // @Volatile garantiza que el valor de INSTANCE sea siempre visible
        // a todos los hilos — sin caché de CPU que pueda causar lecturas stale.
        @Volatile
        private var INSTANCE: ParkingStatsDatabase? = null

        fun getInstance(context: Context): ParkingStatsDatabase {
            // Double-checked locking: evita bloquear el hilo cada vez que
            // se solicita la instancia. Solo bloquea la primera vez.
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): ParkingStatsDatabase {
            return Room.databaseBuilder(
                context.applicationContext,  // applicationContext evita memory leaks
                ParkingStatsDatabase::class.java,
                "parking_stats_db"           // nombre del archivo .db en disco
            )
                // fallbackToDestructiveMigration: si la versión cambia y no hay
                // Migration definida, Room recrea la BD desde cero en lugar de
                // crashear. Aceptable para un caché — los datos se recuperan de
                // Firestore en el siguiente refresh.
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}