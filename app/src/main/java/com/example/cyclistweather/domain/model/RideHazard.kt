package com.example.cyclistweather.domain.model

/** Severity of a riding hazard. WARNING surfaces a yellow banner, DANGER a red one. */
enum class HazardSeverity { WARNING, DANGER }

/**
 * Language-neutral identity of a hazard. Used for de-duplication in the domain and to look up the
 * localized title/detail in the UI layer (the displayed text never lives in the domain).
 */
enum class HazardKind {
    THUNDERSTORM,
    HEAVY_SNOW,
    SNOW_ICE,
    HEAVY_RAIN,
    RAIN_LIKELY,
    DENSE_FOG,
    LOW_VISIBILITY,
    VIOLENT_GUSTS,
    STRONG_GUSTS,
    EXTREME_HEAT,
    HIGH_HEAT,
    SEVERE_COLD,
    FREEZING,
    POOR_AIR,
    VERY_HIGH_UV,
    NIGHT_RIDE
}

/**
 * A safety concern detected along the route for the chosen departure time. Carries only structured
 * data: the [kind], a formatted [distanceLabel] for the "near X" phrasing, and an optional numeric
 * [value] (gust km/h, apparent °C, AQI, or UV index, depending on [kind]). The banner composable
 * turns this into localized text.
 */
data class RideHazard(
    val severity: HazardSeverity,
    val kind: HazardKind,
    val distanceLabel: String? = null,
    val value: Int? = null
)
