package com.example.cyclistweather.domain.usecase

import com.example.cyclistweather.data.weather.RouteWeatherRepository
import com.example.cyclistweather.data.weather.WeatherDataFreshnessProvider
import com.example.cyclistweather.domain.model.ImportedRoute
import com.example.cyclistweather.domain.model.RouteWeatherSnapshot
import com.example.cyclistweather.domain.model.SegmentWeather
import com.example.cyclistweather.domain.model.WeatherDataFreshness
import kotlin.math.abs

class BuildRouteWeatherUseCase(
    private val weatherRepository: RouteWeatherRepository,
    private val sampleRouteUseCase: SampleRouteUseCase = SampleRouteUseCase(),
    private val computeRelativeWindUseCase: ComputeRelativeWindUseCase = ComputeRelativeWindUseCase(),
    private val scoreRideUseCase: ScoreRideUseCase = ScoreRideUseCase()
) {
    suspend operator fun invoke(
        route: ImportedRoute,
        departureEpochMillis: Long,
        averageSpeedKmh: Double
    ): RouteWeatherSnapshot {
        val freshnessProvider = weatherRepository as? WeatherDataFreshnessProvider
        freshnessProvider?.beginRouteWeatherLoad()
        val samples = sampleRouteUseCase(route, departureEpochMillis, averageSpeedKmh)

        val segmentWeather = samples.map { sample ->
            val hourlyWeather = weatherRepository.getWeatherForPoint(
                latitude = sample.point.latitude,
                longitude = sample.point.longitude
            )
            val weather = hourlyWeather.minByOrNull {
                abs(it.timeEpochMillis - sample.estimatedArrivalEpochMillis)
            } ?: throw IllegalStateException("No forecast available for route sample.")

            val relativeAngle = computeRelativeWindUseCase.relativeWindAngle(
                routeBearingDegrees = sample.routeBearingDegrees,
                windDirectionDegrees = weather.windDirectionDegrees
            )
            val components = computeRelativeWindUseCase.components(
                windSpeedKmh = weather.windSpeedKmh,
                relativeAngleDegrees = relativeAngle
            )

            SegmentWeather(
                sample = sample,
                weather = weather,
                relativeWind = computeRelativeWindUseCase.classify(relativeAngle),
                windComponents = components,
                score = scoreRideUseCase.scoreSegment(weather, components)
            )
        }

        return RouteWeatherSnapshot(
            routeId = route.id,
            departureEpochMillis = departureEpochMillis,
            averageSpeedKmh = averageSpeedKmh,
            segmentWeather = segmentWeather,
            rideScore = scoreRideUseCase.scoreRide(segmentWeather),
            weatherDataFreshness = freshnessProvider?.currentWeatherDataFreshness ?: WeatherDataFreshness.LIVE
        )
    }
}
