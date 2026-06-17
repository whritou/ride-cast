package com.example.cyclistweather.presentation.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.cyclistweather.R
import com.example.cyclistweather.domain.model.AppLanguage
import com.example.cyclistweather.domain.model.ThemeMode
import com.example.cyclistweather.ui.theme.RideSegmentedControl
import com.example.cyclistweather.ui.theme.RideWeatherColors
import com.example.cyclistweather.ui.theme.RouteFirstPanel
import com.example.cyclistweather.ui.theme.Sizes
import com.example.cyclistweather.ui.theme.Spacing
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    averageSpeedKmh: Double,
    themeMode: ThemeMode,
    language: AppLanguage,
    notificationsEnabled: Boolean,
    onSetAverageSpeed: (Double) -> Unit,
    onSetThemeMode: (ThemeMode) -> Unit,
    onSetLanguage: (AppLanguage) -> Unit,
    onSetNotificationsEnabled: (Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        containerColor = RideWeatherColors.Stone,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = RideWeatherColors.Stone,
                    titleContentColor = RideWeatherColors.TextPrimary,
                    navigationIconContentColor = RideWeatherColors.Accent
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.xl, vertical = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            AverageSpeedSetting(averageSpeedKmh = averageSpeedKmh, onSetAverageSpeed = onSetAverageSpeed)
            ThemeSetting(themeMode = themeMode, onSetThemeMode = onSetThemeMode)
            LanguageSetting(language = language, onSetLanguage = onSetLanguage)
            NotificationSetting(enabled = notificationsEnabled, onSetEnabled = onSetNotificationsEnabled)
            AboutSection()
        }
    }
}

@Composable
private fun SettingSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = RideWeatherColors.TextPrimary
        )
        RouteFirstPanel {
            Column(modifier = Modifier.padding(Spacing.lg)) { content() }
        }
    }
}

@Composable
private fun AverageSpeedSetting(
    averageSpeedKmh: Double,
    onSetAverageSpeed: (Double) -> Unit
) {
    var value by remember(averageSpeedKmh) { mutableFloatStateOf(averageSpeedKmh.toFloat()) }
    SettingSection(title = stringResource(R.string.settings_average_speed)) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text(
                text = stringResource(R.string.speed_kmh_value, value.roundToInt()),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = RideWeatherColors.Accent
            )
            Text(
                text = stringResource(R.string.settings_speed_hint),
                style = MaterialTheme.typography.bodySmall,
                color = RideWeatherColors.TextSecondary
            )
            Slider(
                value = value,
                onValueChange = { value = it },
                onValueChangeFinished = { onSetAverageSpeed(value.roundToInt().toDouble()) },
                valueRange = 5f..60f
            )
        }
    }
}

@Composable
private fun ThemeSetting(
    themeMode: ThemeMode,
    onSetThemeMode: (ThemeMode) -> Unit
) {
    val modes = ThemeMode.entries
    SettingSection(title = stringResource(R.string.settings_appearance)) {
        RideSegmentedControl(
            options = modes.map { stringResource(it.labelRes) },
            selectedIndex = modes.indexOf(themeMode).coerceAtLeast(0),
            onSelect = { index -> onSetThemeMode(modes[index]) }
        )
    }
}

@Composable
private fun LanguageSetting(
    language: AppLanguage,
    onSetLanguage: (AppLanguage) -> Unit
) {
    SettingSection(title = stringResource(R.string.settings_language)) {
        Column(modifier = Modifier.selectableGroup()) {
            AppLanguage.entries.forEach { option ->
                val selected = option == language
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = Sizes.minTouchTarget)
                        .selectable(
                            selected = selected,
                            role = Role.RadioButton,
                            onClick = { onSetLanguage(option) }
                        ),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = selected, onClick = null)
                    Text(
                        text = languageLabel(option),
                        style = MaterialTheme.typography.bodyLarge,
                        color = RideWeatherColors.TextPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun languageLabel(language: AppLanguage): String = when (language) {
    AppLanguage.SYSTEM -> stringResource(R.string.language_system)
    // Endonyms — a language is always shown in its own name, not translated.
    AppLanguage.ENGLISH -> "English"
    AppLanguage.FRENCH -> "Français"
    AppLanguage.GERMAN -> "Deutsch"
    AppLanguage.SPANISH -> "Español"
}

@Composable
private fun NotificationSetting(
    enabled: Boolean,
    onSetEnabled: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> onSetEnabled(granted) }

    SettingSection(title = stringResource(R.string.settings_reminders)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(R.string.settings_reminders_title),
                    style = MaterialTheme.typography.bodyMedium,
                    color = RideWeatherColors.TextPrimary
                )
                Text(
                    text = stringResource(R.string.settings_reminders_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = RideWeatherColors.TextSecondary
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = { checked ->
                    if (!checked) {
                        onSetEnabled(false)
                        return@Switch
                    }
                    val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
                        PackageManager.PERMISSION_GRANTED
                    if (needsPermission) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        onSetEnabled(true)
                    }
                }
            )
        }
    }
}

@Composable
private fun AboutSection() {
    val context = LocalContext.current
    val version = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "1.0"
    }
    SettingSection(title = stringResource(R.string.settings_about)) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = RideWeatherColors.TextPrimary
            )
            Text(
                text = stringResource(R.string.settings_version, version),
                style = MaterialTheme.typography.bodySmall,
                color = RideWeatherColors.TextSecondary
            )
            Text(
                text = stringResource(R.string.settings_about_blurb),
                style = MaterialTheme.typography.bodySmall,
                color = RideWeatherColors.TextSecondary
            )
        }
    }
}
