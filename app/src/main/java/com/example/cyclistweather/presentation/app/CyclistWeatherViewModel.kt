package com.example.cyclistweather.presentation.app

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cyclistweather.R
import com.example.cyclistweather.core.common.LatestJob
import com.example.cyclistweather.data.gpx.GpxRepository
import com.example.cyclistweather.data.settings.AppSettingsRepository
import com.example.cyclistweather.data.settings.RouteWeatherSummarySnapshot
import com.example.cyclistweather.domain.model.ImportedRoute
import com.example.cyclistweather.domain.optimizer.OptimizeDepartureTimeUseCase
import com.example.cyclistweather.domain.usecase.BuildRouteWeatherUseCase
import com.example.cyclistweather.domain.model.AppLanguage
import com.example.cyclistweather.domain.model.MapWeatherFilter
import com.example.cyclistweather.domain.model.ThemeMode
import com.example.cyclistweather.presentation.routeweather.DepartureOptimizationLoadState
import com.example.cyclistweather.presentation.routeweather.DepartureTimeEditor
import com.example.cyclistweather.presentation.routeweather.RouteWeatherErrorFormatter
import com.example.cyclistweather.presentation.routeweather.RouteWeatherLoadState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CyclistWeatherViewModel(
    private val gpxRepository: GpxRepository,
    private val settingsRepository: AppSettingsRepository,
    private val buildRouteWeatherUseCase: BuildRouteWeatherUseCase,
    private val optimizeDepartureTimeUseCase: OptimizeDepartureTimeUseCase,
    private val clock: () -> Long = { System.currentTimeMillis() }
) : ViewModel() {
    private val routeWeatherJob = LatestJob()
    private val departureOptimizationJob = LatestJob()

    private val _uiState = MutableStateFlow(
        CyclistWeatherUiState(selectedDepartureEpochMillis = currentDepartureEpochMillis())
    )
    val uiState: StateFlow<CyclistWeatherUiState> = _uiState

    init {
        observeSettings()
        refreshRoutes()
    }

    fun importGpx(uri: Uri) {
        importRoute { gpxRepository.importGpx(uri) }
    }

    fun loadDemoRoute() {
        importRoute { gpxRepository.importBundledSampleRoute() }
    }

    fun openRoute(routeId: String) {
        val route = _uiState.value.routes.firstOrNull { it.id == routeId } ?: return
        val departure = currentDepartureEpochMillis()
        _uiState.update {
            it.copy(
                selectedRoute = route,
                selectedDepartureEpochMillis = departure
            )
        }
        loadRouteWeather(route, departure)
    }

    fun closeRoute() {
        routeWeatherJob.cancel()
        departureOptimizationJob.cancel()
        _uiState.update {
            it.copy(
                selectedRoute = null,
                routeWeatherState = RouteWeatherLoadState.Idle,
                departureOptimizationState = DepartureOptimizationLoadState.Idle
            )
        }
    }

    fun deleteRoute(routeId: String) {
        viewModelScope.launch {
            gpxRepository.deleteRoute(routeId)
            val routes = gpxRepository.getRoutes()
            _uiState.update { state ->
                state.copy(
                    routes = routes,
                    selectedRoute = state.selectedRoute?.takeUnless { it.id == routeId },
                    departureOptimizationState = if (state.selectedRoute?.id == routeId) {
                        DepartureOptimizationLoadState.Idle
                    } else {
                        state.departureOptimizationState
                    }
                )
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessageRes = null) }
    }

    fun selectMapFilter(filter: MapWeatherFilter) {
        _uiState.update { it.copy(selectedMapFilter = filter) }
        viewModelScope.launch {
            settingsRepository.setSelectedMapFilter(filter)
        }
    }

    fun refreshRouteWeather() {
        val route = _uiState.value.selectedRoute ?: return
        loadRouteWeather(route)
    }

    fun optimizeDepartureTime(autoApply: Boolean = false) {
        val state = _uiState.value
        val route = state.selectedRoute ?: return
        if (state.routeWeatherState is RouteWeatherLoadState.Loading ||
            state.departureOptimizationState is DepartureOptimizationLoadState.Loading
        ) {
            return
        }

        val requestedDeparture = state.selectedDepartureEpochMillis
        _uiState.update {
            it.copy(
                departureOptimizationState = DepartureOptimizationLoadState.Loading,
                errorMessageRes = null
            )
        }

        departureOptimizationJob.launch(viewModelScope) {
            runCatching {
                optimizeDepartureTimeUseCase(
                    route = route,
                    requestedDepartureEpochMillis = requestedDeparture,
                    averageSpeedKmh = _uiState.value.averageSpeedKmh
                )
            }.onSuccess { result ->
                var appliedBest = false
                _uiState.update { current ->
                    if (current.matches(route.id, requestedDeparture)) {
                        val updated = current.copy(
                            departureOptimizationState = DepartureOptimizationLoadState.Ready(result)
                        )
                        if (autoApply) {
                            appliedBest = true
                            updated.copy(selectedDepartureEpochMillis = result.bestCandidate.departureEpochMillis)
                        } else {
                            updated
                        }
                    } else {
                        current
                    }
                }
                if (appliedBest) {
                    loadRouteWeather(route, result.bestCandidate.departureEpochMillis)
                }
            }.onFailure { error ->
                _uiState.update { current ->
                    if (current.matches(route.id, requestedDeparture)) {
                        current.copy(
                            departureOptimizationState = DepartureOptimizationLoadState.Error(
                                RouteWeatherErrorFormatter.messageResFor(error)
                            )
                        )
                    } else {
                        current
                    }
                }
            }
        }
    }

    fun applyOptimizedDeparture(departureEpochMillis: Long) {
        val route = _uiState.value.selectedRoute ?: return
        loadRouteWeather(route, departureEpochMillis)
    }

    fun setDepartureDate(selectedDateUtcMillis: Long) {
        val route = _uiState.value.selectedRoute ?: return
        val newDeparture = DepartureTimeEditor.withDate(
            currentEpochMillis = _uiState.value.selectedDepartureEpochMillis,
            selectedDateUtcMillis = selectedDateUtcMillis
        )
        _uiState.update { it.copy(selectedDepartureEpochMillis = newDeparture) }
        loadRouteWeather(route, newDeparture)
    }

    fun setAverageSpeed(speedKmh: Double) {
        viewModelScope.launch {
            settingsRepository.setAverageSpeedKmh(speedKmh)
        }
    }

    fun setDepartureTime(
        hourOfDay: Int,
        minute: Int
    ) {
        val route = _uiState.value.selectedRoute ?: return
        val newDeparture = DepartureTimeEditor.withTime(
            currentEpochMillis = _uiState.value.selectedDepartureEpochMillis,
            hourOfDay = hourOfDay,
            minute = minute
        )
        _uiState.update { it.copy(selectedDepartureEpochMillis = newDeparture) }
        loadRouteWeather(route, newDeparture)
    }

    fun setThemeMode(mode: ThemeMode) {
        _uiState.update { it.copy(themeMode = mode) }
        viewModelScope.launch {
            settingsRepository.setThemeMode(mode)
        }
    }

    fun setLanguage(language: AppLanguage) {
        _uiState.update { it.copy(language = language) }
        viewModelScope.launch {
            settingsRepository.setLanguage(language)
        }
    }

    fun setDepartureNotificationsEnabled(enabled: Boolean) {
        _uiState.update { it.copy(departureNotificationsEnabled = enabled) }
        viewModelScope.launch {
            settingsRepository.setDepartureNotificationsEnabled(enabled)
        }
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsRepository.settings.collectLatest { settings ->
                _uiState.update { state ->
                    state.copy(
                        averageSpeedKmh = settings.validAverageSpeedKmh(),
                        selectedMapFilter = settings.selectedMapFilter(),
                        themeMode = settings.themeMode(),
                        language = settings.language(),
                        departureNotificationsEnabled = settings.departureNotificationsEnabled,
                        averageSpeedConfigured = settings.averageSpeedConfigured,
                        routeWeatherSummaries = settings.routeWeatherSummaries
                    )
                }
            }
        }
    }

    private fun refreshRoutes() {
        viewModelScope.launch {
            _uiState.update { it.copy(routes = gpxRepository.getRoutes()) }
        }
    }

    private fun importRoute(importAction: suspend () -> ImportedRoute) {
        _uiState.update { it.copy(isImporting = true, errorMessageRes = null) }

        viewModelScope.launch {
            runCatching {
                importAction()
            }.onSuccess { route ->
                val routes = gpxRepository.getRoutes()
                val departure = currentDepartureEpochMillis()
                _uiState.update {
                    it.copy(
                        routes = routes,
                        selectedRoute = route,
                        selectedDepartureEpochMillis = departure,
                        departureOptimizationState = DepartureOptimizationLoadState.Idle,
                        isImporting = false
                    )
                }
                loadRouteWeather(route, departure)
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isImporting = false,
                        errorMessageRes = R.string.gpx_import_failed
                    )
                }
            }
        }
    }

    private fun loadRouteWeather(
        route: ImportedRoute,
        departureEpochMillis: Long = _uiState.value.selectedDepartureEpochMillis
    ) {
        val currentSelected = _uiState.value.selectedDepartureEpochMillis
        val isSameDay = DepartureTimeEditor.datePickerSelectionMillis(currentSelected) ==
                DepartureTimeEditor.datePickerSelectionMillis(departureEpochMillis)
        val shouldKeepOptimization = _uiState.value.selectedRoute?.id == route.id && isSameDay

        if (!shouldKeepOptimization) {
            departureOptimizationJob.cancel()
        }

        _uiState.update {
            it.copy(
                routeWeatherState = RouteWeatherLoadState.Loading,
                selectedDepartureEpochMillis = departureEpochMillis,
                departureOptimizationState = if (shouldKeepOptimization) it.departureOptimizationState else DepartureOptimizationLoadState.Idle,
                errorMessageRes = null
            )
        }

        routeWeatherJob.launch(viewModelScope) {
            runCatching {
                buildRouteWeatherUseCase(
                    route = route,
                    departureEpochMillis = departureEpochMillis,
                    averageSpeedKmh = _uiState.value.averageSpeedKmh
                )
            }.onSuccess { snapshot ->
                val summary = RouteWeatherSummarySnapshot(
                    routeId = route.id,
                    score = snapshot.rideScore.total,
                    checkedAtEpochMillis = clock(),
                    departureEpochMillis = snapshot.departureEpochMillis,
                    freshnessName = snapshot.weatherDataFreshness.name
                )
                _uiState.update { state ->
                    if (state.matches(route.id, departureEpochMillis)) {
                        state.copy(
                            routeWeatherState = RouteWeatherLoadState.Ready(snapshot),
                            routeWeatherSummaries = state.routeWeatherSummaries + (route.id to summary)
                        )
                    } else {
                        state
                    }
                }
                settingsRepository.setRouteWeatherSummary(summary)
            }.onFailure { error ->
                _uiState.update { state ->
                    if (state.matches(route.id, departureEpochMillis)) {
                        state.copy(
                            routeWeatherState = RouteWeatherLoadState.Error(
                                RouteWeatherErrorFormatter.messageResFor(error)
                            )
                        )
                    } else {
                        state
                    }
                }
            }
        }
    }

    private fun CyclistWeatherUiState.matches(routeId: String, departureEpochMillis: Long): Boolean {
        return selectedRoute?.id == routeId && selectedDepartureEpochMillis == departureEpochMillis
    }

    private fun currentDepartureEpochMillis(): Long {
        return System.currentTimeMillis()
    }
}
