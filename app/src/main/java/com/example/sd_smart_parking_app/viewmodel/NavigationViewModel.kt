package com.example.sd_smart_parking_app.viewmodel

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sd_smart_parking_app.data.LocationManager
import com.example.sd_smart_parking_app.data.repository.LocationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

data class NavigationState(
    val bearing: Float = 0f,
    val currentLocation: Location? = null,
    val distanceToDestination: Float = 0f,
    val isLocationAvailable: Boolean = false,
    val isCompassAvailable: Boolean = false
)

class NavigationViewModel(context: Context) : ViewModel(), SensorEventListener {

    private val _navigationState = MutableStateFlow(NavigationState())
    val navigationState: StateFlow<NavigationState> = _navigationState

    // Repository
    private val locationRepository = LocationRepository(LocationManager(context))

    // Sensores
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
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
        startLocationUpdates()
    }

    private fun startSensors() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        magnetometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }

        updateCompassAvailability()
    }

    private fun startLocationUpdates() {
        viewModelScope.launch {
            locationRepository.getLocationUpdates().collect { location ->
                updateLocationAndBearing(location)
            }
        }
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

        SensorManager.getRotationMatrix(
            rotationMatrix,
            null,
            accelerometerReading,
            magnetometerReading
        )

        SensorManager.getOrientation(rotationMatrix, orientationAngles)

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

    private fun updateLocationAndBearing(location: Location) {
        viewModelScope.launch {
            // Calcular distancia y bearing usando el Repository
            val distance = locationRepository.calculateDistance(
                location.latitude,
                location.longitude,
                destinationLatitude,
                destinationLongitude
            )

            val bearing = locationRepository.calculateBearing(
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