package com.example.cyclistweather.presentation.map

import com.example.cyclistweather.R
import com.example.cyclistweather.core.common.RouteMath
import com.example.cyclistweather.domain.model.ImportedRoute
import com.example.cyclistweather.domain.model.MapWeatherFilter
import com.example.cyclistweather.domain.model.RelativeWind
import com.example.cyclistweather.domain.model.SegmentWeather
import com.example.cyclistweather.domain.usecase.SampleRouteUseCase
import kotlin.math.roundToInt

internal object RouteMapRenderPlanner {
    val neutralRouteCasingColor: Int = argb(235, 30, 77, 43)
    val neutralRouteColor: Int = rgb(246, 247, 244)
    val transparentOverlayStrokeColor: Int = argb(0, 0, 0, 0)
    val transparentWeatherColor: Int = argb(0, 0, 0, 0)

    fun routeCoordinates(route: ImportedRoute): List<GeoCoordinate> {
        return route.points.map { point ->
            GeoCoordinate(
                latitude = point.latitude,
                longitude = point.longitude
            )
        }
    }

    fun weatherFieldOverlays(
        route: ImportedRoute,
        segmentWeather: List<SegmentWeather>,
        selectedFilter: MapWeatherFilter
    ): List<WeatherFieldOverlay> {
        if (selectedFilter == MapWeatherFilter.WIND) {
            return emptyList()
        }

        if (route.points.isEmpty() || segmentWeather.isEmpty()) {
            return emptyList()
        }

        val bounds = weatherFieldBounds(route, segmentWeather)
        val latitudeStep = (bounds.north - bounds.south) / WEATHER_FIELD_ROWS
        val longitudeStep = (bounds.east - bounds.west) / WEATHER_FIELD_COLUMNS

        return buildList {
            for (row in 0 until WEATHER_FIELD_ROWS) {
                for (column in 0 until WEATHER_FIELD_COLUMNS) {
                    val south = bounds.south + latitudeStep * row
                    val north = south + latitudeStep
                    val west = bounds.west + longitudeStep * column
                    val east = west + longitudeStep
                    val center = GeoCoordinate(
                        latitude = (south + north) / 2.0,
                        longitude = (west + east) / 2.0
                    )
                    val color = colorForFieldPoint(center, segmentWeather, selectedFilter)

                    add(
                        WeatherFieldOverlay(
                            polygon = listOf(
                                GeoCoordinate(latitude = south, longitude = west),
                                GeoCoordinate(latitude = north, longitude = west),
                                GeoCoordinate(latitude = north, longitude = east),
                                GeoCoordinate(latitude = south, longitude = east)
                            ),
                            fillColor = color,
                            strokeColor = transparentOverlayStrokeColor
                        )
                    )
                }
            }
        }
    }

    fun windFieldArrows(
        route: ImportedRoute,
        segmentWeather: List<SegmentWeather>
    ): List<WindArrowOverlay> {
        if (route.points.isEmpty() || segmentWeather.isEmpty()) {
            return emptyList()
        }

        val bounds = windFieldBounds(route, segmentWeather)
        val latitudeStep = (bounds.north - bounds.south) / WIND_FIELD_ROWS
        val longitudeStep = (bounds.east - bounds.west) / WIND_FIELD_COLUMNS

        return buildList {
            for (row in 0 until WIND_FIELD_ROWS) {
                for (column in 0 until WIND_FIELD_COLUMNS) {
                    val center = GeoCoordinate(
                        latitude = bounds.south + latitudeStep * (row + 0.5),
                        longitude = bounds.west + longitudeStep * (column + 0.5)
                    )
                    val nearestSegment = nearestSegment(center, segmentWeather)

                    add(windArrowOverlayAt(route, nearestSegment, center))
                }
            }
        }
    }

