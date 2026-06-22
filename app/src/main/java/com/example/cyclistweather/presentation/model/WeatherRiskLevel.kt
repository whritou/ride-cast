package com.example.cyclistweather.presentation.model

import androidx.annotation.StringRes
import com.example.cyclistweather.R

enum class WeatherRiskLevel(
    val label: String,
    val shortLabel: String,
    @StringRes val labelRes: Int,
    @StringRes val shortLabelRes: Int
) {
    EXCELLENT("Excellent", "Excellent", R.string.risk_excellent, R.string.risk_excellent),
    GOOD("Good", "Good", R.string.risk_good, R.string.risk_good),
    CAUTION("Fair", "Caution", R.string.risk_fair, R.string.risk_caution),
    RISK("Difficult", "Risk", R.string.risk_difficult, R.string.risk_risk),
    DANGEROUS("Avoid", "Avoid", R.string.risk_avoid, R.string.risk_avoid);

    companion object {
        fun fromScore(score: Int): WeatherRiskLevel {
            return when (score.coerceIn(0, 100)) {
                in 85..100 -> EXCELLENT
                in 70..84 -> GOOD
                in 50..69 -> CAUTION
                in 30..49 -> RISK
                else -> DANGEROUS
            }
        }
    }
}
