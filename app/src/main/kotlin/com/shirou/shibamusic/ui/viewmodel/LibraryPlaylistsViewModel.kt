package com.shirou.shibamusic.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.shirou.shibamusic.data.repository.MusicRepository
import com.shirou.shibamusic.ui.model.PlaylistItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LibraryPlaylistsUiState(
    val sortOption: PlaylistSortOption = PlaylistSortOption.NAME_ASC,
    val error: String? = null
)

enum class PlaylistSortOption(val displayName: String) {
    NAME_ASC("Name A-Z"),
    NAME_DESC("Name Z-A"),
    SONG_COUNT_DESC("Most Songs"),
    SONG_COUNT_ASC("Fewest Songs"),
    RECENTLY_ADDED("Recently Added")
}

@HiltViewModel
class LibraryPlaylistsViewModel @Inject constructor(
    private val musicRepository: MusicRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(LibraryPlaylistsUiState())
    val uiState: StateFlow<LibraryPlaylistsUiState> = _uiState.asStateFlow()

    private val sortOptionFlow = MutableStateFlow(PlaylistSortOption.NAME_ASC)

    val playlists: Flow<PagingData<PlaylistItem>> = sortOptionFlow
        .flatMapLatest { option ->
            val orderClause = buildOrderClause(option)
            musicRepository.observePlaylistsPaged(orderClause)
        }
        .cachedIn(viewModelScope)
    
    companion object {
        private const val TAG = "LibraryPlaylists"
    }
    
    fun changeSortOption(option: PlaylistSortOption) {
        if (option == _uiState.value.sortOption) return
        sortOptionFlow.value = option
        _uiState.value = _uiState.value.copy(sortOption = option)
    }

    fun createPlaylist(name: String, comment: String = "") {
        viewModelScope.launch {
            try {
                musicRepository.createPlaylist(name, comment)
            } catch (e: Exception) {
                Log.e(TAG, "Error creating playlist", e)
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to create playlist"
                )
            }
        }
    }
    
    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch {
            try {
                musicRepository.deletePlaylist(playlistId)
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting playlist", e)
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to delete playlist"
                )
            }
        }
    }
    
    private fun buildOrderClause(option: PlaylistSortOption): String {
        return when (option) {
            PlaylistSortOption.NAME_ASC -> "ORDER BY name COLLATE NOCASE ASC, id COLLATE NOCASE ASC"
            PlaylistSortOption.NAME_DESC -> "ORDER BY name COLLATE NOCASE DESC, id COLLATE NOCASE ASC"
            PlaylistSortOption.SONG_COUNT_DESC -> "ORDER BY song_count DESC, name COLLATE NOCASE ASC"
            PlaylistSortOption.SONG_COUNT_ASC -> "ORDER BY song_count ASC, name COLLATE NOCASE ASC"
            PlaylistSortOption.RECENTLY_ADDED -> "ORDER BY date_modified DESC, name COLLATE NOCASE ASC"
        }
    }
}
