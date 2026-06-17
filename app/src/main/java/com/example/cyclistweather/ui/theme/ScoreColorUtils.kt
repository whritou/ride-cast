package com.example.cyclistweather.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import com.example.cyclistweather.presentation.model.WeatherRiskLevel

object ScoreColorUtils {
    /** Vivid, theme-independent mapping. Used by the M3 scheme/map context and as the dark-mode
     * score hue. For in-app badges/markers prefer the theme-aware [themedColorForScore]. */
    fun colorForScore(score: Int): Color {
        return colorForRisk(WeatherRiskLevel.fromScore(score))
    }

    fun colorForRisk(riskLevel: WeatherRiskLevel): Color {
        return when (riskLevel) {
            WeatherRiskLevel.EXCELLENT -> RideWeatherColors.Excellent
            WeatherRiskLevel.GOOD -> RideWeatherColors.Good
            WeatherRiskLevel.CAUTION -> RideWeatherColors.Caution
            WeatherRiskLevel.RISK -> RideWeatherColors.Risk
            WeatherRiskLevel.DANGEROUS -> RideWeatherColors.Dangerous
        }
    }

    /** Theme-aware score hue for in-app surfaces — darker in light mode so badges meet WCAG AA,
     * vivid in dark mode. Must be read in a `@Composable` scope (hoist before any `DrawScope`). */
    @Composable
    @ReadOnlyComposable
    fun themedColorForScore(score: Int): Color = themedColorForRisk(WeatherRiskLevel.fromScore(score))

    @Composable
    @ReadOnlyComposable
    fun themedColorForRisk(riskLevel: WeatherRiskLevel): Color = when (riskLevel) {
        WeatherRiskLevel.EXCELLENT -> RideWeatherColors.ScoreExcellent
        WeatherRiskLevel.GOOD -> RideWeatherColors.ScoreGood
        WeatherRiskLevel.CAUTION -> RideWeatherColors.ScoreCaution
        WeatherRiskLevel.RISK -> RideWeatherColors.ScoreRisk
        WeatherRiskLevel.DANGEROUS -> RideWeatherColors.ScoreDangerous
    }

    /**
     * Spoken description for a ride score. Score badges encode quality with color, so screen
     * readers need the numeric value *and* the qualitative label to avoid color-only meaning.
     */
    fun describeScore(score: Int): String {
        val level = WeatherRiskLevel.fromScore(score)
        return "Ride score $score out of 100, ${level.label}"
    }
}
