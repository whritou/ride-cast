package com.example.cyclistweather.presentation.model

data class RideRouteUiModel(
    val id: String,
    val name: String,
    val distance: String,
    val elevationGain: String,
    val estimatedDuration: String,
    val averageSpeed: String,
    val pointCount: String
)

data class RideScoreUiModel(
    val score: Int,
    val label: String,
    val riskLevel: WeatherRiskLevel,
    val bestDepartureWindow: String?,
    val risks: List<String>,
    val factors: List<RideScoreFactorUiModel>,
    val stats: List<RideScoreStatUiModel>
)

data class RideScoreFactorUiModel(
    val label: String,
    val score: Int,
    val description: String
)

data class RideScoreStatUiModel(
    val label: String,
    val value: String,
    val riskLevel: WeatherRiskLevel
)

data class WeatherFreshnessUiModel(
    val label: String,
    val description: String,
    val riskLevel: WeatherRiskLevel
)

data class RouteWeatherSummaryUiModel(
    val routeId: String,
    val score: Int,
    val label: String,
    val riskLevel: WeatherRiskLevel,
    val checkedAt: String,
    val freshness: WeatherFreshnessUiModel
)

data class WeatherPointUiModel(
    val id: String,
    val distance: String,
    val arrivalTime: String,
    val temperature: String,
    val wind: String,
    val gust: String,
    val rain: String,
    val relativeWind: String,
    @androidx.annotation.StringRes val relativeWindRes: Int,
    val condition: String,
    @androidx.annotation.StringRes val conditionRes: Int,
    val riskLabel: String,
    val riskLevel: WeatherRiskLevel,
    val reasons: List<String>
)

data class DepartureScenarioUiModel(
    val departureEpochMillis: Long,
    val departureTime: String,
    val score: Int,
    val label: String,
    val duration: String,
    val windSummary: String,
    val rainSummary: String,
    val temperatureSummary: String,
    val isBest: Boolean,
    val isCurrent: Boolean,
    val riskLevel: WeatherRiskLevel
)
