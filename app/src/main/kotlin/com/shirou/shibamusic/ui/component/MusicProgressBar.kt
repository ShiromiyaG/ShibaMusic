package com.shirou.shibamusic.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Tipos de estilo para a barra de progresso
 */
enum class MusicProgressBarStyle {
    /** Barra linear tradicional */
    LINEAR,
    /** Barra linear minimalista sem thumb */
    LINEAR_MINIMAL,
    /** Indicador circular ao redor do botão play */
    CIRCULAR,
    /** Barra curva com design moderno */
    CURVED
}

/**
 * Componente principal de barra de progresso para música
 * Inspirado no design moderno com melhorias para o ShibaMusic
 */
@Composable
fun MusicProgressBar(
    progress: Float,
    onProgressChange: (Float) -> Unit,
    onProgressChangeFinished: (() -> Unit)? = null,
    style: MusicProgressBarStyle = MusicProgressBarStyle.LINEAR,
    isPlaying: Boolean = false,
    modifier: Modifier = Modifier,
    trackColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
    progressColor: Color = MaterialTheme.colorScheme.primary,
    thumbColor: Color = MaterialTheme.colorScheme.primary,
    trackHeight: Dp = 4.dp,
    thumbSize: Dp = 20.dp,
    animateProgress: Boolean = true
) {
    val animatedProgress by animateFloatAsState(
        targetValue = if (animateProgress) progress else progress,
        animationSpec = if (isPlaying && animateProgress) {
            tween(durationMillis = 200, easing = LinearEasing)
        } else {
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        },
        label = "progress_animation"
    )

    when (style) {
        MusicProgressBarStyle.LINEAR -> {
            LinearMusicProgressBar(
                progress = animatedProgress,
                onProgressChange = onProgressChange,
                onProgressChangeFinished = onProgressChangeFinished,
                modifier = modifier,
                trackColor = trackColor,
                progressColor = progressColor,
                thumbColor = thumbColor,
                trackHeight = trackHeight,
                thumbSize = thumbSize
            )
        }
        MusicProgressBarStyle.LINEAR_MINIMAL -> {
            MinimalMusicProgressBar(
                progress = animatedProgress,
                onProgressChange = onProgressChange,
                onProgressChangeFinished = onProgressChangeFinished,
                modifier = modifier,
                trackColor = trackColor,
                progressColor = progressColor,
                trackHeight = trackHeight
            )
        }
        MusicProgressBarStyle.CIRCULAR -> {
            CircularMusicProgressBar(
                progress = animatedProgress,
                onProgressChange = onProgressChange,
                onProgressChangeFinished = onProgressChangeFinished,
                modifier = modifier,
                trackColor = trackColor,
                progressColor = progressColor,
                isPlaying = isPlaying
            )
        }
        MusicProgressBarStyle.CURVED -> {
            CurvedMusicProgressBar(
                progress = animatedProgress,
                onProgressChange = onProgressChange,
                onProgressChangeFinished = onProgressChangeFinished,
                modifier = modifier,
                trackColor = trackColor,
                progressColor = progressColor
            )
        }
    }
}

/**
 * Barra de progresso linear tradicional com thumb
 */
@Composable
private fun LinearMusicProgressBar(
    progress: Float,
    onProgressChange: (Float) -> Unit,
    onProgressChangeFinished: (() -> Unit)?,
    modifier: Modifier,
    trackColor: Color,
    progressColor: Color,
    thumbColor: Color,
    trackHeight: Dp,
    thumbSize: Dp
) {
    Slider(
        value = progress.coerceIn(0f, 1f),
        onValueChange = onProgressChange,
        onValueChangeFinished = onProgressChangeFinished,
        modifier = modifier,
        colors = SliderDefaults.colors(
            thumbColor = thumbColor,
            activeTrackColor = progressColor,
            inactiveTrackColor = trackColor
        )
    )
}

/**
 * Barra de progresso minimalista sem thumb
 */
@Composable
private fun MinimalMusicProgressBar(
    progress: Float,
    onProgressChange: (Float) -> Unit,
    onProgressChangeFinished: (() -> Unit)?,
    modifier: Modifier,
    trackColor: Color,
    progressColor: Color,
    trackHeight: Dp
) {
    Box(
        modifier = modifier
            .height(trackHeight + 8.dp)
            .clickable {
                // Implementar click para mudar posição se necessário
            }
    ) {
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(trackHeight)
                .clip(RoundedCornerShape(trackHeight / 2))
                .align(Alignment.Center),
            color = progressColor,
            trackColor = trackColor,
            strokeCap = StrokeCap.Round,
        )
    }
}

/**
 * Barra de progresso circular ao redor do botão play
 */
