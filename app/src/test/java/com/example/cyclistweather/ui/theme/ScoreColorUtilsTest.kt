package com.example.cyclistweather.ui.theme

import com.example.cyclistweather.presentation.model.WeatherRiskLevel
import org.junit.Assert.assertEquals
import org.junit.Test

class ScoreColorUtilsTest {
    @Test
    fun `score colors follow ride weather risk palette`() {
        assertEquals(RideWeatherColors.Excellent, ScoreColorUtils.colorForScore(91))
        assertEquals(RideWeatherColors.Good, ScoreColorUtils.colorForScore(76))
        assertEquals(RideWeatherColors.Caution, ScoreColorUtils.colorForScore(58))
        assertEquals(RideWeatherColors.Risk, ScoreColorUtils.colorForScore(42))
        assertEquals(RideWeatherColors.Dangerous, ScoreColorUtils.colorForScore(18))
    }

    @Test
    fun `risk level colors match semantic tokens`() {
        assertEquals(RideWeatherColors.Excellent, ScoreColorUtils.colorForRisk(WeatherRiskLevel.EXCELLENT))
        assertEquals(RideWeatherColors.Dangerous, ScoreColorUtils.colorForRisk(WeatherRiskLevel.DANGEROUS))
    }

    @Test
    fun `score description pairs the number with a qualitative label for screen readers`() {
        // Score badges encode quality with color, so the spoken description must carry both the
        // value and the label — meaning never depends on color alone.
        assertEquals("Ride score 91 out of 100, Excellent", ScoreColorUtils.describeScore(91))
        assertEquals("Ride score 58 out of 100, Fair", ScoreColorUtils.describeScore(58))
        assertEquals("Ride score 18 out of 100, Avoid", ScoreColorUtils.describeScore(18))
    }
}
