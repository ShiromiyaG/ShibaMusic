package com.shirou.shibamusic.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shirou.shibamusic.data.repository.MusicRepository
import com.shirou.shibamusic.ui.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Playlists Screen
 */
@HiltViewModel
class PlaylistsViewModel @Inject constructor(
    private val repository: MusicRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(PlaylistsUiState())
    val uiState: StateFlow<PlaylistsUiState> = _uiState.asStateFlow()
    
    private val _events = Channel<PlaylistEvent>()
    val events = _events.receiveAsFlow()
    
    init {
        loadPlaylists()
        observePlaylists()
    }
    
    private fun loadPlaylists() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                val playlists = repository.getAllPlaylists()
                _uiState.update { 
                    it.copy(
                        playlists = playlists,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load playlists"
                    )
                }
            }
        }
    }
    
    private fun observePlaylists() {
        repository.observePlaylists()
            .onEach { playlists ->
                _uiState.update { it.copy(playlists = playlists) }
            }
            .catch { e ->
                _uiState.update { it.copy(error = e.message) }
            }
            .launchIn(viewModelScope)
    }
    
    /**
     * Create new playlist
     */
    fun createPlaylist(name: String, description: String = "") {
        viewModelScope.launch {
            try {
                val playlist = repository.createPlaylist(name, description)
                _events.send(PlaylistEvent.Created(playlist))
                loadPlaylists() // Refresh list
            } catch (e: Exception) {
                _events.send(PlaylistEvent.Error(e.message ?: "Failed to create playlist"))
            }
        }
    }
    
    /**
     * Delete playlist
     */
    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch {
            try {
                repository.deletePlaylist(playlistId)
                _events.send(PlaylistEvent.Deleted)
                loadPlaylists() // Refresh list
            } catch (e: Exception) {
                _events.send(PlaylistEvent.Error(e.message ?: "Failed to delete playlist"))
            }
        }
    }
    
    /**
     * Add song to playlist
     */
    fun addSongToPlaylist(playlistId: String, songId: String) {
        viewModelScope.launch {
            try {
                repository.addSongToPlaylist(playlistId, songId)
                _events.send(PlaylistEvent.SongAdded)
            } catch (e: Exception) {
                _events.send(PlaylistEvent.Error(e.message ?: "Failed to add song"))
            }
        }
    }
    
    fun refresh() {
        loadPlaylists()
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

data class PlaylistsUiState(
    val playlists: List<PlaylistItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed class PlaylistEvent {
    data class Created(val playlist: PlaylistItem) : PlaylistEvent()
    object Deleted : PlaylistEvent()
    object SongAdded : PlaylistEvent()
    data class Error(val message: String) : PlaylistEvent()
}
