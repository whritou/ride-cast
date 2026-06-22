package com.example.cyclistweather.presentation.model

import com.example.cyclistweather.core.common.RouteFormatters
import com.example.cyclistweather.domain.model.ImportedRoute
import com.example.cyclistweather.domain.model.RideScore
import com.example.cyclistweather.domain.model.SegmentWeather
import com.example.cyclistweather.domain.model.mapWeatherCode
import com.example.cyclistweather.domain.optimizer.DepartureOptimizationResult
import kotlin.math.roundToInt

object RideWeatherUiMapper {
    fun route(
        route: ImportedRoute,
        averageSpeedKmh: Double
    ): RideRouteUiModel {
        val durationMinutes = RouteFormatters.estimatedDurationMinutes(
            distanceMeters = route.totalDistanceMeters,
            averageSpeedKmh = averageSpeedKmh
        )

        return RideRouteUiModel(
            id = route.id,
            name = route.name,
            distance = RouteFormatters.formatDistance(route.totalDistanceMeters),
            elevationGain = RouteFormatters.formatElevationGain(route.totalElevationGainMeters),
            estimatedDuration = RouteFormatters.formatDuration(durationMinutes)
        )
    }

    fun rideScore(score: RideScore): RideScoreUiModel {
        return RideScoreUiModel(
            score = score.total,
            riskLevel = WeatherRiskLevel.fromScore(score.total)
        )
    }

    fun weatherPoint(segment: SegmentWeather): WeatherPointUiModel {
        val rainProbability = segment.weather.precipitationProbabilityPercent ?: 0
        // Unit-only values; the surrounding UI supplies the localized label ("Gusts", "Rain", …).
        val gust = segment.weather.windGustKmh?.roundToInt()?.let { "$it km/h" }

        return WeatherPointUiModel(
            id = segment.sample.id,
            distance = RouteFormatters.formatDistance(segment.sample.distanceFromStartMeters),
            arrivalTime = RouteFormatters.formatClock(segment.sample.estimatedArrivalEpochMillis),
            temperature = RouteFormatters.formatTemperature(segment.weather.temperatureCelsius),
            wind = "${segment.weather.windSpeedKmh.roundToInt()} km/h ${
                RouteFormatters.formatWindDirection(segment.weather.windDirectionDegrees)
            }",
            gust = gust,
            rain = "$rainProbability%",
            relativeWindRes = segment.relativeWind.labelRes,
            conditionRes = mapWeatherCode(segment.weather.weatherCode).labelRes,
            riskLevel = WeatherRiskLevel.fromScore(segment.score.total)
        )
    }

    fun routeWeatherSummary(
        routeId: String,
        score: Int,
        checkedAtEpochMillis: Long
    ): RouteWeatherSummaryUiModel {
        return RouteWeatherSummaryUiModel(
            routeId = routeId,
            score = score,
            riskLevel = WeatherRiskLevel.fromScore(score),
            checkedAt = RouteFormatters.formatClock(checkedAtEpochMillis)
        )
    }

    fun departureScenarios(result: DepartureOptimizationResult): List<DepartureScenarioUiModel> {
        return result.candidates.map { candidate ->
            val snapshot = candidate.snapshot
            val score = snapshot.rideScore.total
            val riskLevel = WeatherRiskLevel.fromScore(score)
            val segments = snapshot.segmentWeather

            DepartureScenarioUiModel(
                departureEpochMillis = candidate.departureEpochMillis,
                departureTime = RouteFormatters.formatShortDateTime(candidate.departureEpochMillis),
                score = score,
                label = riskLevel.label,
                duration = RouteFormatters.formatDuration(
                    RouteFormatters.estimatedDurationMinutes(
                        distanceMeters = segments.lastOrNull()?.sample?.distanceFromStartMeters ?: 0.0,
                        averageSpeedKmh = snapshot.averageSpeedKmh
                    )
                ),
                windSummary = windSummary(segments),
                rainSummary = rainSummary(segments),
                temperatureSummary = temperatureSummary(segments),
                isBest = candidate.departureEpochMillis == result.bestCandidate.departureEpochMillis,
                isCurrent = candidate.departureEpochMillis == result.currentCandidate.departureEpochMillis,
                riskLevel = riskLevel
            )
        }.sortedBy { it.departureEpochMillis }
    }

    private fun windSummary(segments: List<SegmentWeather>): String {
        if (segments.isEmpty()) {
            return "No wind samples"
        }

        val averageHeadwind = segments.map { it.windComponents.headwindKmh }.average().roundToInt()
        return when {
            averageHeadwind > 2 -> "Avg headwind $averageHeadwind km/h"
            averageHeadwind < -2 -> "Avg tailwind ${kotlin.math.abs(averageHeadwind)} km/h"
            else -> "Mostly crosswind"
        }
    }

    private fun rainSummary(segments: List<SegmentWeather>): String {
        val maxRain = segments.map { it.weather.precipitationProbabilityPercent ?: 0 }.maxOrNull() ?: 0
        return "$maxRain% peak rain"
    }

    private fun temperatureSummary(segments: List<SegmentWeather>): String {
        if (segments.isEmpty()) {
            return "No temp samples"
        }

        val minTemp = segments.minOf { it.weather.temperatureCelsius }.roundToInt()
        val maxTemp = segments.maxOf { it.weather.temperatureCelsius }.roundToInt()
        return if (minTemp == maxTemp) {
            "$minTemp C"
        } else {
            "$minTemp-$maxTemp C"
        }
    }
}
