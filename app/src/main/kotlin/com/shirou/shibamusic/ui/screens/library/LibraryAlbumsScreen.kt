package com.shirou.shibamusic.ui.screens.library

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.shirou.shibamusic.ui.component.*
import com.shirou.shibamusic.ui.component.shimmer.GridItemPlaceholder
import com.shirou.shibamusic.ui.model.AlbumItem
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
    modifier: Modifier = Modifier,
    viewModel: LibraryAlbumsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LibraryAlbumsContent(
        albums = uiState.albums,
        isLoading = uiState.isLoading,
        onAlbumClick = onAlbumClick,
        onAlbumPlay = onAlbumPlay,
        onSortChange = viewModel::changeSortOption,
        selectedSortOption = uiState.sortOption,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryAlbumsContent(
    albums: List<AlbumItem>,
    isLoading: Boolean,
    onAlbumClick: (AlbumItem) -> Unit,
    onAlbumPlay: (AlbumItem) -> Unit,
    onSortChange: (AlbumSortOption) -> Unit,
    selectedSortOption: AlbumSortOption,
    modifier: Modifier = Modifier
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0.dp)
    ) { paddingValues ->
        when {
            isLoading -> {
                // Show loading placeholders
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(8.dp)
                ) {
                    items(10) {
                        GridItemPlaceholder(isCircular = false)
                    }
                }
            }
            albums.isEmpty() -> {
                EmptyPlaceholder(
                    icon = Icons.Rounded.Album,
                    text = "No albums in your library",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
            else -> {
                val gridState = rememberLazyGridState()
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    state = gridState,
                    modifier = modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(8.dp)
                ) {
                    items(
                        count = albums.size,
                        key = { index -> albums[index].id },
                        contentType = { "album" }
                    ) { index ->
                        val album = albums[index]
                        GridItem(
                            title = album.title,
                            subtitle = album.artistName,
                            thumbnailUrl = album.albumArtUrl,
                            isCircular = false,
                            onClick = { onAlbumClick(album) },
                            onPrimaryAction = { onAlbumPlay(album) }
                        )
                    }
                }
            }
        }
    }
}
