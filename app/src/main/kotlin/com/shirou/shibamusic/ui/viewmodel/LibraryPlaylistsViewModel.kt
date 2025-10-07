package com.shirou.shibamusic.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shirou.shibamusic.data.repository.MusicRepository
import com.shirou.shibamusic.ui.model.PlaylistItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LibraryPlaylistsUiState(
    val playlists: List<PlaylistItem> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val sortOption: PlaylistSortOption = PlaylistSortOption.NAME_ASC
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
    
    companion object {
        private const val TAG = "LibraryPlaylists"
    }
    
    init {
        loadPlaylists()
    }
    
    fun loadPlaylists() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                val playlists = musicRepository.getAllPlaylists()
                Log.d(TAG, "Loaded ${playlists.size} playlists from database")
                playlists.forEach { playlist ->
                    Log.d(TAG, "Playlist: ${playlist.name}, songs: ${playlist.songCount}")
                }
                
                val sortedPlaylists = sortPlaylists(playlists, _uiState.value.sortOption)
                
                _uiState.value = _uiState.value.copy(
                    playlists = sortedPlaylists,
                    isLoading = false,
                    error = null
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error loading playlists", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load playlists"
                )
            }
        }
    }
    
    fun changeSortOption(option: PlaylistSortOption) {
        val sortedPlaylists = sortPlaylists(_uiState.value.playlists, option)
        _uiState.value = _uiState.value.copy(
            playlists = sortedPlaylists,
            sortOption = option
        )
    }
    
    fun createPlaylist(name: String, comment: String = "") {
        viewModelScope.launch {
            try {
                musicRepository.createPlaylist(name, comment)
                loadPlaylists() // Reload to show new playlist
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
                loadPlaylists() // Reload to remove deleted playlist
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting playlist", e)
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to delete playlist"
                )
            }
        }
    }
    
    private fun sortPlaylists(playlists: List<PlaylistItem>, option: PlaylistSortOption): List<PlaylistItem> {
        return when (option) {
            PlaylistSortOption.NAME_ASC -> playlists.sortedBy { it.name.lowercase() }
            PlaylistSortOption.NAME_DESC -> playlists.sortedByDescending { it.name.lowercase() }
            PlaylistSortOption.SONG_COUNT_DESC -> playlists.sortedByDescending { it.songCount }
            PlaylistSortOption.SONG_COUNT_ASC -> playlists.sortedBy { it.songCount }
            PlaylistSortOption.RECENTLY_ADDED -> playlists.reversed()
        }
    }
}
