package com.example.cyclistweather.core.common

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

object RouteFormatters {
    fun formatDistance(meters: Double): String {
        return if (meters < 1_000.0) {
            "${meters.roundToInt()} m"
        } else {
            String.format(Locale.US, "%.1f km", meters / 1_000.0)
        }
    }

    fun estimatedDurationMinutes(distanceMeters: Double, averageSpeedKmh: Double): Int {
        if (distanceMeters <= 0.0 || averageSpeedKmh <= 0.0) {
            return 0
        }

        val hours = (distanceMeters / 1_000.0) / averageSpeedKmh
        return (hours * 60).roundToInt().coerceAtLeast(1)
    }

    fun formatDuration(minutes: Int): String {
        val hours = minutes / 60
        val remainingMinutes = minutes % 60
        return when {
            hours <= 0 -> "${remainingMinutes}m"
            remainingMinutes == 0 -> "${hours}h"
            else -> "${hours}h ${remainingMinutes}m"
        }
    }

    fun formatElevationGain(meters: Double?): String {
        return meters?.let { "+${it.roundToInt()} m" } ?: "No elevation"
    }

    fun formatClock(epochMillis: Long): String {
        return SimpleDateFormat("HH:mm", Locale.US).format(Date(epochMillis))
    }

    fun formatShortDateTime(epochMillis: Long): String {
        return SimpleDateFormat("EEE HH:mm", Locale.US).format(Date(epochMillis))
    }

    fun formatTemperature(valueCelsius: Double): String {
        return "${valueCelsius.roundToInt()} C"
    }

    fun formatWindDirection(degrees: Double): String {
        val directions = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
        val index = ((degrees / 45.0).roundToInt() % directions.size).coerceAtLeast(0)
        return directions[index]
    }

    fun formatUvIndex(uvIndex: Double): String = uvIndex.roundToInt().toString()

    fun uvCategory(uvIndex: Double): String {
        return when {
            uvIndex < 3.0 -> "Low"
            uvIndex < 6.0 -> "Moderate"
            uvIndex < 8.0 -> "High"
            uvIndex < 11.0 -> "Very high"
            else -> "Extreme"
        }
    }

    fun aqiCategory(europeanAqi: Int): String {
        return when {
            europeanAqi <= 20 -> "Good"
            europeanAqi <= 40 -> "Fair"
            europeanAqi <= 60 -> "Moderate"
            europeanAqi <= 80 -> "Poor"
            europeanAqi <= 100 -> "Very poor"
            else -> "Extreme"
        }
    }

    fun formatVisibility(meters: Double): String {
        return if (meters < 1_000.0) {
            "${meters.roundToInt()} m"
        } else {
            "${(meters / 1_000.0).roundToInt()} km"
        }
    }
}
