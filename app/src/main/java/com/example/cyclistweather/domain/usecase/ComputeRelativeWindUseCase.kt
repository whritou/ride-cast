package com.example.cyclistweather.domain.usecase

import com.example.cyclistweather.core.common.RouteMath
import com.example.cyclistweather.domain.model.RelativeWind
import com.example.cyclistweather.domain.model.WindComponents
import kotlin.math.cos
import kotlin.math.sin

class ComputeRelativeWindUseCase {
    fun relativeWindAngle(
        routeBearingDegrees: Double,
        windDirectionDegrees: Double
    ): Double {
        return RouteMath.normalizeDegrees(windDirectionDegrees - routeBearingDegrees)
    }

    fun classify(relativeAngleDegrees: Double): RelativeWind {
        return when {
            relativeAngleDegrees <= 45.0 || relativeAngleDegrees >= 315.0 -> RelativeWind.HEADWIND
            relativeAngleDegrees in 135.0..225.0 -> RelativeWind.TAILWIND
            relativeAngleDegrees > 45.0 && relativeAngleDegrees < 135.0 -> RelativeWind.CROSSWIND_RIGHT
            else -> RelativeWind.CROSSWIND_LEFT
        }
    }

    fun components(
        windSpeedKmh: Double,
        relativeAngleDegrees: Double
    ): WindComponents {
        val radians = Math.toRadians(relativeAngleDegrees)
        return WindComponents(
            headwindKmh = windSpeedKmh * cos(radians),
            crosswindKmh = windSpeedKmh * sin(radians)
        )
    }
}
