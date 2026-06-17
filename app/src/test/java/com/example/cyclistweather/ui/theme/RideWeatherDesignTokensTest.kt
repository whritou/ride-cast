package com.example.cyclistweather.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RideWeatherDesignTokensTest {
    @Test
    fun `light surface palette uses outdoor tokens`() {
        assertEquals(Color(0xFFF6F7F4), LightRideWeatherColors.stone)
        assertEquals(Color(0xFFE6F0E6), LightRideWeatherColors.sage)
        assertEquals(Color(0xFFFFFFFF), LightRideWeatherColors.surface)
    }

    @Test
    fun `dark surface palette flips background and text`() {
        assertEquals(Color(0xFF0F1410), DarkRideWeatherColors.stone)
        assertEquals(Color(0xFFE7ECE3), DarkRideWeatherColors.textPrimary)
    }

    @Test
    fun `brand and status accents are theme-independent`() {
        assertEquals(Color(0xFF1E4D2B), RideWeatherColors.Forest)
        assertEquals(Color(0xFFA7D129), RideWeatherColors.Chartreuse)
        assertEquals(Color(0xFF4AA3E0), RideWeatherColors.Sky)
        assertEquals(Color(0xFFF5A623), RideWeatherColors.Caution)
        assertEquals(Color(0xFFE53935), RideWeatherColors.Dangerous)
    }

    @Test
    fun `interactive accents have readable contrast on surfaces in both themes`() {
        assertContrastAtLeast(4.5, LightRideWeatherColors.accent, LightRideWeatherColors.surface)
        assertContrastAtLeast(4.5, DarkRideWeatherColors.accent, DarkRideWeatherColors.surface)
        assertContrastAtLeast(4.5, LightRideWeatherColors.onAccent, LightRideWeatherColors.accent)
        assertContrastAtLeast(4.5, DarkRideWeatherColors.onAccent, DarkRideWeatherColors.accent)
    }

    @Test
    fun `light score hues meet AA contrast on the light surface`() {
        // Score badges/markers are theme-aware: light mode darkens the hues so they clear WCAG AA
        // (4.5:1) on the white surface, instead of the vivid brand constants which fail there.
        val surface = LightRideWeatherColors.surface
        assertContrastAtLeast(4.5, LightRideWeatherColors.scoreExcellent, surface)
        assertContrastAtLeast(4.5, LightRideWeatherColors.scoreGood, surface)
        assertContrastAtLeast(4.5, LightRideWeatherColors.scoreCaution, surface)
        assertContrastAtLeast(4.5, LightRideWeatherColors.scoreRisk, surface)
        assertContrastAtLeast(4.5, LightRideWeatherColors.scoreDangerous, surface)
    }

    @Test
    fun `dark score hues preserve the vivid brand status colors`() {
        // Dark mode keeps the vivid hues (they read well on the dark surfaces) — no regression.
        assertEquals(RideWeatherColors.Excellent, DarkRideWeatherColors.scoreExcellent)
        assertEquals(RideWeatherColors.Good, DarkRideWeatherColors.scoreGood)
        assertEquals(RideWeatherColors.Caution, DarkRideWeatherColors.scoreCaution)
        assertEquals(RideWeatherColors.Risk, DarkRideWeatherColors.scoreRisk)
        assertEquals(RideWeatherColors.Dangerous, DarkRideWeatherColors.scoreDangerous)
    }

    private fun assertContrastAtLeast(
        minimumRatio: Double,
        foreground: Color,
        background: Color
    ) {
        val ratio = contrastRatio(foreground, background)
        assertTrue("Expected contrast >= $minimumRatio, got $ratio", ratio >= minimumRatio)
    }

    private fun contrastRatio(foreground: Color, background: Color): Double {
        val foregroundLuminance = relativeLuminance(foreground)
        val backgroundLuminance = relativeLuminance(background)
        val lighter = maxOf(foregroundLuminance, backgroundLuminance)
        val darker = minOf(foregroundLuminance, backgroundLuminance)
        return (lighter + 0.05) / (darker + 0.05)
    }

    private fun relativeLuminance(color: Color): Double {
        fun channel(component: Float): Double {
            val value = component.toDouble()
            return if (value <= 0.03928) {
                value / 12.92
            } else {
                Math.pow((value + 0.055) / 1.055, 2.4)
            }
        }

        return 0.2126 * channel(color.red) +
            0.7152 * channel(color.green) +
            0.0722 * channel(color.blue)
    }
}
