package com.example.cyclistweather.domain.model

data class ImportedRoute(
    val id: String,
    val name: String,
    val points: List<RoutePoint>,
    val totalDistanceMeters: Double,
    val totalElevationGainMeters: Double?,
    val createdAtEpochMillis: Long,
    val isFavorite: Boolean = false
)
