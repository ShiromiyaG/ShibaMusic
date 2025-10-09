package com.shirou.shibamusic.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.roundToInt

/**
 * Modelo de dados para representação de música no mini player
 */
data class MiniPlayerTrack(
    val id: String,
    val title: String,
    val artist: String,
    val albumArtUrl: String?,
    val isFavorite: Boolean = false,
    val isSubscribed: Boolean = false
)

/**
 * Estado de reprodução para o mini player
 */
data class MiniPlayerState(
    val isPlaying: Boolean = false,
    val isEnded: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val canSkipNext: Boolean = false,
    val canSkipPrevious: Boolean = false,
    val currentTrack: MiniPlayerTrack? = null
)

/**
 * Ações disponíveis no mini player
 */
interface MiniPlayerActions {
    fun onPlayPause()
    fun onSkipNext()
    fun onSkipPrevious()
    fun onToggleFavorite()
    fun onToggleSubscribe()
    fun onSeek(positionMs: Long)
    fun onPlayerClick()
}

/**
 * Mini Player moderno inspirado no design do Metrolist
 * 
 * Características principais:
 * - Design circular com progresso ao redor do botão de play
 * - Suporte a swipe para trocar músicas
 * - Botões de favorito e subscribe
 * - Animações suaves
 * - Progress bar no topo estilo Metrolist
 */
@Composable
fun ModernMiniPlayer(
    state: MiniPlayerState,
    actions: MiniPlayerActions,
    modifier: Modifier = Modifier,
    useNewDesign: Boolean = true,
    enableSwipeGestures: Boolean = true,
    swipeSensitivity: Float = 0.73f
) {
    val currentTrack = state.currentTrack ?: return

    if (useNewDesign) {
        NewStyleMiniPlayer(
            state = state,
            actions = actions,
            modifier = modifier,
            enableSwipeGestures = enableSwipeGestures,
            swipeSensitivity = swipeSensitivity
        )
    } else {
        LegacyStyleMiniPlayer(
            state = state,
            actions = actions,
            modifier = modifier,
            enableSwipeGestures = enableSwipeGestures,
            swipeSensitivity = swipeSensitivity
        )
    }
}

/**
 * Versão moderna do mini player com design circular
 */
