package com.example.sd_smart_parking_app.data.repository

import com.example.sd_smart_parking_app.data.WeatherData
import com.example.sd_smart_parking_app.data.WeatherService

class WeatherRepository(private val weatherService: WeatherService) {

    /**
     * Obtiene los datos del clima actual
     */
    suspend fun getWeatherData(): WeatherData? {
        return weatherService.getWeatherData()
    }

    /**
     * Retorna recomendación basada en el clima
     */
    fun getWeatherRecommendation(weather: WeatherData): String {
        return weatherService.getWeatherRecommendation(weather)
    }

    /**
     * Retorna si hay riesgo de lluvia
     */
    fun hasRainRisk(weather: WeatherData): Boolean {
        return weatherService.hasRainRisk(weather)
    }
}