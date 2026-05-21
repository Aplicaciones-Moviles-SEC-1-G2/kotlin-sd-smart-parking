package com.example.sd_smart_parking_app.data

import android.graphics.Bitmap
import android.util.LruCache
import android.util.Log

// ── Modelo de stats cacheados ─────────────────────────────────────────────────
// Contiene todos los valores calculados que la UI necesita mostrar.
// Se almacena completo en el LRU para evitar recalcular en cada acceso.
data class CostBreakdownStats(
    val allTimeSpentCOP: Double,
    val avgPerSessionCOP: Double,
    val hitDailyCapPercent: Double,
    val monthlySpendingCOP: Double,
    val monthLabel: String,
    val maxMonthlySpendingCOP: Double,
    val avgCostByDay: Map<String, Double>,
    val cachedAt: Long = System.currentTimeMillis()
)

// ── LRU Cache para CostBreakdownStats ────────────────────────────────────────
// LRU (Least Recently Used): cuando el caché está lleno, descarta la entrada
// que fue accedida hace más tiempo. Es la política más adecuada para datos
// de usuario porque los datos recientes son los más relevantes.
//
// Decisión de diseño — parámetros del LRU:
//
//   MAX_ENTRIES = 3
//   Razón: en un dispositivo compartido pueden haber hasta 3 usuarios activos.
//   Cada entrada ocupa ~1KB (solo primitivos y un mapa pequeño, sin Bitmaps).
//   3 entradas = ~3KB en memoria — completamente despreciable.
//   Mantener más entradas no tiene sentido porque los stats son por usuario
//   y solo uno está logueado a la vez.
//
//   TTL = 30 minutos
//   Razón: los costos cambian si el usuario tiene sesiones nuevas. 30 min es
//   un balance entre frescura y ahorro de llamadas a Firestore. Es más corto
//   que el TTL de ParkingStats (1h) porque el costo acumulado es más sensible
//   a cambios recientes.
//
//   Clave = email del usuario
//   Razón: identifica unívocamente al usuario sin exponer datos sensibles en
//   logs. Si el email cambia (raro pero posible), el caché simplemente falla
//   y hace un fetch fresco.
object CostBreakdownCache {

    // ── Parámetros del LRU ────────────────────────────────────────────────
    private const val MAX_ENTRIES = 3
    const val TTL_MS = 30 * 60 * 1000L  // 30 minutos en milisegundos

    // ── LRU Cache para stats de costo ─────────────────────────────────────
    // sizeOf no se sobreescribe porque el tamaño se mide en número de entradas
    // (maxSize = MAX_ENTRIES), no en bytes. Cada entrada vale 1 unidad.
    // Decisión: medir en entradas en lugar de bytes simplifica el código y es
    // suficientemente preciso dado que todas las entradas tienen tamaño similar.
    private val statsCache = object : LruCache<String, CostBreakdownStats>(MAX_ENTRIES) {
        override fun entryRemoved(
            evicted: Boolean,
            key: String,
            oldValue: CostBreakdownStats,
            newValue: CostBreakdownStats?
        ) {
            // Log para observar cuándo el LRU descarta entradas por capacidad
            if (evicted) {
                Log.d("CostBreakdownCache", "[LRU] Entrada eviccionada por capacidad — key: $key")
            }
        }
    }

    // ── LRU Cache para imágenes de perfil (Coil) ──────────────────────────
    // Caché en memoria para Bitmaps de foto de perfil.
    //
    // Decisión de parámetros:
    //   Tamaño = 1/8 de la memoria disponible de la app (en KB)
    //   Razón: es la recomendación estándar de Android para caché de imágenes
    //   en memoria. Suficiente para ~10-20 avatares a resolución normal (56dp).
    //   No usamos más porque Coil ya tiene su propio caché en disco — este
    //   caché en memoria es solo para acceso instantáneo sin I/O.
    //
    //   sizeOf = tamaño real del Bitmap en KB
    //   Razón: a diferencia del caché de stats, los Bitmaps tienen tamaños
    //   variables. Medir en bytes reales evita que imágenes grandes consuman
    //   más slots de los esperados.
    private val maxMemoryKB = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val imageCacheSizeKB = maxMemoryKB / 8

    val imageCache = object : LruCache<String, Bitmap>(imageCacheSizeKB) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            // byteCount / 1024 = tamaño en KB
            return bitmap.byteCount / 1024
        }
        override fun entryRemoved(
            evicted: Boolean,
            key: String,
            oldValue: Bitmap,
            newValue: Bitmap?
        ) {
            if (evicted) {
                Log.d("CostBreakdownCache", "[LRU-Image] Bitmap eviccionado — key: $key, size: ${oldValue.byteCount / 1024}KB")
            }
        }
    }

    // ── Operaciones del caché de stats ────────────────────────────────────

    // Guardar stats en el LRU
    fun put(userEmail: String, stats: CostBreakdownStats) {
        statsCache.put(userEmail, stats)
        Log.d("CostBreakdownCache", "[LRU] Stats guardados — key: $userEmail | size: ${statsCache.size()}/$MAX_ENTRIES")
    }

    // Leer stats del LRU — retorna null si no existe o si expiró el TTL
    fun get(userEmail: String): CostBreakdownStats? {
        val cached = statsCache.get(userEmail) ?: run {
            Log.d("CostBreakdownCache", "[LRU] Miss — no hay entrada para: $userEmail")
            return null
        }
        val age = System.currentTimeMillis() - cached.cachedAt
        return if (age < TTL_MS) {
            Log.d("CostBreakdownCache", "[LRU] Hit — edad: ${age / 1000}s | TTL: ${TTL_MS / 1000}s")
            cached
        } else {
            // Entrada expirada — la eliminamos del LRU activamente
            statsCache.remove(userEmail)
            Log.d("CostBreakdownCache", "[LRU] Expirado — edad: ${age / 1000}s > TTL: ${TTL_MS / 1000}s")
            null
        }
    }

    // Invalidar caché de un usuario (ej: al cerrar sesión)
    fun invalidate(userEmail: String) {
        statsCache.remove(userEmail)
        Log.d("CostBreakdownCache", "[LRU] Caché invalidado para: $userEmail")
    }

    // Limpiar todo el caché (ej: al detectar cambios críticos)
    fun clear() {
        statsCache.evictAll()
        imageCache.evictAll()
        Log.d("CostBreakdownCache", "[LRU] Caché completo limpiado")
    }

    // Métricas del LRU — útiles para Logcat durante sustentación
    fun logStats() {
        Log.d("CostBreakdownCache", "[LRU] Stats caché — hits: ${statsCache.hitCount()} | misses: ${statsCache.missCount()} | evictions: ${statsCache.evictionCount()} | size: ${statsCache.size()}/$MAX_ENTRIES")
        Log.d("CostBreakdownCache", "[LRU-Image] Image caché — hits: ${imageCache.hitCount()} | misses: ${imageCache.missCount()} | size: ${imageCache.size()}KB/${imageCacheSizeKB}KB")
    }
}