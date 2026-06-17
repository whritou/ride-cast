package com.example.cyclistweather.data.gpx

import com.example.cyclistweather.domain.model.ImportedRoute
import com.example.cyclistweather.domain.model.RoutePoint
import org.junit.Assert.assertTrue
import org.junit.Test

class GpxExporterTest {
    @Test
    fun `exported route includes name points and elevation`() {
        val route = ImportedRoute(
            id = "route",
            name = "Morning Loop",
            points = listOf(
                RoutePoint(37.0, 127.0, 12.3, 0.0),
                RoutePoint(37.1, 127.1, null, 1_000.0)
            ),
            totalDistanceMeters = 1_000.0,
            totalElevationGainMeters = 12.3,
            createdAtEpochMillis = 0L
        )

        val xml = GpxExporter.toGpx(route)

        assertTrue(xml.contains("<name>Morning Loop</name>"))
        assertTrue(xml.contains("""<trkpt lat="37.0" lon="127.0">"""))
        assertTrue(xml.contains("<ele>12.3</ele>"))
        assertTrue(xml.contains("""<trkpt lat="37.1" lon="127.1">"""))
    }
}
