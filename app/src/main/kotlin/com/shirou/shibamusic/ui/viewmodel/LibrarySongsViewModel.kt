package com.shirou.shibamusic.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shirou.shibamusic.data.repository.MusicRepository
import com.shirou.shibamusic.ui.model.SongItem
import com.shirou.shibamusic.worker.AlbumSyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LibrarySongsUiState(
    val songs: List<SongItem> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val sortOption: SongSortOption = SongSortOption.TITLE_ASC
)

enum class SongSortOption(val displayName: String) {
    TITLE_ASC("Title A-Z"),
    TITLE_DESC("Title Z-A"),
    ARTIST_ASC("Artist A-Z"),
    ARTIST_DESC("Artist Z-A"),
    ALBUM_ASC("Album A-Z"),
    ALBUM_DESC("Album Z-A"),
    DURATION_ASC("Shortest First"),
    DURATION_DESC("Longest First"),
    RECENTLY_ADDED("Recently Added")
}

@HiltViewModel
class LibrarySongsViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val albumSyncScheduler: AlbumSyncScheduler
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(LibrarySongsUiState())
    val uiState: StateFlow<LibrarySongsUiState> = _uiState.asStateFlow()
    
    companion object {
        private const val TAG = "LibrarySongs"
    }
    
    private var hasRequestedSync = false
    private var syncInProgress = false

    init {
        observeSongs()
        observeSyncStatus()
        requestAlbumSync(force = false, syncSongs = true)
    }
    
    fun loadSongs(force: Boolean = false) {
        requestAlbumSync(force = force, syncSongs = true)
    }
    
    fun changeSortOption(option: SongSortOption) {
        val sortedSongs = sortSongs(_uiState.value.songs, option)
        _uiState.value = _uiState.value.copy(
            songs = sortedSongs,
            sortOption = option
        )
    }
    
    private fun observeSongs() {
        viewModelScope.launch {
            musicRepository
                .observeSongs()
                .onEach { songs ->
                    Log.d(TAG, "Song flow emitted ${songs.size} items")

                    if (songs.isEmpty()) {
                        if (!hasRequestedSync) {
                            requestAlbumSync(force = false, syncSongs = true)
                        }

                        _uiState.value = _uiState.value.copy(
                            songs = emptyList(),
                            isLoading = syncInProgress,
                            error = null
                        )
                    } else {
                        syncInProgress = false
                        val sortedSongs = sortSongs(songs, _uiState.value.sortOption)
                        _uiState.value = _uiState.value.copy(
                            songs = sortedSongs,
                            isLoading = false,
                            error = null
                        )
                    }
                }
                .catch { throwable ->
                    Log.e(TAG, "Error observing songs", throwable)
                    syncInProgress = false
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = throwable.message ?: "Failed to load songs"
                    )
                }
                .collect()
        }
    }

    private fun requestAlbumSync(force: Boolean, syncSongs: Boolean) {
        if (!force && hasRequestedSync) {
            return
        }

        hasRequestedSync = true
        syncInProgress = true
        Log.d(TAG, "Requesting song sync (force=$force, syncSongs=$syncSongs)")
        albumSyncScheduler.enqueueAlbumSync(
            force = force,
            syncSongs = syncSongs
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
                        info.state == androidx.work.WorkInfo.State.ENQUEUED ||
                            info.state == androidx.work.WorkInfo.State.RUNNING
                    }
                    syncInProgress = isRunning

                    if (!isRunning && _uiState.value.songs.isEmpty()) {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                    }
                }
                .catch { throwable ->
                    Log.e(TAG, "Error observing sync status", throwable)
                }
                .collect()
        }
    }
    
    private fun sortSongs(songs: List<SongItem>, option: SongSortOption): List<SongItem> {
        if (songs.isEmpty()) return songs
        return when (option) {
            SongSortOption.TITLE_ASC -> songs.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
            SongSortOption.TITLE_DESC -> songs.sortedWith(compareByDescending(String.CASE_INSENSITIVE_ORDER) { it.title })
            SongSortOption.ARTIST_ASC -> songs.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.artistName })
            SongSortOption.ARTIST_DESC -> songs.sortedWith(compareByDescending(String.CASE_INSENSITIVE_ORDER) { it.artistName })
            SongSortOption.ALBUM_ASC -> songs.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.albumName ?: "" })
            SongSortOption.ALBUM_DESC -> songs.sortedWith(compareByDescending(String.CASE_INSENSITIVE_ORDER) { it.albumName ?: "" })
            SongSortOption.DURATION_ASC -> songs.sortedBy { it.duration }
            SongSortOption.DURATION_DESC -> songs.sortedByDescending { it.duration }
            SongSortOption.RECENTLY_ADDED -> songs.asReversed()
        }
    }
}
