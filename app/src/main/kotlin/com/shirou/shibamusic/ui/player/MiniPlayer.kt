package com.shirou.shibamusic.ui.player

import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.shirou.shibamusic.ui.component.MiniPlayerSeekBar
import com.shirou.shibamusic.ui.model.PlaybackState
import com.shirou.shibamusic.ui.model.getThumbnailUrl
import com.shirou.shibamusic.ui.viewmodel.PlaybackViewModel
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * MiniPlayer
 * 
 * Mudanças principais:
 * - Thumbnail circular 56dp (era 48dp quadrado)
 * - MiniPlayerSeekBar customizado (era PlayerSlider)
 * - Progress bar mais fino no topo (3dp)
 * - Botão de favorito adicionado
 * - Melhor espaçamento
 * - Animações suaves
 */
@OptIn(ExperimentalFoundationApi::class, androidx.compose.animation.ExperimentalSharedTransitionApi::class)
@Composable
fun MiniPlayer(
    onClick: () -> Unit,
    animatedVisibilityScope: androidx.compose.animation.AnimatedVisibilityScope,
    sharedTransitionScope: androidx.compose.animation.SharedTransitionScope,
    viewModel: PlaybackViewModel = hiltViewModel()
) {
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val nowPlaying = playbackState.nowPlaying ?: return

    val sharedElementKey = "album_artwork_${nowPlaying.id}"

    val durationMs = playbackState.progress.duration.takeIf { it > 0 } ?: 0L
    val positionMs = playbackState.progress.currentPosition.coerceIn(0, durationMs)
    val progressFraction = if (durationMs > 0) positionMs.toFloat() / durationMs else 0f

    val coroutineScope = rememberCoroutineScope()
    val layoutDirection = LocalLayoutDirection.current
    val offsetX = remember { Animatable(0f) }
    var dragStartTime by remember { mutableLongStateOf(0L) }
    var totalDragDistance by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val viewConfiguration = LocalViewConfiguration.current
    val verticalSwipeThreshold: Float = with(density) { 72.dp.toPx() }
    val touchSlop = viewConfiguration.touchSlop

    val animationSpec = remember {
        spring<Float>(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        )
    }

    val autoSwipeThresholdPx = remember {
        val swipeSensitivity = 0.73f
        600f / (1f + exp(-(-11.44748f * swipeSensitivity + 9.04945f)))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
            .padding(horizontal = 12.dp)
            .pointerInput(playbackState.hasNext, playbackState.hasPrevious) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    dragStartTime = System.currentTimeMillis()
                    totalDragDistance = 0f
                    var accumulatedX = 0f
                    var accumulatedY = 0f
                    var orientation: Orientation? = null
                    var navigationTriggered = false

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break

                        if (change.changedToUpIgnoreConsumed()) {
                            change.consume()
                            break
                        }

                        val delta = change.position - change.previousPosition
                        accumulatedX += delta.x
                        accumulatedY += delta.y

                        if (orientation == null) {
                            val absX = abs(accumulatedX)
                            val absY = abs(accumulatedY)
                            if (max(absX, absY) > touchSlop) {
                                orientation = if (absX > absY) Orientation.Horizontal else Orientation.Vertical
                            }
                        }

                        when (orientation) {
                            Orientation.Horizontal -> {
                                val adjusted = if (layoutDirection == LayoutDirection.Rtl) -delta.x else delta.x
                                val allowLeft = adjusted < 0 && playbackState.hasNext
                                val allowRight = adjusted > 0 && playbackState.hasPrevious
                                if (allowLeft || allowRight) {
                                    totalDragDistance += abs(adjusted)
                                    coroutineScope.launch {
                                        offsetX.snapTo(offsetX.value + adjusted)
                                    }
                                    change.consume()
                                }
                            }

                            Orientation.Vertical -> {
                                change.consume()
                                if (!navigationTriggered && accumulatedY <= -verticalSwipeThreshold) {
                                    navigationTriggered = true
                                    onClick()
                                }
                            }

                            null -> Unit
                        }

                        if (navigationTriggered) {
                            change.consume()
                        }
                    }

                    val dragDuration = System.currentTimeMillis() - dragStartTime
                    val velocity = if (dragDuration > 0) {
                        totalDragDistance / dragDuration.toFloat()
                    } else {
                        0f
                    }
                    val currentOffset = offsetX.value

                    val minDistanceThreshold = 60f
                    val velocityThreshold = 2.5f
                    val shouldChangeSong = (
                        abs(currentOffset) > minDistanceThreshold &&
                            velocity > velocityThreshold
                        ) || abs(currentOffset) > autoSwipeThresholdPx

                    if (orientation == Orientation.Horizontal && shouldChangeSong) {
                        val isRightSwipe = currentOffset > 0
                        if (isRightSwipe && playbackState.hasPrevious) {
                            viewModel.skipToPrevious()
                        } else if (!isRightSwipe && playbackState.hasNext) {
                            viewModel.skipToNext()
                        }
                    }

                    coroutineScope.launch {
                        offsetX.animateTo(0f, animationSpec)
                    }
                }
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .clip(RoundedCornerShape(32.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            val isPlaying = playbackState.isPlaying && playbackState.playbackState != PlaybackState.ENDED

            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(48.dp)
                ) {
                    if (durationMs > 0) {
                        CircularProgressIndicator(
                            progress = { progressFraction.coerceIn(0f, 1f) },
                            modifier = Modifier.size(48.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 3.dp,
                            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
                    }

                    val cornerProgress = rememberSharedAlbumCornerProgress(
                        animatedVisibilityScope = animatedVisibilityScope,
                        visibleProgress = 1f,
                        hiddenProgress = 0f
                    )

                    val sharedContentState = with(sharedTransitionScope) {
                        rememberSharedContentState(key = sharedElementKey)
                    }

                    SharedAlbumArtwork(
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        sharedContentState = sharedContentState,
                        cornerProgress = cornerProgress,
                        modifier = Modifier.size(40.dp),
                        backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                    ) {
                        AsyncImage(
                            model = nowPlaying.getThumbnailUrl(),
                            contentDescription = nowPlaying.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .border(
                                width = 1.dp,
                                color = if (isPlaying) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                } else {
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                },
                                shape = CircleShape
                            )
                            .background(
                                color = if (isPlaying) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                } else {
                                    Color.Transparent
                                },
                                shape = CircleShape
                            )
                            .clickable {
                                if (playbackState.playbackState == PlaybackState.ENDED) {
                                    viewModel.seekTo(0L)
                                }
                                viewModel.playPause()
                            }
                    ) {
                        Icon(
                            imageVector = if (isPlaying) {
                                Icons.Rounded.Pause
                            } else {
                                Icons.Rounded.PlayArrow
                            },
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onClick),
                    verticalArrangement = Arrangement.Center
                ) {
                    AnimatedContent(
                        targetState = nowPlaying.title,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "mini_player_title"
                    ) { title ->
                        Text(
                            text = title,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.basicMarquee()
                        )
                    }

                    AnimatedContent(
                        targetState = nowPlaying.artistName,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "mini_player_artist"
                    ) { artist ->
                        Text(
                            text = artist,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.basicMarquee()
                        )
                    }
                }

                IconButton(
                    onClick = { viewModel.toggleFavorite() }
                ) {
                    Icon(
                        imageVector = if (playbackState.isFavorite) {
                            Icons.Rounded.Favorite
                        } else {
                            Icons.Rounded.FavoriteBorder
                        },
                        contentDescription = if (playbackState.isFavorite) {
                            "Remove from favorites"
                        } else {
                            "Add to favorites"
                        },
                        tint = if (playbackState.isFavorite) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                }

                IconButton(
                    onClick = { viewModel.skipToNext() },
                    enabled = playbackState.hasNext
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = "Next",
                        tint = if (playbackState.hasNext) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        }
                    )
                }
            }
        }
    }
}

