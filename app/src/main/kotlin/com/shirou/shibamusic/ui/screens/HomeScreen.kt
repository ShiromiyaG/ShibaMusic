package com.shirou.shibamusic.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import kotlin.math.min
import kotlin.math.roundToInt
import com.shirou.shibamusic.ui.model.AlbumItem
import androidx.compose.ui.layout.ContentScale as UiContentScale
import com.shirou.shibamusic.R
import com.shirou.shibamusic.ui.component.*
import com.shirou.shibamusic.ui.model.SongItem
import com.shirou.shibamusic.ui.viewmodel.HomeViewModel
import com.shirou.shibamusic.ui.viewmodel.PlaybackViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.core.net.toUri
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Precision

private const val RANDOM_CARD_KEY = "home_random_song_card"

/**
 * Home screen with real data from Navidrome server
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    onNavigateToSearch: () -> Unit,
    onNavigateToLibrary: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToAlbum: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
    playbackViewModel: PlaybackViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val playbackState by playbackViewModel.playbackState.collectAsState()
    val listState = rememberLazyListState()
    val isRandomSongCardVisible by remember(listState) {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            if (layoutInfo.visibleItemsInfo.isEmpty()) {
                true
            } else {
                layoutInfo.visibleItemsInfo.any { it.key == RANDOM_CARD_KEY }
            }
        }
    }

    // Content - Column com TopBar integrada
    Column(modifier = modifier.fillMaxSize()) {
        // TopBar personalizada integrada
        TopAppBar(
            title = {
                NavigationTitle(title = "Shiba Music")
            },
            actions = {
                IconButton(onClick = onNavigateToSettings) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = "Settings"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            windowInsets = WindowInsets(0.dp)
        )
        
        when {
            uiState.isLoading || uiState.isSyncing -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        if (uiState.syncMessage != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = uiState.syncMessage!!,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
            
            uiState.error != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = uiState.error ?: "Unknown error",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.refresh() }) {
                        Text(stringResource(R.string.home_retry))
                    }
                }
            }
            
            uiState.allSongs.isEmpty() -> {
                EmptyPlaceholder(
                    icon = Icons.Rounded.MusicNote,
                    text = stringResource(R.string.home_no_music),
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    state = listState
                ) {
                    // Random Song Card at top
                    item(key = RANDOM_CARD_KEY) {
                        RandomSongCard(
                            songs = uiState.allSongs,
                            onPlayClick = { song -> playbackViewModel.playSong(song) },
                            isActive = isRandomSongCardVisible
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    
                    // Favorite Albums Section
                    if (uiState.favoriteAlbums.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = stringResource(R.string.home_favorite_albums),
                                actionText = stringResource(R.string.home_see_all),
                                onActionClick = { /* TODO */ }
                            )
                        }
                        
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(uiState.favoriteAlbums.take(10)) { album ->
                                    Text(album.title) // TODO: Create AlbumCard component
                                }
                            }
                        }
                        
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                    
                    // Recently Added Albums Section
                    item {
                        SectionHeader(
                            title = stringResource(R.string.home_recently_added)
                        )
                    }
                    
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(uiState.recentlyAddedAlbums.take(10)) { album ->
                                AlbumCard(
                                    album = album,
                                    onClick = { onNavigateToAlbum(album.id) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    if (uiState.mostPlayedSongs.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = stringResource(R.string.home_title_top_songs)
                            )
                        }

                        item {
                            val mostPlayedPages = remember(uiState.mostPlayedSongs) {
                                uiState.mostPlayedSongs
                                    .chunked(5)
                                    .take(5)
                            }

                            if (mostPlayedPages.isNotEmpty()) {
                                val pageCount = mostPlayedPages.size
                                val pagerState = rememberPagerState(initialPage = 0) { pageCount }
                                val coroutineScope = rememberCoroutineScope()

                                LaunchedEffect(pageCount) {
                                    if (pagerState.currentPage > pageCount - 1) {
                                        pagerState.scrollToPage(pageCount - 1)
                                    }
                                }

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                ) {
                                    HorizontalPager(
                                        state = pagerState,
                                        modifier = Modifier.fillMaxWidth()
                                    ) { pageIndex ->
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            mostPlayedPages[pageIndex].forEach { song ->
                                                SongListItem(
                                                    song = song,
                                                    isPlaying = playbackState.nowPlaying?.id == song.id && playbackState.isPlaying,
                                                    onClick = { playbackViewModel.playSong(song) }
                                                )
                                            }
                                        }
                                    }

                                    if (pageCount > 1) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            repeat(pageCount) { index ->
                                                val selected = pagerState.currentPage == index
                                                val interactionSource = remember(index) { MutableInteractionSource() }

                                                val targetSize = if (selected) 8.dp else 5.dp
                                                val animatedSize by animateDpAsState(
                                                    targetValue = targetSize,
                                                    animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)
                                                )
                                                val targetColor = if (selected) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                                                }
                                                val animatedColor by animateColorAsState(
                                                    targetValue = targetColor,
                                                    animationSpec = tween(durationMillis = 250)
                                                )

                                                Box(
                                                    modifier = Modifier
                                                        .padding(horizontal = 4.dp)
                                                        .size(animatedSize)
                                                        .clip(CircleShape)
                                                        .background(animatedColor)
                                                        .clickable(
                                                            interactionSource = interactionSource,
                                                            indication = null
                                                        ) {
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
                        }

                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RandomSongCard(
    songs: List<SongItem>,
    onPlayClick: (SongItem) -> Unit,
    isActive: Boolean = true
) {
    var currentSong by remember { mutableStateOf<SongItem?>(null) }

    LaunchedEffect(songs) {
        if (songs.isNotEmpty()) {
            currentSong = songs.random()
        } else {
            currentSong = null
        }
    }

    LaunchedEffect(songs, isActive) {
        if (songs.isEmpty() || !isActive) return@LaunchedEffect
        while (true) {
            delay(10000)
            currentSong = songs.random()
        }
    }

    currentSong?.let { song ->
        Crossfade(
            targetState = song.id,
            animationSpec = tween(durationMillis = 800),
            label = "song_card_crossfade"
        ) {
            ZoomingCard(song = song, onPlayClick = onPlayClick)
        }
    }
}

@Composable
fun ZoomingCard(
    song: SongItem,
    onPlayClick: (SongItem) -> Unit
) {
    var scale by remember { mutableStateOf(1f) }
    
    LaunchedEffect(Unit) {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < 10000) {
            val elapsed = System.currentTimeMillis() - startTime
            scale = 1f + (elapsed / 10000f) * 0.1f
            delay(16)
        }
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(200.dp),
        onClick = { 
            android.util.Log.d("HomeScreen", "Card clicked: ${song.title}")
            onPlayClick(song) 
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val imageRequest = rememberImageRequest(song.albumArtUrl)
            AsyncImage(
                model = imageRequest,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    },
                contentScale = ContentScale.Crop
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                        shape = MaterialTheme.shapes.small
                    )
                    .padding(12.dp)
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(
                    text = song.artistName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun AlbumCard(
    album: AlbumItem,
    onClick: () -> Unit = {}
) {
    val imageRequest = rememberImageRequest(album.albumArtUrl, widthDp = 150.dp, heightDp = 150.dp)
    Card(
        modifier = Modifier
            .width(150.dp)
            .clickable(onClick = onClick)
    ) {
        Column {
            AsyncImage(
                model = imageRequest,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                contentScale = UiContentScale.Crop
            )
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = album.title,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(
                    text = album.artistName,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun SongListItem(
    song: SongItem,
    isPlaying: Boolean = false,
    onClick: () -> Unit = {}
) {
    val imageRequest = rememberImageRequest(song.albumArtUrl, widthDp = 48.dp, heightDp = 48.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = imageRequest,
            contentDescription = null,
            modifier = Modifier
                .size(48.dp)
                .clip(MaterialTheme.shapes.small),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Text(
                text = song.artistName ?: stringResource(R.string.home_unknown_artist),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun EmptySongListItem() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
    )
}

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

    if (hasFormatParam && hasSizeParam) {
        return data
    }

    val targetSize = listOfNotNull(widthPx, heightPx).maxOrNull()
    val builder = uri.buildUpon()

    if (!hasFormatParam) {
        builder.appendQueryParameter("format", "webp")
    }

    if (!hasSizeParam && targetSize != null) {
        builder.appendQueryParameter("size", targetSize.toString())
    }

    return builder.build().toString()
}
