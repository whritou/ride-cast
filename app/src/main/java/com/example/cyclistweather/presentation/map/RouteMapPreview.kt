package com.example.cyclistweather.presentation.map

import android.graphics.PointF
import android.view.MotionEvent
import com.example.cyclistweather.domain.model.MapWeatherFilter
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.cyclistweather.R
import com.example.cyclistweather.domain.model.ImportedRoute
import com.example.cyclistweather.domain.model.SegmentWeather
import com.example.cyclistweather.presentation.model.RideWeatherUiMapper
import com.example.cyclistweather.presentation.routeweather.mapFilterLabelRes
import com.example.cyclistweather.ui.theme.RideWeatherColors
import com.example.cyclistweather.ui.theme.ScoreColorUtils
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import org.maplibre.geojson.Polygon
import kotlin.math.roundToInt

@Composable
fun RouteMapPreview(
    route: ImportedRoute,
    segmentWeather: List<SegmentWeather> = emptyList(),
    selectedFilter: MapWeatherFilter = MapWeatherFilter.WIND,
    modifier: Modifier = Modifier,
    fullScreen: Boolean = false,
    showFullScreenControl: Boolean = true,
    onSelectFilter: ((MapWeatherFilter) -> Unit)? = null,
    onCloseFullScreen: (() -> Unit)? = null
) {
    var fullScreenMapVisible by remember { mutableStateOf(false) }
    var inspectedSegment by remember(route.id, segmentWeather) { mutableStateOf<SegmentWeather?>(null) }
    val latestSegmentWeather by rememberUpdatedState(segmentWeather)
    val routeCoordinates = remember(route.id, route.points) { RouteMapRenderPlanner.routeCoordinates(route) }
    val mapViewState = remember { mutableStateOf<MapView?>(null) }
    val mapState = remember { mutableStateOf<MapLibreMap?>(null) }
    val styleLoadedState = remember { mutableStateOf(false) }
    val lastRenderedKeyState = remember { mutableStateOf<String?>(null) }
    val cameraRenderTickState = remember { mutableStateOf(0) }
    val mapDataKey = remember(route.id, segmentWeather, selectedFilter) {
        buildString {
            append(route.id)
            segmentWeather.forEach { segment ->
                append('|')
                append(segment.sample.id)
                append(':')
                append(segment.weather.timeEpochMillis)
                append(':')
                append(segment.relativeWind.name)
                append(':')
                append(segment.score.total)
                append(':')
                append(segment.weather.temperatureCelsius)
                append(':')
                append(segment.weather.apparentTemperatureCelsius)
                append(':')
                append(segment.weather.windSpeedKmh)
                append(':')
                append(segment.weather.windDirectionDegrees)
                append(':')
                append(segment.weather.precipitationProbabilityPercent)
                append(':')
                append(segment.weather.precipitationMm)
                append(':')
                append(segment.weather.cloudCoverPercent)
            }
            append('|')
            append(selectedFilter.name)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mapViewState.value?.onPause()
            mapViewState.value?.onStop()
            mapViewState.value?.onDestroy()
        }
    }

    LaunchedEffect(mapDataKey, styleLoadedState.value, mapState.value) {
        val map = mapState.value
        if (map != null &&
            styleLoadedState.value &&
            lastRenderedKeyState.value != mapDataKey
        ) {
            drawRouteMap(map, route, routeCoordinates, segmentWeather, selectedFilter)
            lastRenderedKeyState.value = mapDataKey
            cameraRenderTickState.value += 1
        }
    }

    val mapSizeModifier = if (fullScreen) {
        Modifier.fillMaxSize()
    } else {
        Modifier
            .fillMaxWidth()
            .height(420.dp)
    }
    val mapShape = if (fullScreen) RoundedCornerShape(0.dp) else RoundedCornerShape(8.dp)

    Box(
        modifier = modifier
            .then(mapSizeModifier)
            .clip(mapShape)
    ) {
        AndroidView(
            modifier = Modifier
                .matchParentSize()
                .zIndex(0f),
            factory = { viewContext ->
                MapLibre.getInstance(viewContext)
                MapView(viewContext).also { view ->
                    mapViewState.value = view
                    view.onCreate(null)
                    view.onStart()
                    view.onResume()
                    view.setOnTouchListener { touchedView, event ->
                        when (event.actionMasked) {
                            MotionEvent.ACTION_DOWN,
                            MotionEvent.ACTION_MOVE,
                            MotionEvent.ACTION_POINTER_DOWN -> {
                                touchedView.parent?.requestDisallowInterceptTouchEvent(true)
                                val map = mapState.value
                                if (map != null && latestSegmentWeather.isNotEmpty()) {
                                    val latLng = map.projection.fromScreenLocation(PointF(event.x, event.y))
                                    inspectedSegment = RouteMapRenderPlanner.nearestInspectableSegment(
                                        coordinate = GeoCoordinate(
                                            latitude = latLng.latitude,
                                            longitude = latLng.longitude
                                        ),
                                        segmentWeather = latestSegmentWeather
                                    )
                                }
                            }
                            MotionEvent.ACTION_UP,
                            MotionEvent.ACTION_CANCEL,
                            MotionEvent.ACTION_POINTER_UP -> {
                                touchedView.parent?.requestDisallowInterceptTouchEvent(false)
                            }
                        }
                        false
                    }
                    view.addOnCameraIsChangingListener {
                        cameraRenderTickState.value += 1
                    }
                    view.addOnCameraDidChangeListener {
                        cameraRenderTickState.value += 1
                    }
                    view.getMapAsync { map ->
                        mapState.value = map
                        map.uiSettings.isCompassEnabled = true
                        map.uiSettings.isAttributionEnabled = true
                        map.uiSettings.isLogoEnabled = true
                        map.addOnCameraMoveListener {
                            cameraRenderTickState.value += 1
                        }
                        map.addOnCameraIdleListener {
                            cameraRenderTickState.value += 1
                        }
                        map.setStyle(Style.Builder().fromJson(OSM_RASTER_STYLE_JSON)) {
                            styleLoadedState.value = true
                            drawRouteMap(map, route, routeCoordinates, segmentWeather, selectedFilter)
                            lastRenderedKeyState.value = mapDataKey
                            cameraRenderTickState.value += 1
                        }
                    }
                }
            },
            update = {
                val map = mapState.value
                if (map != null &&
                    styleLoadedState.value &&
                    lastRenderedKeyState.value != mapDataKey
                ) {
                    drawRouteMap(map, route, routeCoordinates, segmentWeather, selectedFilter)
                    lastRenderedKeyState.value = mapDataKey
                    cameraRenderTickState.value += 1
                }
            }
        )

        RouteWeatherScreenOverlay(
            route = route,
            segmentWeather = segmentWeather,
            selectedFilter = selectedFilter,
            map = mapState.value,
            cameraRenderTick = cameraRenderTickState.value
        )

        MapFilterBadge(
            totalDistanceMeters = route.totalDistanceMeters,
            selectedFilter = selectedFilter,
            hasWeatherData = segmentWeather.size >= 2,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp)
                .zIndex(MAP_OVERLAY_Z_INDEX)
        )

        when {
            fullScreen -> MapControlButton(
                icon = MapControlIcons.Close,
                contentDescription = stringResource(R.string.map_close_fullscreen),
                onClick = { onCloseFullScreen?.invoke() },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .zIndex(MAP_OVERLAY_Z_INDEX)
            )
            showFullScreenControl -> MapControlButton(
                icon = MapControlIcons.Fullscreen,
                contentDescription = stringResource(R.string.map_open_fullscreen),
                onClick = { fullScreenMapVisible = true },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .zIndex(MAP_OVERLAY_Z_INDEX)
            )
        }

        if (fullScreen && onSelectFilter != null) {
            FullScreenMapFilterSelector(
                selectedFilter = selectedFilter,
                onSelectFilter = onSelectFilter,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 10.dp, end = 76.dp, bottom = 12.dp)
                    .zIndex(MAP_OVERLAY_Z_INDEX)
            )
        }

        MapNavigationControls(
            onZoomIn = {
                mapState.value?.easeCamera(CameraUpdateFactory.zoomIn(), MAP_CONTROL_ANIMATION_MS)
                cameraRenderTickState.value += 1
            },
            onZoomOut = {
                mapState.value?.easeCamera(CameraUpdateFactory.zoomOut(), MAP_CONTROL_ANIMATION_MS)
                cameraRenderTickState.value += 1
            },
            onCenterRoute = {
                val map = mapState.value ?: return@MapNavigationControls
                runCatching {
                    fitRoute(map, routeCoordinates.map { it.toLatLng() })
                }
                cameraRenderTickState.value += 1
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 10.dp, bottom = 12.dp)
                .zIndex(MAP_OVERLAY_Z_INDEX)
        )

        inspectedSegment?.let { segment ->
            RouteMapInspectionCard(
                segment = segment,
                onDismiss = { inspectedSegment = null },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 10.dp, end = 84.dp, bottom = 70.dp)
                    .zIndex(MAP_OVERLAY_Z_INDEX)
            )
        }
    }

    if (fullScreenMapVisible) {
        Dialog(
            onDismissRequest = { fullScreenMapVisible = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = RideWeatherColors.Stone,
                tonalElevation = 0.dp
            ) {
                RouteMapPreview(
                    route = route,
                    segmentWeather = segmentWeather,
                    selectedFilter = selectedFilter,
                    modifier = Modifier.fillMaxSize(),
                    fullScreen = true,
                    showFullScreenControl = false,
                    onSelectFilter = onSelectFilter,
                    onCloseFullScreen = { fullScreenMapVisible = false }
                )
            }
        }
    }
}

