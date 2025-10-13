package com.shirou.shibamusic.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.shirou.shibamusic.ui.component.*
import com.shirou.shibamusic.ui.model.*
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import com.shirou.shibamusic.ui.theme.rememberPlayerColors

import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background

/**
 * Album Detail Screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumScreen(
    albumDetail: AlbumDetailModel?,
    isLoading: Boolean,
    currentlyPlayingSongId: String?,
    onNavigateBack: () -> Unit,
    onPlayClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onSongClick: (SongItem) -> Unit,
    onSongMenuClick: (SongItem) -> Unit,
    onArtistClick: () -> Unit,
    downloadedSongIds: Set<String>,
    activeDownloads: Map<String, com.shirou.shibamusic.data.model.DownloadProgress>,
    modifier: Modifier = Modifier,
    contentBottomPadding: Dp = 0.dp
) {
    val album = albumDetail?.album
    val songs = albumDetail?.songs ?: emptyList()

    val playerColors by rememberPlayerColors(
        imageUrl = album?.getThumbnailUrl(),
        defaultColor = MaterialTheme.colorScheme.background
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AsyncImage(
            model = album?.getThumbnailUrl(),
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
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.0f),
                    shadowElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.Rounded.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                }
            },
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0.dp)
        ) { paddingValues ->
            val layoutDirection = LocalLayoutDirection.current
            val horizontalPadding = PaddingValues(
                start = paddingValues.calculateStartPadding(layoutDirection),
                end = paddingValues.calculateEndPadding(layoutDirection)
            )
            val topPadding = paddingValues.calculateTopPadding()
            if (album == null || isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontalPadding)
                        .padding(top = topPadding)
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(horizontalPadding)
                        .padding(top = topPadding),
                    contentPadding = PaddingValues(bottom = contentBottomPadding)
                ) {
                    // Album header with artwork
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            // Album artwork
                            AsyncImage(
                                model = album?.getPlayerArtworkUrl(),
                                contentDescription = album?.title,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f),
                                contentScale = ContentScale.Crop
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Album title
                            Text(
                                text = album.title,
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Artist name (clickable)
                            TextButton(
                                onClick = onArtistClick,
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(
                                    text = album.artistName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Album info
                            Text(
                                text = buildString {
                                    album.year?.let { append("$it • ") }
                                    append("${songs.size} songs")
                                    if (album.duration > 0) {
                                        append(" • ${album.duration.formatDuration()}")
                                    }
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Action buttons
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

                    item {
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                    }

                    // Songs list
                    items(songs, key = { it.id }) { song ->
                        val isDownloaded = downloadedSongIds.contains(song.id)
                        val downloadInfo = activeDownloads[song.id]

                        SongListItem(
                            title = song.title,
                            artist = song.artistName,
                            album = null, // Don't show album since we're on album screen
                            thumbnailUrl = song.getThumbnailUrl() ?: album?.getThumbnailUrl(),
                            isPlaying = song.id == currentlyPlayingSongId,
                            onClick = { onSongClick(song) },
                            onMoreClick = { onSongMenuClick(song) },
                            isDownloaded = isDownloaded,
                            downloadInfo = downloadInfo
                        )
                    }
                }
            }
        }
    }
}




