package com.example.cyclistweather.domain.model

enum class WeatherDataFreshness {
    LIVE,
    FRESH_CACHE,
    STALE_CACHE;

    fun mostConservative(other: WeatherDataFreshness): WeatherDataFreshness {
        return if (rank >= other.rank) this else other
    }

    private val rank: Int
        get() = when (this) {
            LIVE -> 0
            FRESH_CACHE -> 1
            STALE_CACHE -> 2
        }
}
