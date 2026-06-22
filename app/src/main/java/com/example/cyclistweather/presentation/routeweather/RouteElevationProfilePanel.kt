package com.example.cyclistweather.presentation.routeweather

import android.graphics.Paint as AndroidPaint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cyclistweather.core.common.RouteFormatters
import com.example.cyclistweather.domain.model.ImportedRoute
import com.example.cyclistweather.domain.model.SegmentWeather
import com.example.cyclistweather.ui.theme.Radius
import com.example.cyclistweather.ui.theme.RideWeatherColors
import com.example.cyclistweather.ui.theme.RouteFirstPanel
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * Interactive elevation profile chart for the route overview. Tapping/dragging reveals a cursor
 * with the nearest segment's weather. Extracted from RouteOverviewContent to keep that file focused.
 */
@Composable
internal fun ElevationProfilePanel(route: ImportedRoute, segmentWeather: List<SegmentWeather> = emptyList()) {
    val profile = remember(route.id, route.points) {
        RouteElevationProfilePlanner.profile(route.points)
    }
    val ticks = remember(profile.minElevationMeters, profile.maxElevationMeters) {
        elevationTicks(profile.minElevationMeters, profile.maxElevationMeters)
    }
    val yAxisMin = ticks.first().toFloat()
    val yAxisMax = ticks.last().toFloat()
    val yAxisRange = (yAxisMax - yAxisMin).coerceAtLeast(1f)

    var cursorFraction by remember { mutableStateOf<Float?>(null) }
    var canvasWidthPx by remember { mutableStateOf(1f) }
    val density = LocalDensity.current
    val labelAreaDp = 44.dp
    val labelAreaPx = with(density) { labelAreaDp.toPx() }

    // Hoisted out of the Canvas/DrawScope, which is not a @Composable context.
    val gridLabelColor = RideWeatherColors.TextSecondary
    val gridLineColor = RideWeatherColors.Border
    val accentColor = RideWeatherColors.Accent
    val onAccentColor = RideWeatherColors.OnAccent

    RouteFirstPanel {
        Column(
            modifier = Modifier.padding(start = 4.dp, end = 12.dp, top = 12.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Elevation profile",
                modifier = Modifier.padding(start = 8.dp),
                style = MaterialTheme.typography.labelMedium,
                color = RideWeatherColors.TextSecondary
            )

            Box {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .onSizeChanged { canvasWidthPx = it.width.toFloat().coerceAtLeast(1f) }
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                val chartW = canvasWidthPx - labelAreaPx
                                if (chartW > 0f && offset.x >= labelAreaPx) {
                                    cursorFraction = ((offset.x - labelAreaPx) / chartW).coerceIn(0f, 1f)
                                }
                            }
                        }
                        .pointerInput(Unit) {
                            detectDragGestures { change, _ ->
                                val chartW = canvasWidthPx - labelAreaPx
                                if (chartW > 0f) {
                                    cursorFraction = ((change.position.x - labelAreaPx) / chartW).coerceIn(0f, 1f)
                                }
                            }
                        }
                ) {
                    val samples = profile.samples
                    val chartLeft = labelAreaPx
                    val chartRight = size.width
                    val chartW = chartRight - chartLeft
                    val paddingTop = 8.dp.toPx()
                    val paddingBottom = 14.dp.toPx()
                    val top = paddingTop
                    val bottom = size.height - paddingBottom
                    val chartHeight = bottom - top

                    val dataMin = profile.minElevationMeters.toFloat()
                    val dataMax = profile.maxElevationMeters.toFloat()
                    val elevSpan = (dataMax - dataMin).coerceAtLeast(1f)

                    fun sampleY(elevFraction: Float): Float {
                        val absElev = dataMin + elevFraction * elevSpan
                        val axisFraction = (absElev - yAxisMin) / yAxisRange
                        return bottom - axisFraction * chartHeight
                    }

                    fun tickY(tick: Int): Float {
                        val axisFraction = (tick - yAxisMin) / yAxisRange
                        return bottom - axisFraction * chartHeight
                    }

                    val textPaint = AndroidPaint().apply {
                        isAntiAlias = true
                        textSize = 10.sp.toPx()
                        color = gridLabelColor.toArgb()
                        textAlign = AndroidPaint.Align.RIGHT
                        typeface = Typeface.DEFAULT
                    }
                    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 5f), 0f)

                    // horizontal grid lines + y-axis labels
                    ticks.forEach { tick ->
                        val y = tickY(tick)
                        drawIntoCanvas { canvas ->
                            canvas.nativeCanvas.drawText(
                                "${tick} m",
                                chartLeft - 6.dp.toPx(),
                                y + textPaint.textSize * 0.35f,
                                textPaint
                            )
                        }
                        drawLine(
                            color = gridLineColor,
                            start = Offset(chartLeft, y),
                            end = Offset(chartRight, y),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = dashEffect
                        )
                    }

                    if (samples.size < 2) return@Canvas

                    // filled area under curve
                    val fillPath = Path()
                    fillPath.moveTo(chartLeft + samples.first().distanceFraction * chartW, bottom)
                    samples.forEach { s ->
                        fillPath.lineTo(chartLeft + s.distanceFraction * chartW, sampleY(s.elevationFraction))
                    }
                    fillPath.lineTo(chartLeft + samples.last().distanceFraction * chartW, bottom)
                    fillPath.close()
                    drawPath(fillPath, color = accentColor.copy(alpha = 0.10f))

                    // elevation line
                    for (i in 0 until samples.lastIndex) {
                        val s = samples[i]
                        val e = samples[i + 1]
                        drawLine(
                            color = accentColor,
                            start = Offset(chartLeft + s.distanceFraction * chartW, sampleY(s.elevationFraction)),
                            end = Offset(chartLeft + e.distanceFraction * chartW, sampleY(e.elevationFraction)),
                            strokeWidth = 2.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }

                    // cursor
                    cursorFraction?.let { fraction ->
                        val cx = chartLeft + fraction * chartW
                        val nearestSample = samples.minByOrNull { abs(it.distanceFraction - fraction) }
                        val dotY = nearestSample?.let { sampleY(it.elevationFraction) } ?: bottom
                        drawLine(
                            color = accentColor.copy(alpha = 0.55f),
                            start = Offset(cx, top),
                            end = Offset(cx, bottom),
                            strokeWidth = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 3f), 0f)
                        )
                        drawCircle(
                            color = accentColor,
                            radius = 4.5.dp.toPx(),
                            center = Offset(cx, dotY)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 2.5.dp.toPx(),
                            center = Offset(cx, dotY)
                        )
                    }
                }

                // cursor popup
                cursorFraction?.let { fraction ->
                    val nearestSegment = remember(fraction, segmentWeather, profile.totalDistanceMeters) {
                        nearestSegmentAt(fraction, segmentWeather, profile.totalDistanceMeters)
                    }
                    if (nearestSegment != null) {
                        val popupWidthDp = 128.dp
                        val popupWidthPx = with(density) { popupWidthDp.toPx() }
                        val chartW = canvasWidthPx - labelAreaPx
                        val cursorX = labelAreaPx + fraction * chartW
                        val rawX = cursorX - popupWidthPx / 2f
                        val clampedX = rawX.coerceIn(0f, (canvasWidthPx - popupWidthPx).coerceAtLeast(0f))
                        Surface(
                            modifier = Modifier
                                .offset { IntOffset(clampedX.roundToInt(), 0) }
                                .size(width = popupWidthDp, height = 60.dp),
                            shape = RoundedCornerShape(Radius.sm),
                            color = accentColor,
                            contentColor = onAccentColor
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                val w = nearestSegment.weather
                                Text(
                                    text = "${w.windSpeedKmh.roundToInt()} km/h  ${RouteFormatters.formatWindDirection(w.windDirectionDegrees)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${w.temperatureCelsius.roundToInt()}°C  ${w.precipitationProbabilityPercent ?: 0}% rain",
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Text(
                                    text = "@ ${RouteFormatters.formatDistance(nearestSegment.sample.distanceFromStartMeters)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = onAccentColor.copy(alpha = 0.70f)
                                )
                            }
                        }
                    }
                }
            }

            // x-axis distance labels
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = labelAreaDp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "0 km",
                    style = MaterialTheme.typography.labelSmall,
                    color = RideWeatherColors.TextSecondary
                )
                Text(
                    text = RouteFormatters.formatDistance(profile.totalDistanceMeters),
                    style = MaterialTheme.typography.labelSmall,
                    color = RideWeatherColors.TextSecondary
                )
            }
        }
    }
}

private fun elevationTicks(minM: Int, maxM: Int): List<Int> {
    val range = (maxM - minM).coerceAtLeast(1)
    val rawStep = ceil(range / 4.0)
    val step = when {
        rawStep <= 5 -> 5
        rawStep <= 10 -> 10
        rawStep <= 25 -> 25
        rawStep <= 50 -> 50
        rawStep <= 100 -> 100
        rawStep <= 200 -> 200
        else -> 500
    }
    val first = (minM / step) * step
    val last = ((maxM + step - 1) / step) * step
    return (first..last step step).toList()
}

private fun nearestSegmentAt(
    fraction: Float,
    segments: List<SegmentWeather>,
    totalDistanceMeters: Double
): SegmentWeather? {
    if (segments.isEmpty() || totalDistanceMeters <= 0.0) return null
    val targetDistance = fraction * totalDistanceMeters
    return segments.minByOrNull { abs(it.sample.distanceFromStartMeters - targetDistance) }
}
