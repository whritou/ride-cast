package com.example.cyclistweather.presentation.routeweather

import androidx.annotation.StringRes
import com.example.cyclistweather.R
import java.net.UnknownHostException
import java.nio.channels.UnresolvedAddressException

/**
 * Classifies a failure into a localized message resource. Returns a `@StringRes` (not a String) so
 * the text is resolved in the Compose layer and follows the in-app language; the ViewModel stays
 * Context-free.
 */
internal object RouteWeatherErrorFormatter {
    @StringRes
    fun messageResFor(error: Throwable): Int {
        return if (error.hasUnknownHostFailure()) {
            R.string.error_offline
        } else {
            R.string.error_generic
        }
    }

    private fun Throwable.hasUnknownHostFailure(): Boolean {
        return causeChain().any { throwable ->
            throwable is UnknownHostException ||
                throwable is UnresolvedAddressException ||
                throwable.message?.contains("Unable to resolve host", ignoreCase = true) == true ||
                throwable.message?.contains("No address associated with hostname", ignoreCase = true) == true
        }
    }

    private fun Throwable.causeChain(): Sequence<Throwable> {
        return generateSequence(this) { it.cause }
    }
}
