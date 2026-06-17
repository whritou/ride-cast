package com.example.cyclistweather.presentation.routeweather

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.example.cyclistweather.R
import com.example.cyclistweather.presentation.model.WeatherRiskLevel
import com.example.cyclistweather.ui.theme.RideWeatherColors
import com.example.cyclistweather.ui.theme.ScoreColorUtils

/** Thin vertical hairline used to separate inline stats in the fact strip / summary row. */
@Composable
internal fun StatDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(32.dp)
            .background(RideWeatherColors.Border)
    )
}

/** Pill badge showing a segment's qualitative rating (Good / Fair / Avoid …). */
@Composable
internal fun RiskBadge(riskLevel: WeatherRiskLevel) {
    val color = ScoreColorUtils.themedColorForRisk(riskLevel)
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.18f),
        contentColor = color
    ) {
        Text(
            text = stringResource(riskLevel.labelRes),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/** Vertical timeline rail with a colored node, used in the conditions list. */
@Composable
internal fun RouteTimelineMarker(
    isFirst: Boolean,
    isLast: Boolean,
    riskLevel: WeatherRiskLevel
) {
    val color = ScoreColorUtils.themedColorForRisk(riskLevel)
    val railColor = RideWeatherColors.Border
    Canvas(
        modifier = Modifier
            .width(24.dp)
            .height(48.dp)
    ) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        if (!isFirst) {
            drawLine(
                color = railColor,
                start = Offset(centerX, 0f),
                end = Offset(centerX, centerY - 8.dp.toPx()),
                strokeWidth = 2.dp.toPx()
            )
        }
        if (!isLast) {
            drawLine(
                color = railColor,
                start = Offset(centerX, centerY + 8.dp.toPx()),
                end = Offset(centerX, size.height),
                strokeWidth = 2.dp.toPx()
            )
        }
        drawCircle(color = color, radius = 6.dp.toPx(), center = Offset(centerX, centerY))
    }
}
