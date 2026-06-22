package com.example.cyclistweather.presentation.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.cyclistweather.core.di.AppContainer

/** Creates ViewModels with dependencies pulled from the [AppContainer]. */
class AppViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(CyclistWeatherViewModel::class.java) -> CyclistWeatherViewModel(
                gpxRepository = container.gpxRepository,
                settingsRepository = container.settingsRepository,
                buildRouteWeatherUseCase = container.buildRouteWeatherUseCase,
                optimizeDepartureTimeUseCase = container.optimizeDepartureTimeUseCase
            ) as T

            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
