package com.example.cyclistweather.core.reminder

import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import com.example.cyclistweather.core.common.RouteFormatters

/** Adds a departure reminder: a calendar event, plus (optionally) a 1h-before notification. */
object DepartureReminder {
    fun addReminder(
        context: Context,
        routeName: String,
        departureEpochMillis: Long,
        averageSpeedKmh: Double,
        distanceMeters: Double,
        scheduleNotification: Boolean
    ) {
        val durationMinutes = RouteFormatters.estimatedDurationMinutes(distanceMeters, averageSpeedKmh)
        val endMillis = departureEpochMillis + durationMinutes * 60_000L

        runCatching {
            val intent = Intent(Intent.ACTION_INSERT)
                .setData(CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.Events.TITLE, "Ride: $routeName")
                .putExtra(CalendarContract.Events.DESCRIPTION, "Planned ride — check RideCast for conditions.")
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, departureEpochMillis)
                .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }

        if (scheduleNotification) {
            DepartureReminderScheduler.schedule(context, routeName, departureEpochMillis)
        }
    }
}
