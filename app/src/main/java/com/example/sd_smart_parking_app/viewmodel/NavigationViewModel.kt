package com.example.sd_smart_parking_app.viewmodel

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class NavigationState(
    val bearing: Float = 0f,  // Dirección en grados (0-360)
    val currentLocation: Location? = null,
    val distanceToDestination: Float = 0f,
    val isLocationAvailable: Boolean = false,
    val isCompassAvailable: Boolean = false
)

class NavigationViewModel(context: Context) : ViewModel(), SensorEventListener {

    private val _navigationState = MutableStateFlow(NavigationState())
    val navigationState: StateFlow<NavigationState> = _navigationState

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

    // Sensores
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    // Arrays para cálculos del magnetómetro
    private val accelerometerReading = FloatArray(3)
    private val magnetometerReading = FloatArray(3)
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    // Destino (coordenadas del parqueadero SD Building)
    private val destinationLatitude = 4.7165
    private val destinationLongitude = -74.0447

    init {
        startSensors()
    }

    private fun startSensors() {
        // Registrar listeners de sensores
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        magnetometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }

        updateCompassAvailability()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                System.arraycopy(event.values, 0, accelerometerReading, 0, 3)
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                System.arraycopy(event.values, 0, magnetometerReading, 0, 3)
            }
        }

        // Calcular la orientación
        SensorManager.getRotationMatrix(
            rotationMatrix,
            null,
            accelerometerReading,
            magnetometerReading
        )

        SensorManager.getOrientation(rotationMatrix, orientationAngles)

        // orientationAngles[0] es el azimut (bearing) en radianes
        // Convertir a grados (0-360)
        val bearingDegrees = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
        val bearing = (bearingDegrees + 360) % 360

        _navigationState.value = _navigationState.value.copy(
            bearing = bearing,
            isCompassAvailable = true
        )
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No es necesario hacer nada aquí
    }

    fun updateLocationAndBearing(location: Location) {
        viewModelScope.launch {
            // Calcular el bearing (dirección) hacia el destino
            val bearingToDestination = calculateBearing(
                location.latitude,
                location.longitude,
                destinationLatitude,
                destinationLongitude
            )

            // Calcular la distancia
            val distance = calculateDistance(
                location.latitude,
                location.longitude,
                destinationLatitude,
                destinationLongitude
            )

            _navigationState.value = _navigationState.value.copy(
                currentLocation = location,
                distanceToDestination = distance,
                isLocationAvailable = true
            )
        }
    }

    private fun calculateBearing(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Float {
        val dLon = Math.toRadians(lon2 - lon1)
        val lat1Rad = Math.toRadians(lat1)
        val lat2Rad = Math.toRadians(lat2)

        val y = sin(dLon) * cos(lat2Rad)
        val x = cos(lat1Rad) * sin(lat2Rad) - sin(lat1Rad) * cos(lat2Rad) * cos(dLon)

        val bearingRad = atan2(y, x)
        return (Math.toDegrees(bearingRad).toFloat() + 360) % 360
    }

    private fun calculateDistance(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }

    private fun updateCompassAvailability() {
        _navigationState.value = _navigationState.value.copy(
            isCompassAvailable = accelerometer != null && magnetometer != null
        )
    }

    override fun onCleared() {
        super.onCleared()
        sensorManager.unregisterListener(this)
    }
}