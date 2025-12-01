package com.shirou.shibamusic.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Precision
import com.shirou.shibamusic.R
import com.shirou.shibamusic.ui.component.*
import com.shirou.shibamusic.ui.model.AlbumItem
import com.shirou.shibamusic.ui.model.SongItem
import com.shirou.shibamusic.ui.model.formatDuration
import com.shirou.shibamusic.ui.model.getThumbnailUrl
import com.shirou.shibamusic.ui.viewmodel.HomeViewModel
import com.shirou.shibamusic.ui.viewmodel.PlaybackViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.Stable

private const val RANDOM_CARD_KEY = "home_random_song_card"
private const val DISCOVER_CARD_DURATION = 10000L

// Constantes de tamanho para evitar recriações
private val ALBUM_CARD_WIDTH = 160.dp
private val THUMBNAIL_SIZE = 52.dp
private val DISCOVER_CARD_HEIGHT = 220.dp

// ═══════════════════════════════════════════════════════════════════════════════
// 1. GREETING SECTION - Saudação personalizada por horário
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Data class para armazenar informações de saudação
 * Calculada uma única vez para evitar recomputações
 */
@Stable
private data class GreetingInfo(
    val greetingResId: Int,
    val emoji: String,
    val gradientColors: List<Color>
)

private fun getGreetingInfo(): GreetingInfo {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> GreetingInfo(
            R.string.greeting_morning,
            "☀️",
            listOf(Color(0xFFFFF3E0), Color(0xFFFFE0B2))
        )
        in 12..17 -> GreetingInfo(
            R.string.greeting_afternoon,
            "🌤️",
            listOf(Color(0xFFE3F2FD), Color(0xFFBBDEFB))
        )
        in 18..21 -> GreetingInfo(
            R.string.greeting_evening,
            "🌅",
            listOf(Color(0xFFFCE4EC), Color(0xFFF8BBD9))
        )
        else -> GreetingInfo(
            R.string.greeting_night,
            "🌙",
            listOf(Color(0xFFE8EAF6), Color(0xFFC5CAE9))
        )
    }
}

