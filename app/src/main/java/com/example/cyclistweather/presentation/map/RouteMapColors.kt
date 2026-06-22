package com.example.cyclistweather.presentation.map

import com.example.cyclistweather.domain.model.RelativeWind

/**
 * ARGB color helpers for the weather map overlays. Kept as package-level
 * functions so [RouteMapRenderPlanner] stays focused on geometry/layout.
 * Transparent weather samples encode to 0 (fully transparent black).
 */

internal fun RelativeWind.mapColor(): Int = when (this) {
    RelativeWind.TAILWIND -> rgb(46, 213, 115)
    RelativeWind.HEADWIND -> rgb(249, 115, 22)
    RelativeWind.CROSSWIND_LEFT,
    RelativeWind.CROSSWIND_RIGHT -> rgb(250, 204, 21)
}

internal fun temperatureColor(valueCelsius: Double): Int = when {
    valueCelsius < 4.0 -> rgb(56, 189, 248)
    valueCelsius < 8.0 -> rgb(77, 163, 255)
    valueCelsius < 12.0 -> rgb(0, 191, 166)
    valueCelsius <= 24.0 -> rgb(46, 213, 115)
    valueCelsius <= 28.0 -> rgb(250, 204, 21)
    valueCelsius <= 32.0 -> rgb(249, 115, 22)
    else -> rgb(251, 113, 133)
}

internal fun feelsLikeColor(valueCelsius: Double): Int = temperatureColor(valueCelsius)

internal fun rainColor(probabilityPercent: Int): Int = when {
    probabilityPercent <= 0 -> 0
    probabilityPercent < 20 -> rgb(92, 188, 222)
    probabilityPercent <= 40 -> rgb(49, 151, 196)
    probabilityPercent <= 60 -> rgb(35, 92, 171)
    else -> rgb(28, 57, 130)
}

internal fun precipitationColor(precipitationMm: Double): Int = when {
    precipitationMm <= 0.0 -> 0
    precipitationMm <= 1.0 -> rgb(66, 170, 218)
    precipitationMm <= 3.0 -> rgb(35, 92, 171)
    else -> rgb(28, 57, 130)
}

internal fun cloudColor(cloudCoverPercent: Int): Int = when {
    cloudCoverPercent <= 0 -> 0
    cloudCoverPercent <= 30 -> rgb(214, 220, 226)
    cloudCoverPercent <= 60 -> rgb(158, 166, 176)
    cloudCoverPercent <= 85 -> rgb(112, 124, 139)
    else -> rgb(68, 78, 92)
}

internal fun scoreColor(score: Int): Int = when {
    score >= 85 -> rgb(46, 213, 115)
    score >= 70 -> rgb(163, 230, 53)
    score >= 50 -> rgb(250, 204, 21)
    score >= 30 -> rgb(249, 115, 22)
    else -> rgb(239, 68, 68)
}

internal fun rgb(red: Int, green: Int, blue: Int): Int = argb(255, red, green, blue)

internal fun argb(alpha: Int, red: Int, green: Int, blue: Int): Int {
    return ((alpha.coerceIn(0, 255) and 0xff) shl 24) or
        ((red.coerceIn(0, 255) and 0xff) shl 16) or
        ((green.coerceIn(0, 255) and 0xff) shl 8) or
        (blue.coerceIn(0, 255) and 0xff)
}
