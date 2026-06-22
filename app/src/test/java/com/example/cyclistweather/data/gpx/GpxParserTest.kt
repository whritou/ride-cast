package com.example.cyclistweather.data.gpx

import java.io.File
import java.io.ByteArrayInputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GpxParserTest {
    @Test
    fun `test france asset is a usable bundled course`() {
        val asset = File("src/main/assets/test_france.gpx")
        val content = asset.readText()

        assertTrue("France test course asset should exist", asset.exists())
        assertTrue("France test course should have many track points", Regex("<trkpt\\b").findAll(content).count() > 1_000)
        assertTrue("France test course should include elevation data", content.contains("<ele>"))
    }

    @Test
    fun `malformed gpx reports a clear import error`() {
        val error = runCatching {
            GpxParser().parse("bad.gpx", ByteArrayInputStream("<gpx><trkpt".toByteArray()))
        }.exceptionOrNull()

        assertTrue("Expected GpxParseException, got ${error?.javaClass?.name}: ${error?.message}", error is GpxParseException)
        assertEquals(
            "This GPX file could not be read. Make sure it is a valid GPX track or route file.",
            error?.message
        )
    }
}
