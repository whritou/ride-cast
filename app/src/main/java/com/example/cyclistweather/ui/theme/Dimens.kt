package com.example.cyclistweather.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Spacing scale on a 4dp base grid. Use these instead of ad-hoc dp values so every screen
 * shares a consistent vertical/horizontal rhythm.
 */
object Spacing {
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val xxxl = 32.dp
}

/**
 * Corner-radius scale. The app uses an intentionally flat, border-defined surface style
 * (no drop shadows), so radii — not elevation — carry the visual hierarchy. Keep these in
 * sync with [RideWeatherShapes].
 */
object Radius {
    val sm = 6.dp     // chips, inline tags, small pills
    val md = 8.dp     // buttons, list rows, inputs, badges
    val lg = 12.dp    // cards, panels, sheets
    val pill = 999.dp // fully rounded (avatars, circular icon wells)
}

/** Shared sizing constants for accessible, consistent layouts. */
object Sizes {
    /** Minimum interactive target per Material accessibility guidance. */
    val minTouchTarget = 48.dp
    val iconSm = 16.dp
    val iconMd = 20.dp
    val iconLg = 24.dp
}

/** Standard alpha levels for translucent surfaces/accents. */
object RideWeatherAlpha {
    const val subtle = 0.12f
    const val muted = 0.55f
    const val strong = 0.7f
}