@Composable
private fun RouteMapInspectionCard(
    segment: SegmentWeather,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val model = RideWeatherUiMapper.weatherPoint(segment)
    val scoreColor = ScoreColorUtils.themedColorForScore(segment.score.total)
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = RideWeatherColors.Surface.copy(alpha = 0.96f),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(start = 12.dp, top = 10.dp, end = 8.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = model.distance,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = RideWeatherColors.TextPrimary
                )
                Text(
                    text = model.arrivalTime,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    color = RideWeatherColors.TextSecondary
                )
                Text(
                    text = segment.score.total.toString(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = scoreColor
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = MapControlIcons.Close,
                        contentDescription = stringResource(R.string.map_close_inspection),
                        tint = RideWeatherColors.TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Text(
                text = "${model.wind}  ·  ${model.rain}  ·  ${model.temperature}",
                style = MaterialTheme.typography.bodySmall,
                color = RideWeatherColors.TextPrimary
            )
            Text(
                text = "${stringResource(model.relativeWindRes)}  ·  ${stringResource(model.riskLevel.labelRes)}",
                style = MaterialTheme.typography.bodySmall,
                color = RideWeatherColors.TextSecondary
            )
        }
    }
}

@Composable
private fun FullScreenMapFilterSelector(
    selectedFilter: MapWeatherFilter,
    onSelectFilter: (MapWeatherFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = RideWeatherColors.Surface.copy(alpha = 0.94f),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        LazyRow(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(MapWeatherFilter.entries, key = { it.name }) { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { onSelectFilter(filter) },
                    label = { Text(stringResource(mapFilterLabelRes(filter)), maxLines = 1) },
                    shape = RoundedCornerShape(7.dp),
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
}

// Renders the route + weather coverage as MapLibre style sources/layers (the supported
// replacement for the deprecated annotation API). Sources/layers are created once and then
// updated in place, so this is safe to call repeatedly as the data/filter changes.
private fun drawRouteMap(
    map: MapLibreMap,
    route: ImportedRoute,
    routeCoordinates: List<GeoCoordinate>,
    segmentWeather: List<SegmentWeather>,
    selectedFilter: MapWeatherFilter
) {
    val style = map.style ?: return
    val latLngs = routeCoordinates.map { it.toLatLng() }
    if (latLngs.size < 2) {
        return
    }

    updateWeatherCoverage(style, route, segmentWeather, selectedFilter)
    updateNeutralRoute(style, routeCoordinates)
    runCatching {
        fitRoute(map, latLngs)
    }
}

private fun updateWeatherCoverage(
    style: Style,
    route: ImportedRoute,
    segmentWeather: List<SegmentWeather>,
    selectedFilter: MapWeatherFilter
) {
    val features = RouteMapRenderPlanner.weatherFieldOverlays(
        route = route,
        segmentWeather = segmentWeather,
        selectedFilter = selectedFilter
    ).filter { overlay ->
        overlay.fillColor.hasVisibleAlpha()
    }.map { overlay ->
        val ring = overlay.polygon.map { Point.fromLngLat(it.longitude, it.latitude) }
        val closedRing = if (ring.isNotEmpty() && ring.first() != ring.last()) ring + ring.first() else ring
        Feature.fromGeometry(Polygon.fromLngLats(listOf(closedRing))).apply {
            addStringProperty(WEATHER_COLOR_PROPERTY, overlay.fillColor.toRgbaString())
        }
    }
    upsertGeoJsonSource(style, WEATHER_SOURCE_ID, FeatureCollection.fromFeatures(features))
    if (style.getLayer(WEATHER_FILL_LAYER_ID) == null) {
        style.addLayer(
            FillLayer(WEATHER_FILL_LAYER_ID, WEATHER_SOURCE_ID).withProperties(
                PropertyFactory.fillColor(Expression.toColor(Expression.get(WEATHER_COLOR_PROPERTY))),
                PropertyFactory.fillOpacity(WEATHER_FIELD_ALPHA)
            )
        )
    }
}

private fun updateNeutralRoute(
    style: Style,
    routeCoordinates: List<GeoCoordinate>
) {
    val line = LineString.fromLngLats(routeCoordinates.map { Point.fromLngLat(it.longitude, it.latitude) })
    upsertGeoJsonSource(style, ROUTE_SOURCE_ID, FeatureCollection.fromFeature(Feature.fromGeometry(line)))

    if (style.getLayer(ROUTE_CASING_LAYER_ID) == null) {
        style.addLayer(
            LineLayer(ROUTE_CASING_LAYER_ID, ROUTE_SOURCE_ID).withProperties(
                PropertyFactory.lineColor(RouteMapRenderPlanner.neutralRouteCasingColor),
                PropertyFactory.lineWidth(10f),
                PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND)
            )
        )
    }
    if (style.getLayer(ROUTE_LINE_LAYER_ID) == null) {
        style.addLayerAbove(
            LineLayer(ROUTE_LINE_LAYER_ID, ROUTE_SOURCE_ID).withProperties(
                PropertyFactory.lineColor(RouteMapRenderPlanner.neutralRouteColor),
                PropertyFactory.lineWidth(5f),
                PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND)
            ),
            ROUTE_CASING_LAYER_ID
        )
    }
}

private fun upsertGeoJsonSource(style: Style, sourceId: String, data: FeatureCollection) {
    val existing = style.getSourceAs<GeoJsonSource>(sourceId)
    if (existing != null) {
        existing.setGeoJson(data)
    } else {
        style.addSource(GeoJsonSource(sourceId, data))
    }
}

private fun Int.toRgbaString(): String {
    val alpha = (this ushr 24) and 0xff
    val red = (this ushr 16) and 0xff
    val green = (this ushr 8) and 0xff
    val blue = this and 0xff
    return "rgba($red, $green, $blue, ${alpha / 255.0})"
}

@Composable
private fun RouteWeatherScreenOverlay(
    route: ImportedRoute,
    segmentWeather: List<SegmentWeather>,
    selectedFilter: MapWeatherFilter,
    map: MapLibreMap?,
    cameraRenderTick: Int
) {
    if (map == null || segmentWeather.isEmpty()) {
        return
    }

    val density = LocalDensity.current
    val windSymbolWidthPx = with(density) { WIND_SCREEN_SYMBOL_WIDTH.toPx() }
    val valueSymbolWidthPx = with(density) { VALUE_SCREEN_SYMBOL_WIDTH.toPx() }
    val symbolTopOffsetPx = with(density) { ROUTE_SCREEN_TOP_OFFSET.toPx() }
    val mapZoom = map.cameraPosition.zoom
    val annotations = remember(
        route.id,
        route.points,
        route.totalDistanceMeters,
        segmentWeather,
        selectedFilter,
        mapZoom
    ) {
        RouteMapRenderPlanner.routeWeatherAnnotations(
            route = route,
            segmentWeather = segmentWeather,
            selectedFilter = selectedFilter,
            mapZoom = mapZoom
        )
    }
    val symbols = remember(annotations, map, cameraRenderTick) {
        annotations.mapNotNull { annotation ->
            val point = map.projection.toScreenLocation(annotation.coordinate.toLatLng())
            if (point.x.isNaN() || point.y.isNaN()) {
                null
            } else {
                RouteScreenSymbol(
                    xPx = point.x,
                    yPx = point.y,
                    bearingDegrees = annotation.bearingDegrees?.toFloat(),
                    label = annotation.label,
                    color = annotation.color
                )
            }
        }
    }

    symbols.forEach { symbol ->
        RouteScreenSymbolView(
            symbol = symbol,
            symbolWidthPx = if (symbol.bearingDegrees != null) windSymbolWidthPx else valueSymbolWidthPx,
            symbolWidth = if (symbol.bearingDegrees != null) WIND_SCREEN_SYMBOL_WIDTH else VALUE_SCREEN_SYMBOL_WIDTH,
            symbolTopOffsetPx = symbolTopOffsetPx
        )
    }
}

@Composable
private fun RouteScreenSymbolView(
    symbol: RouteScreenSymbol,
    symbolWidthPx: Float,
    symbolWidth: Dp,
    symbolTopOffsetPx: Float
) {
    Column(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = (symbol.xPx - symbolWidthPx / 2f).roundToInt(),
                    y = (symbol.yPx - symbolTopOffsetPx).roundToInt()
                )
            }
            .width(symbolWidth),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (symbol.bearingDegrees != null) {
            Icon(
                imageVector = MapControlIcons.Navigation,
                contentDescription = null,
                tint = RideWeatherColors.Accent,
                modifier = Modifier
                    .size(WIND_SCREEN_ICON_SIZE)
                    .graphicsLayer(rotationZ = symbol.bearingDegrees)
            )
        }
        RouteValueChip(symbol)
    }
}

