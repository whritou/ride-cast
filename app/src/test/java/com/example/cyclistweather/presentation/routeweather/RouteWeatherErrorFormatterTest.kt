package com.example.cyclistweather.presentation.routeweather

import com.example.cyclistweather.R
import java.net.UnknownHostException
import org.junit.Assert.assertEquals
import org.junit.Test

class RouteWeatherErrorFormatterTest {
    @Test
    fun `unknown host errors map to the offline message`() {
        assertEquals(
            R.string.error_offline,
            RouteWeatherErrorFormatter.messageResFor(
                UnknownHostException("Unable to resolve host \"api.open-meteo.com\"")
            )
        )
    }

    @Test
    fun `nested unknown host errors are recognized`() {
        assertEquals(
            R.string.error_offline,
            RouteWeatherErrorFormatter.messageResFor(
                IllegalStateException(
                    "Weather failed",
                    UnknownHostException("No address associated with hostname")
                )
            )
        )
    }

    @Test
    fun `other errors map to the generic message`() {
        assertEquals(
            R.string.error_generic,
            RouteWeatherErrorFormatter.messageResFor(IllegalStateException("boom"))
        )
    }
}
