package com.example.cyclistweather

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.example.cyclistweather.core.di.AppContainer
import com.example.cyclistweather.core.reminder.DEPARTURE_REMINDER_CHANNEL_ID

class CyclistWeatherApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        createDepartureReminderChannel()
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
