package com.example.cyclistweather.presentation.map

import kotlin.math.roundToInt

internal fun legendLabelStartOffsetPx(
    markerPosition: Float,
    containerWidthPx: Int,
    labelWidthPx: Int
): Int {
    val maxStartOffsetPx = (containerWidthPx - labelWidthPx).coerceAtLeast(0)
    val markerX = markerPosition.coerceIn(0f, 1f) * containerWidthPx
    val centeredStartOffsetPx = (markerX - labelWidthPx / 2f).roundToInt()

    return centeredStartOffsetPx.coerceIn(0, maxStartOffsetPx)
}
