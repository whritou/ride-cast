package com.example.cyclistweather.domain.model

import androidx.annotation.StringRes
import com.example.cyclistweather.R

enum class WeatherCondition(
    val label: String,
    @StringRes val labelRes: Int
) {
    CLEAR("Clear", R.string.wcond_clear),
    MAINLY_CLEAR("Mostly clear", R.string.wcond_mostly_clear),
    PARTLY_CLOUDY("Partly cloudy", R.string.wcond_partly_cloudy),
    OVERCAST("Overcast", R.string.wcond_overcast),
    FOG("Fog", R.string.wcond_fog),
    DRIZZLE("Drizzle", R.string.wcond_drizzle),
    RAIN("Rain", R.string.wcond_rain),
    SNOW("Snow", R.string.wcond_snow),
    THUNDERSTORM("Storm", R.string.wcond_storm),
    UNKNOWN("Unknown", R.string.wcond_unknown)
}

fun mapWeatherCode(code: Int): WeatherCondition {
    return when (code) {
        0 -> WeatherCondition.CLEAR
        1 -> WeatherCondition.MAINLY_CLEAR
        2 -> WeatherCondition.PARTLY_CLOUDY
        3 -> WeatherCondition.OVERCAST
        45, 48 -> WeatherCondition.FOG
        51, 53, 55, 56, 57 -> WeatherCondition.DRIZZLE
        61, 63, 65, 66, 67, 80, 81, 82 -> WeatherCondition.RAIN
        71, 73, 75, 77, 85, 86 -> WeatherCondition.SNOW
        95, 96, 99 -> WeatherCondition.THUNDERSTORM
        else -> WeatherCondition.UNKNOWN
    }
}
