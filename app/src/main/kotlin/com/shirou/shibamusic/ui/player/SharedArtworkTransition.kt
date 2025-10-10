package com.shirou.shibamusic.ui.player

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp

@Composable
internal fun rememberSharedAlbumCornerProgress(
    animatedVisibilityScope: AnimatedVisibilityScope,
    visibleProgress: Float,
    hiddenProgress: Float,
    durationMillis: Int = 400
): Float {
    val transition = animatedVisibilityScope.transition
    val targetProgress = if (transition.targetState == EnterExitState.Visible) {
        visibleProgress
    } else {
        hiddenProgress
    }
    val currentProgress = if (transition.currentState == EnterExitState.Visible) {
        visibleProgress
    } else {
        hiddenProgress
    }

    val cornerProgress = remember { Animatable(currentProgress) }

    LaunchedEffect(currentProgress) {
        if (!cornerProgress.isRunning && cornerProgress.value != currentProgress) {
            cornerProgress.snapTo(currentProgress)
        }
    }

    LaunchedEffect(targetProgress) {
        if (cornerProgress.targetValue != targetProgress) {
            cornerProgress.animateTo(
                targetValue = targetProgress,
                animationSpec = tween(durationMillis = durationMillis, easing = FastOutSlowInEasing)
            )
        }
    }

    return cornerProgress.value
}

@Composable
internal fun rememberSharedAlbumShape(
    cornerProgress: Float,
    startCornerSize: Dp = 28.dp
): Shape {
    val clampedProgressState = rememberUpdatedState(cornerProgress.coerceIn(0f, 1f))
    return remember(startCornerSize) {
        SharedAlbumShape(startCornerSize) { clampedProgressState.value }
    }
}

private class SharedAlbumShape(
    private val startCornerSize: Dp,
    private val progressProvider: () -> Float
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        if (size.width == 0f || size.height == 0f) {
            return Outline.Rectangle(Rect(0f, 0f, size.width, size.height))
        }

        val fraction = progressProvider().coerceIn(0f, 1f)
        val startCornerPx = density.run { startCornerSize.toPx() }.coerceAtMost(size.minDimension / 2f)
        val endCornerPx = size.minDimension / 2f
        val currentCornerPx = lerp(startCornerPx, endCornerPx, fraction)
        val cornerRadius = CornerRadius(currentCornerPx, currentCornerPx)

        return Outline.Rounded(
            RoundRect(
                left = 0f,
                top = 0f,
                right = size.width,
                bottom = size.height,
                radiusX = cornerRadius.x,
                radiusY = cornerRadius.y
            )
        )
    }
}
