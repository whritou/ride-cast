package com.example.cyclistweather.presentation.model

import com.example.cyclistweather.core.common.RouteFormatters
import com.example.cyclistweather.domain.model.ImportedRoute
import com.example.cyclistweather.domain.model.RelativeWind
import com.example.cyclistweather.domain.model.RideScore
import com.example.cyclistweather.domain.model.RoutePoint
import com.example.cyclistweather.domain.model.RouteWeatherSample
import com.example.cyclistweather.domain.model.RouteWeatherSnapshot
import com.example.cyclistweather.domain.model.SegmentScore
import com.example.cyclistweather.domain.model.SegmentWeather
import com.example.cyclistweather.domain.model.WeatherPoint
import com.example.cyclistweather.domain.model.WeatherDataFreshness
import com.example.cyclistweather.domain.model.WindComponents
import com.example.cyclistweather.domain.optimizer.DepartureCandidate
import com.example.cyclistweather.domain.optimizer.DepartureOptimizationResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RideWeatherUiMapperTest {
    @Test
    fun `score bands map to cycling risk labels`() {
        assertEquals(WeatherRiskLevel.EXCELLENT, WeatherRiskLevel.fromScore(92))
        assertEquals(WeatherRiskLevel.GOOD, WeatherRiskLevel.fromScore(74))
        assertEquals(WeatherRiskLevel.CAUTION, WeatherRiskLevel.fromScore(55))
        assertEquals(WeatherRiskLevel.RISK, WeatherRiskLevel.fromScore(39))
        assertEquals(WeatherRiskLevel.DANGEROUS, WeatherRiskLevel.fromScore(12))
    }

    @Test
    fun `route ui model exposes route facts formatted for scanning`() {
        val route = route(
            distanceMeters = 42_195.0,
            elevationMeters = 725.0
        )

        val model = RideWeatherUiMapper.route(route, averageSpeedKmh = 22.0)

        assertEquals("Morning Loop", model.name)
        assertEquals("42.2 km", model.distance)
        assertEquals("+725 m", model.elevationGain)
        assertEquals("1h 55m", model.estimatedDuration)
        assertEquals("22 km/h", model.averageSpeed)
        assertEquals("2 points", model.pointCount)
    }

    @Test
    fun `weather point ui model exposes distance arrival wind rain temp and risk text`() {
        val segment = segment(
            id = "ridge",
            distanceMeters = 12_300.0,
            score = 43,
            rainProbability = 48,
            temperatureCelsius = 17.6,
            windSpeedKmh = 31.2,
            windDirectionDegrees = 225.0,
            relativeWind = RelativeWind.HEADWIND
        )

        val model = RideWeatherUiMapper.weatherPoint(segment)

        assertEquals("ridge", model.id)
        assertEquals("12.3 km", model.distance)
        assertEquals(RouteFormatters.formatClock(segment.sample.estimatedArrivalEpochMillis), model.arrivalTime)
        assertEquals("18 C", model.temperature)
        assertEquals("31 km/h SW", model.wind)
        assertEquals("48% rain", model.rain)
        assertEquals("Headwind", model.relativeWind)
        assertEquals("Difficult", model.riskLabel)
        assertEquals(WeatherRiskLevel.RISK, model.riskLevel)
    }

    @Test
    fun `ride score ui model exposes ride stats instead of only factor scores`() {
        val cool = segment(
            id = "cool",
            distanceMeters = 0.0,
            score = 80,
            rainProbability = 10,
            temperatureCelsius = 16.0,
            windSpeedKmh = 8.0,
            windDirectionDegrees = 225.0,
            relativeWind = RelativeWind.HEADWIND
        )
        val warm = segment(
            id = "warm",
            distanceMeters = 2_000.0,
            score = 90,
            rainProbability = 40,
            temperatureCelsius = 20.0,
            windSpeedKmh = 12.0,
            windDirectionDegrees = 225.0,
            relativeWind = RelativeWind.HEADWIND
        )
        val score = RideScore(
            total = 85,
            risks = emptyList(),
            bestSegments = listOf(warm),
            worstSegments = listOf(cool, warm)
        )

        val model = RideWeatherUiMapper.rideScore(score)

        assertEquals(listOf("Avg temp", "Avg wind", "Peak rain"), model.stats.map { it.label })
        assertEquals(listOf("18 C", "10 km/h", "40%"), model.stats.map { it.value })
    }

    @Test
    fun `departure scenarios mark best and current candidates`() {
        val requestedDeparture = 1_800_000L
        val current = DepartureCandidate(
            departureEpochMillis = requestedDeparture,
            snapshot = snapshot(score = 61, departureEpochMillis = requestedDeparture)
        )
        val best = DepartureCandidate(
            departureEpochMillis = 3_600_000L,
            snapshot = snapshot(score = 88, departureEpochMillis = 3_600_000L)
        )
        val result = DepartureOptimizationResult(
            requestedDepartureEpochMillis = requestedDeparture,
            candidates = listOf(current, best),
            bestCandidate = best,
            currentCandidate = current,
            reasons = listOf("Lower rain risk across the ride.")
        )

        val scenarios = RideWeatherUiMapper.departureScenarios(result)

        assertEquals(2, scenarios.size)
        assertTrue(scenarios.first { it.score == 88 }.isBest)
        assertFalse(scenarios.first { it.score == 88 }.isCurrent)
        assertFalse(scenarios.first { it.score == 61 }.isBest)
        assertTrue(scenarios.first { it.score == 61 }.isCurrent)
        assertEquals("Excellent", scenarios.first { it.score == 88 }.label)
        assertEquals("Fair", scenarios.first { it.score == 61 }.label)
    }

    @Test
    fun `weather freshness labels explain offline cache state`() {
        assertEquals("Live", RideWeatherUiMapper.weatherFreshness(WeatherDataFreshness.LIVE).label)
        assertEquals("Cached", RideWeatherUiMapper.weatherFreshness(WeatherDataFreshness.FRESH_CACHE).label)
        assertEquals("Offline", RideWeatherUiMapper.weatherFreshness(WeatherDataFreshness.STALE_CACHE).label)
        assertTrue(
            RideWeatherUiMapper.weatherFreshness(WeatherDataFreshness.STALE_CACHE)
                .description
                .contains("stale", ignoreCase = true)
        )
    }

    @Test
    fun `route weather summary exposes score checked time and freshness`() {
        val model = RideWeatherUiMapper.routeWeatherSummary(
            routeId = "route-1",
            score = 88,
            checkedAtEpochMillis = 3_600_000L,
            freshness = WeatherDataFreshness.FRESH_CACHE
        )

        assertEquals("route-1", model.routeId)
        assertEquals(88, model.score)
        assertEquals("Excellent", model.label)
        assertEquals(RouteFormatters.formatClock(3_600_000L), model.checkedAt)
        assertEquals("Cached", model.freshness.label)
    }

    private fun route(
        distanceMeters: Double = 10_000.0,
        elevationMeters: Double? = null
    ): ImportedRoute {
        return ImportedRoute(
            id = "route",
            name = "Morning Loop",
            points = listOf(
                RoutePoint(37.0, 127.0, null, 0.0),
                RoutePoint(37.1, 127.1, null, distanceMeters)
            ),
            totalDistanceMeters = distanceMeters,
            totalElevationGainMeters = elevationMeters,
            createdAtEpochMillis = 0L
        )
    }

    private fun snapshot(
        score: Int,
        departureEpochMillis: Long
    ): RouteWeatherSnapshot {
        val segments = listOf(
            segment(
                id = "sample-$score",
                distanceMeters = 0.0,
                score = score,
                rainProbability = if (score >= 80) 5 else 45,
                temperatureCelsius = if (score >= 80) 19.0 else 31.0,
                windSpeedKmh = if (score >= 80) 8.0 else 24.0,
                windDirectionDegrees = 270.0,
                relativeWind = RelativeWind.HEADWIND,
                departureEpochMillis = departureEpochMillis
            )
        )

        return RouteWeatherSnapshot(
            routeId = "route",
            departureEpochMillis = departureEpochMillis,
            averageSpeedKmh = 22.0,
            segmentWeather = segments,
            rideScore = RideScore(
                total = score,
                risks = listOf("Risk $score"),
                bestSegments = segments,
                worstSegments = segments
            )
        )
    }

    private fun segment(
        id: String,
        distanceMeters: Double,
        score: Int,
        rainProbability: Int,
        temperatureCelsius: Double,
        windSpeedKmh: Double,
        windDirectionDegrees: Double,
        relativeWind: RelativeWind,
        departureEpochMillis: Long = 0L
    ): SegmentWeather {
        val sample = RouteWeatherSample(
            id = id,
            point = RoutePoint(37.0, 127.0, null, distanceMeters),
            distanceFromStartMeters = distanceMeters,
            estimatedArrivalEpochMillis = departureEpochMillis + 30 * 60 * 1_000L,
            routeBearingDegrees = 90.0
        )

        return SegmentWeather(
            sample = sample,
            weather = WeatherPoint(
                latitude = sample.point.latitude,
                longitude = sample.point.longitude,
                timeEpochMillis = sample.estimatedArrivalEpochMillis,
                temperatureCelsius = temperatureCelsius,
                apparentTemperatureCelsius = temperatureCelsius,
                windSpeedKmh = windSpeedKmh,
                windDirectionDegrees = windDirectionDegrees,
                windGustKmh = windSpeedKmh + 6.0,
                precipitationProbabilityPercent = rainProbability,
                precipitationMm = if (rainProbability >= 30) 1.4 else 0.0,
                cloudCoverPercent = 55,
                weatherCode = 1
            ),
            relativeWind = relativeWind,
            windComponents = WindComponents(
                headwindKmh = if (relativeWind == RelativeWind.HEADWIND) windSpeedKmh else -windSpeedKmh,
                crosswindKmh = 0.0
            ),
            score = SegmentScore(
                total = score,
                windScore = score,
                temperatureScore = score,
                rainScore = score,
                gustScore = score,
                cloudScore = score,
                reasons = listOf("Reason $score")
            )
        )
    }
}
