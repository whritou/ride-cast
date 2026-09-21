package com.example.cyclistweather

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import com.example.cyclistweather.core.di.AppContainer
import com.example.cyclistweather.core.reminder.DEPARTURE_REMINDER_CHANNEL_ID
import okhttp3.OkHttpClient
import org.maplibre.android.MapLibre
import org.maplibre.android.WellKnownTileServer
import org.maplibre.android.module.http.HttpRequestUtil

class CyclistWeatherApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        try {
            // Initialize MapLibre with explicit tile server choice to satisfy the validator.
            MapLibre.getInstance(this, null, WellKnownTileServer.MapLibre)
            // Then configure the custom OkHttpClient for OSM policy compliance.
            setupMapLibreHttp()
        } catch (e: Throwable) {
            Log.e("CyclistWeatherApp", "Failed to initialize MapLibre", e)
        }
        container = AppContainer(this)
        createDepartureReminderChannel()
    }

    private fun setupMapLibreHttp() {
        // OSM Tile Usage Policy requires a valid User-Agent and sometimes Referer.
        // https://operations.osmfoundation.org/policies/tiles/
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "RideCast/1.0 (com.example.cyclistweather)")
                    // Some OSM blocks require a Referer header to identify the traffic source.
                    .header("Referer", "android-app://com.example.cyclistweather")
                    .build()
                chain.proceed(request)
            }
            .build()
        HttpRequestUtil.setOkHttpClient(client)
    }

    private fun createDepartureReminderChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            DEPARTURE_REMINDER_CHANNEL_ID,
            "Departure reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Reminds you about an upcoming ride departure."
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
