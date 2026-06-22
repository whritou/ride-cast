package com.example.cyclistweather.domain.usecase

import com.example.cyclistweather.core.common.RouteMath
import com.example.cyclistweather.domain.model.ImportedRoute
import com.example.cyclistweather.domain.model.RoutePoint
import com.example.cyclistweather.domain.model.RouteWeatherSample
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.math.roundToLong

class SampleRouteUseCase {
    operator fun invoke(
        route: ImportedRoute,
        departureEpochMillis: Long,
        averageSpeedKmh: Double
    ): List<RouteWeatherSample> {
        if (route.points.size < 2 || averageSpeedKmh <= 0.0) {
            return emptyList()
        }

        val intervalMeters = sampleIntervalMeters(route.totalDistanceMeters)
        val targets = buildList {
            add(0.0)
            var next = intervalMeters
            while (next < route.totalDistanceMeters) {
                add(next)
                next += intervalMeters
            }
            add(route.totalDistanceMeters)
        }.distinctBy { it.roundToInt() }

        return targets.mapIndexed { index, targetDistance ->
            val pointIndex = route.points.indexOfFirst {
                it.distanceFromStartMeters >= targetDistance
            }.takeIf { it >= 0 } ?: route.points.lastIndex

            val point = route.points[pointIndex]
            val arrivalDistanceMeters = targetDistance.coerceIn(0.0, route.totalDistanceMeters)

            RouteWeatherSample(
                id = "sample-$index",
                point = point,
                distanceFromStartMeters = arrivalDistanceMeters,
                estimatedArrivalEpochMillis = departureEpochMillis + elapsedMillis(
                    arrivalDistanceMeters,
                    averageSpeedKmh
                ),
                routeBearingDegrees = route.bearingNear(pointIndex)
            )
        }
    }

    companion object {
        internal const val MAX_WEATHER_SAMPLE_COUNT = 25

        internal fun sampleIntervalMeters(totalDistanceMeters: Double): Double {
            if (totalDistanceMeters <= 0.0) {
                return SHORT_ROUTE_SAMPLE_INTERVAL_METERS
            }

            val baseIntervalMeters = when {
                totalDistanceMeters < MEDIUM_ROUTE_DISTANCE_THRESHOLD_METERS -> SHORT_ROUTE_SAMPLE_INTERVAL_METERS
                else -> STANDARD_ROUTE_SAMPLE_INTERVAL_METERS
            }
            val uncappedSampleCount = ceil(totalDistanceMeters / baseIntervalMeters).toInt() + 1

            return if (uncappedSampleCount > MAX_WEATHER_SAMPLE_COUNT) {
                totalDistanceMeters / (MAX_WEATHER_SAMPLE_COUNT - 1)
            } else {
                baseIntervalMeters
            }
        }
    }

    private fun elapsedMillis(distanceMeters: Double, averageSpeedKmh: Double): Long {
        val hours = (distanceMeters / 1_000.0) / averageSpeedKmh
        return (hours * 60 * 60 * 1_000).roundToLong()
    }

    private fun ImportedRoute.bearingNear(pointIndex: Int): Double {
        val start: RoutePoint
        val end: RoutePoint
        if (pointIndex < points.lastIndex) {
            start = points[pointIndex]
            end = points[pointIndex + 1]
        } else {
            start = points[(pointIndex - 1).coerceAtLeast(0)]
            end = points[pointIndex]
        }

        return RouteMath.bearingDegrees(
            start.latitude,
            start.longitude,
            end.latitude,
            end.longitude
        )
    }
}

private const val SHORT_ROUTE_SAMPLE_INTERVAL_METERS = 2_000.0
private const val STANDARD_ROUTE_SAMPLE_INTERVAL_METERS = 5_000.0
private const val MEDIUM_ROUTE_DISTANCE_THRESHOLD_METERS = 20_000.0
