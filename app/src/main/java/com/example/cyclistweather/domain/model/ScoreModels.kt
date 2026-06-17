package com.example.cyclistweather.domain.model

data class SegmentScore(
    val total: Int,
    val windScore: Int,
    val temperatureScore: Int,
    val rainScore: Int,
    val gustScore: Int,
    val cloudScore: Int,
    val visibilityScore: Int = 100,
    val airQualityScore: Int = 100,
    val uvScore: Int = 100,
    val daylightScore: Int = 100,
    val reasons: List<String>
)

data class SegmentWeather(
    val sample: RouteWeatherSample,
    val weather: WeatherPoint,
    val relativeWind: RelativeWind,
    val windComponents: WindComponents,
    val score: SegmentScore
)

data class RideScore(
    val total: Int,
    val risks: List<String>,
    val bestSegments: List<SegmentWeather>,
    val worstSegments: List<SegmentWeather>,
    val hazards: List<RideHazard> = emptyList()
)

data class RouteWeatherSnapshot(
    val routeId: String,
    val departureEpochMillis: Long,
    val averageSpeedKmh: Double,
    val segmentWeather: List<SegmentWeather>,
    val rideScore: RideScore,
    val weatherDataFreshness: WeatherDataFreshness = WeatherDataFreshness.LIVE
)
