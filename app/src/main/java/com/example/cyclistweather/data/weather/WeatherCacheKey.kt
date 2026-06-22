package com.example.cyclistweather.data.weather

import java.util.Locale

internal object WeatherCacheKey {
    fun forCoordinate(
        latitude: Double,
        longitude: Double
    ): String {
        return "${latitude.roundCoordinate()},${longitude.roundCoordinate()}"
    }

    private fun Double.roundCoordinate(): String {
        return String.format(Locale.US, "%.3f", this)
    }
}
