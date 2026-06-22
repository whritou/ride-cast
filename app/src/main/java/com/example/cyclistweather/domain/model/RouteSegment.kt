package com.example.cyclistweather.domain.model

data class RouteSegment(
    val id: String,
    val start: RoutePoint,
    val end: RoutePoint,
    val distanceMeters: Double,
    val bearingDegrees: Double,
    val estimatedStartEpochMillis: Long,
    val estimatedEndEpochMillis: Long
)
