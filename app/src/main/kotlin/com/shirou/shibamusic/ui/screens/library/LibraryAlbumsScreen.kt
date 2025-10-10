package com.shirou.shibamusic.ui.screens.library

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.shirou.shibamusic.ui.component.*
import com.shirou.shibamusic.ui.component.shimmer.GridItemPlaceholder
import com.shirou.shibamusic.ui.model.AlbumItem
import com.shirou.shibamusic.ui.model.getThumbnailUrl
import com.shirou.shibamusic.ui.viewmodel.AlbumSortOption
import com.shirou.shibamusic.ui.viewmodel.LibraryAlbumsViewModel

/**
 * Library Albums Screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryAlbumsScreen(
    onAlbumClick: (AlbumItem) -> Unit,
    onAlbumPlay: (AlbumItem) -> Unit,
    onAlbumMenuClick: (AlbumItem) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibraryAlbumsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pagingAlbums = viewModel.albums.collectAsLazyPagingItems()

    LibraryAlbumsContent(
        albums = pagingAlbums,
        isSyncing = uiState.isSyncing,
        onAlbumClick = onAlbumClick,
        onAlbumPlay = onAlbumPlay,
        onAlbumMenuClick = onAlbumMenuClick,
        selectedSortOption = uiState.sortOption,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryAlbumsContent(
    albums: LazyPagingItems<AlbumItem>,
    isSyncing: Boolean,
    onAlbumClick: (AlbumItem) -> Unit,
    onAlbumPlay: (AlbumItem) -> Unit,
    onAlbumMenuClick: (AlbumItem) -> Unit,
    selectedSortOption: AlbumSortOption,
    modifier: Modifier = Modifier
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0.dp)
    ) { paddingValues ->
        val refreshState = albums.loadState.refresh
        val appendState = albums.loadState.append
        val gridState = rememberLazyGridState()
        var restoreTopAfterRefresh by remember { mutableStateOf(false) }
        val layoutDirection = LocalLayoutDirection.current

        val isInitialLoading = refreshState is LoadState.Loading && albums.itemCount == 0
        val isRefreshing = refreshState is LoadState.Loading && albums.itemCount > 0
        val isInitialError = refreshState is LoadState.Error && albums.itemCount == 0
        val emptyContent = refreshState is LoadState.NotLoading && albums.itemCount == 0

        LaunchedEffect(refreshState) {
            when (refreshState) {
                is LoadState.Loading -> {
                    restoreTopAfterRefresh =
                        gridState.firstVisibleItemIndex == 0 && gridState.firstVisibleItemScrollOffset == 0
                }
                is LoadState.NotLoading -> {
                    if (restoreTopAfterRefresh &&
                        (gridState.firstVisibleItemIndex != 0 || gridState.firstVisibleItemScrollOffset != 0)
                    ) {
                        gridState.scrollToItem(0)
                    }
                    restoreTopAfterRefresh = false
                }
                is LoadState.Error -> restoreTopAfterRefresh = false
            }
        }

        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(
                    start = paddingValues.calculateStartPadding(layoutDirection),
                    end = paddingValues.calculateEndPadding(layoutDirection)
                )
        ) {
            if (isSyncing || isRefreshing) {
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
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 8.dp,
                            end = 8.dp
                        )
                    ) {
                        items(10) {
                            GridItemPlaceholder(isCircular = false)
                        }
                    }
                }

                isInitialError -> {
                    val message = (refreshState as? LoadState.Error)?.error?.localizedMessage
                        ?: "Failed to load albums."
                    ErrorPlaceholder(
                        message = message,
                        onRetry = albums::retry,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                emptyContent -> {
                    EmptyPlaceholder(
                        icon = Icons.Rounded.Album,
                        text = "No albums in your library",
                        modifier = Modifier.fillMaxSize()
                    )
                }

                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        state = gridState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 8.dp,
                            end = 8.dp
                        )
                    ) {
                        items(albums.itemCount, key = { index -> albums[index]?.id ?: index }) { index ->
                            val album = albums[index] ?: return@items
                            GridItem(
                                title = album.title,
                                subtitle = album.artistName,
                                thumbnailUrl = album.getThumbnailUrl(),
                                isCircular = false,
                                onClick = { onAlbumClick(album) },
                                onPrimaryAction = { onAlbumPlay(album) },
                                onMoreClick = { onAlbumMenuClick(album) }
                            )
                        }

                        if (appendState is LoadState.Loading) {
                            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
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
                            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = appendState.error.localizedMessage
                                            ?: "Couldn't load more albums.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedButton(onClick = albums::retry) {
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

@Composable
private fun ErrorPlaceholder(
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
