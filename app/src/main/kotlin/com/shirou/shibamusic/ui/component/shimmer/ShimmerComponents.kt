package com.shirou.shibamusic.ui.component.shimmer

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Shimmer effect host
 */
@Composable
fun ShimmerHost(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    content()
}

/**
 * Get shimmer brush for loading animation
 */
@Composable
fun shimmerBrush(
    targetValue: Float = 1000f,
    showShimmer: Boolean = true
): Brush {
    return if (showShimmer) {
        val shimmerColors = listOf(
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
        )

        val transition = rememberInfiniteTransition(label = "shimmer")
        val translateAnimation by transition.animateFloat(
            initialValue = 0f,
            targetValue = targetValue,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 1000,
                    easing = FastOutSlowInEasing
                ),
                repeatMode = RepeatMode.Restart
            ),
            label = "shimmer animation"
        )

        Brush.linearGradient(
            colors = shimmerColors,
            start = Offset(x = translateAnimation, y = translateAnimation),
            end = Offset(
                x = translateAnimation + 100f,
                y = translateAnimation + 100f
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(Color.Transparent, Color.Transparent),
            start = Offset.Zero,
            end = Offset.Zero
        )
    }
}

/**
 * List item placeholder with shimmer effect
 */
@Composable
fun ListItemPlaceholder(
    modifier: Modifier = Modifier
) {
    val shimmer = shimmerBrush()
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail placeholder
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(MaterialTheme.shapes.small)
                .background(shimmer)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Text placeholders
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(16.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(shimmer)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(14.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(shimmer)
            )
        }
    }
}

/**
 * Grid item placeholder with shimmer effect
 */
@Composable
fun GridItemPlaceholder(
    modifier: Modifier = Modifier,
    isCircular: Boolean = false
) {
    val shimmer = shimmerBrush()
    
    Column(
        modifier = modifier
            .width(160.dp)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Image placeholder
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(if (isCircular) CircleShape else MaterialTheme.shapes.small)
                .background(shimmer)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Title placeholder
        Box(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(14.dp)
                .clip(MaterialTheme.shapes.small)
                .background(shimmer)
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Subtitle placeholder
        Box(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(12.dp)
                .clip(MaterialTheme.shapes.small)
                .background(shimmer)
        )
    }
}

/**
 * Text placeholder
 */
@Composable
fun TextPlaceholder(
    modifier: Modifier = Modifier,
    width: Float = 1f
) {
    val shimmer = shimmerBrush()
    
    Box(
        modifier = modifier
            .fillMaxWidth(width)
            .height(16.dp)
            .clip(MaterialTheme.shapes.small)
            .background(shimmer)
    )
}

/**
 * Button placeholder
 */
@Composable
fun ButtonPlaceholder(
    modifier: Modifier = Modifier
) {
    val shimmer = shimmerBrush()
    
    Box(
        modifier = modifier
            .size(width = 120.dp, height = 40.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(shimmer)
    )
}