    fun routeWeatherAnnotations(
        route: ImportedRoute,
        segmentWeather: List<SegmentWeather>,
        selectedFilter: MapWeatherFilter,
        mapZoom: Double? = null
    ): List<RouteWeatherAnnotation> {
        if (route.points.isEmpty() || segmentWeather.isEmpty() || route.totalDistanceMeters <= 0.0) {
            return emptyList()
        }

        val visibleSegments = visibleAnnotationSegments(
            segmentWeather = segmentWeather,
            totalDistanceMeters = route.totalDistanceMeters,
            mapZoom = mapZoom
        )
        return visibleSegments.map { segment ->
            RouteWeatherAnnotation(
                coordinate = segment.sampleCoordinate(),
                distanceFromStartMeters = segment.sample.distanceFromStartMeters,
                label = annotationLabel(segment, selectedFilter),
                bearingDegrees = if (selectedFilter == MapWeatherFilter.WIND) {
                    windFlowBearing(segment.weather.windDirectionDegrees)
                } else {
                    null
                },
                color = colorForFilter(segment, selectedFilter)
            )
        }
    }

    fun windArrowOverlay(
        route: ImportedRoute,
        segment: SegmentWeather
    ): WindArrowOverlay {
        val start = GeoCoordinate(
            latitude = segment.sample.point.latitude,
            longitude = segment.sample.point.longitude
        )
        return windArrowOverlayAt(route, segment, start)
    }

    fun nearestInspectableSegment(
        coordinate: GeoCoordinate,
        segmentWeather: List<SegmentWeather>
    ): SegmentWeather? {
        return segmentWeather.minByOrNull { segment ->
            coordinate.distanceMetersTo(segment.sampleCoordinate())
        }
    }

    private fun windArrowOverlayAt(
        route: ImportedRoute,
        segment: SegmentWeather,
        center: GeoCoordinate
    ): WindArrowOverlay {
        val arrowLengthMeters = (route.totalDistanceMeters * 0.025).coerceIn(250.0, 1_200.0)
        val bearing = windFlowBearing(segment.weather.windDirectionDegrees)
        val start = center.destinationPoint(
            bearingDegrees = bearing + 180.0,
            distanceMeters = arrowLengthMeters * 0.5
        )
        val end = center.destinationPoint(
            bearingDegrees = bearing,
            distanceMeters = arrowLengthMeters * 0.5
        )

        return WindArrowOverlay(
            center = center,
            start = start,
            end = end,
            leftHead = end.destinationPoint(
                bearingDegrees = bearing + 150.0,
                distanceMeters = arrowLengthMeters * 0.33
            ),
            rightHead = end.destinationPoint(
                bearingDegrees = bearing - 150.0,
                distanceMeters = arrowLengthMeters * 0.33
            ),
            bearingDegrees = bearing,
            speedLabel = "${segment.weather.windSpeedKmh.toInt()} km/h",
            color = colorForFilter(segment, MapWeatherFilter.WIND)
        )
    }

    fun windFlowBearing(windDirectionDegrees: Double): Double {
        return RouteMath.normalizeDegrees(windDirectionDegrees + 180.0)
    }

    fun legendForFilter(filter: MapWeatherFilter): String {
        return legendSpecForFilter(filter).description
    }

