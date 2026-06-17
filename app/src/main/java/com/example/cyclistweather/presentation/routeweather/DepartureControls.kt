package com.example.cyclistweather.presentation.routeweather

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.cyclistweather.R
import com.example.cyclistweather.domain.model.RouteWeatherSnapshot
import com.example.cyclistweather.domain.optimizer.DepartureOptimizationResult
import com.example.cyclistweather.presentation.model.RideWeatherUiMapper
import com.example.cyclistweather.ui.theme.Radius
import com.example.cyclistweather.ui.theme.RideWeatherColors
import com.example.cyclistweather.ui.theme.ScoreColorUtils
import com.example.cyclistweather.ui.theme.Spacing

@Composable
internal fun DayStrip(
    selectedDepartureEpochMillis: Long,
    onSelectDate: (Long) -> Unit
) {
    val locale = LocalConfiguration.current.locales[0]
    val days = remember(selectedDepartureEpochMillis, locale) {
        dayChipModels(selectedDepartureEpochMillis, locale)
    }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        items(days, key = { it.label }) { day ->
            Surface(
                shape = RoundedCornerShape(Radius.md),
                color = if (day.isSelected) RideWeatherColors.Accent else RideWeatherColors.Surface,
                border = BorderStroke(1.dp, RideWeatherColors.Border),
                contentColor = if (day.isSelected) RideWeatherColors.OnAccent else RideWeatherColors.TextSecondary,
                modifier = Modifier
                    .heightIn(min = 44.dp)
                    .selectable(
                        selected = day.isSelected,
                        role = Role.RadioButton,
                        onClick = { onSelectDate(day.dateUtcMillis) }
                    )
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = day.label,
                        modifier = Modifier.padding(horizontal = Spacing.lg, vertical = 10.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (day.isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DepartureTimePickerPanel(
    selectedDepartureEpochMillis: Long,
    snapshot: RouteWeatherSnapshot,
    optimization: DepartureOptimizationResult?,
    onSelectTime: (Int, Int) -> Unit
) {
    val selectedHour = DepartureTimeEditor.hourOfDay(selectedDepartureEpochMillis)
    val selectedMinute = DepartureTimeEditor.minute(selectedDepartureEpochMillis)
    val qualityLevels = remember(snapshot, optimization) {
        optimization?.let(RideWeatherUiMapper::departureScenarios)?.map { it.riskLevel }
            ?: qualityLevelsFromSnapshot(snapshot)
    }
    var showPicker by remember { mutableStateOf(false) }
    // Resolve theme-aware quality hues in composition; the Canvas DrawScope below is not a
    // @Composable context, so it can't read the theme directly.
    val qualityColors = qualityLevels.map { ScoreColorUtils.themedColorForRisk(it) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Radius.lg),
        color = RideWeatherColors.Surface,
        border = BorderStroke(1.dp, RideWeatherColors.Border)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showPicker = true },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.AccessTime,
                        contentDescription = null,
                        tint = RideWeatherColors.Accent,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(R.string.departure_time),
                        style = MaterialTheme.typography.labelMedium,
                        color = RideWeatherColors.TextSecondary
                    )
                }
                Surface(
                    shape = RoundedCornerShape(Radius.md),
                    color = RideWeatherColors.Accent.copy(alpha = 0.10f),
                    border = BorderStroke(1.dp, RideWeatherColors.Accent.copy(alpha = 0.40f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = Spacing.sm + 2.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = DepartureTimeEditor.formatTimeLabel(selectedDepartureEpochMillis),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = RideWeatherColors.Accent
                        )
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = stringResource(R.string.departure_time),
                            tint = RideWeatherColors.Accent,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
            ) {
                if (qualityLevels.isEmpty()) return@Canvas
                val gap = 1.5.dp.toPx()
                val segW = (size.width - gap * (qualityLevels.size - 1)) / qualityLevels.size
                qualityLevels.forEachIndexed { index, _ ->
                    val isCurrentHour = qualityLevels.size == 24 && index == selectedHour
                    val baseColor = qualityColors[index]
                    drawRoundRect(
                        color = if (isCurrentHour) baseColor else baseColor.copy(alpha = 0.7f),
                        topLeft = Offset(index * (segW + gap), 1.dp.toPx()),
                        size = Size(segW, if (isCurrentHour) 13.dp.toPx() else 12.dp.toPx()),
                        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("00:00", style = MaterialTheme.typography.labelSmall, color = RideWeatherColors.TextSecondary)
                Text("06:00", style = MaterialTheme.typography.labelSmall, color = RideWeatherColors.TextSecondary)
                Text("12:00", style = MaterialTheme.typography.labelSmall, color = RideWeatherColors.TextSecondary)
                Text("18:00", style = MaterialTheme.typography.labelSmall, color = RideWeatherColors.TextSecondary)
                Text("23:00", style = MaterialTheme.typography.labelSmall, color = RideWeatherColors.TextSecondary)
            }
        }
    }

    if (showPicker) {
        val pickerState = rememberTimePickerState(
            initialHour = selectedHour,
            initialMinute = selectedMinute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        onSelectTime(pickerState.hour, pickerState.minute)
                        showPicker = false
                    }
                ) {
                    Text(stringResource(R.string.action_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
            text = { TimePicker(state = pickerState) }
        )
    }
}

@Composable
internal fun AverageSpeedInput(
    averageSpeedKmh: Double,
    onSetAverageSpeed: (Double) -> Unit
) {
    val focusManager = LocalFocusManager.current
    var text by remember(averageSpeedKmh) {
        mutableStateOf(DepartureTimeEditor.formatAverageSpeedInput(averageSpeedKmh))
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Radius.lg),
        color = RideWeatherColors.Surface,
        border = BorderStroke(1.dp, RideWeatherColors.Border)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Speed,
                contentDescription = null,
                tint = RideWeatherColors.Accent,
                modifier = Modifier.size(20.dp)
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(R.string.average_speed),
                    style = MaterialTheme.typography.labelMedium,
                    color = RideWeatherColors.TextSecondary
                )
                Text(
                    text = stringResource(R.string.average_speed_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = RideWeatherColors.TextSecondary
                )
            }
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.size(width = 88.dp, height = 52.dp),
                suffix = { Text(stringResource(R.string.unit_kmh), style = MaterialTheme.typography.labelSmall) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = {
                    DepartureTimeEditor.parseAverageSpeedKmh(text)?.let { onSetAverageSpeed(it) }
                    focusManager.clearFocus()
                }),
                textStyle = MaterialTheme.typography.bodyMedium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RideWeatherColors.Accent,
                    unfocusedBorderColor = RideWeatherColors.Border
                ),
                shape = RoundedCornerShape(Radius.md)
            )
        }
    }
}
