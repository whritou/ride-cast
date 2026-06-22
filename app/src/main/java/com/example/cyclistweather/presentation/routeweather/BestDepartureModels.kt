package com.example.cyclistweather.presentation.routeweather

import androidx.annotation.StringRes
import com.example.cyclistweather.R
import com.example.cyclistweather.core.common.RouteFormatters
import com.example.cyclistweather.domain.model.RouteWeatherSnapshot
import com.example.cyclistweather.domain.model.SegmentWeather
import com.example.cyclistweather.domain.model.WeatherPoint
import com.example.cyclistweather.presentation.model.RideWeatherUiMapper
import com.example.cyclistweather.presentation.model.WeatherPointUiModel
import com.example.cyclistweather.presentation.model.WeatherRiskLevel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToInt

internal enum class ConditionTab(@StringRes val labelRes: Int) {
    SUMMARY(R.string.tab_summary),
    TEMPERATURE(R.string.tab_temp),
    WIND(R.string.tab_wind),
    RAIN(R.string.tab_rain)
}

internal data class DayChipModel(
    val label: String,
    val isSelected: Boolean,
    val dateUtcMillis: Long
)

internal enum class PointPosition { START, MID, FINISH }

internal data class CoursePoint(
    val position: PointPosition,
    val distanceLabel: String,
    val ui: WeatherPointUiModel,
    val segment: SegmentWeather
)

internal data class Metric(val primary: String, val secondary: String)

internal fun detailCoursePoints(snapshot: RouteWeatherSnapshot): List<CoursePoint> {
    val segments = snapshot.segmentWeather
    if (segments.isEmpty()) return emptyList()
    val stepCount = 8
    val indexes = (0 until stepCount).map { step ->
        (step * segments.lastIndex / (stepCount - 1)).coerceIn(0, segments.lastIndex)
    }.distinct()
    val selected = indexes.map { segments[it] }
    return selected.mapIndexed { index, segment ->
        val position = when (index) {
            0 -> PointPosition.START
            selected.lastIndex -> PointPosition.FINISH
            else -> PointPosition.MID
        }
        CoursePoint(
            position = position,
            distanceLabel = RouteFormatters.formatDistance(segment.sample.distanceFromStartMeters),
            ui = RideWeatherUiMapper.weatherPoint(segment),
            segment = segment
        )
    }
}

internal fun dayChipModels(departureEpochMillis: Long, locale: Locale): List<DayChipModel> {
    val labelFormat = SimpleDateFormat("EEE d", locale)
    val utc = TimeZone.getTimeZone("UTC")
    return (-1..3).map { offset ->
        val day = Calendar.getInstance().apply {
            timeInMillis = departureEpochMillis
            add(Calendar.DAY_OF_YEAR, offset)
        }
        val dateUtcMillis = Calendar.getInstance(utc).apply {
            clear()
            set(day.get(Calendar.YEAR), day.get(Calendar.MONTH), day.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
        }.timeInMillis
        DayChipModel(
            label = labelFormat.format(Date(day.timeInMillis)),
            isSelected = offset == 0,
            dateUtcMillis = dateUtcMillis
        )
    }
}

internal fun qualityLevelsFromSnapshot(snapshot: RouteWeatherSnapshot): List<WeatherRiskLevel> {
    val segments = snapshot.segmentWeather
    if (segments.isEmpty()) return listOf(WeatherRiskLevel.GOOD)
    val bucketCount = 24.coerceAtMost(segments.size).coerceAtLeast(1)
    return List(bucketCount) { bucket ->
        val start = bucket * segments.size / bucketCount
        val end = ((bucket + 1) * segments.size / bucketCount).coerceAtMost(segments.size)
        val averageScore = segments.subList(start, end).map { it.score.total }.average().roundToInt()
        WeatherRiskLevel.fromScore(averageScore)
    }
}

internal fun formatHeroDate(epochMillis: Long, locale: Locale): String {
    return SimpleDateFormat("EEE, HH:mm", locale).format(Date(epochMillis))
}

@StringRes
internal fun SegmentWeather.windArrowLabelRes(): Int = when {
    windComponents.headwindKmh > 2.0 -> R.string.wind_headwind
    windComponents.headwindKmh < -2.0 -> R.string.wind_tailwind
    else -> R.string.wind_crosswind
}

internal fun WeatherPoint.apparentTemperatureLabel(): String =
    apparentTemperatureCelsius?.let(RouteFormatters::formatTemperature)
        ?: RouteFormatters.formatTemperature(temperatureCelsius)

internal fun WeatherPoint.precipitationMmLabel(): String =
    precipitationMm?.let { "${it.roundToInt()} mm" } ?: "0 mm"
