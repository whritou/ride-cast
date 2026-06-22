package com.example.cyclistweather.presentation.route

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.annotation.StringRes
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.cyclistweather.R
import com.example.cyclistweather.domain.model.ImportedRoute
import com.example.cyclistweather.domain.model.WeatherDataFreshness
import com.example.cyclistweather.data.settings.RouteWeatherSummarySnapshot
import com.example.cyclistweather.presentation.app.CyclistWeatherUiState
import com.example.cyclistweather.domain.model.MapWeatherFilter
import com.example.cyclistweather.presentation.model.RideWeatherUiMapper
import com.example.cyclistweather.presentation.routeweather.RouteWeatherActions
import com.example.cyclistweather.presentation.routeweather.RouteWeatherScreen
import com.example.cyclistweather.ui.theme.PrimaryRouteButton
import com.example.cyclistweather.ui.theme.Radius
import com.example.cyclistweather.ui.theme.RideScoreBadge
import com.example.cyclistweather.ui.theme.RideWeatherColors
import com.example.cyclistweather.ui.theme.RouteFirstPanel
import com.example.cyclistweather.ui.theme.SecondaryRouteButton
import com.example.cyclistweather.ui.theme.Sizes
import com.example.cyclistweather.ui.theme.Spacing

@Composable
fun RouteScreen(
    state: CyclistWeatherUiState,
    modifier: Modifier = Modifier,
    onImportRoute: () -> Unit,
    onLoadDemoRoute: () -> Unit,
    onOpenRoute: (String) -> Unit,
    onDismissError: () -> Unit,
    routeWeatherActions: RouteWeatherActions,
    onOpenSettings: () -> Unit
) {
    Crossfade(targetState = state.selectedRoute, label = "route-detail") { selectedRoute ->
        if (selectedRoute != null) {
            RouteWeatherScreen(
                route = selectedRoute,
                averageSpeedKmh = state.averageSpeedKmh,
                selectedDepartureEpochMillis = state.selectedDepartureEpochMillis,
                routeWeatherState = state.routeWeatherState,
                departureOptimizationState = state.departureOptimizationState,
                selectedMapFilter = state.selectedMapFilter,
                departureNotificationsEnabled = state.departureNotificationsEnabled,
                actions = routeWeatherActions,
                modifier = modifier
            )
        } else {
            RouteLibraryScreen(
                state = state,
                modifier = modifier,
                onImportRoute = onImportRoute,
                onLoadDemoRoute = onLoadDemoRoute,
                onOpenRoute = onOpenRoute,
                onDismissError = onDismissError,
                onOpenSettings = onOpenSettings
            )
        }
    }
}

@Composable
private fun RouteLibraryScreen(
    state: CyclistWeatherUiState,
    modifier: Modifier,
    onImportRoute: () -> Unit,
    onLoadDemoRoute: () -> Unit,
    onOpenRoute: (String) -> Unit,
    onDismissError: () -> Unit,
    onOpenSettings: () -> Unit
) {
    var bannerVisible by rememberSaveable { mutableStateOf(true) }
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(RideWeatherColors.Stone),
        contentPadding = PaddingValues(horizontal = Spacing.xl, vertical = Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg)
    ) {
        item { RouteLibraryHeader(onOpenSettings = onOpenSettings) }

        item {
            ImportActions(
                isImporting = state.isImporting,
                onImportRoute = onImportRoute,
                onLoadDemoRoute = onLoadDemoRoute
            )
        }

        item {
            RecentRoutesHeader(routeCount = state.routes.size)
        }

        if (state.routes.isEmpty()) {
            item { EmptyRoutePanel() }
        } else {
            items(state.routes, key = { it.id }) { route ->
                RouteLibraryRow(
                    route = route,
                    averageSpeedKmh = state.averageSpeedKmh,
                    summary = state.routeWeatherSummaries[route.id],
                    onOpen = { onOpenRoute(route.id) }
                )
            }
        }

        if (bannerVisible) {
            item { PlanningBanner(onDismiss = { bannerVisible = false }) }
        }
    }
}

@Composable
private fun RouteLibraryHeader(onOpenSettings: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Terrain,
            contentDescription = null,
            tint = RideWeatherColors.Accent,
            modifier = Modifier.size(30.dp)
        )
        Text(
            text = stringResource(R.string.app_name),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = RideWeatherColors.TextPrimary
        )
        IconButton(onClick = onOpenSettings) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = stringResource(R.string.settings_title),
                tint = RideWeatherColors.Accent
            )
        }
    }
}

@Composable
internal fun ImportActions(
    isImporting: Boolean,
    onImportRoute: () -> Unit,
    onLoadDemoRoute: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        PrimaryRouteButton(
            text = stringResource(if (isImporting) R.string.action_importing else R.string.action_import_gpx),
            onClick = onImportRoute,
            enabled = !isImporting,
            modifier = Modifier.weight(1f),
            leadingIcon = {
                if (isImporting) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(end = Spacing.sm)
                            .size(Sizes.iconSm),
                        strokeWidth = 2.dp,
                        color = RideWeatherColors.OnAccent
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(end = Spacing.sm)
                            .size(Sizes.iconSm)
                    )
                }
            }
        )
        SecondaryRouteButton(
            text = stringResource(R.string.action_demo_route),
            onClick = onLoadDemoRoute,
            enabled = !isImporting,
            modifier = Modifier.weight(1f),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Explore,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(end = Spacing.sm)
                        .size(Sizes.iconSm)
                )
            }
        )
    }
}

