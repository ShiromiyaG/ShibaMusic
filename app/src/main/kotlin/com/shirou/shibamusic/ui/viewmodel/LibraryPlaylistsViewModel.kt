package com.shirou.shibamusic.ui.viewmodel

import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.shirou.shibamusic.R
import com.shirou.shibamusic.data.repository.MusicRepository
import com.shirou.shibamusic.ui.model.PlaylistItem
import com.shirou.shibamusic.util.Preferences
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

enum class PlaylistSortOption(
    @StringRes val labelResId: Int,
    val orderClause: String
) {
    NAME_ASC(
        R.string.sort_name_asc,
        "ORDER BY name COLLATE NOCASE ASC, id COLLATE NOCASE ASC"
    ),
    NAME_DESC(
        R.string.sort_name_desc,
        "ORDER BY name COLLATE NOCASE DESC, id COLLATE NOCASE ASC"
    ),
    SONG_COUNT_DESC(
        R.string.sort_most_songs,
        "ORDER BY song_count DESC, name COLLATE NOCASE ASC"
    ),
    SONG_COUNT_ASC(
        R.string.sort_fewest_songs,
        "ORDER BY song_count ASC, name COLLATE NOCASE ASC"
    ),
    RECENTLY_ADDED(
        R.string.sort_recently_added,
        "ORDER BY date_modified DESC, name COLLATE NOCASE ASC"
    )
}

@HiltViewModel
class LibraryPlaylistsViewModel @Inject constructor(
    private val musicRepository: MusicRepository
) : ViewModel() {
    
    private val initialSort = Preferences.getLibraryPlaylistSort()
        ?.let { stored -> runCatching { PlaylistSortOption.valueOf(stored) }.getOrNull() }
        ?: PlaylistSortOption.NAME_ASC

    private val _uiState = MutableStateFlow(LibraryPlaylistsUiState(sortOption = initialSort))
    val uiState: StateFlow<LibraryPlaylistsUiState> = _uiState.asStateFlow()

    private val sortOptionFlow = MutableStateFlow(initialSort)

    val playlists: Flow<PagingData<PlaylistItem>> = sortOptionFlow
        .flatMapLatest { option ->
            musicRepository.observePlaylistsPaged(option.orderClause)
        }
        .cachedIn(viewModelScope)
    
    companion object {
        private const val TAG = "LibraryPlaylists"
    }
    
    fun changeSortOption(option: PlaylistSortOption) {
        if (option == _uiState.value.sortOption) return
        sortOptionFlow.value = option
        _uiState.value = _uiState.value.copy(sortOption = option)
        Preferences.setLibraryPlaylistSort(option.name)
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
    
}
