package com.example.cyclistweather.domain.optimizer

import com.example.cyclistweather.domain.model.ImportedRoute
import com.example.cyclistweather.domain.model.RouteWeatherSnapshot
import com.example.cyclistweather.domain.model.SegmentWeather
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.abs

typealias RouteWeatherSnapshotBuilder = suspend (
    route: ImportedRoute,
    departureEpochMillis: Long,
    averageSpeedKmh: Double
) -> RouteWeatherSnapshot

class OptimizeDepartureTimeUseCase(
    private val buildRouteWeather: RouteWeatherSnapshotBuilder,
    private val config: DepartureOptimizerConfig = DepartureOptimizerConfig(),
    private val timeZone: TimeZone = TimeZone.getDefault()
) {
    suspend operator fun invoke(
        route: ImportedRoute,
        requestedDepartureEpochMillis: Long,
        averageSpeedKmh: Double
    ): DepartureOptimizationResult {
        val candidates = candidateDepartures(requestedDepartureEpochMillis).map { departure ->
            DepartureCandidate(
                departureEpochMillis = departure,
                snapshot = buildRouteWeather(route, departure, averageSpeedKmh)
            )
        }
        val currentCandidate = candidates.minBy { abs(it.departureEpochMillis - requestedDepartureEpochMillis) }
        val bestCandidate = candidates.sortedWith(
            compareByDescending<DepartureCandidate> { it.snapshot.rideScore.total }
                .thenBy { abs(it.departureEpochMillis - requestedDepartureEpochMillis) }
                .thenBy { it.departureEpochMillis }
        ).first()

        return DepartureOptimizationResult(
            requestedDepartureEpochMillis = requestedDepartureEpochMillis,
            candidates = candidates,
            bestCandidate = bestCandidate,
            currentCandidate = currentCandidate,
            reasons = recommendationReasons(bestCandidate, currentCandidate)
        )
    }

    private fun candidateDepartures(requestedDepartureEpochMillis: Long): List<Long> {
        val candidate = Calendar.getInstance(timeZone).apply {
            timeInMillis = requestedDepartureEpochMillis
            set(Calendar.HOUR_OF_DAY, config.startHourOfDay)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val end = Calendar.getInstance(timeZone).apply {
            timeInMillis = requestedDepartureEpochMillis
            set(Calendar.HOUR_OF_DAY, config.endHourOfDay)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        return buildList {
            while (candidate.timeInMillis <= end) {
                add(candidate.timeInMillis)
                candidate.add(Calendar.MINUTE, config.stepMinutes)
            }
        }
    }

    private fun recommendationReasons(
        bestCandidate: DepartureCandidate,
        currentCandidate: DepartureCandidate
    ): List<String> {
        if (bestCandidate.departureEpochMillis == currentCandidate.departureEpochMillis) {
            return listOf("Your selected departure is already the best-scoring window.")
        }

        val bestSegments = bestCandidate.snapshot.segmentWeather
        val currentSegments = currentCandidate.snapshot.segmentWeather
        val scoreGain = bestCandidate.snapshot.rideScore.total - currentCandidate.snapshot.rideScore.total

        return buildList {
            if (scoreGain > 0) {
                add("Improves ride score by $scoreGain points.")
            }
            if (averageRain(currentSegments) - averageRain(bestSegments) >= 5.0) {
                add("Lower rain risk across the ride.")
            }
            if (averageHeadwind(currentSegments) - averageHeadwind(bestSegments) >= 2.0) {
                add("Less headwind on sampled segments.")
            }
            if (temperatureDiscomfort(currentSegments) - temperatureDiscomfort(bestSegments) >= 2.0) {
                add("More comfortable temperature window.")
            }
            if (isEmpty()) {
                add("Best overall weather score in the scanned window.")
            }
        }
    }

    private fun averageRain(segmentWeather: List<SegmentWeather>): Double {
        return segmentWeather.averageOfOrZero {
            (it.weather.precipitationProbabilityPercent ?: 0).toDouble()
        }
    }

    private fun averageHeadwind(segmentWeather: List<SegmentWeather>): Double {
        return segmentWeather.averageOfOrZero {
            it.windComponents.headwindKmh.coerceAtLeast(0.0)
        }
    }

    private fun temperatureDiscomfort(segmentWeather: List<SegmentWeather>): Double {
        return segmentWeather.averageOfOrZero {
            abs(it.weather.temperatureCelsius - IDEAL_TEMPERATURE_CELSIUS)
        }
    }

    private fun List<SegmentWeather>.averageOfOrZero(selector: (SegmentWeather) -> Double): Double {
        return if (isEmpty()) 0.0 else sumOf(selector) / size
    }

    private companion object {
        const val IDEAL_TEMPERATURE_CELSIUS = 18.0
    }
}