    fun legendSpecForFilter(filter: MapWeatherFilter): WeatherLegendSpec {
        return when (filter) {
            MapWeatherFilter.WIND -> WeatherLegendSpec(
                description = "Arrows show flow | labels show km/h",
                descriptionRes = R.string.leg_wind,
                stops = emptyList()
            )
            MapWeatherFilter.TEMPERATURE -> WeatherLegendSpec(
                description = "Temperature",
                descriptionRes = R.string.leg_temperature,
                stops = listOf(
                    legendStop(0f, "0 C", temperatureColor(0.0)),
                    legendStop(0.25f, "12 C", temperatureColor(12.0)),
                    legendStop(0.5f, "24 C", temperatureColor(24.0)),
                    legendStop(0.75f, "30 C", temperatureColor(30.0)),
                    legendStop(1f, "35+ C", temperatureColor(35.0))
                )
            )
            MapWeatherFilter.FEELS_LIKE -> WeatherLegendSpec(
                description = "Feels like",
                descriptionRes = R.string.leg_feels_like,
                stops = listOf(
                    legendStop(0f, "0 C", feelsLikeColor(0.0)),
                    legendStop(0.25f, "12 C", feelsLikeColor(12.0)),
                    legendStop(0.5f, "24 C", feelsLikeColor(24.0)),
                    legendStop(0.75f, "30 C", feelsLikeColor(30.0)),
                    legendStop(1f, "35+ C", feelsLikeColor(35.0))
                )
            )
            MapWeatherFilter.RAIN -> WeatherLegendSpec(
                description = "Rain chance",
                descriptionRes = R.string.leg_rain_chance,
                stops = listOf(
                    legendStop(0f, "0%", rainColor(0)),
                    legendStop(0.25f, "25%", rainColor(25)),
                    legendStop(0.5f, "50%", rainColor(50)),
                    legendStop(0.75f, "75%", rainColor(75)),
                    legendStop(1f, "100%", rainColor(100))
                )
            )
            MapWeatherFilter.PRECIPITATION -> WeatherLegendSpec(
                description = "Precipitation",
                descriptionRes = R.string.leg_precipitation,
                stops = listOf(
                    legendStop(0f, "Dry", rgb(220, 235, 242), labelRes = R.string.leg_dry),
                    legendStop(0.25f, "0.5 mm", precipitationColor(0.5)),
                    legendStop(0.5f, "2 mm", precipitationColor(2.0)),
                    legendStop(0.75f, "3.5 mm", precipitationColor(3.5)),
                    legendStop(1f, "5+ mm", precipitationColor(5.0))
                )
            )
            MapWeatherFilter.CLOUD_COVER -> WeatherLegendSpec(
                description = "Cloud cover",
                descriptionRes = R.string.leg_cloud_cover,
                stops = listOf(
                    legendStop(0f, "Clear", cloudColor(0), labelRes = R.string.leg_clear),
                    legendStop(0.25f, "25%", cloudColor(25)),
                    legendStop(0.5f, "50%", cloudColor(50)),
                    legendStop(0.75f, "75%", cloudColor(75)),
                    legendStop(1f, "100%", cloudColor(100))
                )
            )
            MapWeatherFilter.RIDE_SCORE -> WeatherLegendSpec(
                description = "Ride score",
                descriptionRes = R.string.leg_ride_score,
                stops = listOf(
                    legendStop(0f, "Hard", scoreColor(30), labelRes = R.string.leg_hard),
                    legendStop(0.25f, "Mixed", scoreColor(50), labelRes = R.string.leg_mixed),
                    legendStop(0.5f, "Good", scoreColor(70), labelRes = R.string.leg_good),
                    legendStop(0.75f, "Great", scoreColor(85), labelRes = R.string.leg_great),
                    legendStop(1f, "Best", scoreColor(95), labelRes = R.string.leg_best)
                )
            )
        }
    }

    fun sampleSpacingLabel(totalDistanceMeters: Double): String {
        val intervalKilometers = SampleRouteUseCase.sampleIntervalMeters(totalDistanceMeters) / 1_000.0
        val roundedTenths = (intervalKilometers * 10.0).roundToInt() / 10.0

        return if (roundedTenths == roundedTenths.roundToInt().toDouble()) {
            "${roundedTenths.roundToInt()} km"
        } else {
            "$roundedTenths km"
        }
    }

    fun colorForFilter(
        segment: SegmentWeather,
        filter: MapWeatherFilter
    ): Int {
        return when (filter) {
            MapWeatherFilter.WIND -> segment.relativeWind.mapColor()
            MapWeatherFilter.TEMPERATURE -> temperatureColor(segment.weather.temperatureCelsius)
            MapWeatherFilter.FEELS_LIKE -> feelsLikeColor(
                segment.weather.apparentTemperatureCelsius ?: segment.weather.temperatureCelsius
            )
            MapWeatherFilter.RAIN -> rainColor(segment.weather.precipitationProbabilityPercent ?: 0)
            MapWeatherFilter.PRECIPITATION -> precipitationColor(segment.weather.precipitationMm ?: 0.0)
            MapWeatherFilter.CLOUD_COVER -> cloudColor(segment.weather.cloudCoverPercent ?: 0)
            MapWeatherFilter.RIDE_SCORE -> scoreColor(segment.score.total)
        }
    }

