package com.example.cyclistweather.domain.usecase

import com.example.cyclistweather.core.common.RouteFormatters
import com.example.cyclistweather.domain.model.HazardCategory
import com.example.cyclistweather.domain.model.HazardKind
import com.example.cyclistweather.domain.model.HazardSeverity
import com.example.cyclistweather.domain.model.RideHazard
import com.example.cyclistweather.domain.model.RideScore
import com.example.cyclistweather.domain.model.SegmentScore
import com.example.cyclistweather.domain.model.SegmentWeather
import com.example.cyclistweather.domain.model.WeatherPoint
import com.example.cyclistweather.domain.model.WindComponents
import kotlin.math.roundToInt

class ScoreRideUseCase {
    fun scoreSegment(
        weather: WeatherPoint,
        windComponents: WindComponents
    ): SegmentScore {
        val apparent = weather.apparentTemperatureCelsius ?: weather.temperatureCelsius

        val temperatureScore = temperatureScore(apparent)
        val windScore = windScore(windComponents.headwindKmh)
        val rainScore = rainScore(
            probabilityPercent = weather.precipitationProbabilityPercent,
            precipitationMm = weather.precipitationMm
        )
        val gustScore = gustScore(weather.windGustKmh)
        val cloudScore = cloudScore(weather.cloudCoverPercent)
        val visibilityScore = visibilityScore(weather.visibilityMeters, weather.weatherCode)
        val airQualityScore = airQualityScore(weather.europeanAqi)
        val uvScore = uvScore(weather.uvIndex)
        val daylightScore = if (weather.isDay == false) 0 else 100

        // Weighted comfort blend (higher = better). Weights sum to 1.0.
        val comfort = (
            windScore * 0.28 +
                rainScore * 0.24 +
                temperatureScore * 0.20 +
                gustScore * 0.10 +
                visibilityScore * 0.08 +
                airQualityScore * 0.05 +
                uvScore * 0.05
            )

        // A night ride is never as good as the same ride in daylight.
        val nightFactor = if (weather.isDay == false) NIGHT_FACTOR else 1.0
        val blended = (comfort * nightFactor).roundToInt()

        // Hard safety ceilings so a genuinely dangerous segment can never read as "good".
        val total = blended.coerceIn(0, safetyCeiling(weather, apparent))

        return SegmentScore(
            total = total,
            windScore = windScore,
            temperatureScore = temperatureScore,
            rainScore = rainScore,
            gustScore = gustScore,
            cloudScore = cloudScore,
            visibilityScore = visibilityScore,
            airQualityScore = airQualityScore,
            uvScore = uvScore,
            daylightScore = daylightScore,
            reasons = reasonsFor(weather, windComponents, apparent, total)
        )
    }

    fun scoreRide(segmentWeather: List<SegmentWeather>): RideScore {
        if (segmentWeather.isEmpty()) {
            return RideScore(
                total = 0,
                risks = emptyList(),
                bestSegments = emptyList(),
                worstSegments = emptyList(),
                hazards = emptyList()
            )
        }

        // Bias the overall score toward the worst stretch — a single dangerous section
        // should drag the whole ride down, not be averaged away.
        val average = segmentWeather.map { it.score.total }.average()
        val worst = segmentWeather.minOf { it.score.total }
        val total = (average * 0.7 + worst * 0.3).roundToInt().coerceIn(0, 100)

        val hazards = hazardsFor(segmentWeather)
        val risks = buildList {
            val worstWind = segmentWeather.maxByOrNull { it.windComponents.headwindKmh }
            if (worstWind != null && worstWind.windComponents.headwindKmh >= 10.0) {
                add("Headwind near ${RouteFormatters.formatDistance(worstWind.sample.distanceFromStartMeters)}")
            }

            val wettest = segmentWeather.maxByOrNull {
                it.weather.precipitationProbabilityPercent ?: 0
            }
            if (wettest != null && (wettest.weather.precipitationProbabilityPercent ?: 0) >= 40) {
                add("Rain risk near ${RouteFormatters.formatDistance(wettest.sample.distanceFromStartMeters)}")
            }

            val hottestOrColdest = segmentWeather.minByOrNull { it.score.temperatureScore }
            if (hottestOrColdest != null && hottestOrColdest.score.temperatureScore <= 50) {
                add("Temperature stress near ${RouteFormatters.formatDistance(hottestOrColdest.sample.distanceFromStartMeters)}")
            }
        }

        return RideScore(
            total = total,
            risks = risks.ifEmpty { listOf("No major weather risk") },
            bestSegments = segmentWeather.sortedByDescending { it.score.total }.take(3),
            worstSegments = segmentWeather.sortedBy { it.score.total }.take(3),
            hazards = hazards
        )
    }

    private fun temperatureScore(apparentCelsius: Double): Int {
        return when {
            apparentCelsius in 12.0..24.0 -> 100
            apparentCelsius in 8.0..12.0 || apparentCelsius in 24.0..28.0 -> 75
            apparentCelsius in 4.0..8.0 || apparentCelsius in 28.0..32.0 -> 50
            else -> 25
        }
    }

