package com.example.cyclistweather.core.di

import android.content.Context
import com.example.cyclistweather.data.gpx.GpxRepository
import com.example.cyclistweather.data.gpx.LocalGpxRepository
import com.example.cyclistweather.data.local.CyclistWeatherDatabase
import com.example.cyclistweather.data.settings.AppSettingsRepository
import com.example.cyclistweather.data.settings.DataStoreAppSettingsRepository
import com.example.cyclistweather.data.weather.CachedRouteWeatherRepository
import com.example.cyclistweather.data.weather.OpenMeteoWeatherRepository
import com.example.cyclistweather.data.weather.RoomWeatherCacheStore
import com.example.cyclistweather.domain.optimizer.OptimizeDepartureTimeUseCase
import com.example.cyclistweather.domain.usecase.BuildRouteWeatherUseCase

/**
 * Manual dependency container: the single place the app's object graph is wired together.
 * Built once in [com.example.cyclistweather.CyclistWeatherApplication]; consumers receive
 * ready-made collaborators so the ViewModel stays constructor-injectable and testable.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val database = CyclistWeatherDatabase.getInstance(appContext)

    val gpxRepository: GpxRepository = LocalGpxRepository(appContext)

    val settingsRepository: AppSettingsRepository = DataStoreAppSettingsRepository(appContext)

    val buildRouteWeatherUseCase = BuildRouteWeatherUseCase(
        CachedRouteWeatherRepository(
            networkRepository = OpenMeteoWeatherRepository(),
            cacheStore = RoomWeatherCacheStore(database)
        )
    )

    val optimizeDepartureTimeUseCase = OptimizeDepartureTimeUseCase(
        buildRouteWeather = { route, departureEpochMillis, averageSpeedKmh ->
            buildRouteWeatherUseCase(route, departureEpochMillis, averageSpeedKmh)
        }
    )
}
