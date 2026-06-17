package com.example.cyclistweather.presentation.routeweather

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.cyclistweather.R
import com.example.cyclistweather.ui.theme.RideWeatherColors
import com.example.cyclistweather.ui.theme.RouteFirstPanel
import com.example.cyclistweather.ui.theme.SecondaryRouteButton
import com.example.cyclistweather.ui.theme.Spacing

@Composable
internal fun LoadingCard(message: String) {
    RouteFirstPanel {
        Row(
            modifier = Modifier.padding(Spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.dp
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = RideWeatherColors.TextPrimary
            )
        }
    }
}

@Composable
internal fun ErrorCard(
    title: String,
    message: String,
    onRetry: () -> Unit
) {
    RouteFirstPanel(containerColor = MaterialTheme.colorScheme.errorContainer) {
        Column(
            modifier = Modifier.padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            SecondaryRouteButton(text = stringResource(R.string.action_retry), onClick = onRetry)
        }
    }
}

@Composable
internal fun StatusCard(
    title: String,
    message: String
) {
    RouteFirstPanel(containerColor = RideWeatherColors.Sage.copy(alpha = 0.55f)) {
        Column(
            modifier = Modifier.padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = RideWeatherColors.TextPrimary
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = RideWeatherColors.TextSecondary
            )
        }
    }
}