    private fun colorForFieldPoint(
        point: GeoCoordinate,
        segmentWeather: List<SegmentWeather>,
        filter: MapWeatherFilter
    ): Int {
        return when (filter) {
            MapWeatherFilter.WIND -> nearestSegment(point, segmentWeather).relativeWind.mapColor()
            MapWeatherFilter.TEMPERATURE -> temperatureColor(
                weightedAverage(point, segmentWeather) { it.weather.temperatureCelsius }
            )
            MapWeatherFilter.FEELS_LIKE -> feelsLikeColor(
                weightedAverage(point, segmentWeather) {
                    it.weather.apparentTemperatureCelsius ?: it.weather.temperatureCelsius
                }
            )
            MapWeatherFilter.RAIN -> rainColor(
                weightedAverage(point, segmentWeather) {
                    (it.weather.precipitationProbabilityPercent ?: 0).toDouble()
                }.toInt()
            )
            MapWeatherFilter.PRECIPITATION -> precipitationColor(
                weightedAverage(point, segmentWeather) { it.weather.precipitationMm ?: 0.0 }
            )
            MapWeatherFilter.CLOUD_COVER -> cloudColor(
                weightedAverage(point, segmentWeather) {
                    (it.weather.cloudCoverPercent ?: 0).toDouble()
                }.toInt()
            )
            MapWeatherFilter.RIDE_SCORE -> scoreColor(
                weightedAverage(point, segmentWeather) { it.score.total.toDouble() }.toInt()
            )
        }
    }

    private fun weatherFieldBounds(
        route: ImportedRoute,
        segmentWeather: List<SegmentWeather>
    ): GeoBounds {
        return expandedFieldBounds(
            route = route,
            segmentWeather = segmentWeather,
            paddingMeters = (route.totalDistanceMeters * 0.75).coerceIn(8_000.0, 40_000.0)
        )
    }

    private fun windFieldBounds(
        route: ImportedRoute,
        segmentWeather: List<SegmentWeather>
    ): GeoBounds {
        return expandedFieldBounds(
            route = route,
            segmentWeather = segmentWeather,
            paddingMeters = (route.totalDistanceMeters * 0.28).coerceIn(3_000.0, 14_000.0)
        )
    }

    private fun expandedFieldBounds(
        route: ImportedRoute,
        segmentWeather: List<SegmentWeather>,
        paddingMeters: Double
    ): GeoBounds {
        val coordinates = routeCoordinates(route) + segmentWeather.map { it.sampleCoordinate() }
        val centerLatitude = coordinates.map { it.latitude }.average()
        val latitudePadding = metersToLatitudeDegrees(paddingMeters)
        val longitudePadding = metersToLongitudeDegrees(paddingMeters, centerLatitude)

        return GeoBounds(
            south = coordinates.minOf { it.latitude } - latitudePadding,
            west = coordinates.minOf { it.longitude } - longitudePadding,
            north = coordinates.maxOf { it.latitude } + latitudePadding,
            east = coordinates.maxOf { it.longitude } + longitudePadding
        )
    }

    private fun weightedAverage(
        point: GeoCoordinate,
        segmentWeather: List<SegmentWeather>,
        selector: (SegmentWeather) -> Double
    ): Double {
        val weightedSegments = weightedSegments(point, segmentWeather)
        val weightSum = weightedSegments.sumOf { it.weight }

        return weightedSegments.sumOf { it.weight * selector(it.segment) } / weightSum
    }

    private fun weightedSegments(
        point: GeoCoordinate,
        segmentWeather: List<SegmentWeather>
    ): List<WeightedSegment> {
        return segmentWeather
            .map { segment ->
                val distance = point.distanceMetersTo(segment.sampleCoordinate())
                    .coerceAtLeast(FIELD_WEIGHT_DISTANCE_FLOOR_METERS)
                WeightedSegment(
                    segment = segment,
                    weight = 1.0 / (distance * distance)
                )
            }
            .sortedByDescending { it.weight }
            .take(FIELD_INTERPOLATION_SAMPLE_COUNT)
    }

    private fun nearestSegment(
        point: GeoCoordinate,
        segmentWeather: List<SegmentWeather>
    ): SegmentWeather {
        return segmentWeather.minByOrNull { segment ->
            point.distanceMetersTo(segment.sampleCoordinate())
        } ?: segmentWeather.first()
    }

