package com.example.cyclistweather.domain.model

data class RoutePoint(
    val latitude: Double,
    val longitude: Double,
    val elevationMeters: Double?,
    val distanceFromStartMeters: Double
)