@Composable
fun GreetingSection(
    modifier: Modifier = Modifier
) {
    val greetingInfo = remember { getGreetingInfo() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            text = "${stringResource(greetingInfo.greetingResId)} ${greetingInfo.emoji}",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.home_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 2. QUICK ACTION CHIPS - Ações rápidas com ícones
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun QuickActionChips(
    onShuffleAll: () -> Unit,
    onFavorites: () -> Unit,
    onRecent: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(key = "shuffle") {
            QuickActionChip(
                icon = Icons.Rounded.Shuffle,
                label = stringResource(R.string.action_shuffle_all),
                onClick = onShuffleAll,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        item(key = "favorites") {
            QuickActionChip(
                icon = Icons.Rounded.Favorite,
                label = stringResource(R.string.action_favorites),
                onClick = onFavorites,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
        item(key = "recent") {
            QuickActionChip(
                icon = Icons.Rounded.History,
                label = stringResource(R.string.home_recently_played),
                onClick = onRecent,
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

@Composable
fun QuickActionChip(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "chip_scale"
    )

    Surface(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        interactionSource = interactionSource
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 2.5 LIBRARY STATS CARDS - Cards de estatísticas da biblioteca
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun LibraryStatsRow(
    songCount: Int,
    albumCount: Int,
    artistCount: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(
            icon = Icons.Rounded.MusicNote,
            value = songCount.formatNumber(),
            label = stringResource(R.string.tab_songs),
            gradientColors = listOf(
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            ),
            modifier = Modifier.weight(1f)
        )
        StatCard(
            icon = Icons.Rounded.Album,
            value = albumCount.formatNumber(),
            label = stringResource(R.string.tab_albums),
            gradientColors = listOf(
                MaterialTheme.colorScheme.secondary,
                MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
            ),
            modifier = Modifier.weight(1f)
        )
        StatCard(
            icon = Icons.Rounded.Person,
            value = artistCount.formatNumber(),
            label = stringResource(R.string.tab_artists),
            gradientColors = listOf(
                MaterialTheme.colorScheme.tertiary,
                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.7f)
            ),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatCard(
    icon: ImageVector,
    value: String,
    label: String,
    gradientColors: List<Color>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = gradientColors,
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    )
                )
                .padding(12.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}

private fun Int.formatNumber(): String {
    return when {
        this >= 1000 -> String.format("%.1fK", this / 1000.0)
        else -> this.toString()
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 3. RANDOM SONG CARD MELHORADO - Com gradiente dinâmico e animações
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun RandomSongCardEnhanced(
    songs: List<SongItem>,
    onPlayClick: (SongItem) -> Unit,
    isActive: Boolean = true
) {
    var currentSong by remember { mutableStateOf<SongItem?>(null) }

    LaunchedEffect(songs) {
        if (songs.isNotEmpty()) {
            currentSong = songs.random()
        }
    }

    LaunchedEffect(songs, isActive) {
        if (songs.isEmpty() || !isActive) return@LaunchedEffect
        while (true) {
            delay(DISCOVER_CARD_DURATION)
            currentSong = songs.random()
        }
    }

    currentSong?.let { song ->
        AnimatedContent(
            targetState = song,
            transitionSpec = {
                (fadeIn(animationSpec = tween(600)) + 
                    scaleIn(initialScale = 0.95f, animationSpec = tween(600)))
                    .togetherWith(
                        fadeOut(animationSpec = tween(400)) + 
                        scaleOut(targetScale = 1.02f, animationSpec = tween(400))
                    )
            },
            label = "song_card_transition"
        ) { targetSong ->
            EnhancedZoomingCard(
                song = targetSong,
                onPlayClick = onPlayClick
            )
        }
    }
}

@Composable
fun EnhancedZoomingCard(
    song: SongItem,
    onPlayClick: (SongItem) -> Unit
) {
    var scale by remember { mutableStateOf(1f) }
    var progress by remember { mutableStateOf(0f) }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current
    
    val cardScale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "card_press_scale"
    )

    // Ken Burns effect (zoom lento) e barra de progresso
    LaunchedEffect(song.id) {
        scale = 1f
        progress = 0f
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < DISCOVER_CARD_DURATION) {
            val elapsed = System.currentTimeMillis() - startTime
            val fraction = elapsed.toFloat() / DISCOVER_CARD_DURATION
            scale = 1f + fraction * 0.15f
            progress = fraction
            delay(16)
        }
        progress = 1f
        scale = 1.15f
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(220.dp)
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            },
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onPlayClick(song)
        },
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 8.dp,
            pressedElevation = 4.dp
        ),
        interactionSource = interactionSource
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background image com zoom
            SubcomposeAsyncImage(
                model = rememberImageRequest(song.getThumbnailUrl()),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    },
                contentScale = ContentScale.Crop
            ) {
                when (painter.state) {
                    is AsyncImagePainter.State.Loading -> {
                        ShimmerBox(modifier = Modifier.fillMaxSize())
                    }
                    is AsyncImagePainter.State.Error -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MusicNote,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    else -> SubcomposeAsyncImageContent()
                }
            }

            // Gradiente overlay sofisticado
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.3f),
                                Color.Black.copy(alpha = 0.8f)
                            ),
                            startY = 0f,
                            endY = Float.POSITIVE_INFINITY
                        )
                    )
            )
            
            // Efeito de brilho sutil no topo
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.1f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Conteúdo
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    // Label "Now Playing" ou "Discover"
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                    ) {
                        Text(
                            text = stringResource(R.string.label_discover),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            shadow = Shadow(
                                color = Color.Black.copy(alpha = 0.5f),
                                offset = Offset(0f, 2f),
                                blurRadius = 4f
                            )
                        ),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Text(
                        text = song.artistName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // Botão de play flutuante
                FloatingPlayButton(
                    onClick = { onPlayClick(song) }
                )
            }
            
            // Indicador de progresso sutil
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .align(Alignment.BottomCenter),
                progress = { progress } ,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                trackColor = Color.Transparent
            )
        }
    }
}

@Composable
fun FloatingPlayButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "play_button_scale"
    )

    Surface(
        modifier = modifier
            .size(56.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary,
        shadowElevation = 8.dp,
        interactionSource = interactionSource
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = stringResource(R.string.cd_play),
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 4. SHIMMER LOADING EFFECT
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier
) {
    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    )

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    Box(
        modifier = modifier
            .background(
                brush = Brush.linearGradient(
                    colors = shimmerColors,
                    start = Offset(translateAnim - 200f, 0f),
                    end = Offset(translateAnim, 0f)
                )
            )
    )
}

