package com.example.cyclistweather.data.settings

import com.example.cyclistweather.domain.model.AppLanguage
import com.example.cyclistweather.domain.model.MapWeatherFilter
import com.example.cyclistweather.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

/** Read/write access to persisted app settings. */
interface AppSettingsRepository {
    val settings: Flow<AppSettingsSnapshot>
    suspend fun setAverageSpeedKmh(value: Double)
    suspend fun setSelectedMapFilter(filter: MapWeatherFilter)
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setLanguage(language: AppLanguage)
    suspend fun setDepartureNotificationsEnabled(enabled: Boolean)
    suspend fun setRouteWeatherSummary(summary: RouteWeatherSummarySnapshot)
}
