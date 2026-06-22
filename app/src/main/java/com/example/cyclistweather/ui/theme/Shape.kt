package com.example.cyclistweather.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Material 3 shape scale, mapped onto the app's [Radius] tokens so framework components
 * (chips, dialogs, menus) match hand-built surfaces. extraSmall stays at 4dp for the few
 * truly tiny affordances; everything else follows the design-system radii.
 */
val RideWeatherShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(Radius.sm),    // 6dp — chips, tags
    medium = RoundedCornerShape(Radius.md),   // 8dp — buttons, rows, inputs
    large = RoundedCornerShape(Radius.lg),    // 12dp — cards, panels
    extraLarge = RoundedCornerShape(Radius.lg) // 12dp — dialogs, sheets
)
