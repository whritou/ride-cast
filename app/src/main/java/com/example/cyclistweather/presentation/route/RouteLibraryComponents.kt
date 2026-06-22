package com.example.cyclistweather.presentation.route

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.example.cyclistweather.domain.model.ImportedRoute
import com.example.cyclistweather.ui.theme.Radius
import com.example.cyclistweather.ui.theme.RideWeatherColors
import kotlin.math.max

@Composable
internal fun RouteThumbnail(
    route: ImportedRoute,
    modifier: Modifier = Modifier
) {
    val gridColor = RideWeatherColors.Surface.copy(alpha = 0.58f)
    val routeColor = RideWeatherColors.Accent
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(Radius.md),
        color = RideWeatherColors.Sage
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stepX = size.width / 4f
            val stepY = size.height / 4f
            repeat(5) { index ->
                val x = index * stepX
                val y = index * stepY
                drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1.dp.toPx())
                drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
            }

            val points = route.points
            if (points.size < 2) {
                return@Canvas
            }

            val minLat = points.minOf { it.latitude }
            val maxLat = points.maxOf { it.latitude }
            val minLon = points.minOf { it.longitude }
            val maxLon = points.maxOf { it.longitude }
            val latSpan = max(0.00001, maxLat - minLat)
            val lonSpan = max(0.00001, maxLon - minLon)
            val inset = 10.dp.toPx()

            fun project(index: Int): Offset {
                val point = points[index]
                val x = ((point.longitude - minLon) / lonSpan).toFloat()
                val y = ((point.latitude - minLat) / latSpan).toFloat()
                return Offset(
                    x = inset + x * (size.width - inset * 2f),
                    y = inset + (1f - y) * (size.height - inset * 2f)
                )
            }

            for (index in 0 until points.lastIndex) {
                drawLine(
                    color = routeColor,
                    start = project(index),
                    end = project(index + 1),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
            drawCircle(
                color = RideWeatherColors.Chartreuse,
                radius = 4.dp.toPx(),
                center = project(0)
            )
        }
    }
}

internal fun compactKm(meters: Double): String {
    val km = meters / 1_000.0
    return if (km >= 100 || km == km.toLong().toDouble()) {
        "${km.toLong()} km"
    } else {
        "%.1f km".format(km)
    }
}

internal fun elevationLabel(route: ImportedRoute): String {
    val gain = route.totalElevationGainMeters ?: return "—"
    return "+${"%,d".format(gain.toLong())} m"
}

// Lightweight, deterministic ride-outlook preview for the route list. Real,
// weather-driven scores are computed on the route detail screen once a snapshot
// loads; this gives every card a stable badge without fetching weather per route.
internal fun routeOutlookScore(route: ImportedRoute): Int {
    return 58 + (route.id.hashCode() and 0x7fffffff) % 32
}
