package com.example.cyclistweather.domain.model

/** Severity of a riding hazard. WARNING surfaces a yellow banner, DANGER a red one. */
enum class HazardSeverity { WARNING, DANGER }

/**
 * Groups hazard kinds that describe the same underlying phenomenon at different severities
 * (e.g. [HazardKind.HIGH_HEAT] vs [HazardKind.EXTREME_HEAT]). De-duplication keeps only the most
 * severe hazard per category, so a single condition never surfaces as two banners at once.
 */
enum class HazardCategory { STORM, SNOW, RAIN, VISIBILITY, GUSTS, HEAT, COLD, AIR, UV, NIGHT }

/**
 * Language-neutral identity of a hazard. Used for de-duplication in the domain and to look up the
 * localized title/detail in the UI layer (the displayed text never lives in the domain). The
 * [category] groups warning/danger variants of the same phenomenon for de-duplication.
 */
enum class HazardKind(val category: HazardCategory) {
    THUNDERSTORM(HazardCategory.STORM),
    HEAVY_SNOW(HazardCategory.SNOW),
    SNOW_ICE(HazardCategory.SNOW),
    HEAVY_RAIN(HazardCategory.RAIN),
    RAIN_LIKELY(HazardCategory.RAIN),
    DENSE_FOG(HazardCategory.VISIBILITY),
    LOW_VISIBILITY(HazardCategory.VISIBILITY),
    VIOLENT_GUSTS(HazardCategory.GUSTS),
    STRONG_GUSTS(HazardCategory.GUSTS),
    EXTREME_HEAT(HazardCategory.HEAT),
    HIGH_HEAT(HazardCategory.HEAT),
    SEVERE_COLD(HazardCategory.COLD),
    FREEZING(HazardCategory.COLD),
    POOR_AIR(HazardCategory.AIR),
    VERY_HIGH_UV(HazardCategory.UV),
    NIGHT_RIDE(HazardCategory.NIGHT)
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
