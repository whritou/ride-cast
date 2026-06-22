package com.example.cyclistweather.domain.usecase

import com.example.cyclistweather.domain.model.ImportedRoute
import com.example.cyclistweather.domain.model.RoutePoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SampleRouteUseCaseTest {
    private val useCase = SampleRouteUseCase()

    @Test
    fun `short routes sample at the start, fixed interval and finish`() {
        val route = route(0.0, 1_000.0, 2_000.0, 3_000.0, 4_000.0, 5_000.0)

        val samples = useCase(route, departureEpochMillis = 0L, averageSpeedKmh = 20.0)

        // 5 km route uses the 2 km short-route interval: 0, 2000, 4000, then the 5000 finish.
        assertEquals(listOf(0.0, 2_000.0, 4_000.0, 5_000.0), samples.map { it.distanceFromStartMeters })
        assertEquals(0.0, samples.first().distanceFromStartMeters, 1e-6)
        assertEquals(route.totalDistanceMeters, samples.last().distanceFromStartMeters, 1e-6)
    }

    @Test
    fun `arrival time scales with distance and average speed`() {
        val route = route(0.0, 2_000.0)

        val samples = useCase(route, departureEpochMillis = 1_000L, averageSpeedKmh = 20.0)

        // Start arrives at the departure instant; 2 km at 20 km/h adds 360_000 ms.
        assertEquals(1_000L, samples.first().estimatedArrivalEpochMillis)
        assertEquals(361_000L, samples.last().estimatedArrivalEpochMillis)
    }

    @Test
    fun `sample count never exceeds the cap on long routes`() {
        val route = route(*DoubleArray(101) { it * 5_000.0 }) // 500 km

        val samples = useCase(route, departureEpochMillis = 0L, averageSpeedKmh = 25.0)

        assertTrue(samples.size <= SampleRouteUseCase.MAX_WEATHER_SAMPLE_COUNT)
    }

    @Test
    fun `returns empty for fewer than two points or non-positive speed`() {
        assertTrue(useCase(route(0.0), 0L, 20.0).isEmpty())
        assertTrue(useCase(route(0.0, 1_000.0), 0L, averageSpeedKmh = 0.0).isEmpty())
    }

    @Test
    fun `interval widens with distance and then caps the sample count`() {
        assertEquals(2_000.0, SampleRouteUseCase.sampleIntervalMeters(0.0), 1e-6)
        assertEquals(2_000.0, SampleRouteUseCase.sampleIntervalMeters(10_000.0), 1e-6)
        assertEquals(5_000.0, SampleRouteUseCase.sampleIntervalMeters(100_000.0), 1e-6)
        // 200 km at 5 km would need 41 samples; collapse to total / (cap - 1).
        assertEquals(200_000.0 / 24.0, SampleRouteUseCase.sampleIntervalMeters(200_000.0), 1e-6)
    }

    private fun route(vararg distances: Double): ImportedRoute {
        val points = distances.mapIndexed { index, distance ->
            RoutePoint(37.0 + index * 0.001, 127.0 + index * 0.001, null, distance)
        }
        return ImportedRoute(
            id = "route",
            name = "Test route",
            points = points,
            totalDistanceMeters = distances.lastOrNull() ?: 0.0,
            totalElevationGainMeters = null,
            createdAtEpochMillis = 0L
        )
    }
}
