package com.example.cyclistweather.domain.usecase

import com.example.cyclistweather.core.common.RouteMath
import com.example.cyclistweather.domain.model.ImportedRoute
import com.example.cyclistweather.domain.model.RouteSegment
import kotlin.math.roundToLong

class BuildRouteSegmentsUseCase {
    operator fun invoke(
        route: ImportedRoute,
        departureEpochMillis: Long,
        averageSpeedKmh: Double
    ): List<RouteSegment> {
        if (route.points.size < 2 || averageSpeedKmh <= 0.0) {
            return emptyList()
        }

        return route.points.zipWithNext().mapIndexedNotNull { index, (start, end) ->
            val segmentDistanceMeters = end.distanceFromStartMeters - start.distanceFromStartMeters
            if (segmentDistanceMeters <= 0.0) {
                return@mapIndexedNotNull null
            }

            RouteSegment(
                id = "segment-$index",
                start = start,
                end = end,
                distanceMeters = segmentDistanceMeters,
                bearingDegrees = RouteMath.bearingDegrees(
                    start.latitude,
                    start.longitude,
                    end.latitude,
                    end.longitude
                ),
                estimatedStartEpochMillis = departureEpochMillis + elapsedMillis(
                    start.distanceFromStartMeters,
                    averageSpeedKmh
                ),
                estimatedEndEpochMillis = departureEpochMillis + elapsedMillis(
                    end.distanceFromStartMeters,
                    averageSpeedKmh
                )
            )
        }
    }

    private fun elapsedMillis(distanceMeters: Double, averageSpeedKmh: Double): Long {
        val hours = (distanceMeters / 1_000.0) / averageSpeedKmh
        return (hours * 60 * 60 * 1_000).roundToLong()
    }
}
