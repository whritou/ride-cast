package com.example.cyclistweather.presentation.model

data class RideRouteUiModel(
    val id: String,
    val name: String,
    val distance: String,
    val elevationGain: String,
    val estimatedDuration: String
)

data class RideScoreUiModel(
    val score: Int,
    val riskLevel: WeatherRiskLevel
)

data class RouteWeatherSummaryUiModel(
    val routeId: String,
    val score: Int,
    val riskLevel: WeatherRiskLevel,
    val checkedAt: String
)

data class WeatherPointUiModel(
    val id: String,
    val distance: String,
    val arrivalTime: String,
    val temperature: String,
    val wind: String,
    /** Gust speed formatted as a unit-only value (e.g. "32 km/h"), or null when unavailable. */
    val gust: String?,
    val rain: String,
    @androidx.annotation.StringRes val relativeWindRes: Int,
    @androidx.annotation.StringRes val conditionRes: Int,
    val riskLevel: WeatherRiskLevel
)

data class DepartureScenarioUiModel(
    val departureEpochMillis: Long,
    val departureTime: String,
    val score: Int,
    val duration: String,
    val isBest: Boolean,
    val isCurrent: Boolean,
    val riskLevel: WeatherRiskLevel
)
