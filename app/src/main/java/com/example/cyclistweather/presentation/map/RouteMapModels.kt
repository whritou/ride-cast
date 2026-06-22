package com.example.cyclistweather.presentation.map

import androidx.annotation.StringRes
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

internal const val EARTH_RADIUS_METERS = 6_371_000.0
internal const val METERS_PER_DEGREE_LATITUDE = 111_320.0

internal data class WeatherFieldOverlay(
    val polygon: List<GeoCoordinate>,
    val fillColor: Int,
    val strokeColor: Int
)

internal data class WeatherLegendSpec(
    val description: String,
    @StringRes val descriptionRes: Int,
    val stops: List<WeatherLegendStop>
)

internal data class WeatherLegendStop(
    val position: Float,
    val label: String,
    val color: Int,
    /** Localized label resource for word labels; 0 means use the numeric/unit [label] as-is. */
    @StringRes val labelRes: Int = 0
)

internal data class RouteWeatherAnnotation(
    val coordinate: GeoCoordinate,
    val distanceFromStartMeters: Double,
    val label: String,
    val bearingDegrees: Double?,
    val color: Int
)

internal data class WindArrowOverlay(
    val center: GeoCoordinate,
    val start: GeoCoordinate,
    val end: GeoCoordinate,
    val leftHead: GeoCoordinate,
    val rightHead: GeoCoordinate,
    val bearingDegrees: Double,
    val speedLabel: String,
    val color: Int
)

internal data class GeoCoordinate(
    val latitude: Double,
    val longitude: Double
) {
    fun destinationPoint(
        bearingDegrees: Double,
        distanceMeters: Double
    ): GeoCoordinate {
        val angularDistance = distanceMeters / EARTH_RADIUS_METERS
        val bearing = Math.toRadians(bearingDegrees)
        val lat1 = Math.toRadians(latitude)
        val lon1 = Math.toRadians(longitude)

        val lat2 = asin(
            sin(lat1) * cos(angularDistance) +
                cos(lat1) * sin(angularDistance) * cos(bearing)
        )
        val lon2 = lon1 + atan2(
            sin(bearing) * sin(angularDistance) * cos(lat1),
            cos(angularDistance) - sin(lat1) * sin(lat2)
        )

        return GeoCoordinate(
            latitude = Math.toDegrees(lat2),
            longitude = Math.toDegrees(lon2)
        )
    }

    fun distanceMetersTo(other: GeoCoordinate): Double {
        val latitudeMeters = (latitude - other.latitude) * METERS_PER_DEGREE_LATITUDE
        val meanLatitude = Math.toRadians((latitude + other.latitude) / 2.0)
        val longitudeMeters = (longitude - other.longitude) * METERS_PER_DEGREE_LATITUDE * cos(meanLatitude)

        return sqrt(latitudeMeters * latitudeMeters + longitudeMeters * longitudeMeters)
    }
}

internal data class GeoBounds(
    val south: Double,
    val west: Double,
    val north: Double,
    val east: Double
)

internal data class WeightedSegment(
    val segment: com.example.cyclistweather.domain.model.SegmentWeather,
    val weight: Double
)

internal fun metersToLatitudeDegrees(meters: Double): Double {
    return meters / METERS_PER_DEGREE_LATITUDE
}

internal fun metersToLongitudeDegrees(
    meters: Double,
    latitude: Double
): Double {
    val longitudeMetersAtLatitude = METERS_PER_DEGREE_LATITUDE * cos(Math.toRadians(latitude))
    return meters / longitudeMetersAtLatitude.coerceAtLeast(1.0)
}

internal fun legendStop(
    position: Float,
    label: String,
    color: Int,
    @StringRes labelRes: Int = 0
): WeatherLegendStop = WeatherLegendStop(position = position, label = label, color = color, labelRes = labelRes)
