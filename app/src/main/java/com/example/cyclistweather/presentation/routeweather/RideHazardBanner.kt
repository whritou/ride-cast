package com.example.cyclistweather.presentation.routeweather

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.cyclistweather.R
import com.example.cyclistweather.domain.model.HazardKind
import com.example.cyclistweather.domain.model.HazardSeverity
import com.example.cyclistweather.domain.model.RideHazard
import com.example.cyclistweather.ui.theme.RideWeatherColors

@Composable
internal fun RideHazardBanners(
    hazards: List<RideHazard>,
    modifier: Modifier = Modifier
) {
    if (hazards.isEmpty()) return

    val dangers = hazards.filter { it.severity == HazardSeverity.DANGER }
    val warnings = hazards.filter { it.severity == HazardSeverity.WARNING }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (dangers.isNotEmpty()) {
            HazardBanner(
                accent = RideWeatherColors.Dangerous,
                icon = Icons.Filled.Error,
                heading = stringResource(if (dangers.size == 1) R.string.hazard_danger else R.string.hazard_dangers),
                hazards = dangers
            )
        }
        if (warnings.isNotEmpty()) {
            HazardBanner(
                accent = RideWeatherColors.Caution,
                icon = Icons.Filled.Warning,
                heading = stringResource(if (warnings.size == 1) R.string.hazard_warning else R.string.hazard_warnings),
                hazards = warnings
            )
        }
    }
}

@Composable
private fun HazardBanner(
    accent: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    heading: String,
    hazards: List<RideHazard>
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(accent.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
            .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = heading,
            tint = accent,
            modifier = Modifier.size(22.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = heading,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = RideWeatherColors.TextPrimary
            )
            hazards.forEach { hazard ->
                Column {
                    Text(
                        text = stringResource(hazardTitleRes(hazard.kind)),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = RideWeatherColors.TextPrimary
                    )
                    Text(
                        text = hazardDetail(hazard),
                        style = MaterialTheme.typography.bodySmall,
                        color = RideWeatherColors.TextSecondary
                    )
                }
            }
        }
    }
}

@androidx.annotation.StringRes
private fun hazardTitleRes(kind: HazardKind): Int = when (kind) {
    HazardKind.THUNDERSTORM -> R.string.hz_thunderstorm_title
    HazardKind.HEAVY_SNOW -> R.string.hz_heavy_snow_title
    HazardKind.SNOW_ICE -> R.string.hz_snow_ice_title
    HazardKind.HEAVY_RAIN -> R.string.hz_heavy_rain_title
    HazardKind.RAIN_LIKELY -> R.string.hz_rain_likely_title
    HazardKind.DENSE_FOG -> R.string.hz_dense_fog_title
    HazardKind.LOW_VISIBILITY -> R.string.hz_low_visibility_title
    HazardKind.VIOLENT_GUSTS -> R.string.hz_violent_gusts_title
    HazardKind.STRONG_GUSTS -> R.string.hz_strong_gusts_title
    HazardKind.EXTREME_HEAT -> R.string.hz_extreme_heat_title
    HazardKind.HIGH_HEAT -> R.string.hz_high_heat_title
    HazardKind.SEVERE_COLD -> R.string.hz_severe_cold_title
    HazardKind.FREEZING -> R.string.hz_freezing_title
    HazardKind.POOR_AIR -> R.string.hz_poor_air_title
    HazardKind.VERY_HIGH_UV -> R.string.hz_uv_title
    HazardKind.NIGHT_RIDE -> R.string.hz_night_title
}

@Composable
private fun hazardDetail(hazard: RideHazard): String {
    val near = hazard.distanceLabel.orEmpty()
    val value = hazard.value ?: 0
    return when (hazard.kind) {
        HazardKind.THUNDERSTORM -> stringResource(R.string.hz_thunderstorm_detail, near)
        HazardKind.HEAVY_SNOW -> stringResource(R.string.hz_heavy_snow_detail, near)
        HazardKind.SNOW_ICE -> stringResource(R.string.hz_snow_ice_detail, near)
        HazardKind.HEAVY_RAIN -> stringResource(R.string.hz_heavy_rain_detail, near)
        HazardKind.RAIN_LIKELY -> stringResource(R.string.hz_rain_likely_detail, near)
        HazardKind.DENSE_FOG -> stringResource(R.string.hz_dense_fog_detail, near)
        HazardKind.LOW_VISIBILITY -> stringResource(R.string.hz_low_visibility_detail, near)
        HazardKind.VIOLENT_GUSTS -> stringResource(R.string.hz_violent_gusts_detail, value, near)
        HazardKind.STRONG_GUSTS -> stringResource(R.string.hz_strong_gusts_detail, value, near)
        HazardKind.EXTREME_HEAT -> stringResource(R.string.hz_extreme_heat_detail, value, near)
        HazardKind.HIGH_HEAT -> stringResource(R.string.hz_high_heat_detail, value, near)
        HazardKind.SEVERE_COLD -> stringResource(R.string.hz_severe_cold_detail, value, near)
        HazardKind.FREEZING -> stringResource(R.string.hz_freezing_detail, value, near)
        HazardKind.POOR_AIR -> stringResource(R.string.hz_poor_air_detail, value, near)
        HazardKind.VERY_HIGH_UV -> stringResource(R.string.hz_uv_detail, value)
        HazardKind.NIGHT_RIDE -> stringResource(R.string.hz_night_detail)
    }
}
