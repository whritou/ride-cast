package com.example.cyclistweather.presentation.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.cyclistweather.R
import com.example.cyclistweather.ui.theme.RideWeatherColors

internal const val MAP_CONTROL_ANIMATION_MS = 260
private const val MATERIAL_ICON_VIEWPORT = 24f
private val MAP_CONTROL_BUTTON_SIZE = 36.dp
private val MAP_CONTROL_ICON_SIZE = 18.dp
private val MAP_CONTROL_SPACING = 4.dp

@Composable
internal fun MapNavigationControls(
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onCenterRoute: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MAP_CONTROL_SPACING)
    ) {
        MapControlButton(icon = MapControlIcons.Add, contentDescription = stringResource(R.string.map_zoom_in), onClick = onZoomIn)
        MapControlButton(icon = MapControlIcons.Center, contentDescription = stringResource(R.string.map_center_course), onClick = onCenterRoute)
        MapControlButton(icon = MapControlIcons.Remove, contentDescription = stringResource(R.string.map_zoom_out), onClick = onZoomOut)
    }
}

@Composable
internal fun MapControlButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(7.dp),
        color = RideWeatherColors.Surface.copy(alpha = 0.96f),
        tonalElevation = 0.dp
    ) {
        IconButton(
            modifier = Modifier.size(MAP_CONTROL_BUTTON_SIZE),
            onClick = onClick
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = RideWeatherColors.Accent,
                modifier = Modifier.size(MAP_CONTROL_ICON_SIZE)
            )
        }
    }
}

internal object MapControlIcons {
    val Navigation: ImageVector = materialIcon("MapNavigation") {
        moveTo(12f, 2f)
        lineTo(3.5f, 20.29f)
        lineTo(4.21f, 21f)
        lineTo(12f, 18f)
        lineTo(19.79f, 21f)
        lineTo(20.5f, 20.29f)
        close()
    }

    val Add: ImageVector = materialIcon("MapAdd") {
        moveTo(19f, 13f)
        horizontalLineTo(13f)
        verticalLineTo(19f)
        horizontalLineTo(11f)
        verticalLineTo(13f)
        horizontalLineTo(5f)
        verticalLineTo(11f)
        horizontalLineTo(11f)
        verticalLineTo(5f)
        horizontalLineTo(13f)
        verticalLineTo(11f)
        horizontalLineTo(19f)
        verticalLineTo(13f)
        close()
    }

    val Remove: ImageVector = materialIcon("MapRemove") {
        moveTo(19f, 13f)
        horizontalLineTo(5f)
        verticalLineTo(11f)
        horizontalLineTo(19f)
        verticalLineTo(13f)
        close()
    }

    val Center: ImageVector = materialIcon("MapCenter") {
        moveTo(11f, 11f)
        horizontalLineTo(13f)
        verticalLineTo(13f)
        horizontalLineTo(11f)
        verticalLineTo(11f)
        close()
        moveTo(4f, 10f)
        horizontalLineTo(2f)
        verticalLineTo(2f)
        horizontalLineTo(10f)
        verticalLineTo(4f)
        horizontalLineTo(4f)
        verticalLineTo(10f)
        close()
        moveTo(20f, 10f)
        verticalLineTo(4f)
        horizontalLineTo(14f)
        verticalLineTo(2f)
        horizontalLineTo(22f)
        verticalLineTo(10f)
        horizontalLineTo(20f)
        close()
        moveTo(4f, 14f)
        verticalLineTo(20f)
        horizontalLineTo(10f)
        verticalLineTo(22f)
        horizontalLineTo(2f)
        verticalLineTo(14f)
        horizontalLineTo(4f)
        close()
        moveTo(20f, 20f)
        verticalLineTo(14f)
        horizontalLineTo(22f)
        verticalLineTo(22f)
        horizontalLineTo(14f)
        verticalLineTo(20f)
        horizontalLineTo(20f)
        close()
    }

    val Fullscreen: ImageVector = materialIcon("MapFullscreen") {
        moveTo(5f, 5f)
        horizontalLineTo(10f)
        verticalLineTo(7f)
        horizontalLineTo(7f)
        verticalLineTo(10f)
        horizontalLineTo(5f)
        verticalLineTo(5f)
        close()
        moveTo(14f, 5f)
        horizontalLineTo(19f)
        verticalLineTo(10f)
        horizontalLineTo(17f)
        verticalLineTo(7f)
        horizontalLineTo(14f)
        verticalLineTo(5f)
        close()
        moveTo(7f, 14f)
        verticalLineTo(17f)
        horizontalLineTo(10f)
        verticalLineTo(19f)
        horizontalLineTo(5f)
        verticalLineTo(14f)
        horizontalLineTo(7f)
        close()
        moveTo(17f, 17f)
        verticalLineTo(14f)
        horizontalLineTo(19f)
        verticalLineTo(19f)
        horizontalLineTo(14f)
        verticalLineTo(17f)
        horizontalLineTo(17f)
        close()
    }

    val Close: ImageVector = materialIcon("MapClose") {
        moveTo(18.3f, 5.71f)
        lineTo(16.89f, 4.3f)
        lineTo(12f, 9.17f)
        lineTo(7.11f, 4.3f)
        lineTo(5.7f, 5.71f)
        lineTo(10.59f, 10.6f)
        lineTo(5.7f, 15.49f)
        lineTo(7.11f, 16.9f)
        lineTo(12f, 12.01f)
        lineTo(16.89f, 16.9f)
        lineTo(18.3f, 15.49f)
        lineTo(13.41f, 10.6f)
        lineTo(18.3f, 5.71f)
        close()
    }

    private fun materialIcon(
        name: String,
        pathBuilder: PathBuilder.() -> Unit
    ): ImageVector {
        return ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = MATERIAL_ICON_VIEWPORT,
            viewportHeight = MATERIAL_ICON_VIEWPORT
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                pathBuilder()
            }
        }.build()
    }
}
