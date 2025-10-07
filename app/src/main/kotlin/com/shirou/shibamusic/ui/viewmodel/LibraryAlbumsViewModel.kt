package com.shirou.shibamusic.ui.viewmodel

import android.util.Log
import androidx.work.WorkInfo
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shirou.shibamusic.data.repository.MusicRepository
import com.shirou.shibamusic.worker.AlbumSyncScheduler
import com.shirou.shibamusic.ui.model.AlbumItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LibraryAlbumsUiState(
    val albums: List<AlbumItem> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val sortOption: AlbumSortOption = AlbumSortOption.TITLE_ASC
)

enum class AlbumSortOption(val displayName: String) {
    TITLE_ASC("Title A-Z"),
    TITLE_DESC("Title Z-A"),
    ARTIST_ASC("Artist A-Z"),
    ARTIST_DESC("Artist Z-A"),
    YEAR_DESC("Newest First"),
    YEAR_ASC("Oldest First"),
    RECENTLY_ADDED("Recently Added")
}

@HiltViewModel
class LibraryAlbumsViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val albumSyncScheduler: AlbumSyncScheduler
) : ViewModel() {
    
    companion object {
        private const val TAG = "LibraryAlbums"
    }
    
    private val _uiState = MutableStateFlow(LibraryAlbumsUiState())
    val uiState: StateFlow<LibraryAlbumsUiState> = _uiState.asStateFlow()

    private var hasRequestedSync = false
    private var syncInProgress = false
    
    init {
        Log.d(TAG, "LibraryAlbumsViewModel initialized")
        observeAlbums()
        observeSyncStatus()
        requestAlbumSync(force = false)
    }
    
    fun loadAlbums(force: Boolean = false) {
        requestAlbumSync(force)
    }

    private fun observeAlbums() {
        viewModelScope.launch {
            musicRepository
                .observeAlbums()
                .onEach { albums ->
                    Log.d(TAG, "Album flow emitted ${albums.size} items")

                    val sortedAlbums = sortAlbums(albums, _uiState.value.sortOption)

                    if (albums.isEmpty()) {
                        if (!hasRequestedSync) {
                            requestAlbumSync(force = false)
                        }

                        _uiState.value = _uiState.value.copy(
                            albums = emptyList(),
                            isLoading = syncInProgress,
                            error = null
                        )
                    } else {
                        syncInProgress = false
                        _uiState.value = _uiState.value.copy(
                            albums = sortedAlbums,
                            isLoading = false,
                            error = null
                        )
                    }
                }
                .catch { throwable ->
                    Log.e(TAG, "Error observing albums", throwable)
                    syncInProgress = false
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = throwable.message ?: "Failed to load albums"
                    )
                }
                .collect()
        }
    }
    
    fun changeSortOption(option: AlbumSortOption) {
        val sortedAlbums = sortAlbums(_uiState.value.albums, option)
        _uiState.value = _uiState.value.copy(
            albums = sortedAlbums,
            sortOption = option
        )
    }

    private fun requestAlbumSync(force: Boolean) {
        if (!force && hasRequestedSync) {
            return
        }

        hasRequestedSync = true
        syncInProgress = true
        Log.d(TAG, "Requesting album sync (force=$force)")
        albumSyncScheduler.enqueueAlbumSync(
            force = force,
            syncSongs = true
        )

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
                        info.state == WorkInfo.State.ENQUEUED || info.state == WorkInfo.State.RUNNING
                    }
                    syncInProgress = isRunning

                    if (!isRunning && _uiState.value.albums.isEmpty()) {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                    }
                }
                .catch { throwable ->
                    Log.e(TAG, "Error observing sync status", throwable)
                }
                .collect()
        }
    }
    
    private fun sortAlbums(albums: List<AlbumItem>, option: AlbumSortOption): List<AlbumItem> {
        if (albums.isEmpty()) return albums
        return when (option) {
            AlbumSortOption.TITLE_ASC -> albums.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
            AlbumSortOption.TITLE_DESC -> albums.sortedWith(compareByDescending(String.CASE_INSENSITIVE_ORDER) { it.title })
            AlbumSortOption.ARTIST_ASC -> albums.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.artistName })
            AlbumSortOption.ARTIST_DESC -> albums.sortedWith(compareByDescending(String.CASE_INSENSITIVE_ORDER) { it.artistName })
            AlbumSortOption.YEAR_DESC -> albums.sortedByDescending { it.year ?: 0 }
            AlbumSortOption.YEAR_ASC -> albums.sortedBy { it.year ?: 0 }
            AlbumSortOption.RECENTLY_ADDED -> albums.asReversed()
        }
    }
}
