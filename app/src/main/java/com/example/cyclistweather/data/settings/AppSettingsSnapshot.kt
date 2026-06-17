package com.example.cyclistweather.data.settings

import com.example.cyclistweather.domain.model.AppLanguage
import com.example.cyclistweather.domain.model.MapWeatherFilter
import com.example.cyclistweather.domain.model.ThemeMode
import com.example.cyclistweather.domain.model.WeatherDataFreshness
import kotlinx.serialization.Serializable

@Serializable
data class AppSettingsSnapshot(
    val averageSpeedKmh: Double = DEFAULT_AVERAGE_SPEED_KMH,
    val selectedMapFilterName: String = MapWeatherFilter.WIND.name,
    val themeModeName: String = ThemeMode.SYSTEM.name,
    val languageName: String = AppLanguage.SYSTEM.name,
    val departureNotificationsEnabled: Boolean = false,
    val routeWeatherSummaries: Map<String, RouteWeatherSummarySnapshot> = emptyMap()
) {
    fun validAverageSpeedKmh(): Double {
        return averageSpeedKmh.takeIf { it > 0.0 } ?: DEFAULT_AVERAGE_SPEED_KMH
    }

    fun selectedMapFilter(): MapWeatherFilter {
        return MapWeatherFilter.entries.firstOrNull { it.name == selectedMapFilterName } ?: MapWeatherFilter.WIND
    }

    fun themeMode(): ThemeMode {
        return ThemeMode.entries.firstOrNull { it.name == themeModeName } ?: ThemeMode.SYSTEM
    }

    fun language(): AppLanguage {
        return AppLanguage.entries.firstOrNull { it.name == languageName } ?: AppLanguage.SYSTEM
    }
}

@Serializable
data class RouteWeatherSummarySnapshot(
    val routeId: String,
    val score: Int,
    val checkedAtEpochMillis: Long,
    val departureEpochMillis: Long,
    val freshnessName: String
) {
    fun freshness(): WeatherDataFreshness {
        return WeatherDataFreshness.entries.firstOrNull { it.name == freshnessName }
            ?: WeatherDataFreshness.STALE_CACHE
    }
}

const val DEFAULT_AVERAGE_SPEED_KMH = 22.0
