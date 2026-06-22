package com.example.cyclistweather.presentation.routeweather

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.example.cyclistweather.R
import com.example.cyclistweather.ui.theme.Radius
import com.example.cyclistweather.ui.theme.RideWeatherColors
import com.example.cyclistweather.ui.theme.SecondaryRouteButton
import com.example.cyclistweather.ui.theme.SkeletonBox
import com.example.cyclistweather.ui.theme.Spacing
import com.example.cyclistweather.ui.theme.rememberShimmerBrush

@Composable
internal fun BestDepartureContent(
    routeWeatherState: RouteWeatherLoadState,
    departureOptimizationState: DepartureOptimizationLoadState,
    selectedDepartureEpochMillis: Long,
    averageSpeedKmh: Double,
    onRetry: () -> Unit,
    onSetDepartureDate: (Long) -> Unit,
    onSetDepartureTime: (Int, Int) -> Unit,
    onSetAverageSpeed: (Double) -> Unit,
    onUseBestDeparture: (Long) -> Unit,
    onReoptimize: () -> Unit
) {
    when (routeWeatherState) {
        RouteWeatherLoadState.Idle -> StatusCard(
            title = stringResource(R.string.weather_not_loaded_title),
            message = stringResource(R.string.weather_not_loaded_message)
        )
        RouteWeatherLoadState.Loading -> DepartureLoadingSkeleton()
        is RouteWeatherLoadState.Error -> ErrorCard(
            title = stringResource(R.string.weather_unavailable_title),
            message = stringResource(routeWeatherState.messageRes),
            onRetry = onRetry
        )
        is RouteWeatherLoadState.Ready -> {
            val optimization = departureOptimizationState as? DepartureOptimizationLoadState.Ready
            val snapshot = routeWeatherState.snapshot
            val departure = selectedDepartureEpochMillis
            val isBest = optimization?.result?.bestCandidate?.let {
                DepartureTimeEditor.hourOfDay(it.departureEpochMillis) == DepartureTimeEditor.hourOfDay(departure)
            } ?: false

            var selectedTab by remember(snapshot.routeId, departure) {
                mutableStateOf(ConditionTab.SUMMARY)
            }
            val bestCandidate = optimization?.result?.bestCandidate
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                BestDepartureHero(
                    snapshot = snapshot,
                    departureEpochMillis = departure,
                    isBest = isBest
                )
                if (bestCandidate != null && !isBest) {
                    SecondaryRouteButton(
                        text = stringResource(
                            R.string.use_best_time,
                            DepartureTimeEditor.formatTimeLabel(bestCandidate.departureEpochMillis),
                            bestCandidate.snapshot.rideScore.total
                        ),
                        onClick = { onUseBestDeparture(bestCandidate.departureEpochMillis) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                DayStrip(
                    selectedDepartureEpochMillis = selectedDepartureEpochMillis,
                    onSelectDate = onSetDepartureDate
                )
                DepartureTimePickerPanel(
                    selectedDepartureEpochMillis = selectedDepartureEpochMillis,
                    snapshot = snapshot,
                    optimization = optimization?.result,
                    onSelectTime = onSetDepartureTime
                )
                AverageSpeedInput(
                    averageSpeedKmh = averageSpeedKmh,
                    onSetAverageSpeed = onSetAverageSpeed
                )
                DepartureWindowSection(
                    optimizationState = departureOptimizationState,
                    selectedDepartureEpochMillis = departure,
                    onSelectWindow = onUseBestDeparture,
                    onReoptimize = onReoptimize
                )
                ConditionsSection(
                    snapshot = snapshot,
                    selectedTab = selectedTab,
                    onSelectTab = { selectedTab = it }
                )
            }
        }
    }
}

/**
 * First-load placeholder for the departure planner. Mirrors the hero panel + summary lines so the
 * real analysis drops in without a layout jump. Reports a single "Analysing departure" description.
 */
@Composable
private fun DepartureLoadingSkeleton() {
    val brush = rememberShimmerBrush()
    val description = stringResource(R.string.analysing_departure)
    Column(
        modifier = Modifier.clearAndSetSemantics { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(Radius.lg),
            color = RideWeatherColors.Sage
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.lg),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SkeletonBox(
                    modifier = Modifier.size(56.dp),
                    shape = RoundedCornerShape(Radius.md),
                    brush = brush
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    SkeletonBox(modifier = Modifier.fillMaxWidth().height(18.dp), brush = brush)
                    SkeletonBox(modifier = Modifier.width(140.dp).height(12.dp), brush = brush)
                }
            }
        }
        SkeletonBox(modifier = Modifier.fillMaxWidth().height(44.dp), shape = RoundedCornerShape(Radius.md), brush = brush)
        SkeletonBox(modifier = Modifier.width(180.dp).height(14.dp), brush = brush)
    }
}
