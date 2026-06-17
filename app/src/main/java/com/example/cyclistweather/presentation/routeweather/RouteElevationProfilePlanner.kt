package com.example.cyclistweather.presentation.routeweather

import com.example.cyclistweather.domain.model.RoutePoint
import kotlin.math.roundToInt

internal object RouteElevationProfilePlanner {
    fun profile(points: List<RoutePoint>): RouteElevationProfile {
        if (points.isEmpty()) {
            return RouteElevationProfile(
                minElevationMeters = 0,
                maxElevationMeters = 0,
                totalDistanceMeters = 0.0,
                samples = emptyList()
            )
        }

        val elevations = points.map { it.elevationMeters ?: 0.0 }
        val minElevation = elevations.minOrNull() ?: 0.0
        val maxElevation = elevations.maxOrNull() ?: minElevation
        val elevationSpan = (maxElevation - minElevation).takeIf { it > 0.0 } ?: 1.0
        val totalDistance = points.maxOf { it.distanceFromStartMeters }.coerceAtLeast(0.0)
        val distanceSpan = totalDistance.takeIf { it > 0.0 } ?: 1.0

        return RouteElevationProfile(
            minElevationMeters = minElevation.roundToInt(),
            maxElevationMeters = maxElevation.roundToInt(),
            totalDistanceMeters = totalDistance,
            samples = points.mapIndexed { index, point ->
                val elevation = elevations[index]
                RouteElevationSample(
                    distanceFraction = (point.distanceFromStartMeters / distanceSpan).toFloat().coerceIn(0f, 1f),
                    elevationFraction = ((elevation - minElevation) / elevationSpan).toFloat().coerceIn(0f, 1f)
                )
            }
        )
    }
}

internal data class RouteElevationProfile(
    val minElevationMeters: Int,
    val maxElevationMeters: Int,
    val totalDistanceMeters: Double,
    val samples: List<RouteElevationSample>
)

internal data class RouteElevationSample(
    val distanceFraction: Float,
    val elevationFraction: Float
)
