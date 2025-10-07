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
 * ViewModel for Albums Screen
 */
@HiltViewModel
class AlbumsViewModel @Inject constructor(
    private val repository: MusicRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AlbumsUiState())
    val uiState: StateFlow<AlbumsUiState> = _uiState.asStateFlow()
    
    init {
        loadAlbums()
        observeAlbums()
    }
    
    /**
     * Load albums from repository
     */
    private fun loadAlbums() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                val albums = repository.getAllAlbums()
                _uiState.update { 
                    it.copy(
                        albums = sortAlbums(albums, it.sortOption),
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load albums"
                    )
                }
            }
        }
    }
    
    /**
     * Observe albums for real-time updates
     */
    private fun observeAlbums() {
        repository.observeAlbums()
            .onEach { albums ->
                _uiState.update { 
                    it.copy(albums = sortAlbums(albums, it.sortOption))
                }
            }
            .catch { e ->
                _uiState.update { 
                    it.copy(error = e.message ?: "Error observing albums")
                }
            }
            .launchIn(viewModelScope)
    }
    
    /**
     * Update sort option and re-sort
     */
    fun updateSortOption(option: AlbumSortOption) {
        _uiState.update { 
            it.copy(
                sortOption = option,
                albums = sortAlbums(it.albums, option)
            )
        }
    }
    
    /**
     * Sort albums based on option
     */
    private fun sortAlbums(albums: List<AlbumItem>, option: AlbumSortOption): List<AlbumItem> {
        return when (option) {
            AlbumSortOption.TITLE_ASC -> albums.sortedBy { it.title.lowercase() }
            AlbumSortOption.TITLE_DESC -> albums.sortedByDescending { it.title.lowercase() }
            AlbumSortOption.ARTIST_ASC -> albums.sortedBy { it.artistName.lowercase() }
            AlbumSortOption.ARTIST_DESC -> albums.sortedByDescending { it.artistName.lowercase() }
            AlbumSortOption.YEAR_ASC -> albums.sortedBy { it.year ?: 0 }
            AlbumSortOption.YEAR_DESC -> albums.sortedByDescending { it.year ?: 0 }
            AlbumSortOption.RECENTLY_ADDED -> albums.reversed()
        }
    }
    
    /**
     * Refresh albums
     */
    fun refresh() {
        loadAlbums()
    }
    
    /**
     * Clear error
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

/**
 * UI State for Albums Screen
 */
data class AlbumsUiState(
    val albums: List<AlbumItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val sortOption: AlbumSortOption = AlbumSortOption.TITLE_ASC
)
