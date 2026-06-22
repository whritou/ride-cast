package com.example.cyclistweather.data.weather

import com.example.cyclistweather.data.local.CyclistWeatherDatabase

class RoomWeatherCacheStore(
    private val weatherForecastDao: WeatherForecastDao
) : WeatherCacheStore {
    constructor(database: CyclistWeatherDatabase) : this(database.weatherForecastDao())

    override suspend fun getForecast(cacheKey: String): CachedWeatherForecast? {
        val entity = weatherForecastDao.getForecast(cacheKey) ?: return null
        val points = WeatherPointJsonCodec.decode(entity.payloadJson)
        if (points.isEmpty()) {
            return null
        }

        return CachedWeatherForecast(
            points = points,
            fetchedAtEpochMillis = entity.fetchedAtEpochMillis
        )
    }

    override suspend fun putForecast(
        cacheKey: String,
        latitude: Double,
        longitude: Double,
        forecast: CachedWeatherForecast
    ) {
        weatherForecastDao.upsertForecast(
            WeatherForecastEntity(
                cacheKey = cacheKey,
                latitude = latitude,
                longitude = longitude,
                fetchedAtEpochMillis = forecast.fetchedAtEpochMillis,
                payloadJson = WeatherPointJsonCodec.encode(forecast.points)
            )
        )
    }
}