@Composable
private fun RouteValueChip(symbol: RouteScreenSymbol) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = RideWeatherColors.Surface.copy(alpha = 0.94f),
        contentColor = RideWeatherColors.TextPrimary,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Canvas(modifier = Modifier.size(6.dp)) {
                drawCircle(color = ComposeColor(symbol.color))
            }
            Text(
                text = symbol.label,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1
            )
        }
    }
}


private fun fitRoute(
    map: MapLibreMap,
    latLngs: List<LatLng>
) {
    val boundsBuilder = LatLngBounds.Builder()
    latLngs.forEach(boundsBuilder::include)
    val bounds = boundsBuilder.build()
    val camera = CameraUpdateFactory.newLatLngBounds(bounds, MAP_CAMERA_PADDING)
    map.moveCamera(camera)
}

private fun GeoCoordinate.toLatLng(): LatLng {
    return LatLng(latitude, longitude)
}

private fun Int.hasVisibleAlpha(): Boolean {
    return ((this ushr 24) and 0xff) > 0
}

private data class RouteScreenSymbol(
    val xPx: Float,
    val yPx: Float,
    val bearingDegrees: Float?,
    val label: String,
    val color: Int
)

private const val MAP_CAMERA_PADDING = 72
private const val MAP_OVERLAY_Z_INDEX = 1f
private const val WEATHER_FIELD_ALPHA = 0.34f
private const val ROUTE_SOURCE_ID = "ride-route-source"
private const val ROUTE_CASING_LAYER_ID = "ride-route-casing"
private const val ROUTE_LINE_LAYER_ID = "ride-route-line"
private const val WEATHER_SOURCE_ID = "ride-weather-source"
private const val WEATHER_FILL_LAYER_ID = "ride-weather-fill"
private const val WEATHER_COLOR_PROPERTY = "color"
private val WIND_SCREEN_ICON_SIZE = 26.dp
private val WIND_SCREEN_SYMBOL_WIDTH = 58.dp
private val VALUE_SCREEN_SYMBOL_WIDTH = 72.dp
private val ROUTE_SCREEN_TOP_OFFSET = 13.dp
private val OSM_RASTER_STYLE_JSON = """
{
  "version": 8,
  "name": "OpenStreetMap Raster",
  "sources": {
    "osm": {
      "type": "raster",
      "tiles": ["https://tile.openstreetmap.org/{z}/{x}/{y}.png"],
      "tileSize": 256,
      "minzoom": 0,
      "maxzoom": 19,
      "attribution": "(c) OpenStreetMap contributors"
    }
  },
  "layers": [
    {
      "id": "osm-raster",
      "type": "raster",
      "source": "osm"
    }
  ]
}
""".trimIndent()
