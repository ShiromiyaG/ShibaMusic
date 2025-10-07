package com.shirou.shibamusic.ui.player

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage

import com.shirou.shibamusic.ui.theme.rememberPlayerColors
import com.shirou.shibamusic.ui.utils.TimeUtils
import com.shirou.shibamusic.ui.viewmodel.PlaybackViewModel
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * PlayerScreen
 * 
 * Mudanças principais:
 * - Background dinâmico com cores extraídas da artwork
 * - BigSeekBar customizado (era PlayerSlider)
 * - Controles maiores e melhor espaçados (56dp/72dp)
 * - Thumbnail maior com componente dedicado
 * - Botões extras: Favorite, Equalizer
 * - Animações suaves
 * - Melhor hierarquia visual
 */
@OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)
@Composable
fun PlayerScreen(
    onNavigateBack: () -> Unit,
    onShowQueue: () -> Unit,
    onShowLyrics: () -> Unit,
    onMoreClick: () -> Unit,
    onNavigateToAlbum: (String) -> Unit = {},
    onNavigateToArtist: (String) -> Unit = {},
    animatedVisibilityScope: androidx.compose.animation.AnimatedVisibilityScope,
    sharedTransitionScope: androidx.compose.animation.SharedTransitionScope,
    viewModel: PlaybackViewModel = hiltViewModel()
) {
    

    val playerState by viewModel.playbackState.collectAsStateWithLifecycle()
    val nowPlaying = playerState.nowPlaying
    
    // Placeholder se nada está tocando
    if (nowPlaying == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "No song playing",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Select a song to start playing",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
        return
    }
    
    BackHandler { onNavigateBack() }

    val playerColors by rememberPlayerColors(
        imageUrl = nowPlaying.albumArtUrl,
        defaultColor = MaterialTheme.colorScheme.background
    )

    PlayerScreenContent(
                title = nowPlaying.title,
                artist = nowPlaying.artistName,
                artistId = nowPlaying.artistId,
                album = nowPlaying.albumName,
                albumId = nowPlaying.albumId,
                thumbnailUrl = nowPlaying.albumArtUrl,
                isPlaying = playerState.isPlaying,
                position = playerState.progress.currentPosition,
                duration = playerState.progress.duration,
                bufferedPosition = playerState.progress.bufferedPosition,
                isShuffle = playerState.shuffleMode,
                repeatMode = when (playerState.repeatMode) {
                    com.shirou.shibamusic.ui.model.RepeatMode.OFF -> RepeatMode.OFF
                    com.shirou.shibamusic.ui.model.RepeatMode.ONE -> RepeatMode.ONE
                    com.shirou.shibamusic.ui.model.RepeatMode.ALL -> RepeatMode.ALL
                },
                playerColors = playerColors,
                isFavorite = playerState.isFavorite,
                onNavigateBack = onNavigateBack,
                onPlayPauseClick = { viewModel.playPause() },
                onNextClick = { viewModel.skipToNext() },
                onPreviousClick = { viewModel.skipToPrevious() },
                onSeekTo = { position -> viewModel.seekTo(position) },
                onShuffleClick = { viewModel.toggleShuffle() },
                onRepeatClick = { viewModel.toggleRepeat() },
                onToggleFavorite = { viewModel.toggleFavorite() },
                onShowQueue = onShowQueue,
                onShowLyrics = onShowLyrics,
                onShowEqualizer = { /* Navigate to equalizer - Not implemented yet */ },
                onShowTimer = { /* Navigate to sleep timer - Not implemented yet */ },
                onMoreClick = onMoreClick,
                onNavigateToAlbum = onNavigateToAlbum,
                onNavigateToArtist = onNavigateToArtist,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope
            )
}

@Composable
private fun PlayerMetadataChip(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        tonalElevation = 1.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PlayerArtwork(
    imageUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        tonalElevation = 8.dp,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    ) {
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun PlayerSecondaryButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    active: Boolean,
    activeColor: Color,
    modifier: Modifier = Modifier
) {
    val container = if (active) activeColor.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.35f)
    val content = if (active) activeColor else MaterialTheme.colorScheme.onSurfaceVariant

    FilledTonalIconButton(
        onClick = onClick,
        modifier = modifier.size(56.dp),
        shape = RoundedCornerShape(18.dp),
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = container,
            contentColor = content
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(26.dp)
        )
    }
}

@Composable
private fun PlayerPrimaryTransportButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(46.dp),
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        contentColor = contentColor,
        tonalElevation = 3.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

private enum class RepeatMode {
    OFF,
    ONE,
    ALL
}

