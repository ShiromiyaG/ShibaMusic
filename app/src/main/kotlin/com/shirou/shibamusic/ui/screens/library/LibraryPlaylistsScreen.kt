package com.shirou.shibamusic.ui.screens.library

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.shirou.shibamusic.ui.component.*
import com.shirou.shibamusic.ui.component.shimmer.ListItemPlaceholder
import com.shirou.shibamusic.ui.model.*
import com.shirou.shibamusic.ui.viewmodel.LibraryPlaylistsViewModel

/**
 * Library Playlists Screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryPlaylistsScreen(
    onPlaylistClick: (PlaylistItem) -> Unit,
    onCreatePlaylist: () -> Unit,
    onPlaylistPlay: (PlaylistItem) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibraryPlaylistsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LibraryPlaylistsContent(
        playlists = uiState.playlists,
        isLoading = uiState.isLoading,
        onPlaylistClick = onPlaylistClick,
        onPlaylistPlay = onPlaylistPlay,
        onCreatePlaylist = onCreatePlaylist,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryPlaylistsContent(
    playlists: List<PlaylistItem>,
    isLoading: Boolean,
    onPlaylistClick: (PlaylistItem) -> Unit,
    onPlaylistPlay: (PlaylistItem) -> Unit,
    onCreatePlaylist: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreatePlaylist,
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Create Playlist"
                )
            }
        },
        contentWindowInsets = WindowInsets(0.dp)
    ) { paddingValues ->
        when {
            isLoading -> {
                LazyColumn(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    items(10) {
                        ListItemPlaceholder()
                    }
                }
            }
            playlists.isEmpty() -> {
                EmptyPlaylistsState(
                    onCreatePlaylist = onCreatePlaylist,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
            else -> {
                val listState = rememberLazyListState()
                LazyColumn(
                    state = listState,
                    modifier = modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    items(playlists, key = { it.id }) { playlist ->
                        PlaylistListItem(
                            playlist = playlist,
                            onClick = { onPlaylistClick(playlist) },
                            onPlayClick = { onPlaylistPlay(playlist) }
                        )
                        Divider(modifier = Modifier.padding(start = 80.dp))
                    }
                    
                    // Bottom spacing
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }
}

/**
 * Empty state for playlists
 */
@Composable
private fun EmptyPlaylistsState(
    onCreatePlaylist: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Rounded.QueueMusic,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "No playlists yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Text(
            text = "Create your first playlist",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(onClick = onCreatePlaylist) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create Playlist")
        }
    }
}

/**
 * Playlist list item
 */
@Composable
private fun PlaylistListItem(
    playlist: PlaylistItem,
    onClick: () -> Unit,
    onPlayClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Playlist Thumbnail
            Card(
                modifier = Modifier.size(56.dp)
            ) {
                if (playlist.thumbnailUrl != null) {
                    AsyncImage(
                        model = playlist.thumbnailUrl,
                        contentDescription = playlist.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Default playlist icon
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.QueueMusic,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Playlist Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = buildString {
                        append("${playlist.songCount} songs")
                        if (!playlist.description.isNullOrBlank()) {
                            append(" • ${playlist.description}")
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            
            if (onPlayClick != null) {
                IconButton(onClick = onPlayClick) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = "Play playlist"
                    )
                }
            }

            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
