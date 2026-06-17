package com.example.cyclistweather.presentation.map

import com.example.cyclistweather.presentation.map.legendLabelStartOffsetPx
import com.example.cyclistweather.domain.model.ImportedRoute
import com.example.cyclistweather.domain.model.MapWeatherFilter
import com.example.cyclistweather.domain.model.RelativeWind
import com.example.cyclistweather.domain.model.RoutePoint
import com.example.cyclistweather.domain.model.RouteWeatherSample
import com.example.cyclistweather.domain.model.SegmentScore
import com.example.cyclistweather.domain.model.SegmentWeather
import com.example.cyclistweather.domain.model.WeatherPoint
import com.example.cyclistweather.domain.model.WindComponents
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteMapRenderPlannerTest {
    @Test
    fun `weather field overlays cover an expanded map area instead of sample circles`() {
        val route = testRoute()
        val samples = testSegmentWeather()

        val overlays = RouteMapRenderPlanner.weatherFieldOverlays(
            route = route,
            segmentWeather = samples,
            selectedFilter = MapWeatherFilter.RAIN
        )

        val routeLatitudes = route.points.map { it.latitude }
        val routeLongitudes = route.points.map { it.longitude }
        val overlayPoints = overlays.flatMap { it.polygon }

        assertTrue("Field should use a coverage mesh, not one patch per sample", overlays.size > samples.size)
        assertTrue("Each field cell should be a polygon tile", overlays.all { it.polygon.size == 4 })
        assertTrue("Field should extend north/south beyond the route", overlayPoints.minOf { it.latitude } < routeLatitudes.min())
        assertTrue("Field should extend north/south beyond the route", overlayPoints.maxOf { it.latitude } > routeLatitudes.max())
        assertTrue("Field should extend east/west beyond the route", overlayPoints.minOf { it.longitude } < routeLongitudes.min())
        assertTrue("Field should extend east/west beyond the route", overlayPoints.maxOf { it.longitude } > routeLongitudes.max())
        assertTrue(
            "Field should have enough padding to cover the visible map around the route",
            (routeLatitudes.min() - overlayPoints.minOf { it.latitude }) * 111_320.0 >= 6_000.0
        )
        assertTrue(overlays.all { it.fillColor != RouteMapRenderPlanner.neutralRouteColor })
        assertTrue("Field tiles should not draw visible grid borders", overlays.all { it.strokeColor == 0 })
    }

    @Test
    fun `wind filter does not draw colored coverage overlays`() {
        val overlays = RouteMapRenderPlanner.weatherFieldOverlays(
            route = testRoute(),
            segmentWeather = testSegmentWeather(),
            selectedFilter = MapWeatherFilter.WIND
        )

        assertTrue("Wind mode should use arrows only, not overlay colors", overlays.isEmpty())
    }

    @Test
    fun `route coordinates preserve every parsed gpx point in order`() {
        val route = testRoute().copy(
            points = listOf(
                RoutePoint(37.000, 127.000, null, 0.0),
                RoutePoint(37.010, 127.004, null, 1_000.0),
                RoutePoint(36.996, 127.015, null, 2_000.0),
                RoutePoint(37.020, 127.040, null, 3_000.0)
            )
        )

        val coordinates = RouteMapRenderPlanner.routeCoordinates(route)

        assertEquals(route.points.size, coordinates.size)
        route.points.zip(coordinates).forEach { (point, coordinate) ->
            assertEquals(point.latitude, coordinate.latitude, 0.0)
            assertEquals(point.longitude, coordinate.longitude, 0.0)
        }
    }

    @Test
    fun `wind annotations are placed on weather samples`() {
        val route = testRoute()
        val samples = testSegmentWeather()

        val annotations = RouteMapRenderPlanner.routeWeatherAnnotations(
            route = route,
            segmentWeather = samples,
            selectedFilter = MapWeatherFilter.WIND
        )

        assertEquals(listOf(5_000.0), annotations.map { it.distanceFromStartMeters })
        assertEquals(127.05, annotations[0].coordinate.longitude, 0.001)
        assertTrue("Wind annotations should label speed in km/h", annotations.all { it.label.endsWith(" km/h") })
        assertTrue("Wind annotations should expose wind bearing", annotations.all { it.bearingDegrees != null })
    }

    @Test
    fun `nearest inspectable segment is chosen from tap coordinate`() {
        val samples = testSegmentWeather()

        val inspected = RouteMapRenderPlanner.nearestInspectableSegment(
            coordinate = GeoCoordinate(latitude = 37.0, longitude = 127.052),
            segmentWeather = samples
        )

        assertEquals("sample-1", inspected?.sample?.id)
    }

    @Test
    fun `nearest inspectable segment is absent without weather samples`() {
        val inspected = RouteMapRenderPlanner.nearestInspectableSegment(
            coordinate = GeoCoordinate(latitude = 37.0, longitude = 127.052),
            segmentWeather = emptyList()
        )

        assertEquals(null, inspected)
    }

    @Test
    fun `non wind annotations expose useful values on weather samples`() {
        val route = testRoute()
        val samples = testSegmentWeather()

        val temperatureLabels = RouteMapRenderPlanner.routeWeatherAnnotations(
            route = route,
            segmentWeather = samples,
            selectedFilter = MapWeatherFilter.TEMPERATURE
        ).map { it.label }
        val rainLabels = RouteMapRenderPlanner.routeWeatherAnnotations(
            route = route,
            segmentWeather = samples,
            selectedFilter = MapWeatherFilter.RAIN
        ).map { it.label }
        val precipitationLabels = RouteMapRenderPlanner.routeWeatherAnnotations(
            route = route,
            segmentWeather = samples,
            selectedFilter = MapWeatherFilter.PRECIPITATION
        ).map { it.label }
        val scoreLabels = RouteMapRenderPlanner.routeWeatherAnnotations(
            route = route,
            segmentWeather = samples,
            selectedFilter = MapWeatherFilter.RIDE_SCORE
        ).map { it.label }

        assertEquals(1, temperatureLabels.size)
        assertTrue(temperatureLabels.all { it.endsWith(" C") })
        assertTrue(rainLabels.all { it.endsWith("%") })
        assertTrue(precipitationLabels.all { it == "Dry" || it.endsWith(" mm") })
        assertTrue(scoreLabels.all { it.endsWith("/100") })
    }

    @Test
    fun `loop annotations skip same start and finish position`() {
        val route = ImportedRoute(
            id = "loop",
            name = "Loop",
            points = listOf(
                RoutePoint(37.0, 127.0, null, 0.0),
                RoutePoint(37.01, 127.01, null, 5_000.0),
                RoutePoint(37.0, 127.0, null, 10_000.0)
            ),
            totalDistanceMeters = 10_000.0,
            totalElevationGainMeters = null,
            createdAtEpochMillis = 0L
        )
        val samples = route.points.mapIndexed { index, point ->
            testSegmentWeather().first().copy(
                sample = testSegmentWeather().first().sample.copy(
                    id = "loop-sample-$index",
                    point = point,
                    distanceFromStartMeters = point.distanceFromStartMeters
                )
            )
        }

        val annotations = RouteMapRenderPlanner.routeWeatherAnnotations(
            route = route,
            segmentWeather = samples,
            selectedFilter = MapWeatherFilter.WIND,
            mapZoom = 14.0
        )

        assertEquals(listOf(5_000.0), annotations.map { it.distanceFromStartMeters })
        assertEquals(1, annotations.map { it.coordinate }.distinctBy { "${it.latitude}:${it.longitude}" }.size)
    }

    @Test
    fun `route annotations thin out on low zoom for long courses`() {
        val route = testRoute().copy(
            points = listOf(
                RoutePoint(37.0, 127.0, null, 0.0),
                RoutePoint(37.0, 129.0, null, 200_000.0)
            ),
            totalDistanceMeters = 200_000.0
        )
        val samples = longRouteSegmentWeather(route)

        val annotations = RouteMapRenderPlanner.routeWeatherAnnotations(
            route = route,
            segmentWeather = samples,
            selectedFilter = MapWeatherFilter.WIND,
            mapZoom = 7.0
        )

        assertTrue("Low zoom should cap label count on long courses", annotations.size <= 7)
        assertTrue("Low zoom should still keep enough route context", annotations.size >= 5)
        assertTrue("Low zoom should not prioritize departure", annotations.first().distanceFromStartMeters > 0.0)
        assertTrue("Low zoom should not prioritize arrival", annotations.last().distanceFromStartMeters < 200_000.0)
    }

    @Test
    fun `route annotations become denser when zoomed in`() {
        val route = testRoute().copy(
            points = listOf(
                RoutePoint(37.0, 127.0, null, 0.0),
                RoutePoint(37.0, 129.0, null, 200_000.0)
            ),
            totalDistanceMeters = 200_000.0
        )
        val samples = longRouteSegmentWeather(route)

        val lowZoomAnnotations = RouteMapRenderPlanner.routeWeatherAnnotations(
            route = route,
            segmentWeather = samples,
            selectedFilter = MapWeatherFilter.WIND,
            mapZoom = 7.0
        )
        val highZoomAnnotations = RouteMapRenderPlanner.routeWeatherAnnotations(
            route = route,
            segmentWeather = samples,
            selectedFilter = MapWeatherFilter.WIND,
            mapZoom = 14.0
        )

        assertTrue("Zoomed-in map should show more labels than overview", highZoomAnnotations.size > lowZoomAnnotations.size)
        assertTrue("Zoomed-in labels should still be capped", highZoomAnnotations.size <= 24)
    }

    @Test
    fun `every map filter resolves to a visible color`() {
        val segment = testSegmentWeather().first().copy(
            weather = testWeather(
                windDirectionDegrees = 90.0,
                precipitationProbabilityPercent = 55,
                precipitationMm = 2.0,
                cloudCoverPercent = 65
            )
        )

        MapWeatherFilter.entries.forEach { filter ->
            val color = RouteMapRenderPlanner.colorForFilter(segment, filter)

            assertNotEquals("Color should be non-transparent for $filter", 0, color)
        }
    }

    @Test
    fun `every map filter has a coverage legend`() {
        MapWeatherFilter.entries.forEach { filter ->
            assertTrue(
                "Legend should explain $filter",
                RouteMapRenderPlanner.legendForFilter(filter).isNotBlank()
            )
        }
    }

    @Test
    fun `dry and clear weather filters are transparent on the map`() {
        val dryClearSegment = testSegmentWeather().first().copy(
            weather = testWeather(
                windDirectionDegrees = 90.0,
                precipitationProbabilityPercent = 0,
                precipitationMm = 0.0,
                cloudCoverPercent = 0
            )
        )

        assertEquals(0, RouteMapRenderPlanner.colorForFilter(dryClearSegment, MapWeatherFilter.RAIN))
        assertEquals(0, RouteMapRenderPlanner.colorForFilter(dryClearSegment, MapWeatherFilter.PRECIPITATION))
        assertEquals(0, RouteMapRenderPlanner.colorForFilter(dryClearSegment, MapWeatherFilter.CLOUD_COVER))
    }

    @Test
    fun `non wind filters expose gradient legend stops`() {
        val expectedStopPositions = listOf(0f, 0.25f, 0.5f, 0.75f, 1f)

        MapWeatherFilter.entries
            .filterNot { it == MapWeatherFilter.WIND }
            .forEach { filter ->
                val legend = RouteMapRenderPlanner.legendSpecForFilter(filter)

                assertEquals("Legend should expose quarter markers for $filter", expectedStopPositions, legend.stops.map { it.position })
                assertTrue("Legend should have a start marker for $filter", legend.stops.first().position == 0f)
                assertTrue("Legend should have a middle marker for $filter", legend.stops.any { it.position == 0.5f })
                assertTrue("Legend should have an end marker for $filter", legend.stops.last().position == 1f)
                assertTrue("Legend labels should be visible for $filter", legend.stops.all { it.label.isNotBlank() })
            }
    }

    @Test
    fun `wind filter keeps text legend instead of a gradient`() {
        val legend = RouteMapRenderPlanner.legendSpecForFilter(MapWeatherFilter.WIND)

        assertTrue(legend.description.contains("km/h"))
        assertTrue(legend.stops.isEmpty())
    }

    @Test
    fun `sample spacing label reflects long route cap`() {
        assertEquals("8.3 km", RouteMapRenderPlanner.sampleSpacingLabel(200_000.0))
    }

    @Test
    fun `legend label offsets center interior labels on their markers`() {
        assertEquals(0, legendLabelStartOffsetPx(markerPosition = 0f, containerWidthPx = 200, labelWidthPx = 40))
        assertEquals(30, legendLabelStartOffsetPx(markerPosition = 0.25f, containerWidthPx = 200, labelWidthPx = 40))
        assertEquals(80, legendLabelStartOffsetPx(markerPosition = 0.5f, containerWidthPx = 200, labelWidthPx = 40))
        assertEquals(130, legendLabelStartOffsetPx(markerPosition = 0.75f, containerWidthPx = 200, labelWidthPx = 40))
        assertEquals(160, legendLabelStartOffsetPx(markerPosition = 1f, containerWidthPx = 200, labelWidthPx = 40))
    }

    @Test
    fun `legend label offsets keep oversized labels visible`() {
        assertEquals(0, legendLabelStartOffsetPx(markerPosition = 0.5f, containerWidthPx = 30, labelWidthPx = 40))
    }

    @Test
    fun `wind flow bearing points where meteorological wind blows toward`() {
        val flowBearing = RouteMapRenderPlanner.windFlowBearing(270.0)

        assertEquals(90.0, flowBearing, 0.001)
    }

    @Test
    fun `wind arrow geometry follows wind flow direction`() {
        val route = testRoute()
        val westWind = testSegmentWeather().first().copy(
            weather = testWeather(windDirectionDegrees = 270.0)
        )

        val arrow = RouteMapRenderPlanner.windArrowOverlay(route, westWind)

        assertTrue("West wind should blow east", arrow.end.longitude > arrow.start.longitude)
        assertEquals(arrow.color, RouteMapRenderPlanner.colorForFilter(westWind, MapWeatherFilter.WIND))
    }

    private fun testRoute(): ImportedRoute {
        val points = listOf(
            RoutePoint(37.0, 127.0, null, 0.0),
            RoutePoint(37.0, 127.05, null, 5_000.0),
            RoutePoint(37.0, 127.10, null, 10_000.0)
        )

        return ImportedRoute(
            id = "route",
            name = "Route",
            points = points,
            totalDistanceMeters = 10_000.0,
            totalElevationGainMeters = null,
            createdAtEpochMillis = 0L
        )
    }

    private fun testSegmentWeather(): List<SegmentWeather> {
        val points = testRoute().points
        return points.mapIndexed { index, point ->
            SegmentWeather(
                sample = RouteWeatherSample(
                    id = "sample-$index",
                    point = point,
                    distanceFromStartMeters = point.distanceFromStartMeters,
                    estimatedArrivalEpochMillis = index * 3_600_000L,
                    routeBearingDegrees = 90.0
                ),
                weather = testWeather(
                    windDirectionDegrees = if (index == 0) 90.0 else 270.0,
                    precipitationProbabilityPercent = index * 30
                ),
                relativeWind = if (index == 0) RelativeWind.HEADWIND else RelativeWind.TAILWIND,
                windComponents = WindComponents(headwindKmh = if (index == 0) 14.0 else -14.0, crosswindKmh = 0.0),
                score = SegmentScore(
                    total = 80 - index * 10,
                    windScore = 75,
                    temperatureScore = 100,
                    rainScore = 100 - index * 25,
                    gustScore = 100,
                    cloudScore = 100,
                    reasons = emptyList()
                )
            )
        }
    }

    private fun longRouteSegmentWeather(route: ImportedRoute): List<SegmentWeather> {
        return (0..20).map { index ->
            val distanceMeters = index * 10_000.0
            val progress = distanceMeters / route.totalDistanceMeters
            val point = RoutePoint(
                latitude = 37.0,
                longitude = 127.0 + 2.0 * progress,
                elevationMeters = null,
                distanceFromStartMeters = distanceMeters
            )
            testSegmentWeather().first().copy(
                sample = testSegmentWeather().first().sample.copy(
                    id = "long-sample-$index",
                    point = point,
                    distanceFromStartMeters = distanceMeters
                )
            )
        }
    }

    private fun testWeather(
        windDirectionDegrees: Double,
        precipitationProbabilityPercent: Int = 10,
        precipitationMm: Double = 0.0,
        cloudCoverPercent: Int = 40
    ): WeatherPoint {
        return WeatherPoint(
            latitude = 37.0,
            longitude = 127.0,
            timeEpochMillis = 0L,
            temperatureCelsius = 18.0,
            apparentTemperatureCelsius = 17.0,
            windSpeedKmh = 20.0,
            windDirectionDegrees = windDirectionDegrees,
            windGustKmh = 25.0,
            precipitationProbabilityPercent = precipitationProbabilityPercent,
            precipitationMm = precipitationMm,
            cloudCoverPercent = cloudCoverPercent,
            weatherCode = 1
        )
    }

    private fun GeoCoordinate.isSamePositionAs(other: GeoCoordinate): Boolean {
        return kotlin.math.abs(latitude - other.latitude) < 0.000001 &&
            kotlin.math.abs(longitude - other.longitude) < 0.000001
    }
}
