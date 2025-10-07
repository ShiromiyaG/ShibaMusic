package com.shirou.shibamusic.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shirou.shibamusic.data.repository.MusicRepository
import com.shirou.shibamusic.ui.model.ArtistItem
import com.shirou.shibamusic.worker.AlbumSyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LibraryArtistsUiState(
    val artists: List<ArtistItem> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val sortOption: ArtistSortOption = ArtistSortOption.NAME_ASC
)

enum class ArtistSortOption(val displayName: String) {
    NAME_ASC("Name A-Z"),
    NAME_DESC("Name Z-A"),
    ALBUM_COUNT_DESC("Most Albums"),
    ALBUM_COUNT_ASC("Fewest Albums"),
    SONG_COUNT_DESC("Most Songs"),
    SONG_COUNT_ASC("Fewest Songs")
}

@HiltViewModel
class LibraryArtistsViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val albumSyncScheduler: AlbumSyncScheduler
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(LibraryArtistsUiState())
    val uiState: StateFlow<LibraryArtistsUiState> = _uiState.asStateFlow()
    
    companion object {
        private const val TAG = "LibraryArtists"
    }
    
    private var hasRequestedSync = false
    private var syncInProgress = false
    
    init {
        observeArtists()
        observeSyncStatus()
        requestAlbumSync(force = false)
    }
    
    fun loadArtists(force: Boolean = false) {
        requestAlbumSync(force = force)
    }
    
    fun changeSortOption(option: ArtistSortOption) {
        val sortedArtists = sortArtists(_uiState.value.artists, option)
        _uiState.value = _uiState.value.copy(
            artists = sortedArtists,
            sortOption = option
        )
    }
    
    private fun observeArtists() {
        viewModelScope.launch {
            musicRepository
                .observeArtists()
                .onEach { artists ->
                    Log.d(TAG, "Artist flow emitted ${artists.size} items")

                    if (artists.isEmpty()) {
                        if (!hasRequestedSync) {
                            requestAlbumSync(force = false)
                        }

                        _uiState.value = _uiState.value.copy(
                            artists = emptyList(),
                            isLoading = syncInProgress,
                            error = null
                        )
                    } else {
                        syncInProgress = false
                        val sortedArtists = sortArtists(artists, _uiState.value.sortOption)
                        _uiState.value = _uiState.value.copy(
                            artists = sortedArtists,
                            isLoading = false,
                            error = null
                        )
                    }
                }
                .catch { throwable ->
                    Log.e(TAG, "Error observing artists", throwable)
                    syncInProgress = false
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = throwable.message ?: "Failed to load artists"
                    )
                }
                .collect()
        }
    }

    private fun requestAlbumSync(force: Boolean) {
        if (!force && hasRequestedSync) {
            return
        }

        hasRequestedSync = true
        syncInProgress = true
        Log.d(TAG, "Requesting artist sync (force=$force)")
        albumSyncScheduler.enqueueAlbumSync(force = force)

        _uiState.value = _uiState.value.copy(
            isLoading = true,
            error = null
        )
    }

    private fun observeSyncStatus() {
        viewModelScope.launch {
            albumSyncScheduler
                .observeStatus()
                .onEach { infos ->
                    val isRunning = infos.any { info ->
                        info.state == androidx.work.WorkInfo.State.ENQUEUED ||
                            info.state == androidx.work.WorkInfo.State.RUNNING
                    }
                    syncInProgress = isRunning

                    if (!isRunning && _uiState.value.artists.isEmpty()) {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                    }
                }
                .catch { throwable ->
                    Log.e(TAG, "Error observing sync status", throwable)
                }
                .collect()
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
}
