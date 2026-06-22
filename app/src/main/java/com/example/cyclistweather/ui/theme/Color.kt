package com.example.cyclistweather.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Surface/text tokens that flip between light and dark. Provided through
 * [LocalRideWeatherColors] by `CyclistWeatherTheme`; read via the theme-aware accessors on
 * [RideWeatherColors].
 */
data class RideWeatherSurfaceColors(
    val stone: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val sage: Color,
    val accent: Color,
    val onAccent: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val border: Color,
    val divider: Color,
    // Score/quality hues for in-app badges & markers. These are theme-aware (unlike the vivid
    // brand/status constants used by the map and M3 scheme): a single constant can't meet WCAG AA
    // on both a near-white and a near-black surface, so light mode uses darkened variants while
    // dark mode keeps the vivid hues. The map overlays use their own palette (RouteMapColors).
    val scoreExcellent: Color,
    val scoreGood: Color,
    val scoreCaution: Color,
    val scoreRisk: Color,
    val scoreDangerous: Color
)

val LightRideWeatherColors = RideWeatherSurfaceColors(
    stone = Color(0xFFF6F7F4),
    surface = Color(0xFFFFFFFF),
    surfaceElevated = Color(0xFFE6F0E6),
    sage = Color(0xFFE6F0E6),
    accent = Color(0xFF1E4D2B),
    onAccent = Color(0xFFFFFFFF),
    textPrimary = Color(0xFF1F2A1F),
    textSecondary = Color(0xFF5B675B),
    border = Color(0xFFE1E6E1),
    divider = Color(0xFFE1E6E1),
    // Darkened for AA contrast on the light stone/sage/surface backgrounds.
    scoreExcellent = Color(0xFF2E7D42),
    scoreGood = Color(0xFF4E7D1C),
    scoreCaution = Color(0xFF9A5B00),
    scoreRisk = Color(0xFFB44E15),
    scoreDangerous = Color(0xFFC62828)
)

val DarkRideWeatherColors = RideWeatherSurfaceColors(
    stone = Color(0xFF0F1410),
    surface = Color(0xFF181D17),
    surfaceElevated = Color(0xFF1F261E),
    sage = Color(0xFF222C20),
    accent = Color(0xFFA7D129),
    onAccent = Color(0xFF14351E),
    textPrimary = Color(0xFFE7ECE3),
    textSecondary = Color(0xFFA7B3A5),
    border = Color(0xFF2E342B),
    divider = Color(0xFF2E342B),
    // Vivid hues read well on the dark surfaces; identical to the brand status constants.
    scoreExcellent = Color(0xFF2E7D42),
    scoreGood = Color(0xFF8FC63D),
    scoreCaution = Color(0xFFF5A623),
    scoreRisk = Color(0xFFE9782E),
    scoreDangerous = Color(0xFFE53935)
)

val LocalRideWeatherColors = staticCompositionLocalOf { LightRideWeatherColors }

/**
 * Brand/status colors are constant across themes; surface/text tokens are theme-aware getters
 * resolved from [LocalRideWeatherColors] (so they must be read in a `@Composable` scope — hoist
 * them into a local `val` before any `Canvas`/`DrawScope` block).
 */
object RideWeatherColors {
    // Theme-aware surfaces & text
    val Stone: Color @Composable @ReadOnlyComposable get() = LocalRideWeatherColors.current.stone
    val Surface: Color @Composable @ReadOnlyComposable get() = LocalRideWeatherColors.current.surface
    val SurfaceElevated: Color @Composable @ReadOnlyComposable get() = LocalRideWeatherColors.current.surfaceElevated
    val Sage: Color @Composable @ReadOnlyComposable get() = LocalRideWeatherColors.current.sage
    val Accent: Color @Composable @ReadOnlyComposable get() = LocalRideWeatherColors.current.accent
    val OnAccent: Color @Composable @ReadOnlyComposable get() = LocalRideWeatherColors.current.onAccent
    val TextPrimary: Color @Composable @ReadOnlyComposable get() = LocalRideWeatherColors.current.textPrimary
    val TextSecondary: Color @Composable @ReadOnlyComposable get() = LocalRideWeatherColors.current.textSecondary
    val Border: Color @Composable @ReadOnlyComposable get() = LocalRideWeatherColors.current.border
    val Divider: Color @Composable @ReadOnlyComposable get() = LocalRideWeatherColors.current.divider

    // Theme-aware score/quality hues (darker in light mode for contrast). Use these — via
    // ScoreColorUtils.themedColorFor* — for in-app badges/markers, not the vivid constants below.
    val ScoreExcellent: Color @Composable @ReadOnlyComposable get() = LocalRideWeatherColors.current.scoreExcellent
    val ScoreGood: Color @Composable @ReadOnlyComposable get() = LocalRideWeatherColors.current.scoreGood
    val ScoreCaution: Color @Composable @ReadOnlyComposable get() = LocalRideWeatherColors.current.scoreCaution
    val ScoreRisk: Color @Composable @ReadOnlyComposable get() = LocalRideWeatherColors.current.scoreRisk
    val ScoreDangerous: Color @Composable @ReadOnlyComposable get() = LocalRideWeatherColors.current.scoreDangerous

    // Brand & status accents — identical in both themes.
    val Forest = Color(0xFF1E4D2B)
    val ForestDark = Color(0xFF14351E)
    val Chartreuse = Color(0xFFA7D129)
    val Sky = Color(0xFF4AA3E0)

    val Excellent = Color(0xFF2E7D42)
    val Good = Color(0xFF8FC63D)
    val Caution = Color(0xFFF5A623)
    val Risk = Color(0xFFE9782E)
    val Dangerous = Color(0xFFE53935)

    val Cold = Color(0xFF38BDF8)
    val Heat = Color(0xFFEC6A42)
    val LowVisibility = Color(0xFF7B8F7D)
}
