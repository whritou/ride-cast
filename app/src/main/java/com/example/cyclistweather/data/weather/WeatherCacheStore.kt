package com.example.cyclistweather.data.weather

import com.example.cyclistweather.domain.model.WeatherPoint

interface WeatherCacheStore {
    suspend fun getForecast(cacheKey: String): CachedWeatherForecast?

    suspend fun putForecast(
        cacheKey: String,
        latitude: Double,
        longitude: Double,
        forecast: CachedWeatherForecast
    )
}

data class CachedWeatherForecast(
    val points: List<WeatherPoint>,
    val fetchedAtEpochMillis: Long
)