    private fun windScore(headwindKmh: Double): Int {
        return when {
            headwindKmh <= -10.0 -> 100
            headwindKmh <= 0.0 -> 98
            headwindKmh <= 10.0 -> (98.0 - headwindKmh * 2.3).roundToInt()
            headwindKmh <= 20.0 -> (75.0 - (headwindKmh - 10.0) * 2.5).roundToInt()
            else -> 25
        }.coerceIn(0, 100)
    }

    private fun rainScore(
        probabilityPercent: Int?,
        precipitationMm: Double?
    ): Int {
        val probabilityScore = when (probabilityPercent ?: 0) {
            in 0 until 20 -> 100
            in 20..40 -> 75
            in 41..60 -> 50
            else -> 25
        }

        val precipitation = precipitationMm ?: 0.0
        return when {
            precipitation > 3.0 -> probabilityScore.coerceAtMost(20)
            precipitation > 1.0 -> probabilityScore.coerceAtMost(40)
            else -> probabilityScore
        }
    }

    private fun gustScore(windGustKmh: Double?): Int {
        val gust = windGustKmh ?: return 100
        return when {
            gust < 20.0 -> 100
            gust <= 35.0 -> 70
            gust <= 50.0 -> 40
            else -> 15
        }
    }

    private fun cloudScore(cloudCoverPercent: Int?): Int {
        val cloudCover = cloudCoverPercent ?: return 100
        return when {
            cloudCover <= 60 -> 100
            cloudCover <= 85 -> 75
            else -> 60
        }
    }

    private fun visibilityScore(visibilityMeters: Double?, weatherCode: Int): Int {
        val visibility = visibilityMeters ?: return if (weatherCode in FOG_CODES) 55 else 100
        return when {
            visibility < 200.0 -> 15
            visibility < 500.0 -> 35
            visibility < 1_000.0 -> 55
            visibility < 4_000.0 -> 80
            else -> 100
        }
    }

    private fun airQualityScore(europeanAqi: Int?): Int {
        val aqi = europeanAqi ?: return 100
        return when {
            aqi <= 20 -> 100
            aqi <= 40 -> 90
            aqi <= 60 -> 75
            aqi <= 80 -> 60
            aqi <= 100 -> 45
            else -> 25
        }
    }

    private fun uvScore(uvIndex: Double?): Int {
        val uv = uvIndex ?: return 100
        return when {
            uv < 6.0 -> 100
            uv < 8.0 -> 90
            uv < 11.0 -> 80
            else -> 65
        }
    }

    /** Maximum score a segment may reach given any safety-critical condition. */
    private fun safetyCeiling(weather: WeatherPoint, apparentCelsius: Double): Int {
        var ceiling = 100
        fun cap(value: Int) {
            if (value < ceiling) ceiling = value
        }

        when (weather.weatherCode) {
            in THUNDERSTORM_CODES -> cap(12)
            in HEAVY_SNOW_CODES -> cap(28)
            in SNOW_CODES -> cap(38)
        }

        val precipitation = weather.precipitationMm ?: 0.0
        when {
            precipitation > 7.0 -> cap(25)
            precipitation > 4.0 -> cap(35)
        }

        val visibility = weather.visibilityMeters
        when {
            visibility != null && visibility < 200.0 -> cap(22)
            visibility != null && visibility < 500.0 -> cap(38)
            visibility != null && visibility < 1_000.0 -> cap(55)
            visibility == null && weather.weatherCode in FOG_CODES -> cap(55)
        }

        weather.windGustKmh?.let { gust ->
            when {
                gust > 60.0 -> cap(20)
                gust > 45.0 -> cap(40)
            }
        }

        when {
            apparentCelsius >= 40.0 -> cap(28)
            apparentCelsius >= 35.0 -> cap(45)
            apparentCelsius <= -8.0 -> cap(30)
            apparentCelsius <= -2.0 -> cap(50)
        }

        weather.europeanAqi?.let { aqi ->
            when {
                aqi > 150 -> cap(45)
                aqi > 100 -> cap(62)
            }
        }

        return ceiling
    }

    private fun reasonsFor(
        weather: WeatherPoint,
        windComponents: WindComponents,
        apparentCelsius: Double,
        total: Int
    ): List<String> {
        return buildList {
            if (windComponents.headwindKmh > 10.0) {
                add("Headwind ${windComponents.headwindKmh.roundToInt()} km/h")
            }
            if ((weather.precipitationProbabilityPercent ?: 0) >= 40) {
                add("Rain ${weather.precipitationProbabilityPercent}%")
            }
            if (apparentCelsius < 8.0 || apparentCelsius > 28.0) {
                add("Feels ${apparentCelsius.roundToInt()} C")
            }
            weather.visibilityMeters?.let { visibility ->
                if (visibility < 1_000.0 || weather.weatherCode in FOG_CODES) {
                    add("Low visibility")
                }
            }
            weather.europeanAqi?.let { aqi ->
                if (aqi > 100) add("Poor air quality")
            }
            if (weather.isDay == false) {
                add("Night ride")
            }
            if (total >= 80 && isEmpty()) {
                add("Comfortable segment")
            }
        }
    }

