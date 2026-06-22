package com.example.cyclistweather.domain.model

import androidx.annotation.StringRes
import com.example.cyclistweather.R

/** User-selectable theme preference. */
enum class ThemeMode(val label: String, @StringRes val labelRes: Int) {
    SYSTEM("System", R.string.theme_system),
    LIGHT("Light", R.string.theme_light),
    DARK("Dark", R.string.theme_dark)
}
