package com.example.cyclistweather.presentation.routeweather

import androidx.annotation.StringRes
import com.example.cyclistweather.domain.optimizer.DepartureOptimizationResult

sealed interface DepartureOptimizationLoadState {
    object Idle : DepartureOptimizationLoadState
    object Loading : DepartureOptimizationLoadState
    data class Ready(val result: DepartureOptimizationResult) : DepartureOptimizationLoadState
    /** [messageRes] is a localized message resource, resolved in the UI layer. */
    data class Error(@StringRes val messageRes: Int) : DepartureOptimizationLoadState
}
