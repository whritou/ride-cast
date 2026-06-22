package com.example.cyclistweather.presentation.routeweather

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.cyclistweather.R
import com.example.cyclistweather.ui.theme.Radius
import com.example.cyclistweather.ui.theme.RideSegmentedControl
import com.example.cyclistweather.ui.theme.RideWeatherColors
import com.example.cyclistweather.ui.theme.RouteSectionHeader
import com.example.cyclistweather.ui.theme.Spacing

@Composable
internal fun ConditionsSection(
    snapshot: com.example.cyclistweather.domain.model.RouteWeatherSnapshot,
    selectedTab: ConditionTab,
    onSelectTab: (ConditionTab) -> Unit
) {
    val coursePoints = remember(snapshot) { detailCoursePoints(snapshot) }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        RouteSectionHeader(title = stringResource(R.string.conditions_section_title))
        ConditionTabs(selectedTab = selectedTab, onSelectTab = onSelectTab)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(Radius.lg),
            color = RideWeatherColors.Surface,
            border = BorderStroke(1.dp, RideWeatherColors.Border)
        ) {
            Column {
                coursePoints.forEachIndexed { index, point ->
                    ConditionRow(
                        point = point,
                        selectedTab = selectedTab,
                        isFirst = index == 0,
                        isLast = index == coursePoints.lastIndex
                    )
                    if (index < coursePoints.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 52.dp),
                            color = RideWeatherColors.Border,
                            thickness = 1.dp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConditionTabs(
    selectedTab: ConditionTab,
    onSelectTab: (ConditionTab) -> Unit
) {
    val tabs = ConditionTab.entries
    RideSegmentedControl(
        options = tabs.map { stringResource(it.labelRes) },
        selectedIndex = tabs.indexOf(selectedTab),
        onSelect = { index -> onSelectTab(tabs[index]) }
    )
}

@Composable
private fun ConditionRow(
    point: CoursePoint,
    selectedTab: ConditionTab,
    isFirst: Boolean,
    isLast: Boolean
) {
    val ui = point.ui
    val metrics = point.metrics(selectedTab)
    val pointLabel = when (point.position) {
        PointPosition.START -> stringResource(R.string.point_start)
        PointPosition.FINISH -> stringResource(R.string.point_finish)
        PointPosition.MID -> point.distanceLabel
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RouteTimelineMarker(isFirst = isFirst, isLast = isLast, riskLevel = ui.riskLevel)
        Column(
            modifier = Modifier.weight(1.2f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = pointLabel,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = RideWeatherColors.TextPrimary
            )
            Text(
                text = ui.distance,
                style = MaterialTheme.typography.labelSmall,
                color = RideWeatherColors.TextSecondary
            )
        }
        metrics.take(3).forEach { metric ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = metric.primary,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = RideWeatherColors.TextPrimary,
                    maxLines = 1
                )
                Text(
                    text = metric.secondary,
                    style = MaterialTheme.typography.labelSmall,
                    color = RideWeatherColors.TextSecondary,
                    maxLines = 1
                )
            }
        }
        RiskBadge(riskLevel = ui.riskLevel)
    }
}

@Composable
private fun CoursePoint.metrics(tab: ConditionTab): List<Metric> {
    val windArrow = stringResource(segment.windArrowLabelRes())
    return when (tab) {
        ConditionTab.SUMMARY -> listOf(
            Metric(windArrow, ui.wind),
            Metric(ui.temperature, stringResource(R.string.metric_temp)),
            Metric(ui.rain.substringBefore(" "), stringResource(R.string.metric_rain))
        )
        ConditionTab.TEMPERATURE -> listOf(
            Metric(ui.temperature, stringResource(R.string.metric_temp)),
            Metric(segment.weather.apparentTemperatureLabel(), stringResource(R.string.metric_feels)),
            Metric(stringResource(ui.conditionRes), stringResource(R.string.metric_sky))
        )
        ConditionTab.WIND -> listOf(
            Metric(ui.wind, stringResource(ui.relativeWindRes)),
            Metric(ui.gust, stringResource(R.string.metric_gusts)),
            Metric(windArrow, stringResource(R.string.metric_course))
        )
        ConditionTab.RAIN -> listOf(
            Metric(ui.rain.substringBefore(" "), stringResource(R.string.metric_rain)),
            Metric(segment.weather.precipitationMmLabel(), stringResource(R.string.metric_amount)),
            Metric(stringResource(ui.conditionRes), stringResource(R.string.metric_sky))
        )
    }
}
