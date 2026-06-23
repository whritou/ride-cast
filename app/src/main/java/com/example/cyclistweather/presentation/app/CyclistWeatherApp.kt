package com.example.cyclistweather.presentation.app

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.cyclistweather.R
import com.example.cyclistweather.data.gpx.GpxExporter
import com.example.cyclistweather.domain.model.ImportedRoute
import com.example.cyclistweather.presentation.route.RouteScreen
import com.example.cyclistweather.presentation.routeweather.RouteWeatherActions
import com.example.cyclistweather.presentation.settings.SettingsScreen
import com.example.cyclistweather.ui.theme.RideWeatherColors
import kotlinx.coroutines.launch

@Composable
fun CyclistWeatherApp(viewModel: CyclistWeatherViewModel) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var pendingExportRoute by remember { mutableStateOf<ImportedRoute?>(null) }

    // Surface transient errors (e.g. failed import) as a snackbar, then clear them.
    LaunchedEffect(state.errorMessageRes) {
        val resId = state.errorMessageRes ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(context.getString(resId))
        viewModel.clearError()
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let(viewModel::importGpx)
    }
    val launchImport = {
        importLauncher.launch(
            arrayOf(
                "application/gpx+xml",
                "application/xml",
                "text/xml",
                "text/plain",
                "*/*"
            )
        )
    }
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/gpx+xml")
    ) { uri ->
        val route = pendingExportRoute
        pendingExportRoute = null
        if (uri == null || route == null) {
            return@rememberLauncherForActivityResult
        }

        val result = runCatching {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(GpxExporter.toGpx(route).encodeToByteArray())
            } ?: error(context.getString(R.string.export_create_failed))
        }
        scope.launch {
            snackbarHostState.showSnackbar(
                result.fold(
                    onSuccess = { context.getString(R.string.exported_route, route.name) },
                    onFailure = { it.message ?: context.getString(R.string.export_failed) }
                )
            )
        }
    }
    val launchExport = { route: ImportedRoute ->
        pendingExportRoute = route
        exportLauncher.launch(route.exportFileName())
    }

    // Single stable handle over the ViewModel + export launcher, replacing the long callback list
    // that used to be threaded down to the route-weather screen.
    val routeWeatherActions = remember(viewModel) {
        object : RouteWeatherActions {
            override fun selectMapFilter(filter: com.example.cyclistweather.domain.model.MapWeatherFilter) =
                viewModel.selectMapFilter(filter)
            override fun refreshWeather() = viewModel.refreshRouteWeather()
            override fun optimizeDeparture(autoApply: Boolean) = viewModel.optimizeDepartureTime(autoApply)
            override fun applyOptimizedDeparture(departureEpochMillis: Long) =
                viewModel.applyOptimizedDeparture(departureEpochMillis)
            override fun setDepartureDate(selectedDateUtcMillis: Long) =
                viewModel.setDepartureDate(selectedDateUtcMillis)
            override fun setDepartureTime(hourOfDay: Int, minute: Int) =
                viewModel.setDepartureTime(hourOfDay, minute)
            override fun setAverageSpeed(speedKmh: Double) = viewModel.setAverageSpeed(speedKmh)
            override fun backToRoutes() = viewModel.closeRoute()
            override fun exportRoute(route: ImportedRoute) {
                launchExport(route)
            }
        }
    }

    BackHandler(enabled = showSettings) { showSettings = false }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = RideWeatherColors.Stone
    ) { innerPadding ->
        Crossfade(targetState = showSettings, label = "settings") { settings ->
            if (settings) {
                SettingsScreen(
                    averageSpeedKmh = state.averageSpeedKmh,
                    themeMode = state.themeMode,
                    language = state.language,
                    notificationsEnabled = state.departureNotificationsEnabled,
                    onSetAverageSpeed = viewModel::setAverageSpeed,
                    onSetThemeMode = viewModel::setThemeMode,
                    onSetLanguage = viewModel::setLanguage,
                    onSetNotificationsEnabled = viewModel::setDepartureNotificationsEnabled,
                    onBack = { showSettings = false },
                    modifier = Modifier.padding(innerPadding)
                )
            } else {
                RouteScreen(
                    state = state,
                    modifier = Modifier.padding(innerPadding),
                    onImportRoute = launchImport,
                    onLoadDemoRoute = viewModel::loadDemoRoute,
                    onOpenRoute = viewModel::openRoute,
                    onDismissError = viewModel::clearError,
                    routeWeatherActions = routeWeatherActions,
                    onOpenSettings = { showSettings = true }
                )
            }
        }
    }

    // First launch only: ask for average speed once, then it lives in Settings.
    if (!state.averageSpeedConfigured) {
        FirstRunSpeedDialog(
            initialSpeedKmh = state.averageSpeedKmh,
            onSave = viewModel::setAverageSpeed
        )
    }
}

private fun ImportedRoute.exportFileName(): String {
    val cleanName = name
        .replace(Regex("""[\\/:*?"<>|]"""), "-")
        .trim()
        .ifBlank { "route" }
    return if (cleanName.endsWith(".gpx", ignoreCase = true)) cleanName else "$cleanName.gpx"
}
