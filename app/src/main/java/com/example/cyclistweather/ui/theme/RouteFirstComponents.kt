package com.example.cyclistweather.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.cyclistweather.R
import com.example.cyclistweather.presentation.model.WeatherRiskLevel

@Composable
fun RouteFirstPanel(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = containerColor,
        border = BorderStroke(1.dp, RideWeatherColors.Border),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        content = content
    )
}

@Composable
fun RouteSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = RideWeatherColors.TextPrimary
        )
        action?.invoke()
    }
}

@Composable
fun RouteMetricPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = RideWeatherColors.Surface,
        border = BorderStroke(1.dp, RideWeatherColors.Border)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.xxs)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = RideWeatherColors.TextSecondary
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                color = RideWeatherColors.TextPrimary
            )
        }
    }
}

@Composable
fun PrimaryRouteButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null
) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = Sizes.minTouchTarget),
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.md),
        colors = ButtonDefaults.buttonColors(
            containerColor = RideWeatherColors.Accent,
            contentColor = RideWeatherColors.OnAccent,
            disabledContainerColor = RideWeatherColors.Accent.copy(alpha = 0.38f),
            disabledContentColor = RideWeatherColors.OnAccent.copy(alpha = 0.75f)
        )
    ) {
        leadingIcon?.invoke()
        Text(text = text, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun SecondaryRouteButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = Sizes.minTouchTarget),
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.md),
        border = BorderStroke(1.dp, RideWeatherColors.Border),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = RideWeatherColors.Accent,
            disabledContentColor = RideWeatherColors.TextSecondary
        )
    ) {
        leadingIcon?.invoke()
        Text(text = text, fontWeight = FontWeight.SemiBold)
    }
}

/**
 * Square ride-score badge. Quality is shown with both a color tint and (optionally) a caption,
 * and the whole badge exposes a single spoken description so the meaning never depends on color
 * alone. Replaces the previously duplicated outlook badges.
 */
@Composable
fun RideScoreBadge(
    score: Int,
    modifier: Modifier = Modifier,
    caption: String? = null,
    size: Dp = 40.dp
) {
    val color = ScoreColorUtils.themedColorForScore(score)
    val description = stringResource(
        R.string.score_description,
        score,
        stringResource(WeatherRiskLevel.fromScore(score).labelRes)
    )
    Column(
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.xxs)
    ) {
        Surface(
            modifier = Modifier.heightIn(min = size).widthIn(min = size),
            shape = MaterialTheme.shapes.medium,
            color = color.copy(alpha = 0.16f),
            contentColor = color
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = score.toString(),
                    modifier = Modifier.padding(horizontal = Spacing.sm),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        if (caption != null) {
            Text(
                text = caption,
                style = MaterialTheme.typography.labelSmall,
                color = RideWeatherColors.TextSecondary
            )
        }
    }
}

/**
 * Accessible segmented control: a bordered row of equal-weight options behaving as a single
 * radio group. Each option meets the 48dp target and reports its selected state to assistive
 * tech. Shared by the appearance picker and the conditions tabs.
 */
@Composable
fun RideSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, RideWeatherColors.Border, MaterialTheme.shapes.medium)
            .padding(Spacing.xxs)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xxs)
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp)
                    .selectable(
                        selected = selected,
                        onClick = { onSelect(index) },
                        role = Role.RadioButton
                    ),
                shape = MaterialTheme.shapes.small,
                color = if (selected) RideWeatherColors.Accent else Color.Transparent,
                contentColor = if (selected) RideWeatherColors.OnAccent else RideWeatherColors.TextSecondary
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = label,
                        modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.md),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
