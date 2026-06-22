package com.example.cyclistweather.presentation.routeweather

import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DepartureTimeEditorTest {
    private val seoulTimeZone: TimeZone = TimeZone.getTimeZone("Asia/Seoul")

    @Test
    fun `changing date preserves local departure time`() {
        val current = localMillis(
            year = 2026,
            month = Calendar.JUNE,
            day = 6,
            hour = 8,
            minute = 30
        )
        val selectedDate = utcDateMillis(
            year = 2026,
            month = Calendar.JUNE,
            day = 7
        )

        val updated = DepartureTimeEditor.withDate(
            currentEpochMillis = current,
            selectedDateUtcMillis = selectedDate,
            timeZone = seoulTimeZone
        )

        assertLocalDateTime(
            epochMillis = updated,
            year = 2026,
            month = Calendar.JUNE,
            day = 7,
            hour = 8,
            minute = 30
        )
    }

    @Test
    fun `changing time preserves local departure date`() {
        val current = localMillis(
            year = 2026,
            month = Calendar.JUNE,
            day = 6,
            hour = 8,
            minute = 30
        )

        val updated = DepartureTimeEditor.withTime(
            currentEpochMillis = current,
            hourOfDay = 17,
            minute = 45,
            timeZone = seoulTimeZone
        )

        assertLocalDateTime(
            epochMillis = updated,
            year = 2026,
            month = Calendar.JUNE,
            day = 6,
            hour = 17,
            minute = 45
        )
    }

    @Test
    fun `time label includes selected minutes`() {
        val current = localMillis(
            year = 2026,
            month = Calendar.JUNE,
            day = 6,
            hour = 7,
            minute = 45
        )

        assertEquals("07:45", DepartureTimeEditor.formatTimeLabel(current, seoulTimeZone))
    }

    @Test
    fun `date picker selection uses utc midnight for local date`() {
        val current = localMillis(
            year = 2026,
            month = Calendar.JUNE,
            day = 6,
            hour = 8,
            minute = 30
        )

        val selectedDate = DepartureTimeEditor.datePickerSelectionMillis(
            currentEpochMillis = current,
            timeZone = seoulTimeZone
        )

        assertEquals(
            utcDateMillis(year = 2026, month = Calendar.JUNE, day = 6),
            selectedDate
        )
    }

    @Test
    fun `average speed parser accepts decimal and comma values`() {
        assertEquals(22.5, DepartureTimeEditor.parseAverageSpeedKmh("22.5")!!, 0.001)
        assertEquals(23.5, DepartureTimeEditor.parseAverageSpeedKmh("23,5")!!, 0.001)
    }

    @Test
    fun `average speed parser rejects invalid values`() {
        assertNull(DepartureTimeEditor.parseAverageSpeedKmh(""))
        assertNull(DepartureTimeEditor.parseAverageSpeedKmh("0"))
        assertNull(DepartureTimeEditor.parseAverageSpeedKmh("-12"))
        assertNull(DepartureTimeEditor.parseAverageSpeedKmh("fast"))
    }

    private fun localMillis(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int
    ): Long {
        return Calendar.getInstance(seoulTimeZone).apply {
            clear()
            set(year, month, day, hour, minute, 0)
        }.timeInMillis
    }

    private fun utcDateMillis(
        year: Int,
        month: Int,
        day: Int
    ): Long {
        return Calendar.getInstance(UTC).apply {
            clear()
            set(year, month, day, 0, 0, 0)
        }.timeInMillis
    }

    private fun assertLocalDateTime(
        epochMillis: Long,
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int
    ) {
        val calendar = Calendar.getInstance(seoulTimeZone).apply {
            timeInMillis = epochMillis
        }

        assertEquals(year, calendar.get(Calendar.YEAR))
        assertEquals(month, calendar.get(Calendar.MONTH))
        assertEquals(day, calendar.get(Calendar.DAY_OF_MONTH))
        assertEquals(hour, calendar.get(Calendar.HOUR_OF_DAY))
        assertEquals(minute, calendar.get(Calendar.MINUTE))
    }

    private companion object {
        val UTC: TimeZone = TimeZone.getTimeZone("UTC")
    }
}
