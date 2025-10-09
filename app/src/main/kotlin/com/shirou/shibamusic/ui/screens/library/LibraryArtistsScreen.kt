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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.shirou.shibamusic.ui.component.*
import com.shirou.shibamusic.ui.component.shimmer.GridItemPlaceholder
import com.shirou.shibamusic.ui.model.ArtistItem
import com.shirou.shibamusic.ui.viewmodel.ArtistSortOption
import com.shirou.shibamusic.ui.viewmodel.LibraryArtistsViewModel

/**
 * Library Artists Screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryArtistsScreen(
    onArtistClick: (ArtistItem) -> Unit,
    onArtistPlay: (ArtistItem) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibraryArtistsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pagingArtists = viewModel.artists.collectAsLazyPagingItems()
    
    LibraryArtistsContent(
        artists = pagingArtists,
        isSyncing = uiState.isSyncing,
        onArtistClick = onArtistClick,
        onArtistPlay = onArtistPlay,
        selectedSortOption = uiState.sortOption,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryArtistsContent(
    artists: LazyPagingItems<ArtistItem>,
    isSyncing: Boolean,
    onArtistClick: (ArtistItem) -> Unit,
    onArtistPlay: (ArtistItem) -> Unit,
    selectedSortOption: ArtistSortOption,
    modifier: Modifier = Modifier
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0.dp)
    ) { paddingValues ->
        val refreshState = artists.loadState.refresh
        val appendState = artists.loadState.append
        val gridState = rememberLazyGridState()

        val isInitialLoading = refreshState is LoadState.Loading && artists.itemCount == 0
        val isRefreshing = refreshState is LoadState.Loading && artists.itemCount > 0
        val isInitialError = refreshState is LoadState.Error && artists.itemCount == 0
        val emptyContent = refreshState is LoadState.NotLoading && artists.itemCount == 0

        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
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
                        contentPadding = PaddingValues(8.dp)
                    ) {
                        items(10) {
                            GridItemPlaceholder(isCircular = true)
                        }
                    }
                }

                isInitialError -> {
                    PagingErrorPlaceholder(
                        message = (refreshState as? LoadState.Error)?.error?.localizedMessage
                            ?: "Failed to load artists.",
                        onRetry = artists::retry,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                emptyContent -> {
                    EmptyPlaceholder(
                        icon = Icons.Rounded.Person,
                        text = "No artists in your library",
                        modifier = Modifier.fillMaxSize()
                    )
                }

                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        state = gridState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(8.dp)
                    ) {
                        items(artists.itemCount, key = { index -> artists[index]?.id ?: index }) { index ->
                            val artist = artists[index] ?: return@items
                            GridItem(
                                title = artist.name,
                                subtitle = "${artist.albumCount} albums",
                                thumbnailUrl = artist.imageUrl,
                                isCircular = true,
                                onClick = { onArtistClick(artist) },
                                onPrimaryAction = { onArtistPlay(artist) }
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
                                            ?: "Couldn't load more artists.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedButton(onClick = artists::retry) {
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
private fun PagingErrorPlaceholder(
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
