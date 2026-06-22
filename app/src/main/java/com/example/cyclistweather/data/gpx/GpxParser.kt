package com.example.cyclistweather.data.gpx

import com.example.cyclistweather.core.common.RouteMath
import com.example.cyclistweather.domain.model.ImportedRoute
import com.example.cyclistweather.domain.model.RoutePoint
import java.io.IOException
import java.io.InputStream
import java.util.UUID
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory

class GpxParseException(message: String) : IllegalArgumentException(message)

class GpxParser {
    fun parse(
        routeName: String,
        inputStream: InputStream,
        createdAtEpochMillis: Long = System.currentTimeMillis()
    ): ImportedRoute {
        val rawPoints = readTrackPoints(inputStream)

        if (rawPoints.size < 2) {
            throw GpxParseException("This GPX file does not contain enough track points.")
        }

        var cumulativeDistanceMeters = 0.0
        var elevationGainMeters = 0.0
        var hasElevation = false

        val routePoints = rawPoints.mapIndexed { index, point ->
            if (index > 0) {
                val previous = rawPoints[index - 1]
                cumulativeDistanceMeters += RouteMath.haversineDistanceMeters(
                    previous.latitude,
                    previous.longitude,
                    point.latitude,
                    point.longitude
                )

                val previousElevation = previous.elevationMeters
                val elevation = point.elevationMeters
                if (previousElevation != null && elevation != null && elevation > previousElevation) {
                    elevationGainMeters += elevation - previousElevation
                }
            }

            if (point.elevationMeters != null) {
                hasElevation = true
            }

            RoutePoint(
                latitude = point.latitude,
                longitude = point.longitude,
                elevationMeters = point.elevationMeters,
                distanceFromStartMeters = cumulativeDistanceMeters
            )
        }

        return ImportedRoute(
            id = UUID.randomUUID().toString(),
            name = routeName.cleanRouteName(),
            points = routePoints,
            totalDistanceMeters = cumulativeDistanceMeters,
            totalElevationGainMeters = elevationGainMeters.takeIf { hasElevation },
            createdAtEpochMillis = createdAtEpochMillis
        )
    }

    private fun readTrackPoints(inputStream: InputStream): List<RawTrackPoint> {
        val parser = try {
            XmlPullParserFactory.newInstance().newPullParser().apply {
                setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                setInput(inputStream, null)
            }
        } catch (_: XmlPullParserException) {
            throw invalidGpx()
        } catch (_: RuntimeException) {
            throw invalidGpx()
        }

        val points = mutableListOf<RawTrackPoint>()
        var currentLatitude: Double? = null
        var currentLongitude: Double? = null
        var currentElevation: Double? = null

        try {
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                val tagName = parser.name
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (tagName) {
                            "trkpt", "rtept" -> {
                                currentLatitude = parser.getAttributeValue(null, "lat")?.toDoubleOrNull()
                                currentLongitude = parser.getAttributeValue(null, "lon")?.toDoubleOrNull()
                                currentElevation = null
                            }

                            "ele" -> {
                                if (currentLatitude != null && currentLongitude != null) {
                                    currentElevation = parser.nextText().trim().toDoubleOrNull()
                                }
                            }
                        }
                    }

                    XmlPullParser.END_TAG -> {
                        if (tagName == "trkpt" || tagName == "rtept") {
                            val latitude = currentLatitude
                            val longitude = currentLongitude
                            if (latitude != null && longitude != null) {
                                points += RawTrackPoint(
                                    latitude = latitude,
                                    longitude = longitude,
                                    elevationMeters = currentElevation
                                )
                            }

                            currentLatitude = null
                            currentLongitude = null
                            currentElevation = null
                        }
                    }
                }

                eventType = parser.next()
            }
        } catch (_: XmlPullParserException) {
            throw invalidGpx()
        } catch (_: IOException) {
            throw invalidGpx()
        } catch (_: RuntimeException) {
            throw invalidGpx()
        }

        return points
    }

    private fun String.cleanRouteName(): String {
        val withoutExtension = removeSuffix(".gpx").removeSuffix(".GPX").trim()
        return withoutExtension.ifBlank { "Imported route" }
    }

    private fun invalidGpx(): GpxParseException {
        return GpxParseException("This GPX file could not be read. Make sure it is a valid GPX track or route file.")
    }

    private data class RawTrackPoint(
        val latitude: Double,
        val longitude: Double,
        val elevationMeters: Double?
    )
}
