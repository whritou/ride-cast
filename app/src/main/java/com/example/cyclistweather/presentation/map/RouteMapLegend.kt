package com.example.cyclistweather.presentation.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.dp
import com.example.cyclistweather.R
import com.example.cyclistweather.domain.model.MapWeatherFilter
import com.example.cyclistweather.domain.model.SegmentWeather
import com.example.cyclistweather.presentation.routeweather.mapFilterLabelRes
import com.example.cyclistweather.ui.theme.RideWeatherColors
import kotlin.math.roundToInt

private val MAP_LEGEND_CARD_WIDTH = 238.dp

@Composable
internal fun MapQuickSummary(
    segmentWeather: List<SegmentWeather>,
    modifier: Modifier = Modifier
) {
    val summary = remember(segmentWeather) { RouteMapQuickSummary.from(segmentWeather) }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = RideWeatherColors.Surface.copy(alpha = 0.95f),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            QuickSummaryItem(value = summary.wind, label = stringResource(R.string.summary_wind), color = RideWeatherColors.Sky)
            QuickSummaryItem(value = summary.temperature, label = stringResource(R.string.summary_temp), color = RideWeatherColors.Caution)
            QuickSummaryItem(value = summary.rain, label = stringResource(R.string.summary_rain), color = RideWeatherColors.Sky)
            QuickSummaryItem(value = summary.gusts, label = stringResource(R.string.summary_gusts), color = RideWeatherColors.LowVisibility)
        }
    }
}

@Composable
private fun QuickSummaryItem(
    value: String,
    label: String,
    color: ComposeColor
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(modifier = Modifier.size(7.dp)) {
            drawCircle(color = color)
        }
        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                color = RideWeatherColors.TextPrimary,
                maxLines = 1
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = RideWeatherColors.TextSecondary,
                maxLines = 1
            )
        }
    }
}

private data class RouteMapQuickSummary(
    val wind: String,
    val temperature: String,
    val rain: String,
    val gusts: String
) {
    companion object {
        fun from(segmentWeather: List<SegmentWeather>): RouteMapQuickSummary {
            if (segmentWeather.isEmpty()) {
                return RouteMapQuickSummary("--", "--", "--", "--")
            }

            val averageWind = segmentWeather.map { it.weather.windSpeedKmh }.average().roundToInt()
            val averageTemperature = segmentWeather.map { it.weather.temperatureCelsius }.average().roundToInt()
            val peakRain = segmentWeather.maxOf { it.weather.precipitationProbabilityPercent ?: 0 }
            val peakGust = segmentWeather.mapNotNull { it.weather.windGustKmh }.maxOrNull()?.roundToInt()

            return RouteMapQuickSummary(
                wind = "$averageWind km/h",
                temperature = "$averageTemperature C",
                rain = "$peakRain%",
                gusts = peakGust?.let { "$it km/h" } ?: "--"
            )
        }
    }
}

@Composable
internal fun MapFilterBadge(
    totalDistanceMeters: Double,
    selectedFilter: MapWeatherFilter,
    hasWeatherData: Boolean,
    modifier: Modifier = Modifier
) {
    val legend = RouteMapRenderPlanner.legendSpecForFilter(selectedFilter)
    val sampleSpacingLabel = RouteMapRenderPlanner.sampleSpacingLabel(totalDistanceMeters)
    Surface(
        modifier = modifier.width(MAP_LEGEND_CARD_WIDTH),
        shape = RoundedCornerShape(8.dp),
        color = RideWeatherColors.Surface.copy(alpha = 0.94f),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = if (hasWeatherData) {
                    if (selectedFilter != MapWeatherFilter.WIND) {
                        stringResource(R.string.map_legend_coverage, stringResource(legend.descriptionRes))
                    } else {
                        stringResource(R.string.map_wind_every, sampleSpacingLabel)
                    }
                } else {
                    stringResource(R.string.map_legend_pending, stringResource(mapFilterLabelRes(selectedFilter)))
                },
                style = MaterialTheme.typography.labelLarge,
                color = RideWeatherColors.TextPrimary
            )
            when {
                !hasWeatherData -> Text(
                    text = stringResource(R.string.map_waiting_samples),
                    style = MaterialTheme.typography.bodySmall,
                    color = RideWeatherColors.TextSecondary
                )
                legend.stops.isEmpty() -> if (selectedFilter != MapWeatherFilter.WIND) {
                    Text(
                        text = stringResource(legend.descriptionRes),
                        style = MaterialTheme.typography.bodySmall,
                        color = RideWeatherColors.TextSecondary
                    )
                }
                else -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    WeatherGradientLegend(legend)
                }
            }
        }
    }
}

@Composable
private fun WeatherGradientLegend(legend: WeatherLegendSpec) {
    val markerColor = RideWeatherColors.TextPrimary.copy(alpha = 0.78f)
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp)
        ) {
            val barHeight = 9.dp.toPx()
            val markerExtension = 2.dp.toPx()
            val strokeWidth = 1.2.dp.toPx()
            val top = (size.height - barHeight) / 2f
            val colorStops = legend.stops
                .map { stop -> stop.position.coerceIn(0f, 1f) to ComposeColor(stop.color) }
                .toTypedArray()

            val barBrush = when (colorStops.size) {
                0 -> null
                1 -> SolidColor(colorStops.first().second)
                else -> Brush.horizontalGradient(colorStops = colorStops)
            }
            barBrush?.let { brush ->
                drawRoundRect(
                    brush = brush,
                    topLeft = Offset(0f, top),
                    size = Size(size.width, barHeight),
                )
            }

            legend.stops.forEach { stop ->
                val x = stop.position.coerceIn(0f, 1f) * size.width
                drawLine(
                    color = markerColor,
                    start = Offset(x, top - markerExtension),
                    end = Offset(x, top + barHeight + markerExtension),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }
        }
        Box(modifier = Modifier.fillMaxWidth()) {
            legend.stops.forEach { stop ->
                Text(
                    text = if (stop.labelRes != 0) stringResource(stop.labelRes) else stop.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = RideWeatherColors.TextSecondary,
                    maxLines = 1,
                    modifier = Modifier.legendStopLabelPosition(stop.position)
                )
            }
        }
    }
}

private fun Modifier.legendStopLabelPosition(markerPosition: Float): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
    val containerWidthPx = if (constraints.hasBoundedWidth) constraints.maxWidth else placeable.width
    val x = previewLegendLabelStartOffsetPx(
        markerPosition = markerPosition,
        containerWidthPx = containerWidthPx,
        labelWidthPx = placeable.width
    )

    layout(containerWidthPx, placeable.height) {
        placeable.placeRelative(x, 0)
    }
}

private fun previewLegendLabelStartOffsetPx(
    markerPosition: Float,
    containerWidthPx: Int,
    labelWidthPx: Int
): Int {
    val maxStartOffsetPx = (containerWidthPx - labelWidthPx).coerceAtLeast(0)
    val markerX = markerPosition.coerceIn(0f, 1f) * containerWidthPx
    val centeredStartOffsetPx = (markerX - labelWidthPx / 2f).roundToInt()

    return centeredStartOffsetPx.coerceIn(0, maxStartOffsetPx)
}
    