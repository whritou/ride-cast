package com.example.cyclistweather.domain.model

data class WeatherPoint(
    val latitude: Double,
    val longitude: Double,
    val timeEpochMillis: Long,
    val temperatureCelsius: Double,
    val apparentTemperatureCelsius: Double?,
    val windSpeedKmh: Double,
    val windDirectionDegrees: Double,
    val windGustKmh: Double?,
    val precipitationProbabilityPercent: Int?,
    val precipitationMm: Double?,
    val cloudCoverPercent: Int?,
    val weatherCode: Int,
    val relativeHumidityPercent: Int? = null,
    val uvIndex: Double? = null,
    val visibilityMeters: Double? = null,
    val europeanAqi: Int? = null,
    val isDay: Boolean? = null
)
