package com.example.cyclistweather.domain.optimizer

import com.example.cyclistweather.domain.model.ImportedRoute
import com.example.cyclistweather.domain.model.RelativeWind
import com.example.cyclistweather.domain.model.RideScore
import com.example.cyclistweather.domain.model.RoutePoint
import com.example.cyclistweather.domain.model.RouteWeatherSample
import com.example.cyclistweather.domain.model.RouteWeatherSnapshot
import com.example.cyclistweather.domain.model.SegmentScore
import com.example.cyclistweather.domain.model.SegmentWeather
import com.example.cyclistweather.domain.model.WeatherPoint
import com.example.cyclistweather.domain.model.WindComponents
import java.util.Calendar
import java.util.TimeZone
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OptimizeDepartureTimeUseCaseTest {
    private val utc = TimeZone.getTimeZone("UTC")

    @Test
    fun `candidate departures cover same day from 0600 to 1800 every 30 minutes`() = runBlocking {
        val requested = utcMillis(2026, Calendar.JUNE, 7, 9, 15)
        val useCase = OptimizeDepartureTimeUseCase(
            buildRouteWeather = { _, departure, speed ->
                snapshot(departureEpochMillis = departure, averageSpeedKmh = speed, score = 70)
            },
            timeZone = utc
        )

        val result = useCase(
            route = route(),
            requestedDepartureEpochMillis = requested,
            averageSpeedKmh = 22.0
        )

        assertEquals(25, result.candidates.size)
        assertEquals(utcMillis(2026, Calendar.JUNE, 7, 6, 0), result.candidates.first().departureEpochMillis)
        assertEquals(utcMillis(2026, Calendar.JUNE, 7, 18, 0), result.candidates.last().departureEpochMillis)
        assertEquals(
            List(25) { index -> utcMillis(2026, Calendar.JUNE, 7, 6 + ((index * 30) / 60), (index * 30) % 60) },
            result.candidates.map { it.departureEpochMillis }
        )
    }

    @Test
    fun `best candidate is highest score with nearest requested departure tie break`() = runBlocking {
        val requested = utcMillis(2026, Calendar.JUNE, 7, 9, 0)
        val bestDeparture = utcMillis(2026, Calendar.JUNE, 7, 9, 30)
        val fartherTiedDeparture = utcMillis(2026, Calendar.JUNE, 7, 12, 0)
        val useCase = OptimizeDepartureTimeUseCase(
            buildRouteWeather = { _, departure, speed ->
                val score = when (departure) {
                    bestDeparture -> 84
                    fartherTiedDeparture -> 84
                    else -> 68
                }
                snapshot(departureEpochMillis = departure, averageSpeedKmh = speed, score = score)
            },
            timeZone = utc
        )

        val result = useCase(
            route = route(),
            requestedDepartureEpochMillis = requested,
            averageSpeedKmh = 22.0
        )

        assertEquals(bestDeparture, result.bestCandidate.departureEpochMillis)
        assertEquals(84, result.bestCandidate.snapshot.rideScore.total)
    }

    @Test
    fun `recommendation reasons compare best candidate with requested departure`() = runBlocking {
        val requested = utcMillis(2026, Calendar.JUNE, 7, 8, 0)
        val bestDeparture = utcMillis(2026, Calendar.JUNE, 7, 10, 0)
        val useCase = OptimizeDepartureTimeUseCase(
            buildRouteWeather = { _, departure, speed ->
                if (departure == bestDeparture) {
                    snapshot(
                        departureEpochMillis = departure,
                        averageSpeedKmh = speed,
                        score = 88,
                        rainProbability = 5,
                        headwindKmh = 3.0,
                        temperatureCelsius = 19.0
                    )
                } else {
                    snapshot(
                        departureEpochMillis = departure,
                        averageSpeedKmh = speed,
                        score = 61,
                        rainProbability = 45,
                        headwindKmh = 16.0,
                        temperatureCelsius = 30.0
                    )
                }
            },
            timeZone = utc
        )

        val result = useCase(
            route = route(),
            requestedDepartureEpochMillis = requested,
            averageSpeedKmh = 22.0
        )

        assertEquals(bestDeparture, result.bestCandidate.departureEpochMillis)
        assertTrue(result.reasons.contains("Improves ride score by 27 points."))
        assertTrue(result.reasons.contains("Lower rain risk across the ride."))
        assertTrue(result.reasons.contains("Less headwind on sampled segments."))
        assertTrue(result.reasons.contains("More comfortable temperature window."))
    }

    private fun utcMillis(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int
    ): Long {
        return Calendar.getInstance(utc).apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun route(): ImportedRoute {
        return ImportedRoute(
            id = "route",
            name = "Test route",
            points = listOf(
                RoutePoint(37.0, 127.0, null, 0.0),
                RoutePoint(37.01, 127.01, null, 2_000.0)
            ),
            totalDistanceMeters = 2_000.0,
            totalElevationGainMeters = null,
            createdAtEpochMillis = 0L
        )
    }

    private fun snapshot(
        departureEpochMillis: Long,
        averageSpeedKmh: Double,
        score: Int,
        rainProbability: Int = 10,
        headwindKmh: Double = 4.0,
        temperatureCelsius: Double = 20.0
    ): RouteWeatherSnapshot {
        val segments = List(2) { index ->
            val sample = RouteWeatherSample(
                id = "sample-$index",
                point = RoutePoint(37.0 + index * 0.01, 127.0 + index * 0.01, null, index * 1_000.0),
                distanceFromStartMeters = index * 1_000.0,
                estimatedArrivalEpochMillis = departureEpochMillis + index * 15 * 60 * 1_000L,
                routeBearingDegrees = 90.0
            )
            val weather = WeatherPoint(
                latitude = sample.point.latitude,
                longitude = sample.point.longitude,
                timeEpochMillis = sample.estimatedArrivalEpochMillis,
                temperatureCelsius = temperatureCelsius,
                apparentTemperatureCelsius = temperatureCelsius,
                windSpeedKmh = headwindKmh,
                windDirectionDegrees = 90.0,
                windGustKmh = headwindKmh + 2.0,
                precipitationProbabilityPercent = rainProbability,
                precipitationMm = if (rainProbability > 20) 1.0 else 0.0,
                cloudCoverPercent = 40,
                weatherCode = 1
            )
            SegmentWeather(
                sample = sample,
                weather = weather,
                relativeWind = RelativeWind.HEADWIND,
                windComponents = WindComponents(headwindKmh = headwindKmh, crosswindKmh = 0.0),
                score = SegmentScore(
                    total = score,
                    windScore = score,
                    temperatureScore = score,
                    rainScore = score,
                    gustScore = score,
                    cloudScore = score
                )
            )
        }
        return RouteWeatherSnapshot(
            routeId = "route",
            departureEpochMillis = departureEpochMillis,
            averageSpeedKmh = averageSpeedKmh,
            segmentWeather = segments,
            rideScore = RideScore(
                total = score,
                bestSegments = segments,
                worstSegments = segments
            )
        )
    }
}
