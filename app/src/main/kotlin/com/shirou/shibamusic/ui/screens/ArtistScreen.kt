package com.shirou.shibamusic.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.shirou.shibamusic.data.model.AudioQuality
import com.shirou.shibamusic.ui.offline.OfflineViewModel
import com.shirou.shibamusic.ui.component.*
import com.shirou.shibamusic.ui.model.*
import com.shirou.shibamusic.ui.viewmodel.PlaybackViewModel
import com.shirou.shibamusic.util.Preferences
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import com.shirou.shibamusic.ui.theme.rememberPlayerColors
import androidx.compose.foundation.background

/**
 * Artist Detail Screen
 * Shows artist image, popular songs, and albums
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistScreen(
    artist: ArtistDetailModel,
    popularSongs: List<SongItem>,
    albums: List<AlbumItem>,
    currentSongId: String?,
    isPlaying: Boolean,
    onBackClick: () -> Unit,
    onPlayClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onSongClick: (SongItem) -> Unit,
    onAlbumClick: (AlbumItem) -> Unit,
    onSongGoToAlbum: (String) -> Unit = {},
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedSong by remember { mutableStateOf<SongItem?>(null) }
    val playbackViewModel: PlaybackViewModel = hiltViewModel()
    val offlineViewModel: OfflineViewModel = hiltViewModel()
    val offlineTracks by offlineViewModel.offlineTracks.collectAsStateWithLifecycle()
    val activeDownloads by offlineViewModel.activeDownloads.collectAsStateWithLifecycle()
    val downloadedSongIds = remember(offlineTracks) { offlineTracks.map { it.id }.toSet() }
    val activeDownloadMap = remember(activeDownloads) { activeDownloads.associateBy { it.trackId } }
    val context = LocalContext.current

    val playerColors by rememberPlayerColors(
        imageUrl = artist.imageUrl,
        defaultColor = MaterialTheme.colorScheme.background
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AsyncImage(
            model = artist.imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .blur(120.dp)
                .alpha(0.6f)
        )

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

        Scaffold(
            topBar = {
                Surface(
                    color = Color.Transparent,
                    shadowElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.Rounded.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(onClick = onMenuClick) {
                            Icon(
                                imageVector = Icons.Rounded.MoreVert,
                                contentDescription = "More"
                            )
                        }
                    }
                }
            },
            modifier = modifier,
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0.dp)
        ) { paddingValues ->
            val layoutDirection = LocalLayoutDirection.current
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = paddingValues.calculateStartPadding(layoutDirection),
                        end = paddingValues.calculateEndPadding(layoutDirection),
                        top = paddingValues.calculateTopPadding()
                    )
            ) {
                // Artist Header with Image and Stats
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Transparent
                                    )
                                )
                            )
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Circular Artist Image
                        Card(
                            modifier = Modifier
                                .size(200.dp)
                                .clip(CircleShape),
                            elevation = CardDefaults.cardElevation(8.dp)
                        ) {
                            AsyncImage(
                                model = artist.imageUrl,
                                contentDescription = artist.name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Artist Name
                        Text(
                            text = artist.name,
                            style = MaterialTheme.typography.headlineMedium,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Stats
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "${artist.albumCount} albums",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${artist.songCount} songs",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilledIconButton(
                                onClick = onPlayClick,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = "Play"
                                )
                            }

                            OutlinedIconButton(
                                onClick = onShuffleClick,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Shuffle,
                                    contentDescription = "Shuffle"
                                )
                            }

                            OutlinedIconButton(
                                onClick = onDownloadClick,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Download,
                                    contentDescription = "Download"
                                )
                            }
                        }
                    }
                }

                // Popular Songs Section
                if (popularSongs.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Popular Songs",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    items(popularSongs.take(5), key = { it.id }) { song ->
                        SongListItem(
                            title = song.title,
                            artist = song.artistName,
                            album = song.albumName,
                            thumbnailUrl = song.getThumbnailUrl(),
                            isPlaying = currentSongId == song.id && isPlaying,
                            onClick = { onSongClick(song) },
                            onMoreClick = {
                                selectedSong = song
                                showBottomSheet = true
                            }
                        )
                    }
                }

                // Albums Section
                if (albums.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Albums",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    items(albums, key = { it.id }) { album ->
                        AlbumListItem(
                            album = album,
                            onClick = { onAlbumClick(album) }
                        )
                    }
                }

                // Bottom spacing
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }

    // Bottom Sheet for Song Menu
    if (showBottomSheet && selectedSong != null) {
        val song = selectedSong!!
        val isDownloaded = downloadedSongIds.contains(song.id)
        val downloadInfo = activeDownloadMap[song.id]
        val isDownloading = downloadInfo != null

        val selectedQuality = Preferences.getOfflineDownloadQuality()
        val downloadAction: (() -> Unit)? =
            if (!isDownloaded && !isDownloading) {
                {
                    offlineViewModel.downloadTrack(
                        trackId = song.id,
                        title = song.title,
                        artist = song.artistName,
                        album = song.albumName ?: artist.name,
                        duration = song.duration,
                        coverArtUrl = song.albumArtUrl,
                        quality = selectedQuality
                    )
                }
            } else null

        val removeDownloadCallback: (() -> Unit)? =
            if (isDownloaded) {
                { offlineViewModel.removeOfflineTrack(song.id) }
            } else null

        val cancelDownloadCallback: (() -> Unit)? =
            if (isDownloading) {
                { offlineViewModel.cancelDownload(song.id) }
            } else null

        SongBottomSheet(
            song = song,
            onDismiss = {
                showBottomSheet = false
                selectedSong = null
            },
            onPlayNext = { playbackViewModel.playNext(song) },
            onAddToQueue = { playbackViewModel.addToQueue(song) },
            onGoToAlbum = {
                val albumId = song.albumId
                if (albumId != null) {
                    onSongGoToAlbum(albumId)
                } else {
                    Toast.makeText(context, "Album info unavailable", Toast.LENGTH_SHORT).show()
                }
            },
            onGoToArtist = {
                Toast.makeText(context, "Already viewing this artist", Toast.LENGTH_SHORT).show()
            },
            onDownloadClick = downloadAction,
            downloadLabel = "Download offline (${selectedQuality.toDownloadLabel()})",
            onCancelDownload = cancelDownloadCallback,
            onRemoveDownload = removeDownloadCallback
        )
    }
}

private fun AudioQuality.toDownloadLabel(): String = when (this) {
    AudioQuality.LOW -> "128 kbps (Opus)"
    AudioQuality.MEDIUM -> "320 kbps (Opus)"
    AudioQuality.HIGH -> "Lossless (FLAC)"
}

/**
 * Album List Item for Artist Screen
 */
@Composable
private fun AlbumListItem(
    album: AlbumItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album Thumbnail
            Card(
                modifier = Modifier.size(56.dp)
            ) {
                AsyncImage(
                    model = album.getThumbnailUrl(),
                    contentDescription = album.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Album Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = album.title,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = album.year?.toString() ?: "Unknown year",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Song Count
            Text(
                text = "${album.songCount} songs",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

