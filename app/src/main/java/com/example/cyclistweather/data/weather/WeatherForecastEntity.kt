package com.example.cyclistweather.data.weather

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weather_forecasts")
data class WeatherForecastEntity(
    @PrimaryKey val cacheKey: String,
    val latitude: Double,
    val longitude: Double,
    val fetchedAtEpochMillis: Long,
    val payloadJson: String
)
