package com.example.cyclistweather.presentation.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.example.cyclistweather.R
import com.example.cyclistweather.ui.theme.Spacing
import kotlin.math.roundToInt

/**
 * One-time prompt shown on first launch so a new rider sets their average speed (used to estimate
 * arrival times along a route). The choice is persisted; afterwards speed is changed only in
 * Settings. Dismissing accepts the suggested default, so the prompt never reappears.
 */
@Composable
internal fun FirstRunSpeedDialog(
    initialSpeedKmh: Double,
    onSave: (Double) -> Unit
) {
    var text by remember { mutableStateOf(initialSpeedKmh.roundToInt().toString()) }
    fun resolvedSpeed() = text.trim().toDoubleOrNull()?.takeIf { it > 0.0 } ?: initialSpeedKmh
    AlertDialog(
        onDismissRequest = { onSave(initialSpeedKmh) },
        title = { Text(stringResource(R.string.first_run_speed_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                Text(stringResource(R.string.first_run_speed_message))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.average_speed)) },
                    suffix = { Text(stringResource(R.string.unit_kmh)) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done
                    )
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(resolvedSpeed()) }) {
                Text(stringResource(R.string.action_ok))
            }
        }
    )
}
