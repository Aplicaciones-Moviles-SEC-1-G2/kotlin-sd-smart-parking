package com.example.sd_smart_parking_app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sd_smart_parking_app.data.WeatherData
import com.example.sd_smart_parking_app.data.WeatherService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

data class WeatherUIState(
    val weatherData: WeatherData? = null,
    val recommendation: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val hasRainRisk: Boolean = false
)

class WeatherViewModel : ViewModel() {

    private val weatherService = WeatherService()

    private val _weatherState = MutableStateFlow(WeatherUIState())
    val weatherState: StateFlow<WeatherUIState> = _weatherState

    init {
        fetchWeather()
        // Actualizar clima cada 30 minutos
        startWeatherUpdates()
    }

    private fun fetchWeather() {
        viewModelScope.launch {
            _weatherState.value = _weatherState.value.copy(isLoading = true)
            try {
                val weather = weatherService.getWeatherData()
                if (weather != null) {
                    val recommendation = weatherService.getWeatherRecommendation(weather)
                    val hasRain = weatherService.hasRainRisk(weather)

                    _weatherState.value = WeatherUIState(
                        weatherData = weather,
                        recommendation = recommendation,
                        isLoading = false,
                        error = null,
                        hasRainRisk = hasRain
                    )
                } else {
                    _weatherState.value = _weatherState.value.copy(
                        isLoading = false,
                        error = "No se pudo obtener datos del clima"
                    )
                }
            } catch (e: Exception) {
                _weatherState.value = _weatherState.value.copy(
                    isLoading = false,
                    error = "Error: ${e.message}"
                )
            }
        }
    }

    private fun startWeatherUpdates() {
        viewModelScope.launch {
            while (true) {
                delay(1800000) // 30 minutos
                fetchWeather()
            }
        }
    }

    fun refreshWeather() {
        fetchWeather()
    }
}