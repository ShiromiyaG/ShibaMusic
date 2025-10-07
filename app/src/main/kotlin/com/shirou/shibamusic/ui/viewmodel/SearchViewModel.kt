package com.shirou.shibamusic.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shirou.shibamusic.data.repository.MusicRepository
import com.shirou.shibamusic.ui.model.SearchResults
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Search Screen
 */
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: MusicRepository
) : ViewModel() {
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()
    
    init {
        observeSearchQuery()
    }
    
    /**
     * Observe search query and perform search with debounce
     */
    @OptIn(FlowPreview::class)
    private fun observeSearchQuery() {
        _searchQuery
            .debounce(300) // Wait 300ms after user stops typing
            .distinctUntilChanged()
            .onEach { query ->
                if (query.isNotBlank()) {
                    performSearch(query)
                } else {
                    _uiState.update { 
                        it.copy(
                            results = SearchResults(),
                            isSearching = false
                        )
                    }
                }
            }
            .launchIn(viewModelScope)
    }
    
    /**
     * Update search query
     */
    fun updateQuery(query: String) {
        _searchQuery.value = query
        if (query.isNotBlank()) {
            _uiState.update { it.copy(isSearching = true) }
        }
    }
    
    /**
     * Perform search
     */
    private fun performSearch(query: String) {
        viewModelScope.launch {
            try {
                val result = repository.searchAll(query)
                _uiState.update { 
                    it.copy(
                        results = SearchResults(
                            songs = result.songs,
                            albums = result.albums,
                            artists = result.artists
                        ),
                        isSearching = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isSearching = false,
                        error = e.message ?: "Search failed"
                    )
                }
            }
        }
    }
    
    /**
     * Clear search query
     */
    fun clearQuery() {
        _searchQuery.value = ""
        _uiState.update { 
            it.copy(
                results = SearchResults(),
                isSearching = false
            )
        }
    }
    
    /**
     * Play song from search
     */
    fun playSong(songId: String) {
        // TODO: Send to MediaService
        // mediaController.playSong(songId)
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

data class SearchUiState(
    val results: SearchResults = SearchResults(),
    val isSearching: Boolean = false,
    val error: String? = null
)
