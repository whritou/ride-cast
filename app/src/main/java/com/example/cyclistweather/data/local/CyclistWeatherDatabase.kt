package com.example.cyclistweather.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 2,
    exportSchema = false
)
abstract class CyclistWeatherDatabase : RoomDatabase() {
    abstract fun routeDao(): RouteDao
    abstract fun weatherForecastDao(): WeatherForecastDao

    companion object {
        @Volatile
        private var instance: CyclistWeatherDatabase? = null

        /** Adds the `isFavorite` flag to existing routes. A column add preserves all stored data,
         * so a destructive fallback (which would wipe the user's imported routes) is never used. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE routes ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getInstance(context: Context): CyclistWeatherDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    CyclistWeatherDatabase::class.java,
                    "cyclist-weather.db"
                ).addMigrations(MIGRATION_1_2).build().also { instance = it }
            }
        }
    }
}
