package com.example.cyclistweather.domain.usecase

import com.example.cyclistweather.domain.model.RelativeWind
import org.junit.Assert.assertEquals
import org.junit.Test

class ComputeRelativeWindUseCaseTest {
    private val useCase = ComputeRelativeWindUseCase()

    @Test
    fun `relative wind angle is the normalized difference of wind and route bearing`() {
        assertEquals(0.0, useCase.relativeWindAngle(routeBearingDegrees = 90.0, windDirectionDegrees = 90.0), 1e-6)
        assertEquals(270.0, useCase.relativeWindAngle(routeBearingDegrees = 0.0, windDirectionDegrees = 270.0), 1e-6)
        // Difference wraps across the 0/360 boundary.
        assertEquals(20.0, useCase.relativeWindAngle(routeBearingDegrees = 350.0, windDirectionDegrees = 10.0), 1e-6)
    }

    @Test
    fun `classify maps angle bands to head, tail and cross winds`() {
        assertEquals(RelativeWind.HEADWIND, useCase.classify(0.0))
        assertEquals(RelativeWind.HEADWIND, useCase.classify(45.0))
        assertEquals(RelativeWind.HEADWIND, useCase.classify(315.0))
        assertEquals(RelativeWind.HEADWIND, useCase.classify(350.0))
        assertEquals(RelativeWind.TAILWIND, useCase.classify(135.0))
        assertEquals(RelativeWind.TAILWIND, useCase.classify(180.0))
        assertEquals(RelativeWind.TAILWIND, useCase.classify(225.0))
        assertEquals(RelativeWind.CROSSWIND_RIGHT, useCase.classify(90.0))
        assertEquals(RelativeWind.CROSSWIND_LEFT, useCase.classify(270.0))
    }

    @Test
    fun `components project wind speed onto the head and cross axes`() {
        val head = useCase.components(windSpeedKmh = 20.0, relativeAngleDegrees = 0.0)
        assertEquals(20.0, head.headwindKmh, 1e-6)
        assertEquals(0.0, head.crosswindKmh, 1e-6)

        val cross = useCase.components(windSpeedKmh = 20.0, relativeAngleDegrees = 90.0)
        assertEquals(0.0, cross.headwindKmh, 1e-6)
        assertEquals(20.0, cross.crosswindKmh, 1e-6)

        // A tailwind is a negative headwind component.
        val tail = useCase.components(windSpeedKmh = 20.0, relativeAngleDegrees = 180.0)
        assertEquals(-20.0, tail.headwindKmh, 1e-6)
        assertEquals(0.0, tail.crosswindKmh, 1e-6)
    }
}
