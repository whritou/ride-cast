package com.example.cyclistweather.domain.usecase

import com.example.cyclistweather.domain.model.ImportedRoute
import com.example.cyclistweather.domain.model.RoutePoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BuildRouteSegmentsUseCaseTest {
    private val useCase = BuildRouteSegmentsUseCase()

    @Test
    fun `builds one segment per consecutive pair with cumulative timing`() {
        val route = route(
            RoutePoint(37.0, 127.0, null, 0.0),
            RoutePoint(37.0, 127.01, null, 1_000.0),
            RoutePoint(37.0, 127.03, null, 3_000.0)
        )

        val segments = useCase(route, departureEpochMillis = 1_000_000L, averageSpeedKmh = 20.0)

        assertEquals(2, segments.size)
        assertEquals("segment-0", segments[0].id)
        assertEquals(1_000.0, segments[0].distanceMeters, 1e-6)
        assertEquals(2_000.0, segments[1].distanceMeters, 1e-6)
        // 1 km at 20 km/h = 180_000 ms; timing accumulates from the departure instant.
        assertEquals(1_000_000L, segments[0].estimatedStartEpochMillis)
        assertEquals(1_180_000L, segments[0].estimatedEndEpochMillis)
        assertEquals(1_180_000L, segments[1].estimatedStartEpochMillis)
        assertEquals(1_540_000L, segments[1].estimatedEndEpochMillis)
    }

    @Test
    fun `skips zero-length segments while keeping stable indices`() {
        val route = route(
            RoutePoint(37.0, 127.0, null, 0.0),
            RoutePoint(37.0, 127.01, null, 1_000.0),
            RoutePoint(37.0, 127.01, null, 1_000.0),
            RoutePoint(37.0, 127.03, null, 2_000.0)
        )

        val segments = useCase(route, departureEpochMillis = 0L, averageSpeedKmh = 20.0)

        assertEquals(listOf("segment-0", "segment-2"), segments.map { it.id })
    }

    @Test
    fun `returns empty for fewer than two points or non-positive speed`() {
        val single = route(RoutePoint(37.0, 127.0, null, 0.0))
        assertTrue(useCase(single, 0L, 20.0).isEmpty())

        val twoPoints = route(
            RoutePoint(37.0, 127.0, null, 0.0),
            RoutePoint(37.0, 127.01, null, 1_000.0)
        )
        assertTrue(useCase(twoPoints, 0L, averageSpeedKmh = 0.0).isEmpty())
    }

    private fun route(vararg points: RoutePoint): ImportedRoute = ImportedRoute(
        id = "route",
        name = "Test route",
        points = points.toList(),
        totalDistanceMeters = points.lastOrNull()?.distanceFromStartMeters ?: 0.0,
        totalElevationGainMeters = null,
        createdAtEpochMillis = 0L
    )
}
