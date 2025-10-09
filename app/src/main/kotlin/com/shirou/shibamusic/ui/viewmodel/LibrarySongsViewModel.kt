package com.shirou.shibamusic.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.shirou.shibamusic.data.repository.MusicRepository
import com.shirou.shibamusic.ui.model.SongItem
import com.shirou.shibamusic.worker.AlbumSyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LibrarySongsUiState(
    val sortOption: SongSortOption = SongSortOption.TITLE_ASC,
    val isSyncing: Boolean = false,
    val error: String? = null
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

    private val sortOptionFlow = MutableStateFlow(SongSortOption.TITLE_ASC)

    @OptIn(ExperimentalCoroutinesApi::class)
    val songs: Flow<PagingData<SongItem>> = sortOptionFlow
        .flatMapLatest { option ->
            val orderClause = buildOrderClause(option)
            musicRepository.observeSongsPaged(orderClause)
        }
        .cachedIn(viewModelScope)
    
    companion object {
        private const val TAG = "LibrarySongs"
    }
    
    private var hasRequestedSync = false
    private var syncInProgress = false

    init {
        observeSyncStatus()
        requestAlbumSync(force = false, syncSongs = true)
    }
    
    fun loadSongs(force: Boolean = false) {
        requestAlbumSync(force = force, syncSongs = true)
    }
    
    fun changeSortOption(option: SongSortOption) {
        if (option == _uiState.value.sortOption) return
        sortOptionFlow.value = option
        _uiState.value = _uiState.value.copy(sortOption = option)
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
            isSyncing = true,
            error = null
        )
    }

    private fun observeSyncStatus() {
        albumSyncScheduler
            .observeStatus()
            .onEach { infos ->
                val isRunning = infos.any { info ->
                    info.state == androidx.work.WorkInfo.State.ENQUEUED ||
                        info.state == androidx.work.WorkInfo.State.RUNNING
                }
                syncInProgress = isRunning
                _uiState.value = _uiState.value.copy(isSyncing = isRunning)
            }
            .catch { throwable ->
                Log.e(TAG, "Error observing sync status", throwable)
            }
            .launchIn(viewModelScope)
    }

    private fun buildOrderClause(option: SongSortOption): String {
        return when (option) {
            SongSortOption.TITLE_ASC -> "ORDER BY title COLLATE NOCASE ASC, id COLLATE NOCASE ASC"
            SongSortOption.TITLE_DESC -> "ORDER BY title COLLATE NOCASE DESC, id COLLATE NOCASE ASC"
            SongSortOption.ARTIST_ASC -> "ORDER BY artist_name COLLATE NOCASE ASC, title COLLATE NOCASE ASC"
            SongSortOption.ARTIST_DESC -> "ORDER BY artist_name COLLATE NOCASE DESC, title COLLATE NOCASE ASC"
            SongSortOption.ALBUM_ASC -> "ORDER BY COALESCE(album_name, '') COLLATE NOCASE ASC, title COLLATE NOCASE ASC"
            SongSortOption.ALBUM_DESC -> "ORDER BY COALESCE(album_name, '') COLLATE NOCASE DESC, title COLLATE NOCASE ASC"
            SongSortOption.DURATION_ASC -> "ORDER BY duration_ms ASC, title COLLATE NOCASE ASC"
            SongSortOption.DURATION_DESC -> "ORDER BY duration_ms DESC, title COLLATE NOCASE ASC"
            SongSortOption.RECENTLY_ADDED -> "ORDER BY date_added DESC, title COLLATE NOCASE ASC"
        }
    }
}
