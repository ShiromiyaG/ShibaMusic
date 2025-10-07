package com.shirou.shibamusic.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shirou.shibamusic.data.repository.MusicRepository
import com.shirou.shibamusic.ui.model.*
import com.shirou.shibamusic.ui.navigation.NavArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Album Detail Screen
 */
@HiltViewModel
class AlbumDetailViewModel @Inject constructor(
    private val repository: MusicRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    private val albumId: String = savedStateHandle.get<String>(NavArgs.ALBUM_ID) ?: ""
    
    private val _uiState = MutableStateFlow(AlbumDetailUiState())
    val uiState: StateFlow<AlbumDetailUiState> = _uiState.asStateFlow()
    
    init {
        loadAlbum()
    }
    
    private fun loadAlbum() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                val album = repository.getAlbumById(albumId)
                val songs = repository.getAlbumSongs(albumId)
                
                if (album != null) {
                    _uiState.update { 
                        it.copy(
                            album = album.toDetailModel(songs),
                            songs = songs,
                            isLoading = false,
                            error = null
                        )
                    }
                } else {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = "Album not found"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load album"
                    )
                }
            }
        }
    }
    
    /**
     * Play entire album
     */
    fun playAlbum() {
        // TODO: Send to MediaService
        val songs = _uiState.value.songs
        if (songs.isNotEmpty()) {
            // mediaController.playAlbum(songs)
        }
    }
    
    /**
     * Shuffle and play album
     */
    fun shuffleAlbum() {
        // TODO: Send to MediaService
        val songs = _uiState.value.songs.shuffled()
        if (songs.isNotEmpty()) {
            // mediaController.playAlbum(songs)
        }
    }
    
    /**
     * Play specific song
     */
    fun playSong(song: SongItem) {
        // TODO: Send to MediaService
        // mediaController.playSong(song, _uiState.value.songs)
    }
    
    fun refresh() {
        loadAlbum()
    }
}

data class AlbumDetailUiState(
    val album: AlbumDetailModel? = null,
    val songs: List<SongItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * Extension to convert AlbumItem to AlbumDetailModel
 */
private fun AlbumItem.toDetailModel(songs: List<SongItem>) = AlbumDetailModel(
    album = this,
    songs = songs
)
