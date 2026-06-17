package com.example.cyclistweather.presentation.app

import com.example.cyclistweather.data.gpx.GpxRepository
import com.example.cyclistweather.data.settings.AppSettingsRepository
import com.example.cyclistweather.data.settings.AppSettingsSnapshot
import com.example.cyclistweather.data.settings.RouteWeatherSummarySnapshot
import com.example.cyclistweather.data.weather.RouteWeatherRepository
import com.example.cyclistweather.data.weather.WeatherDataFreshnessProvider
import com.example.cyclistweather.domain.model.ImportedRoute
import com.example.cyclistweather.domain.model.RoutePoint
import com.example.cyclistweather.domain.model.WeatherPoint
import com.example.cyclistweather.domain.model.WeatherDataFreshness
import com.example.cyclistweather.domain.optimizer.OptimizeDepartureTimeUseCase
import com.example.cyclistweather.domain.usecase.BuildRouteWeatherUseCase
import com.example.cyclistweather.domain.model.MapWeatherFilter
import com.example.cyclistweather.presentation.routeweather.RouteWeatherLoadState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CyclistWeatherViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init loads routes and applies persisted settings`() = runTest {
        val route = route()
        val viewModel = viewModel(routes = listOf(route))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf(route.id), state.routes.map { it.id })
        assertEquals(25.0, state.averageSpeedKmh, 0.0)
    }

    @Test
    fun `opening a route loads weather into the ready state`() = runTest {
        val route = route()
        val viewModel = viewModel(routes = listOf(route))
        advanceUntilIdle()

        viewModel.openRoute(route.id)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(route.id, state.selectedRoute?.id)
        assertTrue(state.routeWeatherState is RouteWeatherLoadState.Ready)
    }

    @Test
    fun `opening a route persists route weather summary for the library`() = runTest {
        val route = route()
        val settings = FakeAppSettingsRepository()
        val viewModel = viewModel(
            routes = listOf(route),
            settings = settings,
            weatherFreshness = WeatherDataFreshness.FRESH_CACHE,
            clock = { 12_345L }
        )
        advanceUntilIdle()

        viewModel.openRoute(route.id)
        advanceUntilIdle()

        val summary = settings.savedRouteSummary
        assertEquals(route.id, summary?.routeId)
        assertEquals(12_345L, summary?.checkedAtEpochMillis)
        assertEquals(WeatherDataFreshness.FRESH_CACHE, summary?.freshness())
        assertEquals(summary, viewModel.uiState.value.routeWeatherSummaries[route.id])
    }

    @Test
    fun `selecting a map filter persists it and updates state`() = runTest {
        val settings = FakeAppSettingsRepository()
        val viewModel = viewModel(routes = emptyList(), settings = settings)
        advanceUntilIdle()

        viewModel.selectMapFilter(MapWeatherFilter.RAIN)
        advanceUntilIdle()

        assertEquals(MapWeatherFilter.RAIN, settings.savedFilter)
        assertEquals(MapWeatherFilter.RAIN, viewModel.uiState.value.selectedMapFilter)
    }

    @Test
    fun `closing a route clears the selection and weather state`() = runTest {
        val route = route()
        val viewModel = viewModel(routes = listOf(route))
        advanceUntilIdle()
        viewModel.openRoute(route.id)
        advanceUntilIdle()

        viewModel.closeRoute()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(null, state.selectedRoute)
        assertTrue(state.routeWeatherState is RouteWeatherLoadState.Idle)
    }

    private fun viewModel(
        routes: List<ImportedRoute>,
        settings: AppSettingsRepository = FakeAppSettingsRepository(),
        weatherFreshness: WeatherDataFreshness = WeatherDataFreshness.LIVE,
        clock: () -> Long = { 0L }
    ): CyclistWeatherViewModel {
        val buildRouteWeather = BuildRouteWeatherUseCase(FakeRouteWeatherRepository(weatherFreshness))
        return CyclistWeatherViewModel(
            gpxRepository = FakeGpxRepository(routes),
            settingsRepository = settings,
            buildRouteWeatherUseCase = buildRouteWeather,
            optimizeDepartureTimeUseCase = OptimizeDepartureTimeUseCase(
                buildRouteWeather = { route, departure, speed -> buildRouteWeather(route, departure, speed) }
            ),
            clock = clock
        )
    }

    private fun route(): ImportedRoute = ImportedRoute(
        id = "route-1",
        name = "Test route",
        points = listOf(
            RoutePoint(37.0, 127.0, null, 0.0),
            RoutePoint(37.0, 127.05, null, 5_000.0)
        ),
        totalDistanceMeters = 5_000.0,
        totalElevationGainMeters = null,
        createdAtEpochMillis = 0L
    )

    private class FakeGpxRepository(private var routes: List<ImportedRoute>) : GpxRepository {
        override suspend fun importGpx(uri: android.net.Uri): ImportedRoute = routes.first()
        override suspend fun importBundledSampleRoute(): ImportedRoute = routes.first()
        override suspend fun getRoutes(): List<ImportedRoute> = routes
        override suspend fun getRoute(routeId: String): ImportedRoute? = routes.firstOrNull { it.id == routeId }
        override suspend fun deleteRoute(routeId: String) {
            routes = routes.filterNot { it.id == routeId }
        }
    }

    private class FakeAppSettingsRepository : AppSettingsRepository {
        private val settingsState = MutableStateFlow(AppSettingsSnapshot(averageSpeedKmh = 25.0))
        override val settings: Flow<AppSettingsSnapshot> = settingsState
        var savedFilter: MapWeatherFilter? = null
        var savedRouteSummary: RouteWeatherSummarySnapshot? = null
        override suspend fun setAverageSpeedKmh(value: Double) = Unit
        override suspend fun setSelectedMapFilter(filter: MapWeatherFilter) {
            savedFilter = filter
        }
        override suspend fun setThemeMode(mode: com.example.cyclistweather.domain.model.ThemeMode) = Unit
        override suspend fun setLanguage(language: com.example.cyclistweather.domain.model.AppLanguage) = Unit
        override suspend fun setDepartureNotificationsEnabled(enabled: Boolean) = Unit
        override suspend fun setRouteWeatherSummary(summary: RouteWeatherSummarySnapshot) {
            savedRouteSummary = summary
            settingsState.value = settingsState.value.copy(
                routeWeatherSummaries = settingsState.value.routeWeatherSummaries + (summary.routeId to summary)
            )
        }
    }

    private class FakeRouteWeatherRepository(
        override val currentWeatherDataFreshness: WeatherDataFreshness
    ) : RouteWeatherRepository,
        WeatherDataFreshnessProvider {
        override fun beginRouteWeatherLoad() = Unit

        override suspend fun getWeatherForPoint(latitude: Double, longitude: Double): List<WeatherPoint> =
            listOf(
                WeatherPoint(
                    latitude = latitude,
                    longitude = longitude,
                    timeEpochMillis = 0L,
                    temperatureCelsius = 18.0,
                    apparentTemperatureCelsius = 18.0,
                    windSpeedKmh = 10.0,
                    windDirectionDegrees = 90.0,
                    windGustKmh = 12.0,
                    precipitationProbabilityPercent = 0,
                    precipitationMm = 0.0,
                    cloudCoverPercent = 20,
                    weatherCode = 1
                )
            )
    }
}
