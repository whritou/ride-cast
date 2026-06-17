package com.example.cyclistweather.domain.model

import androidx.annotation.StringRes
import com.example.cyclistweather.R

enum class RelativeWind(
    val label: String,
    @StringRes val labelRes: Int
) {
    HEADWIND("Headwind", R.string.relwind_headwind),
    TAILWIND("Tailwind", R.string.relwind_tailwind),
    CROSSWIND_LEFT("Crosswind L", R.string.relwind_crosswind_left),
    CROSSWIND_RIGHT("Crosswind R", R.string.relwind_crosswind_right)
}

data class WindComponents(
    val headwindKmh: Double,
    val crosswindKmh: Double
)
