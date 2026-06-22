package com.example.cyclistweather.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.cyclistweather.data.route.RouteEntity
import com.example.cyclistweather.data.route.RoutePointEntity
import com.example.cyclistweather.data.route.RouteDao
import com.example.cyclistweather.data.weather.WeatherForecastDao
import com.example.cyclistweather.data.weather.WeatherForecastEntity

@Database(
    entities = [
        RouteEntity::class,
        RoutePointEntity::class,
        WeatherForecastEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class CyclistWeatherDatabase : RoomDatabase() {
    abstract fun routeDao(): RouteDao
    abstract fun weatherForecastDao(): WeatherForecastDao

    companion object {
        @Volatile
        private var instance: CyclistWeatherDatabase? = null

        fun getInstance(context: Context): CyclistWeatherDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    CyclistWeatherDatabase::class.java,
                    "cyclist-weather.db"
                ).build().also { instance = it }
            }
        }
    }
}
