package com.shirou.shibamusic.ui.screens.library

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.shirou.shibamusic.ui.component.*
import com.shirou.shibamusic.ui.component.shimmer.ListItemPlaceholder
import com.shirou.shibamusic.ui.model.SongItem
import com.shirou.shibamusic.ui.viewmodel.LibrarySongsViewModel
import com.shirou.shibamusic.ui.viewmodel.SongSortOption

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
    viewModel: LibrarySongsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LibrarySongsContent(
        songs = uiState.songs,
        isLoading = uiState.isLoading,
        currentlyPlayingSongId = currentlyPlayingSongId,
        onSongClick = onSongClick,
        onSongMenuClick = onSongMenuClick,
        onShuffleAllClick = onShuffleAllClick,
        onSortChange = viewModel::changeSortOption,
        selectedSortOption = uiState.sortOption,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibrarySongsContent(
    songs: List<SongItem>,
    isLoading: Boolean,
    currentlyPlayingSongId: String?,
    onSongClick: (SongItem) -> Unit,
    onSongMenuClick: (SongItem) -> Unit,
    onShuffleAllClick: () -> Unit,
    onSortChange: (SongSortOption) -> Unit,
    selectedSortOption: SongSortOption,
    modifier: Modifier = Modifier
) {
    Scaffold(
       contentWindowInsets = WindowInsets(0.dp)
    ) { paddingValues ->
        when {
            isLoading -> {
                // Show loading placeholders
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
            songs.isEmpty() -> {
                EmptyPlaceholder(
                    icon = Icons.Rounded.MusicNote,
                    text = "No songs in your library.\nAdd some music to get started!",
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
                        .padding(paddingValues),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    // Songs list com contentType para melhor performance
                    items(
                        count = songs.size,
                        key = { index -> songs[index].id },
                        contentType = { "song" }
                    ) { index ->
                        val song = songs[index]
                        SongListItem(
                            title = song.title,
                            artist = song.artistName,
                            album = song.albumName,
                            thumbnailUrl = song.albumArtUrl,
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
}


