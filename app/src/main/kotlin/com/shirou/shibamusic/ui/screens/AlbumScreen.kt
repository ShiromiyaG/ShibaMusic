package com.shirou.shibamusic.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.shirou.shibamusic.ui.component.*
import com.shirou.shibamusic.ui.model.*

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
    onSongClick: (SongItem) -> Unit,
    onSongMenuClick: (SongItem) -> Unit,
    onArtistClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val album = albumDetail?.album
    val songs = albumDetail?.songs ?: emptyList()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                )
            )
        }
    ) { paddingValues ->
        if (album == null || isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues)
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
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onPlayClick,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Play")
                            }
                            
                            OutlinedButton(
                                onClick = onShuffleClick,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Shuffle,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Shuffle")
                            }
                        }
                    }
                }
                
                item {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                }
                
                // Songs list
                items(songs, key = { it.id }) { song ->
                    SongListItem(
                        title = song.title,
                        artist = song.artistName,
                        album = null, // Don't show album since we're on album screen
                        thumbnailUrl = song.getThumbnailUrl() ?: album?.getThumbnailUrl(),
                        isPlaying = song.id == currentlyPlayingSongId,
                        onClick = { onSongClick(song) },
                        onMoreClick = { onSongMenuClick(song) },
                        trailingIcon = Icons.Rounded.MoreVert
                    )
                }
            }
        }
    }
}




