package com.example.cyclistweather.presentation.routeweather

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.cyclistweather.R
import com.example.cyclistweather.domain.optimizer.DepartureOptimizationResult
import com.example.cyclistweather.domain.optimizer.RecommendationReason
import com.example.cyclistweather.domain.optimizer.RecommendationReasonKind
import com.example.cyclistweather.presentation.model.DepartureScenarioUiModel
import com.example.cyclistweather.presentation.model.RideWeatherUiMapper
import com.example.cyclistweather.ui.theme.Radius
import com.example.cyclistweather.ui.theme.RideScoreBadge
import com.example.cyclistweather.ui.theme.RideWeatherColors
import com.example.cyclistweather.ui.theme.RouteSectionHeader
import com.example.cyclistweather.ui.theme.SecondaryRouteButton
import com.example.cyclistweather.ui.theme.Spacing

/**
 * Surfaces the departure optimizer's full output: a "why this window" explanation plus a ranked,
 * selectable comparison of every scanned window. While the analysis is idle or failed it offers an
 * explicit "find the best window" affordance — the optimizer's pick is always offered, never forced.
 */
@Composable
internal fun DepartureWindowSection(
    optimizationState: DepartureOptimizationLoadState,
    selectedDepartureEpochMillis: Long,
    onSelectWindow: (Long) -> Unit,
    onReoptimize: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        when (optimizationState) {
            is DepartureOptimizationLoadState.Ready -> {
                RecommendationReasons(optimizationState.result.reasons)
                WindowComparison(
                    result = optimizationState.result,
                    selectedDepartureEpochMillis = selectedDepartureEpochMillis,
                    onSelectWindow = onSelectWindow
                )
            }
            DepartureOptimizationLoadState.Loading -> Text(
                text = stringResource(R.string.departure_finding_best),
                style = MaterialTheme.typography.bodyMedium,
                color = RideWeatherColors.TextSecondary
            )
            is DepartureOptimizationLoadState.Error,
            DepartureOptimizationLoadState.Idle -> SecondaryRouteButton(
                text = stringResource(R.string.departure_find_best),
                onClick = onReoptimize,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun RecommendationReasons(reasons: List<RecommendationReason>) {
    if (reasons.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        RouteSectionHeader(title = stringResource(R.string.departure_why_title))
        reasons.forEach { reason ->
            Text(
                text = reasonText(reason),
                style = MaterialTheme.typography.bodyMedium,
                color = RideWeatherColors.TextSecondary
            )
        }
    }
}

@Composable
private fun WindowComparison(
    result: DepartureOptimizationResult,
    selectedDepartureEpochMillis: Long,
    onSelectWindow: (Long) -> Unit
) {
    val scenarios = remember(result) { RideWeatherUiMapper.departureScenarios(result) }
    if (scenarios.size <= 1) return
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        RouteSectionHeader(title = stringResource(R.string.departure_compare_title))
        scenarios.forEach { scenario ->
            WindowRow(
                scenario = scenario,
                isSelected = scenario.departureEpochMillis == selectedDepartureEpochMillis,
                onClick = { onSelectWindow(scenario.departureEpochMillis) }
            )
        }
    }
}

@Composable
private fun WindowRow(
    scenario: DepartureScenarioUiModel,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) RideWeatherColors.Accent else RideWeatherColors.Border
    Surface(
        shape = RoundedCornerShape(Radius.md),
        color = RideWeatherColors.Surface,
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .selectable(selected = isSelected, role = Role.RadioButton, onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RideScoreBadge(score = scenario.score, size = 36.dp)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = scenario.departureTime,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = RideWeatherColors.TextPrimary
                )
                Text(
                    text = stringResource(scenario.riskLevel.labelRes),
                    style = MaterialTheme.typography.labelMedium,
                    color = RideWeatherColors.TextSecondary
                )
            }
            val markerRes = when {
                scenario.isBest -> R.string.departure_marker_best
                scenario.isCurrent -> R.string.departure_marker_current
                else -> null
            }
            if (markerRes != null) {
                WindowMarker(textRes = markerRes, highlighted = scenario.isBest)
            }
        }
    }
}

@Composable
private fun WindowMarker(@StringRes textRes: Int, highlighted: Boolean) {
    val accent = if (highlighted) RideWeatherColors.Accent else RideWeatherColors.TextSecondary
    Surface(
        shape = RoundedCornerShape(Radius.sm),
        color = accent.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.40f))
    ) {
        Text(
            text = stringResource(textRes),
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = accent
        )
    }
}

@StringRes
private fun recommendationReasonRes(kind: RecommendationReasonKind): Int = when (kind) {
    RecommendationReasonKind.ALREADY_BEST -> R.string.reason_already_best
    RecommendationReasonKind.SCORE_GAIN -> R.string.reason_score_gain
    RecommendationReasonKind.LESS_RAIN -> R.string.reason_less_rain
    RecommendationReasonKind.LESS_HEADWIND -> R.string.reason_less_headwind
    RecommendationReasonKind.BETTER_TEMP -> R.string.reason_better_temp
    RecommendationReasonKind.BEST_OVERALL -> R.string.reason_best_overall
}

@Composable
private fun reasonText(reason: RecommendationReason): String {
    val res = recommendationReasonRes(reason.kind)
    return if (reason.value != null) stringResource(res, reason.value) else stringResource(res)
}
