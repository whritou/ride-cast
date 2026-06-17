package com.example.cyclistweather.presentation.routeweather

import com.example.cyclistweather.domain.model.RoutePoint
import org.junit.Assert.assertEquals
import org.junit.Test

class RouteElevationProfilePlannerTest {
    @Test
    fun `profile normalizes route elevation samples`() {
        val points = listOf(
            RoutePoint(0.0, 0.0, 100.0, 0.0),
            RoutePoint(0.0, 0.1, 150.0, 1_000.0),
            RoutePoint(0.0, 0.2, 120.0, 2_000.0)
        )

        val profile = RouteElevationProfilePlanner.profile(points)

        assertEquals(100, profile.minElevationMeters)
        assertEquals(150, profile.maxElevationMeters)
        assertEquals(2_000.0, profile.totalDistanceMeters, 0.0)
        assertEquals(listOf(0f, 0.5f, 1f), profile.samples.map { it.distanceFraction })
        assertEquals(listOf(0f, 1f, 0.4f), profile.samples.map { it.elevationFraction })
    }

    @Test
    fun `profile falls back to zero range when elevations are missing`() {
        val points = listOf(
            RoutePoint(0.0, 0.0, null, 0.0),
            RoutePoint(0.0, 0.1, null, 1_000.0)
        )

        val profile = RouteElevationProfilePlanner.profile(points)

        assertEquals(0, profile.minElevationMeters)
        assertEquals(0, profile.maxElevationMeters)
        assertEquals(1_000.0, profile.totalDistanceMeters, 0.0)
        assertEquals(listOf(0f, 1f), profile.samples.map { it.distanceFraction })
        assertEquals(listOf(0f, 0f), profile.samples.map { it.elevationFraction })
    }
}
