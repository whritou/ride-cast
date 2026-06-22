package com.example.cyclistweather.presentation.routeweather

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.cyclistweather.R
import com.example.cyclistweather.domain.model.RouteWeatherSnapshot
import com.example.cyclistweather.presentation.model.RideWeatherUiMapper
import com.example.cyclistweather.ui.theme.Radius
import com.example.cyclistweather.ui.theme.RideWeatherColors
import com.example.cyclistweather.ui.theme.ScoreColorUtils
import com.example.cyclistweather.ui.theme.Spacing
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date

@Composable
internal fun BestDepartureHero(
    snapshot: RouteWeatherSnapshot,
    departureEpochMillis: Long,
    isBest: Boolean
) {
    val score = RideWeatherUiMapper.rideScore(snapshot.rideScore)
    val locale = LocalConfiguration.current.locales[0]
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Radius.lg),
        color = RideWeatherColors.Sage,
        border = BorderStroke(1.dp, RideWeatherColors.Border)
    ) {
        Row(
            modifier = Modifier.padding(Spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(Radius.pill),
                color = RideWeatherColors.Surface,
                contentColor = RideWeatherColors.Caution
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.WbSunny,
                        contentDescription = null,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(if (isBest) R.string.departure_best else R.string.departure_selected),
                    style = MaterialTheme.typography.labelMedium,
                    color = RideWeatherColors.TextSecondary
                )
                Text(
                    text = formatHeroDate(departureEpochMillis, locale),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = RideWeatherColors.TextPrimary
                )
                Text(
                    text = relativeDepartureLabel(departureEpochMillis),
                    style = MaterialTheme.typography.labelMedium,
                    color = RideWeatherColors.TextSecondary
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = score.score.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = ScoreColorUtils.themedColorForRisk(score.riskLevel)
                )
                Text(
                    text = stringResource(score.riskLevel.labelRes),
                    style = MaterialTheme.typography.labelMedium,
                    color = RideWeatherColors.TextPrimary
                )
            }
        }
    }
}

/** Localized "Today / Tomorrow / In N days / MMM d" relative label for a departure time. */
@Composable
internal fun relativeDepartureLabel(departureEpochMillis: Long): String {
    val today = Calendar.getInstance()
    val departure = Calendar.getInstance().apply { timeInMillis = departureEpochMillis }
    val yearDelta = departure.get(Calendar.YEAR) - today.get(Calendar.YEAR)
    val dayDelta = departure.get(Calendar.DAY_OF_YEAR) - today.get(Calendar.DAY_OF_YEAR) + yearDelta * 365
    val locale = LocalConfiguration.current.locales[0]
    return when (dayDelta) {
        0 -> stringResource(R.string.when_today)
        1 -> stringResource(R.string.when_tomorrow)
        in 2..30 -> stringResource(R.string.when_in_days, dayDelta)
        else -> SimpleDateFormat("MMM d", locale).format(Date(departureEpochMillis))
    }
}