    private fun hazardsFor(segments: List<SegmentWeather>): List<RideHazard> {
        val byCategory = linkedMapOf<HazardCategory, RideHazard>()

        fun record(severity: HazardSeverity, kind: HazardKind, distanceLabel: String? = null, value: Int? = null) {
            val existing = byCategory[kind.category]
            // Keep a single, most-severe hazard per category, so a warning and a danger describing
            // the same phenomenon (e.g. high heat vs extreme heat) never surface together.
            if (existing == null || (existing.severity == HazardSeverity.WARNING && severity == HazardSeverity.DANGER)) {
                byCategory[kind.category] = RideHazard(severity, kind, distanceLabel, value)
            }
        }

        segments.forEach { segment ->
            val w = segment.weather
            val near = RouteFormatters.formatDistance(segment.sample.distanceFromStartMeters)
            val apparent = w.apparentTemperatureCelsius ?: w.temperatureCelsius

            when (w.weatherCode) {
                in THUNDERSTORM_CODES -> record(HazardSeverity.DANGER, HazardKind.THUNDERSTORM, near)
                in HEAVY_SNOW_CODES -> record(HazardSeverity.DANGER, HazardKind.HEAVY_SNOW, near)
                in SNOW_CODES -> record(HazardSeverity.WARNING, HazardKind.SNOW_ICE, near)
            }

            val precipitation = w.precipitationMm ?: 0.0
            val rainProbability = w.precipitationProbabilityPercent ?: 0
            when {
                precipitation > 4.0 || (rainProbability >= 80 && w.weatherCode in RAIN_CODES) ->
                    record(HazardSeverity.DANGER, HazardKind.HEAVY_RAIN, near)
                precipitation > 1.5 || rainProbability >= 55 ->
                    record(HazardSeverity.WARNING, HazardKind.RAIN_LIKELY, near)
            }

            val visibility = w.visibilityMeters
            when {
                (visibility != null && visibility < 200.0) ||
                    (w.weatherCode in FOG_CODES && visibility != null && visibility < 500.0) ->
                    record(HazardSeverity.DANGER, HazardKind.DENSE_FOG, near)
                (visibility != null && visibility < 1_000.0) || w.weatherCode in FOG_CODES ->
                    record(HazardSeverity.WARNING, HazardKind.LOW_VISIBILITY, near)
            }

            w.windGustKmh?.let { gust ->
                when {
                    gust > 60.0 -> record(HazardSeverity.DANGER, HazardKind.VIOLENT_GUSTS, near, gust.roundToInt())
                    gust > 45.0 -> record(HazardSeverity.WARNING, HazardKind.STRONG_GUSTS, near, gust.roundToInt())
                }
            }

            when {
                apparent >= 40.0 -> record(HazardSeverity.DANGER, HazardKind.EXTREME_HEAT, near, apparent.roundToInt())
                apparent >= 33.0 -> record(HazardSeverity.WARNING, HazardKind.HIGH_HEAT, near, apparent.roundToInt())
                apparent <= -8.0 -> record(HazardSeverity.DANGER, HazardKind.SEVERE_COLD, near, apparent.roundToInt())
                apparent <= 0.0 -> record(HazardSeverity.WARNING, HazardKind.FREEZING, near, apparent.roundToInt())
            }

            w.europeanAqi?.let { aqi ->
                if (aqi > 100) record(HazardSeverity.WARNING, HazardKind.POOR_AIR, near, aqi)
            }

            w.uvIndex?.let { uv ->
                if (uv >= 8.0) record(HazardSeverity.WARNING, HazardKind.VERY_HIGH_UV, value = uv.roundToInt())
            }
        }

        // Night is a property of the ride as a whole, not a single point.
        val nightSegments = segments.count { it.weather.isDay == false }
        if (nightSegments > 0 && nightSegments >= segments.size / 2) {
            byCategory.putIfAbsent(
                HazardCategory.NIGHT,
                RideHazard(HazardSeverity.WARNING, HazardKind.NIGHT_RIDE)
            )
        }

        // Danger first, then warnings.
        return byCategory.values.sortedByDescending { it.severity == HazardSeverity.DANGER }
    }

    private companion object {
        val THUNDERSTORM_CODES = setOf(95, 96, 99)
        val RAIN_CODES = setOf(51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82)
        val SNOW_CODES = setOf(71, 73, 75, 77, 85, 86)
        val HEAVY_SNOW_CODES = setOf(75, 86)
        val FOG_CODES = setOf(45, 48)
        const val NIGHT_FACTOR = 0.82
    }
}