@Composable
private fun NewStyleMiniPlayer(
    state: MiniPlayerState,
    actions: MiniPlayerActions,
    modifier: Modifier,
    enableSwipeGestures: Boolean,
    swipeSensitivity: Float
) {
    val currentTrack = state.currentTrack ?: return
    
    val coroutineScope = rememberCoroutineScope()
    val layoutDirection = LocalLayoutDirection.current
    val offsetXAnimatable = remember { Animatable(0f) }
    var dragStartTime by remember { mutableLongStateOf(0L) }
    var totalDragDistance by remember { mutableFloatStateOf(0f) }

    val animationSpec = remember {
        spring<Float>(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        )
    }

    val overlayAlpha by animateFloatAsState(
        targetValue = if (state.isPlaying) 0.0f else 0.4f,
        label = "overlay_alpha",
        animationSpec = animationSpec
    )

    // Cálculo da threshold de swipe baseado na sensibilidade
    val autoSwipeThreshold = remember(swipeSensitivity) {
        (600 / (1f + exp(-(-11.44748 * swipeSensitivity + 9.04945)))).roundToInt()
    }

    val progressFraction = if (state.duration > 0) {
        (state.currentPosition.toFloat() / state.duration).coerceIn(0f, 1f)
    } else {
        0f
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
            .padding(horizontal = 12.dp)
            .let { baseModifier ->
                if (enableSwipeGestures) {
                    baseModifier.pointerInput(state.canSkipNext, state.canSkipPrevious) {
                        detectHorizontalDragGestures(
                            onDragStart = {
                                dragStartTime = System.currentTimeMillis()
                                totalDragDistance = 0f
                            },
                            onDragCancel = {
                                coroutineScope.launch {
                                    offsetXAnimatable.animateTo(0f, animationSpec)
                                }
                            },
                            onHorizontalDrag = { _, dragAmount ->
                                val adjustedDragAmount = if (layoutDirection == LayoutDirection.Rtl) {
                                    -dragAmount
                                } else {
                                    dragAmount
                                }
                                val allowLeft = adjustedDragAmount < 0 && state.canSkipNext
                                val allowRight = adjustedDragAmount > 0 && state.canSkipPrevious
                                if (allowLeft || allowRight) {
                                    totalDragDistance += abs(adjustedDragAmount)
                                    coroutineScope.launch {
                                        offsetXAnimatable.snapTo(offsetXAnimatable.value + adjustedDragAmount)
                                    }
                                }
                            },
                            onDragEnd = {
                                val dragDuration = System.currentTimeMillis() - dragStartTime
                                val velocity = if (dragDuration > 0) {
                                    totalDragDistance / dragDuration
                                } else {
                                    0f
                                }
                                val currentOffset = offsetXAnimatable.value

                                val minDistanceThreshold = 50f
                                val velocityThreshold = (swipeSensitivity * -8.25f) + 8.5f

                                val shouldChangeSong = (
                                    abs(currentOffset) > minDistanceThreshold &&
                                        velocity > velocityThreshold
                                    ) || (abs(currentOffset) > autoSwipeThreshold)

                                if (shouldChangeSong) {
                                    val isRightSwipe = currentOffset > 0
                                    if (isRightSwipe && state.canSkipPrevious) {
                                        actions.onSkipPrevious()
                                    } else if (!isRightSwipe && state.canSkipNext) {
                                        actions.onSkipNext()
                                    }
                                }

                                coroutineScope.launch {
                                    offsetXAnimatable.animateTo(0f, animationSpec)
                                }
                            }
                        )
                    }
                } else {
                    baseModifier
                }
            }
    ) {
        // Container principal do mini player
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .offset { IntOffset(offsetXAnimatable.value.roundToInt(), 0) }
                .clip(RoundedCornerShape(32.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                // Botão Play/Pause com progress circular
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(48.dp)
                ) {
                    // Indicador de progresso circular
                    if (state.duration > 0) {
                        CircularProgressIndicator(
                            progress = { progressFraction },
                            modifier = Modifier.size(48.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 3.dp,
                            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                            strokeCap = StrokeCap.Round
                        )
                    }

                    // Botão com thumbnail de fundo
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                shape = CircleShape
                            )
                            .clickable {
                                if (state.isEnded) {
                                    actions.onSeek(0L)
                                }
                                actions.onPlayPause()
                            }
                    ) {
                        // Thumbnail de fundo
                        AsyncImage(
                            model = currentTrack.albumArtUrl,
                            contentDescription = currentTrack.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )

                        // Overlay semi-transparente para melhor visibilidade do ícone
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    color = Color.Black.copy(alpha = overlayAlpha),
                                    shape = CircleShape
                                )
                        )

                        // Ícone de play/pause
                        AnimatedVisibility(
                            visible = state.isEnded || !state.isPlaying,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Icon(
                                imageVector = if (state.isEnded) {
                                    Icons.Rounded.Replay
                                } else {
                                    Icons.Rounded.PlayArrow
                                },
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Informações da música
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { actions.onPlayerClick() },
                    verticalArrangement = Arrangement.Center
                ) {
                    AnimatedContent(
                        targetState = currentTrack.title,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "title_animation"
                    ) { title ->
                        Text(
                            text = title,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.basicMarquee(
                                iterations = 1,
                                initialDelayMillis = 3000,
                                velocity = 30.dp
                            )
                        )
                    }

                    AnimatedContent(
                        targetState = currentTrack.artist,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "artist_animation"
                    ) { artist ->
                        Text(
                            text = artist,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.basicMarquee(
                                iterations = 1,
                                initialDelayMillis = 3000,
                                velocity = 30.dp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Botão de subscrição/seguir artista
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .border(
                            width = 1.dp,
                            color = if (currentTrack.isSubscribed) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            } else {
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            },
                            shape = CircleShape
                        )
                        .background(
                            color = if (currentTrack.isSubscribed) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            } else {
                                Color.Transparent
                            },
                            shape = CircleShape
                        )
                        .clickable { actions.onToggleSubscribe() }
                ) {
                    Icon(
                        imageVector = if (currentTrack.isSubscribed) {
                            Icons.Rounded.Person
                        } else {
                            Icons.Rounded.PersonAdd
                        },
                        contentDescription = if (currentTrack.isSubscribed) {
                            "Unsubscribe"
                        } else {
                            "Subscribe"
                        },
                        tint = if (currentTrack.isSubscribed) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Botão de favorito
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .border(
                            width = 1.dp,
                            color = if (currentTrack.isFavorite) {
                                MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                            } else {
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            },
                            shape = CircleShape
                        )
                        .background(
                            color = if (currentTrack.isFavorite) {
                                MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                            } else {
                                Color.Transparent
                            },
                            shape = CircleShape
                        )
                        .clickable { actions.onToggleFavorite() }
                ) {
                    Icon(
                        imageVector = if (currentTrack.isFavorite) {
                            Icons.Rounded.Favorite
                        } else {
                            Icons.Rounded.FavoriteBorder
                        },
                        contentDescription = if (currentTrack.isFavorite) {
                            "Remove from favorites"
                        } else {
                            "Add to favorites"
                        },
                        tint = if (currentTrack.isFavorite) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Versão legada do mini player com design tradicional
 */
@Composable
private fun LegacyStyleMiniPlayer(
    state: MiniPlayerState,
    actions: MiniPlayerActions,
    modifier: Modifier,
    enableSwipeGestures: Boolean,
    swipeSensitivity: Float
) {
    val currentTrack = state.currentTrack ?: return
    
    val coroutineScope = rememberCoroutineScope()
    val layoutDirection = LocalLayoutDirection.current
    val offsetXAnimatable = remember { Animatable(0f) }
    var dragStartTime by remember { mutableLongStateOf(0L) }
    var totalDragDistance by remember { mutableFloatStateOf(0f) }

    val animationSpec = remember {
        spring<Float>(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        )
    }

    val autoSwipeThreshold = remember(swipeSensitivity) {
        (600 / (1f + exp(-(-11.44748 * swipeSensitivity + 9.04945)))).roundToInt()
    }

    val progressFraction = if (state.duration > 0) {
        (state.currentPosition.toFloat() / state.duration).coerceIn(0f, 1f)
    } else {
        0f
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .let { baseModifier ->
                if (enableSwipeGestures) {
                    baseModifier.pointerInput(state.canSkipNext, state.canSkipPrevious) {
                        detectHorizontalDragGestures(
                            onDragStart = {
                                dragStartTime = System.currentTimeMillis()
                                totalDragDistance = 0f
                            },
                            onDragCancel = {
                                coroutineScope.launch {
                                    offsetXAnimatable.animateTo(0f, animationSpec)
                                }
                            },
                            onHorizontalDrag = { _, dragAmount ->
                                val adjustedDragAmount = if (layoutDirection == LayoutDirection.Rtl) {
                                    -dragAmount
                                } else {
                                    dragAmount
                                }
                                val allowLeft = adjustedDragAmount < 0 && state.canSkipNext
                                val allowRight = adjustedDragAmount > 0 && state.canSkipPrevious
                                if (allowLeft || allowRight) {
                                    totalDragDistance += abs(adjustedDragAmount)
                                    coroutineScope.launch {
                                        offsetXAnimatable.snapTo(offsetXAnimatable.value + adjustedDragAmount)
                                    }
                                }
                            },
                            onDragEnd = {
                                val dragDuration = System.currentTimeMillis() - dragStartTime
                                val velocity = if (dragDuration > 0) {
                                    totalDragDistance / dragDuration
                                } else {
                                    0f
                                }
                                val currentOffset = offsetXAnimatable.value

                                val minDistanceThreshold = 50f
                                val velocityThreshold = (swipeSensitivity * -8.25f) + 8.5f

                                val shouldChangeSong = (
                                    abs(currentOffset) > minDistanceThreshold &&
                                        velocity > velocityThreshold
                                    ) || (abs(currentOffset) > autoSwipeThreshold)

                                if (shouldChangeSong) {
                                    val isRightSwipe = currentOffset > 0
                                    if (isRightSwipe && state.canSkipPrevious) {
                                        actions.onSkipPrevious()
                                    } else if (!isRightSwipe && state.canSkipNext) {
                                        actions.onSkipNext()
                                    }
                                }

                                coroutineScope.launch {
                                    offsetXAnimatable.animateTo(0f, animationSpec)
                                }
                            }
                        )
                    }
                } else {
                    baseModifier
                }
            }
    ) {
        // Barra de progresso no topo
        LinearProgressIndicator(
            progress = { progressFraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .align(Alignment.BottomCenter),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
            strokeCap = StrokeCap.Round
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(offsetXAnimatable.value.roundToInt(), 0) }
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // Thumbnail
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                AsyncImage(
                    model = currentTrack.albumArtUrl,
                    contentDescription = currentTrack.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Informações da música
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { actions.onPlayerClick() }
            ) {
                Text(
                    text = currentTrack.title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.basicMarquee()
                )
                
                if (currentTrack.artist.isNotBlank()) {
                    Text(
                        text = currentTrack.artist,
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Controles
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.material3.IconButton(
                    onClick = {
                        if (state.isEnded) {
                            actions.onSeek(0L)
                        }
                        actions.onPlayPause()
                    }
                ) {
                    Icon(
                        imageVector = when {
                            state.isEnded -> Icons.Rounded.Replay
                            state.isPlaying -> Icons.Rounded.Pause
                            else -> Icons.Rounded.PlayArrow
                        },
                        contentDescription = when {
                            state.isEnded -> "Replay"
                            state.isPlaying -> "Pause"
                            else -> "Play"
                        },
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                androidx.compose.material3.IconButton(
                    onClick = actions::onSkipNext,
                    enabled = state.canSkipNext
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = "Skip Next",
                        tint = if (state.canSkipNext) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        }
                    )
                }
            }
        }

        // Indicador visual de swipe
        if (enableSwipeGestures && abs(offsetXAnimatable.value) > 50f) {
            Box(
                modifier = Modifier
                    .align(
                        if (offsetXAnimatable.value > 0) {
                            Alignment.CenterStart
                        } else {
                            Alignment.CenterEnd
                        }
                    )
                    .padding(horizontal = 16.dp)
            ) {
                Icon(
                    imageVector = if (offsetXAnimatable.value > 0) {
                        Icons.Rounded.SkipPrevious
                    } else {
                        Icons.Rounded.SkipNext
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(
                        alpha = (abs(offsetXAnimatable.value) / autoSwipeThreshold).coerceIn(0f, 1f)
                    ),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}