package com.example.cyclistweather.presentation.routeweather

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.cyclistweather.R
import com.example.cyclistweather.domain.model.MapWeatherFilter
import com.example.cyclistweather.ui.theme.PrimaryRouteButton
import com.example.cyclistweather.ui.theme.Radius
import com.example.cyclistweather.ui.theme.RideScoreBadge
import com.example.cyclistweather.ui.theme.RideWeatherColors
import com.example.cyclistweather.ui.theme.SecondaryRouteButton
import com.example.cyclistweather.ui.theme.Spacing

/** Localized label for a map weather filter; shared by the selector, map chips, and the legend. */
@StringRes
internal fun mapFilterLabelRes(filter: MapWeatherFilter): Int = when (filter) {
    MapWeatherFilter.WIND -> R.string.filter_wind
    MapWeatherFilter.TEMPERATURE -> R.string.filter_temp
    MapWeatherFilter.FEELS_LIKE -> R.string.filter_feels
    MapWeatherFilter.RAIN -> R.string.filter_rain
    MapWeatherFilter.PRECIPITATION -> R.string.filter_precip
    MapWeatherFilter.CLOUD_COVER -> R.string.filter_clouds
    MapWeatherFilter.RIDE_SCORE -> R.string.filter_score
}

@Composable
internal fun RouteFactStrip(
    distance: String,
    duration: String,
    elevation: String,
    rideScore: Int?
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Radius.lg),
        color = RideWeatherColors.Sage,
        border = BorderStroke(1.dp, RideWeatherColors.Border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg, vertical = Spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RouteFact(
                icon = Icons.Filled.SwapHoriz,
                value = distance,
                label = stringResource(R.string.fact_distance),
                modifier = Modifier.weight(1f)
            )
            StatDivider()
            RouteFact(
                icon = Icons.Filled.NorthEast,
                value = elevation,
                label = stringResource(R.string.fact_elevation_gain),
                modifier = Modifier.weight(1f)
            )
            StatDivider()
            RouteFact(
                icon = Icons.Filled.Schedule,
                value = duration,
                label = stringResource(R.string.fact_est_time),
                modifier = Modifier.weight(1f)
            )
            rideScore?.let { score ->
                StatDivider()
                RideScoreBadge(score = score, caption = stringResource(R.string.ride_outlook))
            }
        }
    }
}

@Composable
private fun RouteFact(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = RideWeatherColors.Accent,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = RideWeatherColors.TextPrimary
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = RideWeatherColors.TextSecondary
        )
    }
}

@Composable
internal fun MapFilterSelector(
    selectedFilter: MapWeatherFilter,
    onSelectFilter: (MapWeatherFilter) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        items(MapWeatherFilter.entries, key = { it.name }) { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onSelectFilter(filter) },
                label = { Text(stringResource(mapFilterLabelRes(filter))) },
                shape = RoundedCornerShape(Radius.md),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = RideWeatherColors.Surface,
                    labelColor = RideWeatherColors.TextSecondary,
                    selectedContainerColor = RideWeatherColors.Accent,
                    selectedLabelColor = RideWeatherColors.OnAccent
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selectedFilter == filter,
                    borderColor = RideWeatherColors.Border,
                    selectedBorderColor = RideWeatherColors.Accent
                )
            )
        }
    }
}

@Composable
internal fun RouteOverviewActions(
    onPlanRide: () -> Unit,
    onExportRoute: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        PrimaryRouteButton(
            text = stringResource(R.string.action_plan_departure),
            onClick = onPlanRide,
            modifier = Modifier.weight(1f),
            leadingIcon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.DirectionsBike,
                    contentDescription = null,
                    modifier = Modifier.padding(end = Spacing.sm).size(18.dp)
                )
            }
        )
        SecondaryRouteButton(
            text = stringResource(R.string.action_export_gpx),
            onClick = onExportRoute,
            modifier = Modifier.weight(1f),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.FileDownload,
                    contentDescription = null,
                    modifier = Modifier.padding(end = Spacing.sm).size(18.dp)
                )
            }
        )
    }
}
