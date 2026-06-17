package com.example.cyclistweather.presentation.app

import com.example.cyclistweather.domain.model.AppLanguage
import com.example.cyclistweather.domain.model.ImportedRoute
import com.example.cyclistweather.domain.model.MapWeatherFilter
import com.example.cyclistweather.domain.model.ThemeMode
import com.example.cyclistweather.data.settings.RouteWeatherSummarySnapshot
import com.example.cyclistweather.presentation.routeweather.DepartureOptimizationLoadState
import com.example.cyclistweather.presentation.routeweather.RouteWeatherLoadState

data class CyclistWeatherUiState(
    val routes: List<ImportedRoute> = emptyList(),
    val routeWeatherSummaries: Map<String, RouteWeatherSummarySnapshot> = emptyMap(),
    val selectedRoute: ImportedRoute? = null,
    val averageSpeedKmh: Double = 22.0,
    val selectedDepartureEpochMillis: Long = 0L,
    val routeWeatherState: RouteWeatherLoadState = RouteWeatherLoadState.Idle,
    val departureOptimizationState: DepartureOptimizationLoadState = DepartureOptimizationLoadState.Idle,
    val selectedMapFilter: MapWeatherFilter = MapWeatherFilter.WIND,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val departureNotificationsEnabled: Boolean = false,
    val isImporting: Boolean = false,
    @androidx.annotation.StringRes val errorMessageRes: Int? = null
)
