package com.example.cyclistweather.data.weather

import com.example.cyclistweather.domain.model.WeatherPoint
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class OpenMeteoWeatherRepository : RouteWeatherRepository {
    private val client = HttpClient {
        install(HttpTimeout) {
            connectTimeoutMillis = WEATHER_CONNECT_TIMEOUT_MS
            requestTimeoutMillis = WEATHER_REQUEST_TIMEOUT_MS
            socketTimeoutMillis = WEATHER_REQUEST_TIMEOUT_MS
        }
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }
    private val cache = mutableMapOf<String, List<WeatherPoint>>()
    private val forecastTimeFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    override suspend fun getWeatherForPoint(
        latitude: Double,
        longitude: Double
    ): List<WeatherPoint> {
        val cacheKey = WeatherCacheKey.forCoordinate(latitude, longitude)
        cache[cacheKey]?.let { return it }

        val response: OpenMeteoForecastResponse = client.get("https://api.open-meteo.com/v1/forecast") {
            parameter("latitude", latitude)
            parameter("longitude", longitude)
            parameter(
                "hourly",
                "temperature_2m,apparent_temperature,relative_humidity_2m,weather_code,cloud_cover,precipitation,precipitation_probability,wind_speed_10m,wind_gusts_10m,wind_direction_10m,uv_index,visibility,is_day"
            )
            parameter("timezone", "UTC")
            parameter("forecast_days", 3)
            parameter("wind_speed_unit", "kmh")
        }.body()

        // Air quality lives on a separate Open-Meteo endpoint; treat it as best-effort so a
        // failure there never blocks the core forecast.
        val aqiByHour = fetchAirQualityByHour(latitude, longitude)

        val weather = response.hourly.toWeatherPoints(latitude, longitude, aqiByHour)
        cache[cacheKey] = weather
        return weather
    }

    private suspend fun fetchAirQualityByHour(
        latitude: Double,
        longitude: Double
    ): Map<Long, Int> {
        return runCatching {
            val response: OpenMeteoAirQualityResponse =
                client.get("https://air-quality-api.open-meteo.com/v1/air-quality") {
                    parameter("latitude", latitude)
                    parameter("longitude", longitude)
                    parameter("hourly", "european_aqi")
                    parameter("timezone", "UTC")
                    parameter("forecast_days", 3)
                }.body()

            val hourly = response.hourly
            buildMap {
                hourly.time.forEachIndexed { index, hour ->
                    val epoch = parseForecastHour(hour) ?: return@forEachIndexed
                    val aqi = hourly.europeanAqi.getOrNull(index) ?: return@forEachIndexed
                    put(epoch, aqi)
                }
            }
        }.getOrDefault(emptyMap())
    }

    private fun OpenMeteoHourly.toWeatherPoints(
        latitude: Double,
        longitude: Double,
        aqiByHour: Map<Long, Int>
    ): List<WeatherPoint> {
        return time.mapIndexedNotNull { index, hour ->
            val epochMillis = parseForecastHour(hour) ?: return@mapIndexedNotNull null
            val temperature = temperatureCelsius.getOrNull(index) ?: return@mapIndexedNotNull null
            val windSpeed = windSpeedKmh.getOrNull(index) ?: return@mapIndexedNotNull null
            val windDirection = windDirectionDegrees.getOrNull(index) ?: return@mapIndexedNotNull null

            WeatherPoint(
                latitude = latitude,
                longitude = longitude,
                timeEpochMillis = epochMillis,
                temperatureCelsius = temperature,
                apparentTemperatureCelsius = apparentTemperatureCelsius.getOrNull(index),
                windSpeedKmh = windSpeed,
                windDirectionDegrees = windDirection,
                windGustKmh = windGustKmh.getOrNull(index),
                precipitationProbabilityPercent = precipitationProbabilityPercent.getOrNull(index),
                precipitationMm = precipitationMm.getOrNull(index),
                cloudCoverPercent = cloudCoverPercent.getOrNull(index),
                weatherCode = weatherCode.getOrNull(index) ?: 0,
                relativeHumidityPercent = relativeHumidityPercent.getOrNull(index),
                uvIndex = uvIndex.getOrNull(index),
                visibilityMeters = visibilityMeters.getOrNull(index),
                europeanAqi = aqiByHour[epochMillis],
                isDay = isDay.getOrNull(index)?.let { it == 1 }
            )
        }
    }

    private fun parseForecastHour(value: String): Long? {
        return synchronized(forecastTimeFormat) {
            forecastTimeFormat.parse(value)?.time
        }
    }

}

@Serializable
private data class OpenMeteoForecastResponse(
    val hourly: OpenMeteoHourly = OpenMeteoHourly()
)

@Serializable
private data class OpenMeteoHourly(
    val time: List<String> = emptyList(),
    @SerialName("temperature_2m") val temperatureCelsius: List<Double> = emptyList(),
    @SerialName("apparent_temperature") val apparentTemperatureCelsius: List<Double?> = emptyList(),
    @SerialName("relative_humidity_2m") val relativeHumidityPercent: List<Int?> = emptyList(),
    @SerialName("weather_code") val weatherCode: List<Int> = emptyList(),
    @SerialName("cloud_cover") val cloudCoverPercent: List<Int?> = emptyList(),
    @SerialName("precipitation") val precipitationMm: List<Double?> = emptyList(),
    @SerialName("precipitation_probability") val precipitationProbabilityPercent: List<Int?> = emptyList(),
    @SerialName("wind_speed_10m") val windSpeedKmh: List<Double> = emptyList(),
    @SerialName("wind_gusts_10m") val windGustKmh: List<Double?> = emptyList(),
    @SerialName("wind_direction_10m") val windDirectionDegrees: List<Double> = emptyList(),
    @SerialName("uv_index") val uvIndex: List<Double?> = emptyList(),
    @SerialName("visibility") val visibilityMeters: List<Double?> = emptyList(),
    @SerialName("is_day") val isDay: List<Int?> = emptyList()
)

@Serializable
private data class OpenMeteoAirQualityResponse(
    val hourly: OpenMeteoAirQualityHourly = OpenMeteoAirQualityHourly()
)

@Serializable
private data class OpenMeteoAirQualityHourly(
    val time: List<String> = emptyList(),
    @SerialName("european_aqi") val europeanAqi: List<Int?> = emptyList()
)

private const val WEATHER_CONNECT_TIMEOUT_MS = 8_000L
private const val WEATHER_REQUEST_TIMEOUT_MS = 15_000L
