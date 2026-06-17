package com.example.cyclistweather.data.gpx

import com.example.cyclistweather.domain.model.ImportedRoute

object GpxExporter {
    fun toGpx(route: ImportedRoute): String {
        return buildString {
            appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
            appendLine("""<gpx version="1.1" creator="RideCast">""")
            appendLine("  <trk>")
            appendLine("    <name>${route.name.escapeXml()}</name>")
            appendLine("    <trkseg>")
            route.points.forEach { point ->
                appendLine("""      <trkpt lat="${point.latitude}" lon="${point.longitude}">""")
                point.elevationMeters?.let { elevation ->
                    appendLine("        <ele>$elevation</ele>")
                }
                appendLine("      </trkpt>")
            }
            appendLine("    </trkseg>")
            appendLine("  </trk>")
            appendLine("</gpx>")
        }
    }

    private fun String.escapeXml(): String {
        return replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
