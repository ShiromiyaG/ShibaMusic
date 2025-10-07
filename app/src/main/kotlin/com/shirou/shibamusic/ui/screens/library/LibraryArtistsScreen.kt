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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
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
    val uiState by viewModel.uiState.collectAsState()
    
    LibraryArtistsContent(
        artists = uiState.artists,
        isLoading = uiState.isLoading,
        onArtistClick = onArtistClick,
        onArtistPlay = onArtistPlay,
        onSortChange = viewModel::changeSortOption,
        selectedSortOption = uiState.sortOption,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryArtistsContent(
    artists: List<ArtistItem>,
    isLoading: Boolean,
    onArtistClick: (ArtistItem) -> Unit,
    onArtistPlay: (ArtistItem) -> Unit,
    onSortChange: (ArtistSortOption) -> Unit,
    selectedSortOption: ArtistSortOption,
    modifier: Modifier = Modifier
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0.dp)
    ) { paddingValues ->
        when {
            isLoading -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(8.dp)
                ) {
                    items(10) {
                        GridItemPlaceholder(isCircular = true)
                    }
                }
            }
            artists.isEmpty() -> {
                EmptyPlaceholder(
                    icon = Icons.Rounded.Person,
                    text = "No artists in your library",
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
                    // Artists grid
                    items(artists, key = { it.id }) { artist ->
                        GridItem(
                            title = artist.name,
                            subtitle = "${artist.albumCount} albums",
                            thumbnailUrl = artist.imageUrl,
                            isCircular = true, // Circular for artists
                            onClick = { onArtistClick(artist) },
                            onPrimaryAction = { onArtistPlay(artist) }
                        )
                    }
                }
            }
        }
    }
}
