package com.example.cyclistweather.ui.theme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.example.cyclistweather.R

/**
 * Animated shimmer brush built from the theme-aware surface tokens. Colors are resolved in
 * composition (per the project convention of hoisting theme colors out of any draw scope), so the
 * gradient automatically tracks light/dark mode. Use via [SkeletonBox].
 */
@Composable
fun rememberShimmerBrush(): Brush {
    val base = RideWeatherColors.Border
    val highlight = RideWeatherColors.Surface
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translate by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer-translate"
    )
    return Brush.linearGradient(
        colors = listOf(base, highlight, base),
        start = Offset(translate - 360f, 0f),
        end = Offset(translate, 0f)
    )
}

/**
 * A single shimmering placeholder block. Decorative by definition — callers should expose one
 * "loading" content description on the enclosing region rather than per-box, so assistive tech
 * announces a single state.
 */
@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(Radius.sm),
    brush: Brush = rememberShimmerBrush()
) {
    Box(modifier = modifier.background(brush = brush, shape = shape))
}

/**
 * First-load placeholder for [com.example.cyclistweather.presentation.routeweather.WeatherSummaryRow].
 * Mirrors that row's Sage panel + four equal stat columns so the real content drops in without a
 * layout jump. The whole panel reports a single "Loading weather" description.
 */
@Composable
fun WeatherSummarySkeleton(modifier: Modifier = Modifier) {
    val brush = rememberShimmerBrush()
    val loadingDescription = stringResource(R.string.loading_weather)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = loadingDescription },
        shape = RoundedCornerShape(Radius.lg),
        color = RideWeatherColors.Sage
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = Spacing.sm),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(4) {
                StatPlaceholder(brush = brush)
            }
        }
    }
}

@Composable
private fun StatPlaceholder(brush: Brush) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        SkeletonBox(
            modifier = Modifier.size(Sizes.iconMd),
            shape = RoundedCornerShape(Radius.pill),
            brush = brush
        )
        SkeletonBox(
            modifier = Modifier
                .width(40.dp)
                .height(14.dp),
            brush = brush
        )
        SkeletonBox(
            modifier = Modifier
                .width(28.dp)
                .height(10.dp),
            brush = brush
        )
    }
}

@PreviewLightDark
@Composable
private fun WeatherSummarySkeletonPreview() {
    CyclistWeatherTheme {
        Box(
            modifier = Modifier
                .background(RideWeatherColors.Stone)
                .padding(Spacing.lg)
        ) {
            WeatherSummarySkeleton()
        }
    }
}
