package com.example.cyclistweather.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private val RideWeatherLightColorScheme = lightColorScheme(
    primary = RideWeatherColors.Forest,
    onPrimary = Color.White,
    primaryContainer = LightRideWeatherColors.sage,
    onPrimaryContainer = RideWeatherColors.ForestDark,
    secondary = RideWeatherColors.Chartreuse,
    onSecondary = RideWeatherColors.ForestDark,
    tertiary = RideWeatherColors.Caution,
    onTertiary = Color(0xFF221A00),
    background = LightRideWeatherColors.stone,
    onBackground = LightRideWeatherColors.textPrimary,
    surface = LightRideWeatherColors.surface,
    onSurface = LightRideWeatherColors.textPrimary,
    surfaceVariant = LightRideWeatherColors.sage,
    onSurfaceVariant = LightRideWeatherColors.textSecondary,
    outline = LightRideWeatherColors.border,
    error = RideWeatherColors.Dangerous,
    onError = Color.White,
    errorContainer = Color(0xFFFFE5E1),
    onErrorContainer = Color(0xFF7A201C)
)

private val RideWeatherDarkColorScheme = darkColorScheme(
    primary = RideWeatherColors.Chartreuse,
    onPrimary = RideWeatherColors.ForestDark,
    primaryContainer = Color(0xFF254A2D),
    onPrimaryContainer = Color(0xFFD7EBD2),
    secondary = RideWeatherColors.Chartreuse,
    onSecondary = RideWeatherColors.ForestDark,
    tertiary = RideWeatherColors.Caution,
    onTertiary = Color(0xFF221A00),
    background = DarkRideWeatherColors.stone,
    onBackground = DarkRideWeatherColors.textPrimary,
    surface = DarkRideWeatherColors.surface,
    onSurface = DarkRideWeatherColors.textPrimary,
    surfaceVariant = DarkRideWeatherColors.sage,
    onSurfaceVariant = DarkRideWeatherColors.textSecondary,
    outline = DarkRideWeatherColors.border,
    error = RideWeatherColors.Dangerous,
    onError = Color.White,
    errorContainer = Color(0xFF5C1B18),
    onErrorContainer = Color(0xFFFFD9D4)
)

@Composable
fun CyclistWeatherTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val palette = if (darkTheme) DarkRideWeatherColors else LightRideWeatherColors
    val colorScheme = if (darkTheme) RideWeatherDarkColorScheme else RideWeatherLightColorScheme
    CompositionLocalProvider(LocalRideWeatherColors provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = RideWeatherTypography,
            shapes = RideWeatherShapes,
            content = content
        )
    }
}
