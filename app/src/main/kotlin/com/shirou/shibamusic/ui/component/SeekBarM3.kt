package com.shirou.shibamusic.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import kotlin.math.max
import kotlin.math.min

/**
 * Material 3 expressive XS-inspired slider with buffered progress support.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeekBarM3(
    value: Float,
    bufferedValue: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    trackHeight: Dp = 4.dp
) {
    val clampedValue = value.coerceIn(valueRange.start, valueRange.endInclusive)
    val clampedBuffer = bufferedValue.coerceIn(valueRange.start, valueRange.endInclusive)
    val rangeSpan = (valueRange.endInclusive - valueRange.start).takeIf { it != 0f } ?: 1f
    val bufferFraction = ((clampedBuffer - valueRange.start) / rangeSpan).coerceIn(0f, 1f)

    val sliderColors = SliderDefaults.colors(
        inactiveTrackColor = Color.Transparent,
        disabledInactiveTrackColor = Color.Transparent
    )
    val shape = RoundedCornerShape(999.dp)

    Slider(
        value = clampedValue,
        onValueChange = { newValue ->
            val clamped = newValue.coerceIn(valueRange.start, valueRange.endInclusive)
            onValueChange(clamped)
        },
        onValueChangeFinished = onValueChangeFinished,
        valueRange = valueRange,
        steps = max(steps, 0),
        enabled = enabled,
        colors = sliderColors,
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp),
        track = { sliderState ->
            val baseTrackColor = if (enabled) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
            }
            val bufferedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = if (enabled) 0.24f else 0.14f)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(trackHeight),
                contentAlignment = Alignment.CenterStart
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(trackHeight)
                        .clip(shape)
                ) {
                    val width = size.width
                    val height = size.height
                    val radius = CornerRadius(height / 2f, height / 2f)

                    drawRoundRect(
                        color = baseTrackColor,
                        size = Size(width, height),
                        cornerRadius = radius
                    )

                    val bufferedWidth = width * bufferFraction
                    if (bufferedWidth > 0f) {
                        drawRoundRect(
                            color = bufferedTrackColor,
                            size = Size(bufferedWidth, height),
                            cornerRadius = radius
                        )
                    }
                }

                SliderDefaults.Track(
                    sliderState = sliderState,
                    colors = sliderColors,
                    enabled = enabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shape)
                )
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun SeekBarM3Preview() {
    var sliderPosition by rememberSaveable { mutableStateOf(0.35f) }

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp)) {
        Text(text = "%.2f".format(sliderPosition))
        SeekBarM3(
            value = sliderPosition,
            bufferedValue = min(1f, sliderPosition + 0.25f),
            valueRange = 0f..1f,
            onValueChange = { sliderPosition = it },
            onValueChangeFinished = {},
            enabled = true
        )
    }
}
