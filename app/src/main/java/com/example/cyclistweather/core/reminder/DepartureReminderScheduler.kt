package com.example.cyclistweather.core.reminder

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

const val DEPARTURE_REMINDER_CHANNEL_ID = "departure_reminder"

/** Schedules a one-shot notification one hour before a chosen departure time via WorkManager. */
object DepartureReminderScheduler {
    private const val LEAD_MILLIS = 60 * 60 * 1000L
    internal const val KEY_ROUTE = "routeName"

    fun schedule(context: Context, routeName: String, departureEpochMillis: Long) {
        val delayMillis = departureEpochMillis - LEAD_MILLIS - System.currentTimeMillis()
        if (delayMillis <= 0L) return // departure is too soon to schedule a 1h-before reminder.

        val request = OneTimeWorkRequestBuilder<DepartureReminderWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(KEY_ROUTE to routeName))
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "departure-reminder-$departureEpochMillis",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}

class DepartureReminderWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {
    override fun doWork(): Result {
        val routeName = inputData.getString(DepartureReminderScheduler.KEY_ROUTE) ?: "Your ride"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return Result.success() // permission revoked since scheduling — nothing to post.
        }

        val notification = NotificationCompat.Builder(applicationContext, DEPARTURE_REMINDER_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Ride in 1 hour")
            .setContentText("$routeName departs soon — check the latest conditions.")
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(applicationContext)
            .notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notification)
        return Result.success()
    }
}
