package com.shirou.shibamusic.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shirou.shibamusic.ui.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Example ViewModel for Songs Screen
 * Demonstrates StateFlow pattern for Compose
 */
class SongsViewModel : ViewModel() {
    
    // Private mutable state
    private val _uiState = MutableStateFlow(SongsUiState())
    
    // Public immutable state
    val uiState: StateFlow<SongsUiState> = _uiState.asStateFlow()
    
    init {
        loadSongs()
    }
    
    /**
     * Load songs from repository
     */
    fun loadSongs() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                // TODO: Load from repository
                // val songs = repository.getAllSongs()
                val songs = emptyList<SongItem>() // Placeholder
                
                _uiState.value = _uiState.value.copy(
                    songs = songs,
                    isLoading = false,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Unknown error"
                )
            }
        }
    }
    
    /**
     * Update sort option
     */
    fun updateSortOption(sortOption: SortOption) {
        _uiState.value = _uiState.value.copy(
            sortOption = sortOption
        )
        // Sort songs based on option
        sortSongs()
    }
    
    /**
     * Search songs
     */
    fun searchSongs(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        // TODO: Filter songs based on query
    }
    
    /**
     * Sort songs based on current sort option
     */
    private fun sortSongs() {
        val currentSongs = _uiState.value.songs
        val sortedSongs = when (_uiState.value.sortOption) {
            SortOption.TITLE_ASC -> currentSongs.sortedBy { it.title }
            SortOption.TITLE_DESC -> currentSongs.sortedByDescending { it.title }
            SortOption.ARTIST_ASC -> currentSongs.sortedBy { it.artistName }
            SortOption.ARTIST_DESC -> currentSongs.sortedByDescending { it.artistName }
            SortOption.DURATION_ASC -> currentSongs.sortedBy { it.duration }
            SortOption.DURATION_DESC -> currentSongs.sortedByDescending { it.duration }
            SortOption.DATE_ADDED_DESC -> currentSongs // TODO: Add date added field
            SortOption.DATE_ADDED_ASC -> currentSongs.reversed()
        }
        
        _uiState.value = _uiState.value.copy(songs = sortedSongs)
    }
    
    /**
     * Play song at index
     */
    fun playSong(song: SongItem) {
        // TODO: Send to MediaService
    }
    
    /**
     * Add song to queue
     */
    fun addToQueue(song: SongItem) {
        // TODO: Send to MediaService
    }
    
    /**
     * Toggle favorite
     */
    fun toggleFavorite(songId: String) {
        viewModelScope.launch {
            // TODO: Update in repository
        }
    }
}

/**
 * UI State for Songs Screen
 */
data class SongsUiState(
    val songs: List<SongItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val sortOption: SortOption = SortOption.TITLE_ASC
)

/**
 * Sort options enum
 */
enum class SortOption {
    TITLE_ASC,
    TITLE_DESC,
    ARTIST_ASC,
    ARTIST_DESC,
    DURATION_ASC,
    DURATION_DESC,
    DATE_ADDED_DESC,
    DATE_ADDED_ASC
}
