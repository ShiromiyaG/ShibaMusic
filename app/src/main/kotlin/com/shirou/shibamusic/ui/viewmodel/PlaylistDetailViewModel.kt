package com.shirou.shibamusic.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shirou.shibamusic.data.repository.MusicRepository
import com.shirou.shibamusic.ui.model.PlaylistItem
import com.shirou.shibamusic.ui.model.SongItem
import com.shirou.shibamusic.ui.navigation.NavArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel responsible for playlist detail screen state.
 */
@HiltViewModel
class PlaylistDetailViewModel @Inject constructor(
    private val repository: MusicRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val playlistId: String = savedStateHandle[NavArgs.PLAYLIST_ID] ?: ""

    private val _uiState = MutableStateFlow(PlaylistDetailUiState())
    val uiState: StateFlow<PlaylistDetailUiState> = _uiState.asStateFlow()

    private val _events = Channel<PlaylistDetailEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        loadPlaylist()
        observeSongs()
    }

    fun refresh() {
        loadPlaylist()
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun removeSong(songId: String) {
        if (playlistId.isBlank()) return

        viewModelScope.launch {
            try {
                repository.removeSongFromPlaylist(playlistId, songId)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "Failed to remove song")
                }
            }
        }
    }

    fun deletePlaylist() {
        if (playlistId.isBlank()) return

        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isProcessing = true) }
                repository.deletePlaylist(playlistId)
                _uiState.update { it.copy(isProcessing = false) }
                _events.send(PlaylistDetailEvent.PlaylistDeleted)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        error = e.message ?: "Failed to delete playlist"
                    )
                }
            }
        }
    }

    private fun loadPlaylist() {
        if (playlistId.isBlank()) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    error = "Invalid playlist id"
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val playlist = repository.getPlaylistById(playlistId)
                val songs = repository.getPlaylistSongs(playlistId)

                if (playlist == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Playlist not found"
                        )
                    }
                    return@launch
                }

                _uiState.update {
                    it.copy(
                        playlist = playlist,
                        songs = songs,
                        isLoading = false,
                        isProcessing = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load playlist"
                    )
                }
            }
        }
    }

    private fun observeSongs() {
        if (playlistId.isBlank()) return

        viewModelScope.launch {
            try {
                repository.observePlaylistSongs(playlistId).collect { songs ->
                    _uiState.update { it.copy(songs = songs) }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "Failed to observe playlist songs")
                }
            }
        }
    }
}

data class PlaylistDetailUiState(
    val playlist: PlaylistItem? = null,
    val songs: List<SongItem> = emptyList(),
    val isLoading: Boolean = true,
    val isProcessing: Boolean = false,
    val error: String? = null
)

sealed class PlaylistDetailEvent {
    object PlaylistDeleted : PlaylistDetailEvent()
}
