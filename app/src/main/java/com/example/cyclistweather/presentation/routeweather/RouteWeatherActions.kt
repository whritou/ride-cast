package com.example.cyclistweather.presentation.routeweather

import androidx.compose.runtime.Stable
import com.example.cyclistweather.domain.model.ImportedRoute
import com.example.cyclistweather.domain.model.MapWeatherFilter

/**
 * Collapses the route-weather screen's callback list into a single stable handle. Implemented once
 * (in `CyclistWeatherApp`) over the ViewModel + export launcher, then threaded through
 * [RouteWeatherScreen] and its content instead of a dozen individual lambda parameters.
 */
@Stable
interface RouteWeatherActions {
    fun selectMapFilter(filter: MapWeatherFilter)
    fun refreshWeather()
    fun optimizeDeparture(autoApply: Boolean)
    fun applyOptimizedDeparture(departureEpochMillis: Long)
    fun setDepartureDate(selectedDateUtcMillis: Long)
    fun setDepartureTime(hourOfDay: Int, minute: Int)
    fun setAverageSpeed(speedKmh: Double)
    fun backToRoutes()
    fun exportRoute(route: ImportedRoute)
}
