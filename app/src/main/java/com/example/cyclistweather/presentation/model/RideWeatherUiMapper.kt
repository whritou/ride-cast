package com.example.cyclistweather.presentation.model

import com.example.cyclistweather.core.common.RouteFormatters
import com.example.cyclistweather.domain.model.ImportedRoute
import com.example.cyclistweather.domain.model.RideScore
import com.example.cyclistweather.domain.model.RouteWeatherSnapshot
import com.example.cyclistweather.domain.model.SegmentWeather
import com.example.cyclistweather.domain.model.WeatherDataFreshness
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
            estimatedDuration = RouteFormatters.formatDuration(durationMinutes),
            averageSpeed = "${averageSpeedKmh.roundToInt()} km/h",
            pointCount = "${route.points.size} ${if (route.points.size == 1) "point" else "points"}"
        )
    }

    fun rideScore(
        score: RideScore,
        bestDepartureWindow: String? = null
    ): RideScoreUiModel {
        val riskLevel = WeatherRiskLevel.fromScore(score.total)
        return RideScoreUiModel(
            score = score.total,
            label = riskLevel.label,
            riskLevel = riskLevel,
            bestDepartureWindow = bestDepartureWindow,
            risks = score.risks,
            factors = factorAverages(score.worstSegments),
            stats = scoreStats(score)
        )
    }

    fun weatherPoint(segment: SegmentWeather): WeatherPointUiModel {
        val riskLevel = WeatherRiskLevel.fromScore(segment.score.total)
        val rainProbability = segment.weather.precipitationProbabilityPercent ?: 0
        val gust = segment.weather.windGustKmh?.roundToInt()?.let { "$it km/h gust" } ?: "No gust data"

        return WeatherPointUiModel(
            id = segment.sample.id,
            distance = RouteFormatters.formatDistance(segment.sample.distanceFromStartMeters),
            arrivalTime = RouteFormatters.formatClock(segment.sample.estimatedArrivalEpochMillis),
            temperature = RouteFormatters.formatTemperature(segment.weather.temperatureCelsius),
            wind = "${segment.weather.windSpeedKmh.roundToInt()} km/h ${
                RouteFormatters.formatWindDirection(segment.weather.windDirectionDegrees)
            }",
            gust = gust,
            rain = "$rainProbability% rain",
            relativeWind = segment.relativeWind.label,
            relativeWindRes = segment.relativeWind.labelRes,
            condition = mapWeatherCode(segment.weather.weatherCode).label,
            conditionRes = mapWeatherCode(segment.weather.weatherCode).labelRes,
            riskLabel = riskLevel.label,
            riskLevel = riskLevel,
            reasons = segment.score.reasons
        )
    }

    fun weatherPoints(snapshot: RouteWeatherSnapshot): List<WeatherPointUiModel> {
        return snapshot.segmentWeather.map(::weatherPoint)
    }

    fun weatherFreshness(freshness: WeatherDataFreshness): WeatherFreshnessUiModel {
        return when (freshness) {
            WeatherDataFreshness.LIVE -> WeatherFreshnessUiModel(
                label = "Live",
                description = "Updated from the forecast service just now.",
                riskLevel = WeatherRiskLevel.EXCELLENT
            )
            WeatherDataFreshness.FRESH_CACHE -> WeatherFreshnessUiModel(
                label = "Cached",
                description = "Using a recent saved forecast while avoiding another network request.",
                riskLevel = WeatherRiskLevel.GOOD
            )
            WeatherDataFreshness.STALE_CACHE -> WeatherFreshnessUiModel(
                label = "Offline",
                description = "Showing a stale saved forecast because live weather is unavailable.",
                riskLevel = WeatherRiskLevel.CAUTION
            )
        }
    }

    fun routeWeatherSummary(
        routeId: String,
        score: Int,
        checkedAtEpochMillis: Long,
        freshness: WeatherDataFreshness
    ): RouteWeatherSummaryUiModel {
        val riskLevel = WeatherRiskLevel.fromScore(score)
        return RouteWeatherSummaryUiModel(
            routeId = routeId,
            score = score,
            label = riskLevel.label,
            riskLevel = riskLevel,
            checkedAt = RouteFormatters.formatClock(checkedAtEpochMillis),
            freshness = weatherFreshness(freshness)
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

    private fun factorAverages(segments: List<SegmentWeather>): List<RideScoreFactorUiModel> {
        if (segments.isEmpty()) {
            return emptyList()
        }

        return listOf(
            RideScoreFactorUiModel("Wind", segments.averageOf { it.score.windScore }, "Relative wind along route"),
            RideScoreFactorUiModel("Rain", segments.averageOf { it.score.rainScore }, "Chance and intensity"),
            RideScoreFactorUiModel("Temperature", segments.averageOf { it.score.temperatureScore }, "Comfort range"),
            RideScoreFactorUiModel("Gusts", segments.averageOf { it.score.gustScore }, "Wind gust risk")
        )
    }

    private fun scoreStats(score: RideScore): List<RideScoreStatUiModel> {
        val segments = (score.worstSegments + score.bestSegments).distinctBy { it.sample.id }
        if (segments.isEmpty()) {
            return emptyList()
        }

        val averageTemperature = segments.map { it.weather.temperatureCelsius }.average().roundToInt()
        val averageWind = segments.map { it.weather.windSpeedKmh }.average().roundToInt()
        val peakRain = segments.maxOf { it.weather.precipitationProbabilityPercent ?: 0 }

        return listOf(
            RideScoreStatUiModel("Avg temp", "$averageTemperature C", WeatherRiskLevel.fromScore(segments.averageOf { it.score.temperatureScore })),
            RideScoreStatUiModel("Avg wind", "$averageWind km/h", WeatherRiskLevel.fromScore(segments.averageOf { it.score.windScore })),
            RideScoreStatUiModel("Peak rain", "$peakRain%", WeatherRiskLevel.fromScore(segments.averageOf { it.score.rainScore }))
        )
    }

    private fun List<SegmentWeather>.averageOf(selector: (SegmentWeather) -> Int): Int {
        return map(selector).average().roundToInt()
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