    private fun visibleAnnotationSegments(
        segmentWeather: List<SegmentWeather>,
        totalDistanceMeters: Double,
        mapZoom: Double?
    ): List<SegmentWeather> {
        val uniqueSegments = segmentWeather
            .filterInteriorSamples(totalDistanceMeters)
            .ifEmpty { segmentWeather }
            .distinctBySamplePosition()
        if (mapZoom == null) {
            return uniqueSegments
        }

        val maxCount = routeAnnotationMaxCountForZoom(mapZoom)
        if (uniqueSegments.size <= maxCount) {
            return uniqueSegments
        }

        val lastIndex = uniqueSegments.lastIndex
        return List(maxCount) { index ->
            (index * lastIndex.toDouble() / (maxCount - 1)).roundToInt()
        }
            .distinct()
            .map { sampleIndex -> uniqueSegments[sampleIndex] }
    }

    private fun List<SegmentWeather>.filterInteriorSamples(totalDistanceMeters: Double): List<SegmentWeather> {
        return filter { segment ->
            segment.sample.distanceFromStartMeters > ROUTE_ANNOTATION_ENDPOINT_DISTANCE_METERS &&
                segment.sample.distanceFromStartMeters < totalDistanceMeters - ROUTE_ANNOTATION_ENDPOINT_DISTANCE_METERS
        }
    }

    private fun List<SegmentWeather>.distinctBySamplePosition(): List<SegmentWeather> {
        return fold(emptyList()) { uniqueSegments, segment ->
            if (uniqueSegments.any { existing ->
                    existing.sampleCoordinate().distanceMetersTo(segment.sampleCoordinate()) <=
                        ROUTE_ANNOTATION_DUPLICATE_DISTANCE_METERS
                }
            ) {
                uniqueSegments
            } else {
                uniqueSegments + segment
            }
        }
    }

    private fun routeAnnotationMaxCountForZoom(mapZoom: Double): Int {
        return when {
            mapZoom < 8.5 -> 7
            mapZoom < 10.5 -> 15
            mapZoom < 12.5 -> 20
            else -> MAX_ROUTE_ANNOTATION_COUNT
        }
    }

    private fun annotationLabel(
        segment: SegmentWeather,
        filter: MapWeatherFilter
    ): String {
        return when (filter) {
            MapWeatherFilter.WIND -> "${segment.weather.windSpeedKmh.roundToInt()} km/h"
            MapWeatherFilter.TEMPERATURE -> "${segment.weather.temperatureCelsius.roundToInt()} C"
            MapWeatherFilter.FEELS_LIKE -> "${(segment.weather.apparentTemperatureCelsius ?: segment.weather.temperatureCelsius).roundToInt()} C"
            MapWeatherFilter.RAIN -> "${segment.weather.precipitationProbabilityPercent ?: 0}%"
            MapWeatherFilter.PRECIPITATION -> {
                val mm = segment.weather.precipitationMm ?: 0.0
                when {
                    mm <= 0.0 -> "Dry"
                    formatMillimeters(mm) == "0" -> "<0.1 mm"
                    else -> "${formatMillimeters(mm)} mm"
                }
            }
            MapWeatherFilter.CLOUD_COVER -> "${segment.weather.cloudCoverPercent ?: 0}%"
            MapWeatherFilter.RIDE_SCORE -> "${segment.score.total}/100"
        }
    }

    private fun formatMillimeters(value: Double): String {
        val roundedTenths = (value * 10.0).roundToInt() / 10.0
        return if (roundedTenths == roundedTenths.roundToInt().toDouble()) {
            roundedTenths.roundToInt().toString()
        } else {
            roundedTenths.toString()
        }
    }

    private fun SegmentWeather.sampleCoordinate(): GeoCoordinate {
        return GeoCoordinate(
            latitude = sample.point.latitude,
            longitude = sample.point.longitude
        )
    }

    private const val WEATHER_FIELD_COLUMNS = 20
    private const val WEATHER_FIELD_ROWS = 14
    private const val WIND_FIELD_COLUMNS = 7
    private const val WIND_FIELD_ROWS = 5
    private const val FIELD_INTERPOLATION_SAMPLE_COUNT = 4
    private const val FIELD_WEIGHT_DISTANCE_FLOOR_METERS = 250.0
    private const val ROUTE_ANNOTATION_ENDPOINT_DISTANCE_METERS = 40.0
    private const val ROUTE_ANNOTATION_DUPLICATE_DISTANCE_METERS = 40.0
    private const val MAX_ROUTE_ANNOTATION_COUNT = 30
}
