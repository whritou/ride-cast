package com.example.cyclistweather.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.cyclistweather.ui.theme.RideWeatherColors
import com.example.cyclistweather.ui.theme.RouteFirstPanel
import com.example.cyclistweather.ui.theme.Spacing

/**
 * Boxed labeled stat tile (icon + value + text label) used in the 2-column conditions grid.
 * Distinct from [IconStat], which is the compact icon-over-value cell in the summary row.
 */
@Composable
fun StatTile(
    icon: ImageVector,
    tint: Color,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    RouteFirstPanel(modifier = modifier) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm + 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = RideWeatherColors.TextPrimary,
                    maxLines = 1
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = RideWeatherColors.TextSecondary,
                    maxLines = 1
                )
            }
        }
    }
}
