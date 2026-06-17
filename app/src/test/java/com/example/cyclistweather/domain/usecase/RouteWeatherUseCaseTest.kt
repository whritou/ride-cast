package com.example.cyclistweather.domain.usecase

import com.example.cyclistweather.domain.model.ImportedRoute
import com.example.cyclistweather.domain.model.RelativeWind
import com.example.cyclistweather.domain.model.RoutePoint
import com.example.cyclistweather.domain.model.WeatherPoint
import com.example.cyclistweather.domain.model.WindComponents
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteWeatherUseCaseTest {
    @Test
    fun `relative wind treats meteorological same-direction wind as headwind`() {
        val useCase = ComputeRelativeWindUseCase()

        val angle = useCase.relativeWindAngle(
            routeBearingDegrees = 90.0,
            windDirectionDegrees = 90.0
        )

        assertEquals(RelativeWind.HEADWIND, useCase.classify(angle))
    }

    @Test
    fun `relative wind treats opposite source direction as tailwind`() {
        val useCase = ComputeRelativeWindUseCase()

        val angle = useCase.relativeWindAngle(
            routeBearingDegrees = 90.0,
            windDirectionDegrees = 270.0
        )

        assertEquals(RelativeWind.TAILWIND, useCase.classify(angle))
    }

    @Test
    fun `route sampling includes start interval and finish points`() {
        val route = ImportedRoute(
            id = "test",
            name = "Test route",
            points = listOf(
                RoutePoint(37.0, 127.0, null, 0.0),
                RoutePoint(37.0, 127.01, null, 1_000.0),
                RoutePoint(37.0, 127.02, null, 2_000.0),
                RoutePoint(37.0, 127.03, null, 3_000.0)
            ),
            totalDistanceMeters = 3_000.0,
            totalElevationGainMeters = null,
            createdAtEpochMillis = 0L
        )

        val samples = SampleRouteUseCase()(
            route = route,
            departureEpochMillis = 0L,
            averageSpeedKmh = 20.0
        )

        assertEquals(listOf(0.0, 2_000.0, 3_000.0), samples.map { it.distanceFromStartMeters })
    }

    @Test
    fun `route sampling uses five kilometer spacing for medium routes`() {
        val route = routeWithDistance(50_000.0)

        val samples = SampleRouteUseCase()(
            route = route,
            departureEpochMillis = 0L,
            averageSpeedKmh = 20.0
        )

        assertEquals(
            listOf(0.0, 5_000.0, 10_000.0, 15_000.0, 20_000.0, 25_000.0, 30_000.0, 35_000.0, 40_000.0, 45_000.0, 50_000.0),
            samples.map { it.distanceFromStartMeters }
        )
    }

    @Test
    fun `route sampling caps long routes at twenty five weather points`() {
        val route = routeWithDistance(200_000.0)

        val samples = SampleRouteUseCase()(
            route = route,
            departureEpochMillis = 0L,
            averageSpeedKmh = 20.0
        )

        assertEquals(25, samples.size)
        assertEquals(0.0, samples.first().distanceFromStartMeters, 0.001)
        assertEquals(200_000.0, samples.last().distanceFromStartMeters, 0.001)
        assertEquals(8_333.333, samples[1].distanceFromStartMeters, 0.001)
    }

    @Test
    fun `headwind scores lower than tailwind in same weather`() {
        val weather = WeatherPoint(
            latitude = 37.0,
            longitude = 127.0,
            timeEpochMillis = 0L,
            temperatureCelsius = 18.0,
            apparentTemperatureCelsius = 18.0,
            windSpeedKmh = 20.0,
            windDirectionDegrees = 90.0,
            windGustKmh = 25.0,
            precipitationProbabilityPercent = 10,
            precipitationMm = 0.0,
            cloudCoverPercent = 20,
            weatherCode = 1
        )
        val useCase = ScoreRideUseCase()

        val headwindScore = useCase.scoreSegment(
            weather = weather,
            windComponents = WindComponents(headwindKmh = 18.0, crosswindKmh = 0.0)
        )
        val tailwindScore = useCase.scoreSegment(
            weather = weather,
            windComponents = WindComponents(headwindKmh = -18.0, crosswindKmh = 0.0)
        )

        assertTrue(headwindScore.total < tailwindScore.total)
    }

    @Test
    fun `very light headwind keeps wind score near ideal`() {
        val weather = WeatherPoint(
            latitude = 37.0,
            longitude = 127.0,
            timeEpochMillis = 0L,
            temperatureCelsius = 18.0,
            apparentTemperatureCelsius = 18.0,
            windSpeedKmh = 1.0,
            windDirectionDegrees = 90.0,
            windGustKmh = 2.0,
            precipitationProbabilityPercent = 0,
            precipitationMm = 0.0,
            cloudCoverPercent = 20,
            weatherCode = 1
        )
        val useCase = ScoreRideUseCase()

        val score = useCase.scoreSegment(
            weather = weather,
            windComponents = WindComponents(headwindKmh = 1.0, crosswindKmh = 0.0)
        )

        assertTrue("1 km/h headwind should be almost ideal", score.windScore >= 95)
        assertTrue("Ideal weather with 1 km/h wind should stay excellent", score.total >= 95)
    }

    private fun routeWithDistance(totalDistanceMeters: Double): ImportedRoute {
        return ImportedRoute(
            id = "route-$totalDistanceMeters",
            name = "Route",
            points = listOf(
                RoutePoint(37.0, 127.0, null, 0.0),
                RoutePoint(37.0, 128.0, null, totalDistanceMeters)
            ),
            totalDistanceMeters = totalDistanceMeters,
            totalElevationGainMeters = null,
            createdAtEpochMillis = 0L
        )
    }
}
