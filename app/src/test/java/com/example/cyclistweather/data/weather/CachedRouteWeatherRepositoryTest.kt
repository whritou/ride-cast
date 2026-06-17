package com.example.cyclistweather.data.weather

import com.example.cyclistweather.domain.model.WeatherDataFreshness
import com.example.cyclistweather.domain.model.WeatherPoint
import java.net.UnknownHostException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CachedRouteWeatherRepositoryTest {
    @Test
    fun `fresh cached forecast is returned before network`() = runBlocking {
        val cache = FakeWeatherCacheStore(
            entry = CachedWeatherForecast(
                points = listOf(testWeather(1_000L)),
                fetchedAtEpochMillis = 9_000L
            )
        )
        val network = FakeRouteWeatherRepository(result = listOf(testWeather(2_000L)))
        val repository = CachedRouteWeatherRepository(
            networkRepository = network,
            cacheStore = cache,
            clock = { 10_000L },
            freshForMillis = 5_000L
        )

        repository.beginRouteWeatherLoad()
        val result = repository.getWeatherForPoint(37.0, 127.0)

        assertEquals(1_000L, result.single().timeEpochMillis)
        assertFalse("Fresh cache should avoid the network", network.wasCalled)
        assertEquals(WeatherDataFreshness.FRESH_CACHE, repository.currentWeatherDataFreshness)
    }

    @Test
    fun `stale cached forecast is returned when network cannot resolve host`() = runBlocking {
        val cache = FakeWeatherCacheStore(
            entry = CachedWeatherForecast(
                points = listOf(testWeather(1_000L)),
                fetchedAtEpochMillis = 1_000L
            )
        )
        val network = FakeRouteWeatherRepository(error = UnknownHostException("api.open-meteo.com"))
        val repository = CachedRouteWeatherRepository(
            networkRepository = network,
            cacheStore = cache,
            clock = { 20_000L },
            freshForMillis = 5_000L
        )

        repository.beginRouteWeatherLoad()
        val result = repository.getWeatherForPoint(37.0, 127.0)

        assertEquals(1_000L, result.single().timeEpochMillis)
        assertTrue("Stale cache should still attempt network first", network.wasCalled)
        assertEquals(WeatherDataFreshness.STALE_CACHE, repository.currentWeatherDataFreshness)
    }

    @Test
    fun `network forecast is stored in cache`() = runBlocking {
        val cache = FakeWeatherCacheStore()
        val network = FakeRouteWeatherRepository(result = listOf(testWeather(2_000L)))
        val repository = CachedRouteWeatherRepository(
            networkRepository = network,
            cacheStore = cache,
            clock = { 12_000L },
            freshForMillis = 5_000L
        )

        repository.beginRouteWeatherLoad()
        val result = repository.getWeatherForPoint(37.0, 127.0)

        assertEquals(2_000L, result.single().timeEpochMillis)
        assertEquals(12_000L, cache.savedEntry?.fetchedAtEpochMillis)
        assertEquals(WeatherDataFreshness.LIVE, repository.currentWeatherDataFreshness)
    }

    @Test(expected = UnknownHostException::class)
    fun `missing cache still reports network failure`() = runBlocking {
        val repository = CachedRouteWeatherRepository(
            networkRepository = FakeRouteWeatherRepository(error = UnknownHostException("api.open-meteo.com")),
            cacheStore = FakeWeatherCacheStore(),
            clock = { 20_000L },
            freshForMillis = 5_000L
        )

        repository.beginRouteWeatherLoad()
        repository.getWeatherForPoint(37.0, 127.0)
        Unit
    }

    private class FakeRouteWeatherRepository(
        private val result: List<WeatherPoint> = emptyList(),
        private val error: Throwable? = null
    ) : RouteWeatherRepository {
        var wasCalled: Boolean = false

        override suspend fun getWeatherForPoint(
            latitude: Double,
            longitude: Double
        ): List<WeatherPoint> {
            wasCalled = true
            error?.let { throw it }
            return result
        }
    }

    private class FakeWeatherCacheStore(
        private val entry: CachedWeatherForecast? = null
    ) : WeatherCacheStore {
        var savedEntry: CachedWeatherForecast? = null

        override suspend fun getForecast(cacheKey: String): CachedWeatherForecast? {
            return entry
        }

        override suspend fun putForecast(
            cacheKey: String,
            latitude: Double,
            longitude: Double,
            forecast: CachedWeatherForecast
        ) {
            savedEntry = forecast
        }
    }

    private fun testWeather(timeEpochMillis: Long): WeatherPoint {
        return WeatherPoint(
            latitude = 37.0,
            longitude = 127.0,
            timeEpochMillis = timeEpochMillis,
            temperatureCelsius = 18.0,
            apparentTemperatureCelsius = 17.0,
            windSpeedKmh = 20.0,
            windDirectionDegrees = 90.0,
            windGustKmh = 24.0,
            precipitationProbabilityPercent = 10,
            precipitationMm = 0.0,
            cloudCoverPercent = 20,
            weatherCode = 1
        )
    }
}
