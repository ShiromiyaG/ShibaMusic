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
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.AsyncImage
import com.shirou.shibamusic.ui.component.*
import com.shirou.shibamusic.ui.component.shimmer.ListItemPlaceholder
import com.shirou.shibamusic.ui.model.*
import com.shirou.shibamusic.ui.viewmodel.LibraryPlaylistsViewModel
import com.shirou.shibamusic.ui.viewmodel.PlaylistSortOption

/**
 * Library Playlists Screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryPlaylistsScreen(
    onPlaylistClick: (PlaylistItem) -> Unit,
    onCreatePlaylist: () -> Unit,
    onPlaylistPlay: (PlaylistItem) -> Unit,
    onPlaylistMenuClick: (PlaylistItem) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibraryPlaylistsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pagingPlaylists = viewModel.playlists.collectAsLazyPagingItems()
    
    LibraryPlaylistsContent(
        playlists = pagingPlaylists,
        selectedSortOption = uiState.sortOption,
        onPlaylistClick = onPlaylistClick,
        onPlaylistPlay = onPlaylistPlay,
        onPlaylistMenuClick = onPlaylistMenuClick,
        onCreatePlaylist = onCreatePlaylist,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryPlaylistsContent(
    playlists: LazyPagingItems<PlaylistItem>,
    selectedSortOption: PlaylistSortOption,
    onPlaylistClick: (PlaylistItem) -> Unit,
    onPlaylistPlay: (PlaylistItem) -> Unit,
    onPlaylistMenuClick: (PlaylistItem) -> Unit,
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
        val refreshState = playlists.loadState.refresh
        val appendState = playlists.loadState.append

        val isInitialLoading = refreshState is LoadState.Loading && playlists.itemCount == 0
        val isRefreshing = refreshState is LoadState.Loading && playlists.itemCount > 0
        val isInitialError = refreshState is LoadState.Error && playlists.itemCount == 0
        val emptyContent = refreshState is LoadState.NotLoading && playlists.itemCount == 0

        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isRefreshing) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )
            }

            AssistChip(
                onClick = {},
                enabled = false,
                label = { Text("Sorted by ${selectedSortOption.displayName}") },
                leadingIcon = { Icon(Icons.Rounded.Sort, contentDescription = null) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            when {
                isInitialLoading -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(10) {
                            ListItemPlaceholder()
                        }
                    }
                }

                isInitialError -> {
                    PagingListErrorPlaceholder(
                        message = (refreshState as? LoadState.Error)?.error?.localizedMessage
                            ?: "Failed to load playlists.",
                        onRetry = playlists::retry,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                emptyContent -> {
                    EmptyPlaylistsState(
                        onCreatePlaylist = onCreatePlaylist,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                else -> {
                    val listState = rememberLazyListState()
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(playlists.itemCount, key = { index -> playlists[index]?.id ?: index }) { index ->
                            val playlist = playlists[index] ?: return@items
                            PlaylistListItem(
                                playlist = playlist,
                                onClick = { onPlaylistClick(playlist) },
                                onPlayClick = { onPlaylistPlay(playlist) },
                                onMoreClick = { onPlaylistMenuClick(playlist) }
                            )
                            Divider(modifier = Modifier.padding(start = 80.dp))
                        }

                        if (appendState is LoadState.Loading) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                                }
                            }
                        }

                        if (appendState is LoadState.Error) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = appendState.error.localizedMessage
                                            ?: "Couldn't load more playlists.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedButton(onClick = playlists::retry) {
                                        Text("Retry")
                                    }
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PagingListErrorPlaceholder(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Rounded.Warning,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onRetry) {
            Text("Retry")
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
    onMoreClick: (() -> Unit)? = null,
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

            if (onMoreClick != null) {
                IconButton(onClick = onMoreClick) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = "More options"
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
