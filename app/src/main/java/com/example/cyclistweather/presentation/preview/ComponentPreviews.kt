package com.example.cyclistweather.presentation.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.example.cyclistweather.domain.model.HazardKind
import com.example.cyclistweather.domain.model.HazardSeverity
import com.example.cyclistweather.domain.model.RideHazard
import com.example.cyclistweather.presentation.components.IconStat
import com.example.cyclistweather.presentation.routeweather.RideHazardBanners
import com.example.cyclistweather.ui.theme.CyclistWeatherTheme
import com.example.cyclistweather.ui.theme.RideWeatherColors
import com.example.cyclistweather.ui.theme.Spacing

@PreviewLightDark
@Composable
private fun IconStatRowPreview() {
    CyclistWeatherTheme {
        Row(
            modifier = Modifier
                .background(RideWeatherColors.Stone)
                .fillMaxWidth()
                .padding(Spacing.lg),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IconStat(Icons.Filled.Air, RideWeatherColors.Sky, "12 km/h", "SW Wind")
            IconStat(Icons.Filled.Thermostat, RideWeatherColors.Caution, "18°C", "Temp")
            IconStat(Icons.Filled.WaterDrop, RideWeatherColors.Sky, "20%", "Rain")
        }
    }
}

@PreviewLightDark
@Composable
private fun RideHazardBannersPreview() {
    CyclistWeatherTheme {
        RideHazardBanners(
            hazards = listOf(
                RideHazard(HazardSeverity.DANGER, HazardKind.THUNDERSTORM, distanceLabel = "12 km"),
                RideHazard(HazardSeverity.WARNING, HazardKind.STRONG_GUSTS, distanceLabel = "5 km", value = 48)
            ),
            modifier = Modifier
                .background(RideWeatherColors.Stone)
                .padding(Spacing.lg)
        )
    }
}
