package com.example.cyclistweather.domain.optimizer

import com.example.cyclistweather.domain.model.RouteWeatherSnapshot

data class DepartureOptimizerConfig(
    val startHourOfDay: Int = 6,
    val endHourOfDay: Int = 18,
    val stepMinutes: Int = 30
) {
    init {
        require(startHourOfDay in 0..23)
        require(endHourOfDay in startHourOfDay..23)
        require(stepMinutes > 0)
    }
}

data class DepartureCandidate(
    val departureEpochMillis: Long,
    val snapshot: RouteWeatherSnapshot
)

data class DepartureOptimizationResult(
    val requestedDepartureEpochMillis: Long,
    val candidates: List<DepartureCandidate>,
    val bestCandidate: DepartureCandidate,
    val currentCandidate: DepartureCandidate,
    val reasons: List<String>
)
