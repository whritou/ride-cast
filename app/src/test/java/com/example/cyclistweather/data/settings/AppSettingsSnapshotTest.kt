package com.example.cyclistweather.data.settings

import com.example.cyclistweather.domain.model.MapWeatherFilter
import com.example.cyclistweather.domain.model.WeatherDataFreshness
import org.junit.Assert.assertEquals
import org.junit.Test

class AppSettingsSnapshotTest {
    @Test
    fun `selected map filter falls back to wind when stored value is invalid`() {
        val settings = AppSettingsSnapshot(selectedMapFilterName = "NOT_A_FILTER")

        assertEquals(MapWeatherFilter.WIND, settings.selectedMapFilter())
    }

    @Test
    fun `average speed ignores non positive stored values`() {
        val settings = AppSettingsSnapshot(averageSpeedKmh = -1.0)

        assertEquals(22.0, settings.validAverageSpeedKmh(), 0.0)
    }

    @Test
    fun `route weather summaries expose valid freshness values`() {
        val summary = RouteWeatherSummarySnapshot(
            routeId = "route-1",
            score = 82,
            checkedAtEpochMillis = 1_000L,
            departureEpochMillis = 2_000L,
            freshnessName = WeatherDataFreshness.FRESH_CACHE.name
        )

        assertEquals(WeatherDataFreshness.FRESH_CACHE, summary.freshness())
    }

    @Test
    fun `route weather summaries fall back to stale when freshness is invalid`() {
        val summary = RouteWeatherSummarySnapshot(
            routeId = "route-1",
            score = 82,
            checkedAtEpochMillis = 1_000L,
            departureEpochMillis = 2_000L,
            freshnessName = "UNKNOWN"
        )

        assertEquals(WeatherDataFreshness.STALE_CACHE, summary.freshness())
    }
}
