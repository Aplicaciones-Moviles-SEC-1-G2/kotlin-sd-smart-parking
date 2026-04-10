package com.example.sd_smart_parking_app.data.repository

import com.example.sd_smart_parking_app.data.WeatherData
import com.example.sd_smart_parking_app.data.WeatherService

class WeatherRepository(private val weatherService: WeatherService) {
    suspend fun getWeatherData(): WeatherData? {
        return weatherService.getWeatherData()
    }
    fun getWeatherRecommendation(weather: WeatherData): String {
        return weatherService.getWeatherRecommendation(weather)
    }
    fun hasRainRisk(weather: WeatherData): Boolean {
        return weatherService.hasRainRisk(weather)
    }
}