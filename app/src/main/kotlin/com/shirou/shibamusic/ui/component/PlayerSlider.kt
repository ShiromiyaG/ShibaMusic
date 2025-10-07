package com.shirou.shibamusic.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.max

/**
 * Slider compacto inspirado no design original do player anexo.
 *
 * Mantém a API simples (value/onValueChange) e aplica um track customizado com cantos arredondados.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    onValueChangeFinished: (() -> Unit)? = null,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    enabled: Boolean = true,
    colors: SliderColors = SliderDefaults.colors(
        thumbColor = MaterialTheme.colorScheme.primary,
        activeTrackColor = MaterialTheme.colorScheme.primary,
        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
    ),
    trackHeight: Dp = 6.dp,
    thumbDiameter: Dp = 16.dp,
    bufferedValue: Float? = null,
    bufferTrackColor: Color = colors.inactiveTrackColor.copy(alpha = 0.65f),
    backgroundColor: Color = MaterialTheme.colorScheme.background
) {
    val interactionSource = remember { MutableInteractionSource() }

    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        onValueChangeFinished = onValueChangeFinished,
        valueRange = valueRange,
        interactionSource = interactionSource,
        colors = colors,
        thumb = {
            Canvas(modifier = Modifier.size(width = 12.dp, height = 45.dp)) {
                // Desenha overlay para "apagar" a barra com gradiente
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            backgroundColor.copy(alpha = 0.9f),
                            backgroundColor.copy(alpha = 1f),
                            backgroundColor.copy(alpha = 0.9f)
                        )
                    ),
                    topLeft = Offset(size.width / 2 - 6.dp.toPx(), 0f),
                    size = androidx.compose.ui.geometry.Size(12.dp.toPx(), size.height)
                )
                // Desenha o thumb vertical
                drawLine(
                    color = colors.thumbColor,
                    start = Offset(size.width / 2, 0f),
                    end = Offset(size.width / 2, size.height),
                    strokeWidth = 4.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        },
        track = { sliderState ->
            PlayerSliderTrack(
                sliderState = sliderState,
                modifier = Modifier.height(trackHeight),
                colors = colors,
                trackHeight = trackHeight,
                bufferedValue = bufferedValue,
                bufferTrackColor = bufferTrackColor
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerSliderTrack(
    sliderState: SliderState,
    modifier: Modifier = Modifier,
    colors: SliderColors = SliderDefaults.colors(),
    trackHeight: Dp = 6.dp,
    bufferedValue: Float? = null,
    bufferTrackColor: Color = colors.inactiveTrackColor.copy(alpha = 0.65f)
) {
    val inactiveTrackColor = colors.inactiveTrackColor
    val activeTrackColor = colors.activeTrackColor
    val inactiveTickColor = colors.inactiveTickColor
    val activeTickColor = colors.activeTickColor
    val valueRange = sliderState.valueRange

    Canvas(
        modifier
            .fillMaxWidth()
            .height(trackHeight)
    ) {
        drawTrack(
            tickFractions = stepsToTickFractions(sliderState.steps),
            activeRangeStart = 0f,
            activeRangeEnd = calcFraction(
                valueRange.start,
                valueRange.endInclusive,
                sliderState.value.coerceIn(valueRange.start, valueRange.endInclusive)
            ),
            inactiveTrackColor = inactiveTrackColor,
            activeTrackColor = activeTrackColor,
            inactiveTickColor = inactiveTickColor,
            activeTickColor = activeTickColor,
            trackHeight = trackHeight,
            bufferedFraction = bufferedValue,
            bufferTrackColor = bufferTrackColor
        )
    }
}

private fun DrawScope.drawTrack(
    tickFractions: FloatArray,
    activeRangeStart: Float,
    activeRangeEnd: Float,
    inactiveTrackColor: Color,
    activeTrackColor: Color,
    inactiveTickColor: Color,
    activeTickColor: Color,
    trackHeight: Dp = 2.dp,
    bufferedFraction: Float? = null,
    bufferTrackColor: Color = inactiveTrackColor.copy(alpha = 0.65f)
) {
    val isRtl = layoutDirection == LayoutDirection.Rtl
    val sliderLeft = Offset(0f, center.y)
    val sliderRight = Offset(size.width, center.y)
    val sliderStart = if (isRtl) sliderRight else sliderLeft
    val sliderEnd = if (isRtl) sliderLeft else sliderRight
    val trackStrokeWidth = trackHeight.toPx()
    val thumbGapWidth = 14.dp.toPx()
    
    val thumbPosition = sliderStart.x + (sliderEnd.x - sliderStart.x) * activeRangeEnd
    val gapStart = (thumbPosition - thumbGapWidth / 2).coerceAtLeast(sliderStart.x)
    val gapEnd = (thumbPosition + thumbGapWidth / 2).coerceAtMost(sliderEnd.x)

    if (gapStart > sliderStart.x) {
        drawLine(
            color = inactiveTrackColor,
            start = sliderStart,
            end = Offset(gapStart, center.y),
            strokeWidth = trackStrokeWidth,
            cap = StrokeCap.Round
        )
    }
    
    if (gapEnd < sliderEnd.x) {
        drawLine(
            color = inactiveTrackColor,
            start = Offset(gapEnd, center.y),
            end = sliderEnd,
            strokeWidth = trackStrokeWidth,
            cap = StrokeCap.Round
        )
    }

    if (activeRangeEnd > 0f && gapStart > sliderStart.x) {
        drawLine(
            color = activeTrackColor,
            start = sliderStart,
            end = Offset(gapStart, center.y),
            strokeWidth = trackStrokeWidth,
            cap = StrokeCap.Round
        )
    }
}

private fun stepsToTickFractions(steps: Int): FloatArray =
    if (steps == 0) floatArrayOf() else FloatArray(steps + 2) { it.toFloat() / (steps + 1) }

private fun calcFraction(a: Float, b: Float, pos: Float): Float =
    (if (b - a == 0f) 0f else (pos - a) / (b - a)).coerceIn(0f, 1f)
