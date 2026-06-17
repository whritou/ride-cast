package com.example.cyclistweather.presentation.routeweather

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Masks
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.cyclistweather.R
import com.example.cyclistweather.core.common.RouteFormatters
import com.example.cyclistweather.domain.model.RouteWeatherSnapshot
import com.example.cyclistweather.ui.theme.Radius
import com.example.cyclistweather.ui.theme.RideWeatherColors
import com.example.cyclistweather.ui.theme.Spacing
import kotlin.math.roundToInt

/** Two-column grid of secondary road conditions (UV, humidity, air quality, visibility …). */
@Composable
internal fun ConditionsGrid(snapshot: RouteWeatherSnapshot) {
    val tiles = conditionTiles(snapshot, RideWeatherColors.Accent)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Radius.lg),
        color = RideWeatherColors.Sage.copy(alpha = 0.42f),
        border = BorderStroke(1.dp, RideWeatherColors.Border.copy(alpha = 0.72f)),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Text(
                text = stringResource(R.string.conditions_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = RideWeatherColors.TextPrimary
            )
            tiles.chunked(2).forEach { rowTiles ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    rowTiles.forEach { tile ->
                        RoadConditionCard(tile = tile, modifier = Modifier.weight(1f))
                    }
                    if (rowTiles.size == 1) {
                        Box(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun RoadConditionCard(
    tile: ConditionTile,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(Radius.md),
        color = RideWeatherColors.Surface.copy(alpha = 0.74f),
        border = BorderStroke(1.dp, RideWeatherColors.Border.copy(alpha = 0.58f)),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(Radius.sm),
                color = tile.tint.copy(alpha = 0.14f),
                contentColor = tile.tint,
                tonalElevation = 0.dp
            ) {
                Icon(
                    imageVector = tile.icon,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(7.dp)
                        .size(20.dp)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = tile.value,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = RideWeatherColors.TextPrimary,
                    maxLines = 1
                )
                Text(
                    text = tile.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = RideWeatherColors.TextSecondary,
                    maxLines = 1
                )
            }
        }
    }
}

private data class ConditionTile(
    val icon: ImageVector,
    val tint: Color,
    val value: String,
    val label: String
)

@Composable
private fun conditionTiles(snapshot: RouteWeatherSnapshot, accent: Color): List<ConditionTile> {
    val segments = snapshot.segmentWeather
    val placeholder = stringResource(R.string.value_placeholder)

    val peakUv = segments.mapNotNull { it.weather.uvIndex }.maxOrNull()
    val avgHumidity = segments.mapNotNull { it.weather.relativeHumidityPercent }
        .takeIf { it.isNotEmpty() }?.average()?.roundToInt()
    val worstAqi = segments.mapNotNull { it.weather.europeanAqi }.maxOrNull()
    val worstVisibility = segments.mapNotNull { it.weather.visibilityMeters }.minOrNull()
    val avgCloud = segments.mapNotNull { it.weather.cloudCoverPercent }
        .takeIf { it.isNotEmpty() }?.average()?.roundToInt()
    val daylightFlags = segments.mapNotNull { it.weather.isDay }
    val dayCount = daylightFlags.count { it }
    val nightCount = daylightFlags.size - dayCount
    val daylightValue = when {
        daylightFlags.isEmpty() -> placeholder
        dayCount > 0 && nightCount > 0 -> stringResource(R.string.daylight_mixed)
        nightCount > 0 -> stringResource(R.string.daylight_night)
        else -> stringResource(R.string.daylight_day)
    }
    val daylightIcon = if (nightCount > dayCount) Icons.Filled.DarkMode else Icons.Filled.LightMode

    val uvValue = peakUv?.let {
        stringResource(R.string.value_with_category, RouteFormatters.formatUvIndex(it), stringResource(uvCategoryRes(it)))
    } ?: placeholder
    val aqiValue = worstAqi?.let {
        stringResource(R.string.value_with_category, it.toString(), stringResource(aqiCategoryRes(it)))
    } ?: placeholder

    return listOf(
        ConditionTile(Icons.Filled.WbSunny, RideWeatherColors.Caution, uvValue, stringResource(R.string.condition_uv)),
        ConditionTile(Icons.Filled.Opacity, RideWeatherColors.Sky, avgHumidity?.let { "$it%" } ?: placeholder, stringResource(R.string.condition_humidity)),
        ConditionTile(Icons.Filled.Masks, accent, aqiValue, stringResource(R.string.condition_air_quality)),
        ConditionTile(Icons.Filled.Visibility, RideWeatherColors.Sky, worstVisibility?.let { RouteFormatters.formatVisibility(it) } ?: placeholder, stringResource(R.string.condition_visibility)),
        ConditionTile(Icons.Filled.Cloud, RideWeatherColors.LowVisibility, avgCloud?.let { "$it%" } ?: placeholder, stringResource(R.string.condition_cloud_cover)),
        ConditionTile(daylightIcon, RideWeatherColors.LowVisibility, daylightValue, stringResource(R.string.condition_daylight))
    )
}

private fun uvCategoryRes(uvIndex: Double): Int = when {
    uvIndex < 3.0 -> R.string.uv_low
    uvIndex < 6.0 -> R.string.uv_moderate
    uvIndex < 8.0 -> R.string.uv_high
    uvIndex < 11.0 -> R.string.uv_very_high
    else -> R.string.uv_extreme
}

private fun aqiCategoryRes(europeanAqi: Int): Int = when {
    europeanAqi <= 20 -> R.string.aqi_good
    europeanAqi <= 40 -> R.string.aqi_fair
    europeanAqi <= 60 -> R.string.aqi_moderate
    europeanAqi <= 80 -> R.string.aqi_poor
    europeanAqi <= 100 -> R.string.aqi_very_poor
    else -> R.string.aqi_extreme
}