/**
 * Versão alternativa com thumbnail quadrado arredondado
 */
@Composable
fun MiniPlayerRounded(
    onClick: () -> Unit,
    viewModel: PlaybackViewModel = hiltViewModel()
) {
    val playerState by viewModel.playbackState.collectAsStateWithLifecycle()
    val nowPlaying = playerState.nowPlaying
    
    if (nowPlaying == null) {
        return
    }
    
    var sliderPosition by remember { mutableStateOf<Float?>(null) }
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 3.dp,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            MiniPlayerSeekBar(
                value = sliderPosition ?: if (playerState.progress.duration > 0) {
                    playerState.progress.currentPosition.toFloat() / playerState.progress.duration
                } else 0f,
                onValueChange = { position ->
                    sliderPosition = position
                },
                onValueChangeFinished = {
                    sliderPosition?.let { position ->
                        viewModel.seekTo((playerState.progress.duration * position).toLong())
                    }
                    sliderPosition = null
                },
                modifier = Modifier.fillMaxWidth()
            )
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Thumbnail quadrado com cantos arredondados
                com.shirou.shibamusic.ui.component.MusicThumbnail(
                    imageUrl = nowPlaying.getThumbnailUrl(),
                    size = 56.dp,
                    contentDescription = nowPlaying.title
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = nowPlaying.title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = nowPlaying.artistName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                Spacer(modifier = Modifier.width(4.dp))
                
                IconButton(
                    onClick = { viewModel.toggleFavorite() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = if (playerState.isFavorite) {
                            Icons.Rounded.Favorite
                        } else {
                            Icons.Rounded.FavoriteBorder
                        },
                        contentDescription = "Favorite",
                        tint = if (playerState.isFavorite) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                IconButton(
                    onClick = { viewModel.playPause() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = if (playerState.isPlaying) {
                            Icons.Rounded.Pause
                        } else {
                            Icons.Rounded.PlayArrow
                        },
                        contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(28.dp)
                    )
                }
                
                IconButton(
                    onClick = { viewModel.skipToNext() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = "Next",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}
