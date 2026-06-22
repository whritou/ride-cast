package com.example.cyclistweather.domain.usecase

import com.example.cyclistweather.domain.model.HazardCategory
import com.example.cyclistweather.domain.model.HazardKind
import com.example.cyclistweather.domain.model.HazardSeverity
import com.example.cyclistweather.domain.model.RelativeWind
import com.example.cyclistweather.domain.model.RoutePoint
import com.example.cyclistweather.domain.model.RouteWeatherSample
import com.example.cyclistweather.domain.model.SegmentWeather
import com.example.cyclistweather.domain.model.WeatherPoint
import com.example.cyclistweather.domain.model.WindComponents
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScoreRideHazardsTest {
    private val useCase = ScoreRideUseCase()

    @Test
    fun `thunderstorm caps the segment score into the danger band`() {
        val score = useCase.scoreSegment(
            weather = baseWeather(weatherCode = 95),
            windComponents = calm()
        )

        assertTrue("Thunderstorm must force a near-zero score", score.total <= 15)
    }

    @Test
    fun `night ride scores lower than the same ride in daylight`() {
        val day = useCase.scoreSegment(baseWeather(isDay = true), calm())
        val night = useCase.scoreSegment(baseWeather(isDay = false), calm())

        assertTrue("Night should score lower than day", night.total < day.total)
    }

    @Test
    fun `dense fog produces a danger hazard and low visibility a warning`() {
        val denseFog = useCase.scoreRide(
            listOf(segment(baseWeather(weatherCode = 45, visibilityMeters = 120.0)))
        )
        val haze = useCase.scoreRide(
            listOf(segment(baseWeather(visibilityMeters = 800.0)))
        )

        val fogHazard = denseFog.hazards.firstOrNull { it.kind == HazardKind.DENSE_FOG }
        assertNotNull("Dense fog should be flagged", fogHazard)
        assertEquals(HazardSeverity.DANGER, fogHazard!!.severity)

        val hazeHazard = haze.hazards.firstOrNull { it.kind == HazardKind.LOW_VISIBILITY }
        assertNotNull("Low visibility should warn", hazeHazard)
        assertEquals(HazardSeverity.WARNING, hazeHazard!!.severity)
    }

    @Test
    fun `strong gusts raise a warning and violent gusts a danger`() {
        val violent = useCase.scoreRide(listOf(segment(baseWeather(windGustKmh = 70.0))))
        val strong = useCase.scoreRide(listOf(segment(baseWeather(windGustKmh = 50.0))))

        assertEquals(HazardSeverity.DANGER, violent.hazards.first { it.kind == HazardKind.VIOLENT_GUSTS }.severity)
        assertEquals(HazardSeverity.WARNING, strong.hazards.first { it.kind == HazardKind.STRONG_GUSTS }.severity)
    }

    @Test
    fun `same-category warning and danger collapse to the single most severe hazard`() {
        // One stretch hits the high-heat warning band, another the extreme-heat danger band.
        val ride = useCase.scoreRide(
            listOf(
                segment(baseWeather(apparentTemperatureCelsius = 34.0)),
                segment(baseWeather(apparentTemperatureCelsius = 41.0))
            )
        )

        val heatHazards = ride.hazards.filter { it.kind.category == HazardCategory.HEAT }
        assertEquals("Only the most severe heat hazard should remain", 1, heatHazards.size)
        assertEquals(HazardKind.EXTREME_HEAT, heatHazards.single().kind)
        assertEquals(HazardSeverity.DANGER, heatHazards.single().severity)
        assertTrue(
            "The high-heat warning must not co-exist with the extreme-heat danger",
            ride.hazards.none { it.kind == HazardKind.HIGH_HEAT }
        )
    }

    @Test
    fun `strong and violent gusts collapse to the single danger hazard`() {
        val ride = useCase.scoreRide(
            listOf(
                segment(baseWeather(windGustKmh = 50.0)),
                segment(baseWeather(windGustKmh = 70.0))
            )
        )

        val gustHazards = ride.hazards.filter { it.kind.category == HazardCategory.GUSTS }
        assertEquals(1, gustHazards.size)
        assertEquals(HazardKind.VIOLENT_GUSTS, gustHazards.single().kind)
        assertEquals(HazardSeverity.DANGER, gustHazards.single().severity)
    }

    @Test
    fun `night majority ride is flagged as a warning hazard`() {
        val ride = useCase.scoreRide(
            listOf(
                segment(baseWeather(isDay = false)),
                segment(baseWeather(isDay = false)),
                segment(baseWeather(isDay = true))
            )
        )

        assertEquals(HazardSeverity.WARNING, ride.hazards.first { it.kind == HazardKind.NIGHT_RIDE }.severity)
    }

    @Test
    fun `calm clear daytime ride has no hazards`() {
        val ride = useCase.scoreRide(listOf(segment(baseWeather(isDay = true))))
        assertTrue("Ideal conditions should not raise hazards", ride.hazards.isEmpty())
    }

    @Test
    fun `ride score is dragged down by the worst segment`() {
        val good = segment(baseWeather(isDay = true))
        val storm = segment(baseWeather(weatherCode = 95, isDay = true))

        val ride = useCase.scoreRide(listOf(good, good, good, storm))

        // A plain average would stay high; the worst-segment bias must pull it down.
        val average = listOf(good, good, good, storm).map { it.score.total }.average()
        assertTrue("Worst segment must lower the ride score below the mean", ride.total < average)
    }

    private fun calm() = WindComponents(headwindKmh = 0.0, crosswindKmh = 0.0)

    private fun baseWeather(
        weatherCode: Int = 1,
        visibilityMeters: Double? = 20_000.0,
        windGustKmh: Double? = 8.0,
        isDay: Boolean? = null,
        europeanAqi: Int? = 15,
        uvIndex: Double? = 2.0,
        apparentTemperatureCelsius: Double = 18.0
    ): WeatherPoint {
        return WeatherPoint(
            latitude = 37.0,
            longitude = 127.0,
            timeEpochMillis = 0L,
            temperatureCelsius = 18.0,
            apparentTemperatureCelsius = apparentTemperatureCelsius,
            windSpeedKmh = 6.0,
            windDirectionDegrees = 90.0,
            windGustKmh = windGustKmh,
            precipitationProbabilityPercent = 0,
            precipitationMm = 0.0,
            cloudCoverPercent = 20,
            weatherCode = weatherCode,
            uvIndex = uvIndex,
            visibilityMeters = visibilityMeters,
            europeanAqi = europeanAqi,
            isDay = isDay
        )
    }

    private fun segment(weather: WeatherPoint): SegmentWeather {
        val components = calm()
        val sample = RouteWeatherSample(
            id = "s",
            point = RoutePoint(weather.latitude, weather.longitude, null, 0.0),
            distanceFromStartMeters = 0.0,
            estimatedArrivalEpochMillis = weather.timeEpochMillis,
            routeBearingDegrees = 90.0
        )
        return SegmentWeather(
            sample = sample,
            weather = weather,
            relativeWind = RelativeWind.CROSSWIND_LEFT,
            windComponents = components,
            score = useCase.scoreSegment(weather, components)
        )
    }
}
