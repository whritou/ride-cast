package com.example.cyclistweather.core.common

import org.junit.Assert.assertEquals
import org.junit.Test

class RouteMathTest {
    @Test
    fun `haversine distance is close for one kilometer east`() {
        val distance = RouteMath.haversineDistanceMeters(
            lat1 = 48.8566,
            lon1 = 2.3522,
            lat2 = 48.8566,
            lon2 = 2.3659
        )

        assertEquals(1_000.0, distance, 35.0)
    }

    @Test
    fun `bearing points east when longitude increases at same latitude`() {
        val bearing = RouteMath.bearingDegrees(
            lat1 = 48.8566,
            lon1 = 2.3522,
            lat2 = 48.8566,
            lon2 = 2.3659
        )

        assertEquals(90.0, bearing, 0.5)
    }

    @Test
    fun `duration uses average cycling speed`() {
        val minutes = RouteFormatters.estimatedDurationMinutes(
            distanceMeters = 44_000.0,
            averageSpeedKmh = 22.0
        )

        assertEquals(120, minutes)
    }
}