@Composable
private fun RecentRoutesHeader(routeCount: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(if (routeCount > 0) R.string.routes_your else R.string.routes_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = RideWeatherColors.TextPrimary
        )
        if (routeCount > 0) {
            Text(
                text = pluralStringResource(R.plurals.route_count, routeCount, routeCount),
                style = MaterialTheme.typography.labelLarge,
                color = RideWeatherColors.TextSecondary
            )
        }
    }
}

@Composable
private fun RouteLibraryRow(
    route: ImportedRoute,
    averageSpeedKmh: Double,
    summary: RouteWeatherSummarySnapshot?,
    onOpen: () -> Unit
) {
    val model = RideWeatherUiMapper.route(route, averageSpeedKmh)
    val summaryModel = summary?.let {
        RideWeatherUiMapper.routeWeatherSummary(
            routeId = it.routeId,
            score = it.score,
            checkedAtEpochMillis = it.checkedAtEpochMillis
        )
    }
    val outlook = summaryModel?.score ?: remember(route.id) { routeOutlookScore(route) }
    RouteFirstPanel(
        modifier = Modifier
            .clickable(onClickLabel = "Open route", role = Role.Button, onClick = onOpen)
            .semantics(mergeDescendants = true) {}
    ) {
        Row(
            modifier = Modifier.padding(Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RouteThumbnail(
                route = route,
                modifier = Modifier
                    .width(104.dp)
                    .height(80.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                Text(
                    text = model.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = RideWeatherColors.TextPrimary,
                    maxLines = 1
                )
                Text(
                    text = "${compactKm(route.totalDistanceMeters)}  ·  ${elevationLabel(route)}  ·  ${model.estimatedDuration}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = RideWeatherColors.TextSecondary
                )
                if (summaryModel != null) {
                    // Quality is shown by the badge color + label text; keep this line in a
                    // readable secondary tone rather than a low-contrast score hue.
                    Text(
                        text = stringResource(
                            R.string.route_summary_line,
                            stringResource(summaryModel.riskLevel.labelRes),
                            summaryModel.checkedAt,
                            stringResource(freshnessLabelRes(summary.freshness()))
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = RideWeatherColors.TextSecondary
                    )
                } else {
                    Text(
                        text = stringResource(R.string.route_tap_to_check),
                        style = MaterialTheme.typography.bodySmall,
                        color = RideWeatherColors.TextSecondary
                    )
                }
            }
            RideScoreBadge(score = outlook)
        }
    }
}

@Composable
private fun EmptyRoutePanel() {
    RouteFirstPanel(containerColor = RideWeatherColors.Sage.copy(alpha = 0.62f)) {
        Column(
            modifier = Modifier.padding(Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Icon(
                imageVector = Icons.Filled.Route,
                contentDescription = null,
                tint = RideWeatherColors.Accent,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = stringResource(R.string.empty_routes_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = RideWeatherColors.TextPrimary
            )
            Text(
                text = stringResource(R.string.empty_routes_message),
                style = MaterialTheme.typography.bodyMedium,
                color = RideWeatherColors.TextSecondary
            )
        }
    }
}

@Composable
private fun PlanningBanner(onDismiss: () -> Unit) {
    RouteFirstPanel(containerColor = RideWeatherColors.Sage.copy(alpha = 0.7f)) {
        Row(
            modifier = Modifier.padding(Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                shape = RoundedCornerShape(Radius.md),
                color = RideWeatherColors.Surface
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.DirectionsBike,
                    contentDescription = null,
                    tint = RideWeatherColors.Accent,
                    modifier = Modifier.padding(Spacing.sm).size(22.dp)
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.xxs)
            ) {
                Text(
                    text = stringResource(R.string.banner_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = RideWeatherColors.TextPrimary
                )
                Text(
                    text = stringResource(R.string.banner_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = RideWeatherColors.TextSecondary
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.banner_dismiss),
                    tint = RideWeatherColors.TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@StringRes
private fun freshnessLabelRes(freshness: WeatherDataFreshness): Int = when (freshness) {
    WeatherDataFreshness.LIVE -> R.string.freshness_live
    WeatherDataFreshness.FRESH_CACHE -> R.string.freshness_cached
    WeatherDataFreshness.STALE_CACHE -> R.string.freshness_offline
}

@Composable
internal fun ErrorNotice(
    message: String,
    onDismiss: () -> Unit
) {
    RouteFirstPanel(containerColor = MaterialTheme.colorScheme.errorContainer) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            SecondaryRouteButton(
                text = stringResource(R.string.action_dismiss),
                onClick = onDismiss
            )
        }
    }
}
