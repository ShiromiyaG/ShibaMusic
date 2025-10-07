package com.shirou.shibamusic.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shirou.shibamusic.data.repository.MusicRepository
import com.shirou.shibamusic.ui.model.AlbumItem
import com.shirou.shibamusic.ui.model.ArtistDetailModel
import com.shirou.shibamusic.ui.model.ArtistItem
import com.shirou.shibamusic.ui.model.SongItem
import com.shirou.shibamusic.ui.navigation.NavArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel responsible for loading artist detail information.
 */
@HiltViewModel
class ArtistDetailViewModel @Inject constructor(
    private val repository: MusicRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val artistId: String = savedStateHandle[NavArgs.ARTIST_ID] ?: ""

    private val _uiState = MutableStateFlow(ArtistDetailUiState())
    val uiState: StateFlow<ArtistDetailUiState> = _uiState.asStateFlow()

    init {
        loadArtist()
    }

    fun refresh() {
        loadArtist()
    }

    private fun loadArtist() {
        if (artistId.isBlank()) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    error = "Invalid artist id"
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val artist = repository.getArtistById(artistId)
                val albums = repository.getArtistAlbums(artistId)
                val songs = repository.getArtistSongs(artistId)

                if (artist == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Artist not found"
                        )
                    }
                    return@launch
                }

                val popularSongs = songs
                    .sortedByDescending { it.playCount }
                    .ifEmpty { songs }
                    .take(10)

                _uiState.update {
                    it.copy(
                        artist = artist.toDetailModel(),
                        albums = albums,
                        songs = songs,
                        popularSongs = popularSongs,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load artist"
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun ArtistItem.toDetailModel(): ArtistDetailModel = ArtistDetailModel(
        id = id,
        name = name,
        imageUrl = imageUrl,
        albumCount = albumCount,
        songCount = songCount
    )
}

data class ArtistDetailUiState(
    val artist: ArtistDetailModel? = null,
    val albums: List<AlbumItem> = emptyList(),
    val songs: List<SongItem> = emptyList(),
    val popularSongs: List<SongItem> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)
