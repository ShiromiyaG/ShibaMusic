package com.shirou.shibamusic.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.max

/**
 * BigSeekBar - Seek bar customizado
 * 
 * Características:
 * - Thumb maior e mais visível
 * - Feedback visual melhorado ao arrastar
 * - Animações suaves
 * - Melhor área de toque
 */
@Composable
fun BigSeekBar(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit = {},
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    activeTrackColor: Color = MaterialTheme.colorScheme.primary,
    inactiveTrackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    thumbColor: Color = MaterialTheme.colorScheme.primary,
    trackHeight: Float = 4f,
    thumbRadius: Float = 8f,
    bufferedValue: Float = value,
    bufferTrackColor: Color = inactiveTrackColor.copy(alpha = 0.6f)
) {
    var isDragging by remember { mutableStateOf(false) }
    var currentValue by remember { mutableStateOf(value) }
    
    LaunchedEffect(value) {
        if (!isDragging) {
            currentValue = value
        }
    }
    
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp) // Área de toque maior
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                
                detectDragGestures(
                    onDragStart = {
                        isDragging = true
                    },
                    onDragEnd = {
                        isDragging = false
                        onValueChangeFinished()
                    },
                    onDragCancel = {
                        isDragging = false
                        onValueChangeFinished()
                    },
                    onDrag = { change, _ ->
                        val newValue = (change.position.x / size.width).coerceIn(0f, 1f)
                        currentValue = newValue
                        onValueChange(newValue)
                    }
                )
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                
                detectTapGestures { offset ->
                    val newValue = (offset.x / size.width).coerceIn(0f, 1f)
                    currentValue = newValue
                    onValueChange(newValue)
                    onValueChangeFinished()
                }
            }
    ) {
        val centerY = size.height / 2
        val trackWidth = size.width - thumbRadius * 2
        val startX = thumbRadius
        
        // Draw inactive track
        drawLine(
            color = inactiveTrackColor,
            start = Offset(startX, centerY),
            end = Offset(size.width - thumbRadius, centerY),
            strokeWidth = trackHeight,
            cap = StrokeCap.Round
        )

        // Draw buffered track
        val bufferedWidth = trackWidth * max(bufferedValue, currentValue).coerceIn(0f, 1f)
        drawLine(
            color = bufferTrackColor,
            start = Offset(startX, centerY),
            end = Offset(startX + bufferedWidth, centerY),
            strokeWidth = trackHeight,
            cap = StrokeCap.Round
        )
        
        // Draw active track
        val activeWidth = trackWidth * currentValue.coerceIn(0f, 1f)
        drawLine(
            color = activeTrackColor,
            start = Offset(startX, centerY),
            end = Offset(startX + activeWidth, centerY),
            strokeWidth = trackHeight,
            cap = StrokeCap.Round
        )
        
        // Draw thumb (círculo maior quando arrastando)
        val currentThumbRadius = if (isDragging) thumbRadius * 1.5f else thumbRadius
        drawCircle(
            color = thumbColor,
            radius = currentThumbRadius,
            center = Offset(startX + activeWidth, centerY)
        )
    }
}

/**
 * Variante mais simples para o MiniPlayer
 */
@Composable
fun MiniPlayerSeekBar(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit = {},
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    activeTrackColor: Color = MaterialTheme.colorScheme.primary,
    inactiveTrackColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    var isDragging by remember { mutableStateOf(false) }
    var currentValue by remember { mutableStateOf(value) }
    
    LaunchedEffect(value) {
        if (!isDragging) {
            currentValue = value
        }
    }
    
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(24.dp) // Área de toque menor que BigSeekBar
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                
                detectDragGestures(
                    onDragStart = {
                        isDragging = true
                    },
                    onDragEnd = {
                        isDragging = false
                        onValueChangeFinished()
                    },
                    onDragCancel = {
                        isDragging = false
                        onValueChangeFinished()
                    },
                    onDrag = { change, _ ->
                        val newValue = (change.position.x / size.width).coerceIn(0f, 1f)
                        currentValue = newValue
                        onValueChange(newValue)
                    }
                )
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                
                detectTapGestures { offset ->
                    val newValue = (offset.x / size.width).coerceIn(0f, 1f)
                    currentValue = newValue
                    onValueChange(newValue)
                    onValueChangeFinished()
                }
            }
    ) {
        val centerY = size.height / 2
        val trackHeight = 3f
        
        // Draw inactive track
        drawLine(
            color = inactiveTrackColor,
            start = Offset(0f, centerY),
            end = Offset(size.width, centerY),
            strokeWidth = trackHeight,
            cap = StrokeCap.Round
        )
        
        // Draw active track
        val activeWidth = size.width * currentValue.coerceIn(0f, 1f)
        drawLine(
            color = activeTrackColor,
            start = Offset(0f, centerY),
            end = Offset(activeWidth, centerY),
            strokeWidth = trackHeight,
            cap = StrokeCap.Round
        )
        
        // Sem thumb no mini player para look mais limpo
    }
}
