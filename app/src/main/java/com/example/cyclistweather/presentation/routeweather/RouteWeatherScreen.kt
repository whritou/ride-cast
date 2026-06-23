package com.example.cyclistweather.presentation.routeweather

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.cyclistweather.R
import com.example.cyclistweather.core.reminder.DepartureReminder
import com.example.cyclistweather.domain.model.ImportedRoute
import com.example.cyclistweather.domain.model.MapWeatherFilter
import com.example.cyclistweather.domain.model.RouteWeatherSnapshot
import com.example.cyclistweather.presentation.map.RouteMapPreview
import com.example.cyclistweather.presentation.model.RideWeatherUiMapper
import com.example.cyclistweather.ui.theme.PrimaryRouteButton
import com.example.cyclistweather.ui.theme.RideWeatherColors
import com.example.cyclistweather.ui.theme.Spacing
import com.example.cyclistweather.ui.theme.WeatherSummarySkeleton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteWeatherScreen(
    route: ImportedRoute,
    averageSpeedKmh: Double,
    selectedDepartureEpochMillis: Long,
    routeWeatherState: RouteWeatherLoadState,
    departureOptimizationState: DepartureOptimizationLoadState,
    selectedMapFilter: MapWeatherFilter,
    departureNotificationsEnabled: Boolean,
    actions: RouteWeatherActions,
    modifier: Modifier = Modifier
) {
    val readySnapshot = (routeWeatherState as? RouteWeatherLoadState.Ready)?.snapshot
    val routeModel = RideWeatherUiMapper.route(route, averageSpeedKmh)
    var mode by remember(route.id) { mutableStateOf(RouteDetailMode.OVERVIEW) }

    // Remember the last good snapshot so a pull-to-refresh keeps the existing map/elevation/
    // conditions on screen (stale-while-revalidate) instead of flashing back to empty placeholders.
    // A skeleton is only shown on the very first load, when there is no snapshot yet.
    var lastSnapshot by remember(route.id) { mutableStateOf<RouteWeatherSnapshot?>(null) }
    LaunchedEffect(readySnapshot) {
        if (readySnapshot != null) lastSnapshot = readySnapshot
    }
    val effectiveSnapshot = readySnapshot ?: lastSnapshot

    // Compute the optimal-departure analysis when entering the planner, but DON'T auto-apply it
    // (autoApply = false) so the rider's own date/time selection is never overwritten.
    LaunchedEffect(mode, routeWeatherState, departureOptimizationState) {
        if (mode == RouteDetailMode.DEPARTURE &&
            routeWeatherState is RouteWeatherLoadState.Ready &&
            departureOptimizationState is DepartureOptimizationLoadState.Idle
        ) {
            actions.optimizeDeparture(false)
        }
    }

    val onBack: () -> Unit = {
        if (mode == RouteDetailMode.OVERVIEW) actions.backToRoutes() else mode = RouteDetailMode.OVERVIEW
    }
    BackHandler(onBack = onBack)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(RideWeatherColors.Stone)
    ) {
        TopAppBar(
            title = {
                Text(
                    text = if (mode == RouteDetailMode.OVERVIEW) route.name else stringResource(R.string.detail_title),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(
                            if (mode == RouteDetailMode.OVERVIEW) R.string.back_to_routes else R.string.back_to_overview
                        ),
                        modifier = Modifier.size(22.dp)
                    )
                }
            },
            actions = {
                IconButton(onClick = actions::refreshWeather) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = stringResource(R.string.refresh_weather),
                        modifier = Modifier.size(22.dp)
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = RideWeatherColors.Stone,
                titleContentColor = RideWeatherColors.TextPrimary,
                navigationIconContentColor = RideWeatherColors.Accent,
                actionIconContentColor = RideWeatherColors.Accent
            ),
            windowInsets = WindowInsets(0, 0, 0, 0)
        )

        PullToRefreshBox(
            isRefreshing = routeWeatherState is RouteWeatherLoadState.Loading,
            onRefresh = actions::refreshWeather,
            modifier = Modifier.weight(1f)
        ) {
            AnimatedContent(
                targetState = mode,
                label = "detail-mode"
            ) { currentMode ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    when (currentMode) {
                    RouteDetailMode.OVERVIEW -> {
                    effectiveSnapshot?.rideScore?.hazards?.takeIf { it.isNotEmpty() }?.let { hazards ->
                        item { RideHazardBanners(hazards = hazards) }
                    }
                    item {
                        RouteFactStrip(
                            distance = routeModel.distance,
                            duration = routeModel.estimatedDuration,
                            elevation = routeModel.elevationGain,
                            rideScore = effectiveSnapshot?.rideScore?.total
                        )
                    }
                    item { MapFilterSelector(selectedFilter = selectedMapFilter, onSelectFilter = actions::selectMapFilter) }
                    item {
                        RouteMapPreview(
                            route = route,
                            segmentWeather = effectiveSnapshot?.segmentWeather.orEmpty(),
                            selectedFilter = selectedMapFilter,
                            onSelectFilter = actions::selectMapFilter
                        )
                    }
                    item {
                        when (routeWeatherState) {
                            is RouteWeatherLoadState.Ready -> WeatherSummaryRow(snapshot = routeWeatherState.snapshot)
                            RouteWeatherLoadState.Loading ->
                                // Stale-while-revalidate: keep the last summary during a refresh,
                                // only show the skeleton when nothing has loaded yet.
                                lastSnapshot?.let { WeatherSummaryRow(snapshot = it) } ?: WeatherSummarySkeleton()
                            is RouteWeatherLoadState.Error -> ErrorCard(
                                title = stringResource(R.string.weather_unavailable_title),
                                message = stringResource(routeWeatherState.messageRes),
                                onRetry = actions::refreshWeather
                            )
                            RouteWeatherLoadState.Idle -> Unit
                        }
                    }
                    item {
                        ElevationProfilePanel(
                            route = route,
                            segmentWeather = effectiveSnapshot?.segmentWeather.orEmpty()
                        )
                    }
                    effectiveSnapshot?.let { snapshot ->
                        item { ConditionsGrid(snapshot = snapshot) }
                    }
                }
                RouteDetailMode.DEPARTURE -> {
                    item {
                        BestDepartureContent(
                            routeWeatherState = routeWeatherState,
                            departureOptimizationState = departureOptimizationState,
                            selectedDepartureEpochMillis = selectedDepartureEpochMillis,
                            onRetry = actions::refreshWeather,
                            onSetDepartureDate = actions::setDepartureDate,
                            onSetDepartureTime = actions::setDepartureTime,
                            onUseBestDeparture = actions::applyOptimizedDeparture,
                            onReoptimize = { actions.optimizeDeparture(false) }
                        )
                    }
                }
                }
                }
            }
        }

        RouteDetailBottomBar(
            mode = mode,
            route = route,
            averageSpeedKmh = averageSpeedKmh,
            selectedDepartureEpochMillis = selectedDepartureEpochMillis,
            notificationsEnabled = departureNotificationsEnabled,
            onPlanRide = { mode = RouteDetailMode.DEPARTURE },
            onExportRoute = { actions.exportRoute(route) }
        )
    }
}

@Composable
private fun RouteDetailBottomBar(
    mode: RouteDetailMode,
    route: ImportedRoute,
    averageSpeedKmh: Double,
    selectedDepartureEpochMillis: Long,
    notificationsEnabled: Boolean,
    onPlanRide: () -> Unit,
    onExportRoute: () -> Unit
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(RideWeatherColors.Stone)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        when (mode) {
            RouteDetailMode.OVERVIEW -> RouteOverviewActions(
                onPlanRide = onPlanRide,
                onExportRoute = onExportRoute
            )
            RouteDetailMode.DEPARTURE -> PrimaryRouteButton(
                text = stringResource(R.string.add_departure_reminder),
                onClick = {
                    DepartureReminder.addReminder(
                        context = context,
                        routeName = route.name,
                        departureEpochMillis = selectedDepartureEpochMillis,
                        averageSpeedKmh = averageSpeedKmh,
                        distanceMeters = route.totalDistanceMeters,
                        scheduleNotification = notificationsEnabled
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Notifications,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(18.dp)
                    )
                }
            )
        }
    }
}

internal enum class RouteDetailMode { OVERVIEW, DEPARTURE }
