package com.shirou.shibamusic.ui.screens.library

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.shirou.shibamusic.ui.component.*
import com.shirou.shibamusic.ui.component.shimmer.ListItemPlaceholder
import com.shirou.shibamusic.ui.model.SongItem
import com.shirou.shibamusic.ui.model.getThumbnailUrl
import com.shirou.shibamusic.ui.viewmodel.LibrarySongsViewModel
import com.shirou.shibamusic.ui.viewmodel.SongSortOption
import com.shirou.shibamusic.data.model.DownloadProgress

/**
 * Library Songs Screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibrarySongsScreen(
    currentlyPlayingSongId: String?,
    onSongClick: (SongItem) -> Unit,
    onSongMenuClick: (SongItem) -> Unit,
    onShuffleAllClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibrarySongsViewModel = hiltViewModel(),
    downloadedSongIds: Set<String>,
    activeDownloads: Map<String, DownloadProgress>
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pagingSongs = viewModel.songs.collectAsLazyPagingItems()
    
    LibrarySongsContent(
        songs = pagingSongs,
        isSyncing = uiState.isSyncing,
        currentlyPlayingSongId = currentlyPlayingSongId,
        onSongClick = onSongClick,
        onSongMenuClick = onSongMenuClick,
        onShuffleAllClick = onShuffleAllClick,
        onSortChange = viewModel::changeSortOption,
        selectedSortOption = uiState.sortOption,
        downloadedSongIds = downloadedSongIds,
        activeDownloads = activeDownloads,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibrarySongsContent(
    songs: LazyPagingItems<SongItem>,
    isSyncing: Boolean,
    currentlyPlayingSongId: String?,
    onSongClick: (SongItem) -> Unit,
    onSongMenuClick: (SongItem) -> Unit,
    onShuffleAllClick: () -> Unit,
    onSortChange: (SongSortOption) -> Unit,
    selectedSortOption: SongSortOption,
    downloadedSongIds: Set<String>,
    activeDownloads: Map<String, DownloadProgress>,
    modifier: Modifier = Modifier
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0.dp)
    ) { paddingValues ->
        val refreshState = songs.loadState.refresh
        val appendState = songs.loadState.append
        val listState = rememberLazyListState()
        var restoreTopAfterRefresh by remember { mutableStateOf(false) }

        val isInitialLoading = refreshState is LoadState.Loading && songs.itemCount == 0
        val isRefreshing = refreshState is LoadState.Loading && songs.itemCount > 0
        val isInitialError = refreshState is LoadState.Error && songs.itemCount == 0
        val emptyContent = refreshState is LoadState.NotLoading && songs.itemCount == 0

        LaunchedEffect(refreshState) {
            when (refreshState) {
                is LoadState.Loading -> {
                    restoreTopAfterRefresh =
                        listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
                }
                is LoadState.NotLoading -> {
                    if (restoreTopAfterRefresh &&
                        (listState.firstVisibleItemIndex != 0 || listState.firstVisibleItemScrollOffset != 0)
                    ) {
                        listState.scrollToItem(0)
                    }
                    restoreTopAfterRefresh = false
                }
                is LoadState.Error -> restoreTopAfterRefresh = false
            }
        }

        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val showTopIndicator = isSyncing || isRefreshing
            if (showTopIndicator) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    isInitialLoading -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(10) {
                                ListItemPlaceholder()
                            }
                        }
                    }

                    isInitialError -> {
                        val message = (refreshState as? LoadState.Error)?.error?.localizedMessage
                            ?: "Failed to load songs."
                        ErrorState(
                            message = message,
                            onRetry = songs::retry,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    emptyContent -> {
                        EmptyPlaceholder(
                            icon = Icons.Rounded.MusicNote,
                            text = "No songs in your library.\nAdd some music to get started!",
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    else -> {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            items(
                                count = songs.itemCount,
                                key = { index -> songs[index]?.id ?: index },
                                contentType = { "song" }
                            ) { index ->
                                val song = songs[index] ?: return@items
                                val isDownloaded = downloadedSongIds.contains(song.id)
                                val downloadInfo = activeDownloads[song.id]

                                SongListItem(
                                    title = song.title,
                                    artist = song.artistName,
                                    album = song.albumName,
                                    thumbnailUrl = song.getThumbnailUrl(),
                                    isPlaying = song.id == currentlyPlayingSongId,
                                    onClick = { onSongClick(song) },
                                    trailingContent = {
                                        when {
                                            downloadInfo != null -> {
                                                val progressValue = downloadInfo.progress.coerceIn(0f, 1f)
                                                Box(
                                                    modifier = Modifier.size(40.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (progressValue > 0f) {
                                                        CircularProgressIndicator(
                                                            progress = progressValue,
                                                            strokeWidth = 2.dp,
                                                            modifier = Modifier.size(22.dp)
                                                        )
                                                    } else {
                                                        CircularProgressIndicator(
                                                            strokeWidth = 2.dp,
                                                            modifier = Modifier.size(22.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            isDownloaded -> {
                                                Box(
                                                    modifier = Modifier.size(40.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.DownloadDone,
                                                        contentDescription = "Disponível offline",
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                            else -> {
                                                Box(modifier = Modifier.size(40.dp))
                                            }
                                        }

                                        IconButton(onClick = { onSongMenuClick(song) }) {
                                            Icon(
                                                imageVector = Icons.Rounded.MoreVert,
                                                contentDescription = "Mais opções",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                )
                            }

                            if (appendState is LoadState.Loading) {
                                item("append_loading") {
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
                                item("append_error") {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = appendState.error.localizedMessage
                                                ?: "Couldn't load more songs.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        OutlinedButton(onClick = songs::retry) {
                                            Text("Retry")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorState(
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


