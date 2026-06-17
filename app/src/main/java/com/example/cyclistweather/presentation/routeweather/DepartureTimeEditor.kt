package com.example.cyclistweather.presentation.routeweather

import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

internal object DepartureTimeEditor {
    fun withDate(
        currentEpochMillis: Long,
        selectedDateUtcMillis: Long,
        timeZone: TimeZone = TimeZone.getDefault()
    ): Long {
        val selectedDate = Calendar.getInstance(UTC).apply {
            timeInMillis = selectedDateUtcMillis
        }
        return Calendar.getInstance(timeZone).apply {
            timeInMillis = currentEpochMillis
            set(Calendar.YEAR, selectedDate.get(Calendar.YEAR))
            set(Calendar.MONTH, selectedDate.get(Calendar.MONTH))
            set(Calendar.DAY_OF_MONTH, selectedDate.get(Calendar.DAY_OF_MONTH))
        }.timeInMillis
    }

    fun withTime(
        currentEpochMillis: Long,
        hourOfDay: Int,
        minute: Int,
        timeZone: TimeZone = TimeZone.getDefault()
    ): Long {
        return Calendar.getInstance(timeZone).apply {
            timeInMillis = currentEpochMillis
            set(Calendar.HOUR_OF_DAY, hourOfDay.coerceIn(0, 23))
            set(Calendar.MINUTE, minute.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    fun datePickerSelectionMillis(
        currentEpochMillis: Long,
        timeZone: TimeZone = TimeZone.getDefault()
    ): Long {
        val current = Calendar.getInstance(timeZone).apply {
            timeInMillis = currentEpochMillis
        }
        return Calendar.getInstance(UTC).apply {
            clear()
            set(
                current.get(Calendar.YEAR),
                current.get(Calendar.MONTH),
                current.get(Calendar.DAY_OF_MONTH),
                0,
                0,
                0
            )
        }.timeInMillis
    }

    fun hourOfDay(
        epochMillis: Long,
        timeZone: TimeZone = TimeZone.getDefault()
    ): Int {
        return Calendar.getInstance(timeZone).apply {
            timeInMillis = epochMillis
        }.get(Calendar.HOUR_OF_DAY)
    }

    fun minute(
        epochMillis: Long,
        timeZone: TimeZone = TimeZone.getDefault()
    ): Int {
        return Calendar.getInstance(timeZone).apply {
            timeInMillis = epochMillis
        }.get(Calendar.MINUTE)
    }

    fun formatTimeLabel(
        epochMillis: Long,
        timeZone: TimeZone = TimeZone.getDefault()
    ): String {
        return "%02d:%02d".format(
            hourOfDay(epochMillis, timeZone),
            minute(epochMillis, timeZone)
        )
    }

    fun parseAverageSpeedKmh(input: String): Double? {
        val normalized = input.trim().replace(',', '.')
        return normalized
            .toDoubleOrNull()
            ?.takeIf { it > 0.0 && it <= MAX_REASONABLE_AVERAGE_SPEED_KMH }
    }

    fun formatAverageSpeedInput(value: Double): String {
        return if (value % 1.0 == 0.0) {
            value.toInt().toString()
        } else {
            String.format(Locale.US, "%.1f", value)
        }
    }

    private const val MAX_REASONABLE_AVERAGE_SPEED_KMH = 80.0
    private val UTC: TimeZone = TimeZone.getTimeZone("UTC")
}
