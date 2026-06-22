package com.example.cyclistweather.data.weather

import com.example.cyclistweather.domain.model.WeatherDataFreshness
import com.example.cyclistweather.domain.model.WeatherPoint
import java.util.concurrent.TimeUnit

interface WeatherDataFreshnessProvider {
    fun beginRouteWeatherLoad()
    val currentWeatherDataFreshness: WeatherDataFreshness
}

class CachedRouteWeatherRepository(
    private val networkRepository: RouteWeatherRepository,
    private val cacheStore: WeatherCacheStore,
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val freshForMillis: Long = TimeUnit.HOURS.toMillis(6)
) : RouteWeatherRepository,
    WeatherDataFreshnessProvider {

    override var currentWeatherDataFreshness: WeatherDataFreshness = WeatherDataFreshness.LIVE
        private set

    override fun beginRouteWeatherLoad() {
        currentWeatherDataFreshness = WeatherDataFreshness.LIVE
    }

    override suspend fun getWeatherForPoint(
        latitude: Double,
        longitude: Double
    ): List<WeatherPoint> {
        val cacheKey = WeatherCacheKey.forCoordinate(latitude, longitude)
        val cachedForecast = cacheStore.getForecast(cacheKey)
        val now = clock()

        if (cachedForecast != null && cachedForecast.isFresh(now)) {
            markFreshness(WeatherDataFreshness.FRESH_CACHE)
            return cachedForecast.points
        }

        return runCatching {
            networkRepository.getWeatherForPoint(latitude, longitude)
        }.mapCatching { liveForecast ->
            cacheStore.putForecast(
                cacheKey = cacheKey,
                latitude = latitude,
                longitude = longitude,
                forecast = CachedWeatherForecast(
                    points = liveForecast,
                    fetchedAtEpochMillis = now
                )
            )
            markFreshness(WeatherDataFreshness.LIVE)
            liveForecast
        }.getOrElse { error ->
            if (cachedForecast != null) {
                markFreshness(WeatherDataFreshness.STALE_CACHE)
                cachedForecast.points
            } else {
                throw error
            }
        }
    }

    private fun CachedWeatherForecast.isFresh(now: Long): Boolean {
        return now - fetchedAtEpochMillis <= freshForMillis
    }

    private fun markFreshness(freshness: WeatherDataFreshness) {
        currentWeatherDataFreshness = currentWeatherDataFreshness.mostConservative(freshness)
    }
}
