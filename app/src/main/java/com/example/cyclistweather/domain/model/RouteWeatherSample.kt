package com.example.cyclistweather.domain.model

data class RouteWeatherSample(
    val id: String,
    val point: RoutePoint,
    val distanceFromStartMeters: Double,
    val estimatedArrivalEpochMillis: Long,
    val routeBearingDegrees: Double
)
