package com.shirou.shibamusic.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shirou.shibamusic.data.repository.MusicRepository
import com.shirou.shibamusic.ui.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Artists Screen
 */
@HiltViewModel
class ArtistsViewModel @Inject constructor(
    private val repository: MusicRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ArtistsUiState())
    val uiState: StateFlow<ArtistsUiState> = _uiState.asStateFlow()
    
    init {
        loadArtists()
        observeArtists()
    }
    
    private fun loadArtists() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                val artists = repository.getAllArtists()
                _uiState.update { 
                    it.copy(
                        artists = sortArtists(artists, it.sortOption),
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load artists"
                    )
                }
            }
        }
    }
    
    private fun observeArtists() {
        repository.observeArtists()
            .onEach { artists ->
                _uiState.update { 
                    it.copy(artists = sortArtists(artists, it.sortOption))
                }
            }
            .catch { e ->
                _uiState.update { 
                    it.copy(error = e.message)
                }
            }
            .launchIn(viewModelScope)
    }
    
    fun updateSortOption(option: ArtistSortOption) {
        _uiState.update { 
            it.copy(
                sortOption = option,
                artists = sortArtists(it.artists, option)
            )
        }
    }
    
    private fun sortArtists(artists: List<ArtistItem>, option: ArtistSortOption): List<ArtistItem> {
        return when (option) {
            ArtistSortOption.NAME_ASC -> artists.sortedBy { it.name.lowercase() }
            ArtistSortOption.NAME_DESC -> artists.sortedByDescending { it.name.lowercase() }
            ArtistSortOption.ALBUM_COUNT_DESC -> artists.sortedByDescending { it.albumCount }
            ArtistSortOption.ALBUM_COUNT_ASC -> artists.sortedBy { it.albumCount }
            ArtistSortOption.SONG_COUNT_DESC -> artists.sortedByDescending { it.songCount }
            ArtistSortOption.SONG_COUNT_ASC -> artists.sortedBy { it.songCount }
        }
    }
    
    fun refresh() {
        loadArtists()
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

data class ArtistsUiState(
    val artists: List<ArtistItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val sortOption: ArtistSortOption = ArtistSortOption.NAME_ASC
)