@Composable
fun ShimmerAlbumCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.width(150.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column {
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            )
            Column(modifier = Modifier.padding(12.dp)) {
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
                Spacer(modifier = Modifier.height(6.dp))
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 5. ALBUM CARD MELHORADO
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun AlbumCardEnhanced(
    album: AlbumItem,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "album_scale"
    )
    val elevation by animateDpAsState(
        targetValue = if (isPressed) 2.dp else 6.dp,
        label = "album_elevation"
    )

    Card(
        modifier = modifier
            .width(160.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        interactionSource = interactionSource
    ) {
        Column {
            Box {
                SubcomposeAsyncImage(
                    model = rememberImageRequest(
                        album.getThumbnailUrl(),
                        widthDp = 160.dp,
                        heightDp = 160.dp
                    ),
                    contentDescription = album.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    contentScale = ContentScale.Crop
                ) {
                    when (painter.state) {
                        is AsyncImagePainter.State.Loading -> {
                            ShimmerBox(modifier = Modifier.fillMaxSize())
                        }
                        is AsyncImagePainter.State.Error -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Album,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                        else -> SubcomposeAsyncImageContent()
                    }
                }
                
                // Overlay com gradiente sutil
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.1f)
                                )
                            )
                        )
                )
                
                // Badge de favorito (se aplicável)
                if (album.isFavorite) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Favorite,
                            contentDescription = stringResource(R.string.cd_favorite),
                            modifier = Modifier
                                .padding(4.dp)
                                .size(14.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
            
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = album.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = album.artistName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 6. SONG LIST ITEM MELHORADO - Com indicador de playing animado
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun SongListItemEnhanced(
    song: SongItem,
    isPlaying: Boolean = false,
    onClick: () -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isPlaying -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            isPressed -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else -> Color.Transparent
        },
        animationSpec = tween(150),
        label = "song_bg_color"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        color = backgroundColor,
        shape = RoundedCornerShape(12.dp),
        interactionSource = interactionSource
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail com indicador de playing
            Box {
                SubcomposeAsyncImage(
                    model = rememberImageRequest(
                        song.getThumbnailUrl(),
                        widthDp = 52.dp,
                        heightDp = 52.dp
                    ),
                    contentDescription = null,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop
                ) {
                    when (painter.state) {
                        is AsyncImagePainter.State.Loading -> {
                            ShimmerBox(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(10.dp))
                            )
                        }
                        is AsyncImagePainter.State.Error -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.MusicNote,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                        else -> SubcomposeAsyncImageContent()
                    }
                }
                
                // Overlay de playing
                androidx.compose.animation.AnimatedVisibility(
                    visible = isPlaying,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        AudioVisualizerBars()
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(14.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (isPlaying) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = song.artistName ?: stringResource(R.string.unknown_artist),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // Duração
            Text(
                text = song.duration.formatDuration(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AudioVisualizerBars() {
    val infiniteTransition = rememberInfiniteTransition(label = "visualizer")
    
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val height by infiniteTransition.animateFloat(
                initialValue = 8f,
                targetValue = 20f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = (400 + (index * 100)).toInt(),
                        easing = LinearEasing
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar_$index"
            )
            
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(height.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(Color.White)
            )
        }
    }
}

private fun formatDuration(seconds: Int): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return "%d:%02d".format(minutes, secs)
}

// ═══════════════════════════════════════════════════════════════════════════════
// 7. SECTION HEADER MELHORADO
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun SectionHeaderEnhanced(
    title: String,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        
        if (actionText != null && onActionClick != null) {
            TextButton(onClick = onActionClick) {
                Text(
                    text = actionText,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 8. HOME SCREEN PRINCIPAL MELHORADA
// ═══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenEnhanced(
    onNavigateToSearch: () -> Unit,
    onNavigateToLibrary: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToAlbum: (String) -> Unit = {},
    onNavigateToFavorites: () -> Unit = {},
    onNavigateToRecent: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
    playbackViewModel: PlaybackViewModel = hiltViewModel(),
    contentBottomPadding: Dp = 0.dp
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val playbackState by playbackViewModel.playbackState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val isRandomSongCardVisible by remember(listState) {
        derivedStateOf {
            listState.layoutInfo.visibleItemsInfo.any { it.key == RANDOM_CARD_KEY }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // TopAppBar com design melhorado
        TopAppBar(
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Logo ou ícone do app
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(id = R.drawable.shiba_vector),
                            contentDescription = null,
                            modifier = Modifier
                                .padding(8.dp)
                                .size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = stringResource(R.string.home_title),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            },
            actions = {
                IconButton(onClick = onNavigateToSearch) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "Search"
                    )
                }
                IconButton(onClick = onNavigateToSettings) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = stringResource(R.string.cd_settings)
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent
            ),
            windowInsets = WindowInsets(0.dp)
        )
        
        // Pull to Refresh
        PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.fillMaxSize()
        ) {
            when {
                uiState.isLoading && uiState.allSongs.isEmpty() -> {
                    // Loading Skeleton
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = contentBottomPadding)
                    ) {
                        item {
                            GreetingSection()
                        }
                        
                        item {
                            ShimmerBox(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .height(220.dp)
                                    .clip(RoundedCornerShape(24.dp))
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                        
                        item {
                            SectionHeaderEnhanced(title = stringResource(R.string.home_sync_in_progress))
                        }
                        
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(4) {
                                    ShimmerAlbumCard()
                                }
                            }
                        }
                    }
                }
                
                uiState.error != null && uiState.allSongs.isEmpty() -> {
                    ErrorState(
                        error = uiState.error ?: "Unknown error",
                        onRetry = { viewModel.refresh() }
                    )
                }
                
                uiState.allSongs.isEmpty() -> {
                    EmptyState()
                }
                
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = contentBottomPadding + 16.dp),
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Saudação
                        item(key = "greeting") {
                            GreetingSection()
                        }
                        
                        // Quick Actions
                        item(key = "quick_actions") {
                            QuickActionChips(
                                onShuffleAll = { 
                                    uiState.allSongs.randomOrNull()?.let {
                                        playbackViewModel.playSong(it)
                                    }
                                },
                                onFavorites = onNavigateToFavorites,
                                onRecent = onNavigateToRecent
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        
                        // Library Stats Cards
                        if (uiState.allSongs.isNotEmpty()) {
                            item(key = "library_stats") {
                                LibraryStatsRow(
                                    songCount = uiState.allSongs.size,
                                    albumCount = uiState.allAlbums.size,
                                    artistCount = uiState.artistCount
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                            }
                        }
                        
                        // Random Song Card
                        item(key = RANDOM_CARD_KEY) {
                            RandomSongCardEnhanced(
                                songs = uiState.allSongs,
                                onPlayClick = { playbackViewModel.playSong(it) },
                                isActive = isRandomSongCardVisible
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                        
                        // Favorite Albums
                        if (uiState.favoriteAlbums.isNotEmpty()) {
                            item(key = "header_favorites") {
                                SectionHeaderEnhanced(
                                    title = stringResource(R.string.home_favorite_albums),
                                    actionText = stringResource(R.string.home_see_all),
                                    onActionClick = onNavigateToLibrary,
                                    icon = Icons.Rounded.Favorite
                                )
                            }
                            
                            item {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(
                                        items = uiState.favoriteAlbums.take(10),
                                        key = { it.id }
                                    ) { album ->
                                        AlbumCardEnhanced(
                                            album = album,
                                            onClick = { onNavigateToAlbum(album.id) }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                        
                        // Recently Added
                        if (uiState.recentlyAddedAlbums.isNotEmpty()) {
                            item(key = "header_recent") {
                                SectionHeaderEnhanced(
                                    title = stringResource(R.string.home_recently_added),
                                    icon = Icons.Rounded.NewReleases
                                )
                            }
                            
                            item(key = "row_recent") {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(
                                        items = uiState.recentlyAddedAlbums.take(10),
                                        key = { it.id }
                                    ) { album ->
                                        AlbumCardEnhanced(
                                            album = album,
                                            onClick = { onNavigateToAlbum(album.id) }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                        
                        // Most Played Songs com paginação melhorada
                        if (uiState.mostPlayedSongs.isNotEmpty()) {
                            item(key = "header_most_played") {
                                SectionHeaderEnhanced(
                                    title = stringResource(R.string.home_title_top_songs),
                                    icon = Icons.Rounded.TrendingUp
                                )
                            }
                            
                            item(key = "pager_most_played") {
                                MostPlayedSongsPager(
                                    songs = uiState.mostPlayedSongs,
                                    nowPlayingId = playbackState.nowPlaying?.id,
                                    isPlaying = playbackState.isPlaying,
                                    onSongClick = { playbackViewModel.playSong(it) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MostPlayedSongsPager(
    songs: List<SongItem>,
    nowPlayingId: String?,
    isPlaying: Boolean,
    onSongClick: (SongItem) -> Unit
) {
    val pages = remember(songs) {
        songs.chunked(5).take(5)
    }
    
    if (pages.isEmpty()) return
    
    val pagerState = rememberPagerState(initialPage = 0) { pages.size }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
            pageSpacing = 16.dp
        ) { pageIndex ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                pages[pageIndex].forEach { song ->
                    SongListItemEnhanced(
                        song = song,
                        isPlaying = nowPlayingId == song.id && isPlaying,
                        onClick = { onSongClick(song) }
                    )
                }
            }
        }
        
        // Page indicators melhorados
        if (pages.size > 1) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(pages.size) { index ->
                    val selected = pagerState.currentPage == index
                    val width by animateDpAsState(
                        targetValue = if (selected) 24.dp else 8.dp,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "indicator_width"
                    )
                    val color by animateColorAsState(
                        targetValue = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        },
                        label = "indicator_color"
                    )
                    
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .width(width)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(color)
                            .clickable {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            }
                    )
                }
            }
        }
    }
}

@Composable
fun ErrorState(
    error: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.errorContainer
        ) {
            Icon(
                imageVector = Icons.Rounded.ErrorOutline,
                contentDescription = null,
                modifier = Modifier
                    .padding(24.dp)
                    .size(48.dp),
                tint = MaterialTheme.colorScheme.error
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.error_generic_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = error,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onRetry,
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Rounded.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.action_retry))
        }
    }
}

@Composable
fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Icon(
                imageVector = Icons.Rounded.LibraryMusic,
                contentDescription = null,
                modifier = Modifier
                    .padding(32.dp)
                    .size(64.dp),
                tint = MaterialTheme.colorScheme.secondary
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.empty_home_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.empty_home_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// Função helper para image request (mantida do original)
@Composable
private fun rememberImageRequest(
    data: Any?,
    widthDp: Dp? = null,
    heightDp: Dp? = widthDp
): ImageRequest {
    val context = LocalContext.current
    val density = LocalDensity.current
    val widthPx = widthDp?.let { with(density) { it.toPx().roundToInt() } }
    val heightPx = (heightDp ?: widthDp)?.let { with(density) { it.toPx().roundToInt() } }

    val optimizedData = remember(data, widthPx, heightPx) {
        optimizeImageData(data, widthPx, heightPx)
    }

    return remember(optimizedData, widthPx, heightPx) {
        ImageRequest.Builder(context)
            .data(optimizedData)
            .crossfade(true)
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .precision(Precision.EXACT)
            .addHeader("Accept", "image/webp,image/*,*/*;q=0.8")
            .apply {
                if (widthPx != null && heightPx != null) {
                    size(widthPx, heightPx)
                }
            }
            .build()
    }
}

private fun optimizeImageData(
    data: Any?,
    widthPx: Int?,
    heightPx: Int?
): Any? {
    if (data !is String) return data

    val uri = data.toUri()
    val hasFormatParam = !uri.getQueryParameter("format").isNullOrBlank()
    val hasSizeParam = !uri.getQueryParameter("size").isNullOrBlank()

    if (hasFormatParam && hasSizeParam) return data

    val targetSize = listOfNotNull(widthPx, heightPx).maxOrNull()
    val builder = uri.buildUpon()

    if (!hasFormatParam) builder.appendQueryParameter("format", "webp")
    if (!hasSizeParam && targetSize != null) builder.appendQueryParameter("size", targetSize.toString())

    return builder.build().toString()
}