@Composable
fun CircularMusicProgressBar(
    progress: Float,
    onProgressChange: (Float) -> Unit,
    onProgressChangeFinished: (() -> Unit)?,
    modifier: Modifier,
    trackColor: Color,
    progressColor: Color,
    isPlaying: Boolean,
    size: Dp = 48.dp,
    strokeWidth: Dp = 3.dp,
    onPlayPauseClick: (() -> Unit)? = null
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = if (isPlaying) {
            tween(durationMillis = 300, easing = LinearEasing)
        } else {
            spring(dampingRatio = Spring.DampingRatioMediumBouncy)
        },
        label = "circular_progress"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(size)
    ) {
        CircularProgressIndicator(
            progress = { animatedProgress.coerceIn(0f, 1f) },
            modifier = Modifier.size(size),
            color = progressColor,
            strokeWidth = strokeWidth,
            trackColor = trackColor,
            strokeCap = StrokeCap.Round
        )
        
        onPlayPauseClick?.let { onClick ->
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(size - strokeWidth * 2)
                    .clip(CircleShape)
                    .clickable(onClick = onClick)
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size((size - strokeWidth * 2) * 0.5f)
                )
            }
        }
    }
}

/**
 * Barra de progresso com design curvo moderno
 */
@Composable
private fun CurvedMusicProgressBar(
    progress: Float,
    onProgressChange: (Float) -> Unit,
    onProgressChangeFinished: (() -> Unit)?,
    modifier: Modifier,
    trackColor: Color,
    progressColor: Color
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "curved_progress"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(24.dp)
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val strokeWidth = 4.dp.toPx()
        val radius = canvasHeight / 3f
        
        // Desenhar track de fundo com curva
        drawCurvedTrack(
            width = canvasWidth,
            height = canvasHeight,
            color = trackColor,
            strokeWidth = strokeWidth,
            radius = radius
        )
        
        // Desenhar progresso com curva
        drawCurvedProgress(
            width = canvasWidth,
            height = canvasHeight,
            progress = animatedProgress.coerceIn(0f, 1f),
            color = progressColor,
            strokeWidth = strokeWidth,
            radius = radius
        )
    }
}

private fun DrawScope.drawCurvedTrack(
    width: Float,
    height: Float,
    color: Color,
    strokeWidth: Float,
    radius: Float
) {
    val y = height / 2f + radius / 2f
    
    drawLine(
        color = color,
        start = Offset(0f, y),
        end = Offset(width, y),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawCurvedProgress(
    width: Float,
    height: Float,
    progress: Float,
    color: Color,
    strokeWidth: Float,
    radius: Float
) {
    val progressWidth = width * progress
    val y = height / 2f + radius / 2f
    
    if (progressWidth > 0f) {
        drawLine(
            color = color,
            start = Offset(0f, y),
            end = Offset(progressWidth, y),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

/**
 * Componente de demonstração com estilos básicos
 */
@Composable
fun MusicProgressBarDemo(
    progress: Float = 0.4f,
    isPlaying: Boolean = true
) {
    var currentProgress by remember { mutableFloatStateOf(progress) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Barras de Progresso",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Linear
        Text(
            text = "Linear Tradicional",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        MusicProgressBar(
            progress = currentProgress,
            onProgressChange = { currentProgress = it },
            style = MusicProgressBarStyle.LINEAR,
            isPlaying = isPlaying,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )
        
        // Linear Minimal
        Text(
            text = "Linear Minimalista",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        MusicProgressBar(
            progress = currentProgress,
            onProgressChange = { currentProgress = it },
            style = MusicProgressBarStyle.LINEAR_MINIMAL,
            isPlaying = isPlaying,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )
        
        // Circular
        Text(
            text = "Progresso Circular",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier.padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularMusicProgressBar(
                progress = currentProgress,
                onProgressChange = { currentProgress = it },
                onProgressChangeFinished = null,
                modifier = Modifier,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                progressColor = MaterialTheme.colorScheme.primary,
                isPlaying = isPlaying,
                onPlayPauseClick = { /* implementar */ }
            )
            CircularMusicProgressBar(
                progress = currentProgress,
                onProgressChange = { currentProgress = it },
                onProgressChangeFinished = null,
                modifier = Modifier,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                progressColor = MaterialTheme.colorScheme.secondary,
                isPlaying = isPlaying,
                size = 64.dp,
                strokeWidth = 4.dp
            )
        }
        
        // Curved
        Text(
            text = "Estilo Curvo",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        MusicProgressBar(
            progress = currentProgress,
            onProgressChange = { currentProgress = it },
            style = MusicProgressBarStyle.CURVED,
            isPlaying = isPlaying,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )
    }
}
