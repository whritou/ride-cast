package com.example.cyclistweather.presentation.routeweather

import androidx.annotation.StringRes
import com.example.cyclistweather.domain.model.RouteWeatherSnapshot

sealed interface RouteWeatherLoadState {
    object Idle : RouteWeatherLoadState
    object Loading : RouteWeatherLoadState
    data class Ready(val snapshot: RouteWeatherSnapshot) : RouteWeatherLoadState
    /** [messageRes] is a localized message resource, resolved in the UI layer. */
    data class Error(@StringRes val messageRes: Int) : RouteWeatherLoadState
}
