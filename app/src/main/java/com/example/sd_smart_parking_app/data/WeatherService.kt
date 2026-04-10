package com.example.sd_smart_parking_app.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import com.google.gson.annotations.SerializedName

data class WeatherResponse(
    @SerializedName("main")
    val main: MainWeatherData,
    @SerializedName("weather")
    val weather: List<WeatherDescription>,
    @SerializedName("wind")
    val wind: WindData,
    @SerializedName("clouds")
    val clouds: CloudsData,
    @SerializedName("rain")
    val rain: RainData?
)

data class MainWeatherData(
    @SerializedName("temp")
    val temperature: Double,
    @SerializedName("feels_like")
    val feelsLike: Double,
    @SerializedName("humidity")
    val humidity: Int
)

data class WeatherDescription(
    @SerializedName("main")
    val main: String,
    @SerializedName("description")
    val description: String
)

data class WindData(
    @SerializedName("speed")
    val speed: Double
)

data class CloudsData(
    @SerializedName("all")
    val cloudiness: Int
)

data class RainData(
    @SerializedName("1h")
    val oneHour: Int?
)

data class WeatherData(
    val temperature: Double,
    val feelsLike: Double,
    val humidity: Int,
    val description: String,
    val main: String,
    val windSpeed: Double,
    val cloudiness: Int,
    val rainProbability: Int
)

// ============ Retrofit API Interface ============

interface WeatherApi {
    @GET("weather")
    suspend fun getWeather(
        @Query("q") city: String,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric"
    ): WeatherResponse
}

// ============ Weather Service ============

class WeatherService {

    companion object {
        private const val BASE_URL = "https://api.openweathermap.org/data/2.5/"
        private const val API_KEY = "1d946e6f6009723334162497c7120530"
        private const val CITY = "Bogota"
    }

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val weatherApi = retrofit.create(WeatherApi::class.java)

    suspend fun getWeatherData(): WeatherData? {
        return try {
            val response = weatherApi.getWeather(
                city = CITY,
                apiKey = API_KEY,
                units = "metric"
            )

            convertToWeatherData(response)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    private fun convertToWeatherData(response: WeatherResponse): WeatherData {
        val weather = response.weather.firstOrNull()
        val rainProbability = response.rain?.oneHour ?: 0

        return WeatherData(
            temperature = response.main.temperature,
            feelsLike = response.main.feelsLike,
            humidity = response.main.humidity,
            description = weather?.description ?: "N/A",
            main = weather?.main ?: "Unknown",
            windSpeed = response.wind.speed,
            cloudiness = response.clouds.cloudiness,
            rainProbability = rainProbability
        )
    }
    fun getWeatherRecommendation(weather: WeatherData): String {
        return when {
            weather.main.contains("Rain", ignoreCase = true) ->
                "⛈️ Rain Detected. Consider parking in a roofed floor."
            weather.main.contains("Cloud", ignoreCase = true) ->
                "☁️ Cloudly sky. Good time to park"
            weather.main.contains("Clear", ignoreCase = true) ->
                "☀️ Sunny day. Your car will be exposed to the sun."
            weather.main.contains("Snow", ignoreCase = true) ->
                "❄️ Snow. Be careful while driving, the parking could be slippery."
            else -> "🌤️ Neutral weather. Choose the floor you prefer."
        }
    }

    fun hasRainRisk(weather: WeatherData): Boolean {
        return weather.main.contains("Rain", ignoreCase = true) ||
                weather.rainProbability > 30
    }
}