@Suppress("UNUSED_PARAMETER")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalAnimationApi::class, androidx.compose.animation.ExperimentalSharedTransitionApi::class)
@Composable
private fun PlayerScreenContent(
    title: String,
    artist: String,
    artistId: String?,
    album: String?,
    albumId: String?,
    thumbnailUrl: String?,
    isPlaying: Boolean,
    position: Long,
    duration: Long,
    bufferedPosition: Long,
    isShuffle: Boolean,
    repeatMode: RepeatMode,
    isFavorite: Boolean,
    playerColors: com.shirou.shibamusic.ui.theme.PlayerColors,
    onNavigateBack: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onShuffleClick: () -> Unit,
    onRepeatClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onShowQueue: () -> Unit,
    onShowLyrics: () -> Unit,
    onShowEqualizer: () -> Unit,
    onShowTimer: () -> Unit,
    onMoreClick: () -> Unit,
    onNavigateToAlbum: (String) -> Unit,
    onNavigateToArtist: (String) -> Unit,
    modifier: Modifier = Modifier,
    sharedTransitionScope: androidx.compose.animation.SharedTransitionScope,
    animatedVisibilityScope: androidx.compose.animation.AnimatedVisibilityScope
) {
    var sliderPosition by remember { mutableStateOf<Float?>(null) }
    var isDragging by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableStateOf(position) }

    // Atualiza a posição periodicamente quando está tocando
    LaunchedEffect(isPlaying, position) {
        currentPosition = position
        if (isPlaying && !isDragging) {
            while (isPlaying && !isDragging) {
                kotlinx.coroutines.delay(100)
                currentPosition += 100
            }
        }
    }

    val rawSliderValue = if (duration > 0) {
        currentPosition.toFloat() / duration
    } else 0f

    val sliderValue = when {
        isDragging -> sliderPosition ?: rawSliderValue
        else -> rawSliderValue
    }

    val displayPosition = if (isDragging) {
        sliderPosition?.let { (duration * it).toLong() } ?: currentPosition
    } else currentPosition
    
    val sliderBufferedValue = if (duration > 0) {
        max(rawSliderValue, bufferedPosition.toFloat() / duration)
    } else rawSliderValue

    val accent = playerColors.accent
    val chipContainer = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val showCopiedToast: (String) -> Unit = { message ->
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (thumbnailUrl != null) {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(120.dp)
                    .alpha(0.6f)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            playerColors.background.copy(alpha = 0.95f),
                            playerColors.surface.copy(alpha = 0.85f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                with(sharedTransitionScope) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .aspectRatio(1f)
                            .skipToLookaheadSize()
                            .sharedElement(
                                state = rememberSharedContentState(key = "album_artwork"),
                                animatedVisibilityScope = animatedVisibilityScope,
                                boundsTransform = { _, _ ->
                                    tween(durationMillis = 400, easing = FastOutSlowInEasing)
                                }
                            )
                            .renderInSharedTransitionScopeOverlay(
                                renderInOverlay = { isTransitionActive },
                                zIndexInOverlay = 1f
                            ),
                        shape = RoundedCornerShape(28.dp),
                        tonalElevation = 8.dp,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ) {
                        if (thumbnailUrl != null) {
                            AsyncImage(
                                model = thumbnailUrl,
                                contentDescription = title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.MusicNote,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AnimatedContent(
                    targetState = title,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "player_title"
                ) { animatedTitle ->
                    Text(
                        text = animatedTitle,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .basicMarquee(iterations = Int.MAX_VALUE)
                            .combinedClickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {},
                                onLongClick = {
                                    clipboardManager.setText(AnnotatedString(animatedTitle))
                                    showCopiedToast("Título copiado")
                                }
                            )
                    )
                }

                AnimatedContent(
                    targetState = artist,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "player_artist"
                ) { animatedArtist ->
                    Text(
                        text = animatedArtist,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .basicMarquee(iterations = Int.MAX_VALUE)
                            .combinedClickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    artistId?.let { onNavigateToArtist(it) }
                                },
                                onLongClick = {
                                    clipboardManager.setText(AnnotatedString(animatedArtist))
                                    showCopiedToast("Artista copiado")
                                }
                            )
                    )
                }

                
                val albumName = album?.takeIf { it.isNotBlank() }
                if (albumName != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PlayerMetadataChip(
                            icon = Icons.Rounded.Album,
                            text = albumName,
                            onClick = { albumId?.let { onNavigateToAlbum(it) } }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Slider(
                    value = sliderValue,
                    onValueChange = {
                        isDragging = true
                        sliderPosition = it
                    },
                    onValueChangeFinished = {
                        sliderPosition?.let { value ->
                            onSeekTo((duration * value).toLong())
                        }
                        sliderPosition = null
                        isDragging = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = duration > 0,
                    colors = SliderDefaults.colors(
                        activeTrackColor = accent,
                        thumbColor = accent,
                        inactiveTrackColor = playerColors.surface.copy(alpha = 0.35f)
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = TimeUtils.formatDuration(displayPosition),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = TimeUtils.formatDuration(duration),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IconButton(
                    onClick = onPreviousClick,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipPrevious,
                        contentDescription = "Previous",
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    onClick = onPlayPauseClick,
                    modifier = Modifier.size(72.dp),
                    shape = RoundedCornerShape(50),
                    color = accent,
                    contentColor = Color.White,
                    tonalElevation = 4.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onNextClick,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = "Next",
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IconButton(
                    onClick = onShowQueue,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.QueueMusic,
                        contentDescription = "Queue",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onShuffleClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (isShuffle) accent else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onRepeatClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = when (repeatMode) {
                            RepeatMode.ONE -> Icons.Rounded.RepeatOne
                            else -> Icons.Rounded.Repeat
                        },
                        contentDescription = "Repeat",
                        tint = if (repeatMode != RepeatMode.OFF) accent else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
