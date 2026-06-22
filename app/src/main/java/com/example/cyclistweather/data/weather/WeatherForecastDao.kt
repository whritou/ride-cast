package com.example.cyclistweather.data.weather

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface WeatherForecastDao {
    @Query("SELECT * FROM weather_forecasts WHERE cacheKey = :cacheKey")
    suspend fun getForecast(cacheKey: String): WeatherForecastEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertForecast(forecast: WeatherForecastEntity)

    @Query("DELETE FROM weather_forecasts WHERE fetchedAtEpochMillis < :cutoffEpochMillis")
    suspend fun deleteOlderThan(cutoffEpochMillis: Long)
}
