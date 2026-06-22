package com.example.cyclistweather.presentation.routeweather

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.cyclistweather.R
import com.example.cyclistweather.core.common.RouteFormatters
import com.example.cyclistweather.domain.model.RouteWeatherSnapshot
import com.example.cyclistweather.presentation.components.IconStat
import com.example.cyclistweather.ui.theme.Radius
import com.example.cyclistweather.ui.theme.RideWeatherColors
import com.example.cyclistweather.ui.theme.Spacing
import kotlin.math.roundToInt

/** Compact at-a-glance row of the route's headline weather stats (wind, temp, rain, gusts). */
@Composable
internal fun WeatherSummaryRow(snapshot: RouteWeatherSnapshot) {
    val summary = remember(snapshot) { weatherSummaryOf(snapshot) }
    val placeholder = stringResource(R.string.value_placeholder)
    val windLabel = summary.windDirection?.let { stringResource(R.string.summary_wind_direction, it) }
        ?: stringResource(R.string.summary_wind)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Radius.lg),
        color = RideWeatherColors.Sage,
        border = BorderStroke(1.dp, RideWeatherColors.Border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.lg, horizontal = Spacing.sm),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconStat(Icons.Filled.Air, RideWeatherColors.Sky, summary.windValue ?: placeholder, windLabel)
            StatDivider()
            IconStat(Icons.Filled.Thermostat, RideWeatherColors.Caution, summary.tempValue ?: placeholder, stringResource(R.string.summary_temp))
            StatDivider()
            IconStat(Icons.Filled.WaterDrop, RideWeatherColors.Sky, summary.rainValue ?: placeholder, stringResource(R.string.summary_rain))
            StatDivider()
            IconStat(Icons.Filled.Speed, RideWeatherColors.Accent, summary.gustValue ?: placeholder, stringResource(R.string.summary_gusts))
        }
    }
}

/**
 * Headline stats for the summary row. Values are kept as nullable formatted strings (null → render
 * a localized em-dash placeholder) and the wind direction stays a structured abbreviation so the
 * UI layer can localize the surrounding "… wind" phrasing.
 */
private data class WeatherSummary(
    val windValue: String?,
    val windDirection: String?,
    val tempValue: String?,
    val rainValue: String?,
    val gustValue: String?
)

private fun weatherSummaryOf(snapshot: RouteWeatherSnapshot): WeatherSummary {
    val segments = snapshot.segmentWeather
    if (segments.isEmpty()) {
        return WeatherSummary(null, null, null, null, null)
    }
    val avgWind = segments.map { it.weather.windSpeedKmh }.average().roundToInt()
    val avgDir = segments.map { it.weather.windDirectionDegrees }.average()
    val avgTemp = segments.map { it.weather.temperatureCelsius }.average().roundToInt()
    val peakRain = segments.maxOf { it.weather.precipitationProbabilityPercent ?: 0 }
    val peakGust = segments.mapNotNull { it.weather.windGustKmh }.maxOrNull()?.roundToInt()
    return WeatherSummary(
        windValue = "$avgWind km/h",
        windDirection = RouteFormatters.formatWindDirection(avgDir),
        tempValue = "$avgTemp°C",
        rainValue = "$peakRain%",
        gustValue = peakGust?.let { "$it km/h" }
    )
}
