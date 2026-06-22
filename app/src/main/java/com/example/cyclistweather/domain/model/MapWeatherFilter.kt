package com.example.cyclistweather.domain.model

/** Weather dimension visualised on the route map. */
enum class MapWeatherFilter(
    val label: String
) {
    WIND("Wind"),
    TEMPERATURE("Temp"),
    FEELS_LIKE("Feels"),
    RAIN("Rain"),
    PRECIPITATION("Precip"),
    CLOUD_COVER("Clouds"),
    RIDE_SCORE("Score")
}
