package com.example.cyclistweather.data.weather

import com.example.cyclistweather.domain.model.WeatherPoint

interface RouteWeatherRepository {
    suspend fun getWeatherForPoint(
        latitude: Double,
        longitude: Double
    ): List<WeatherPoint>
}
