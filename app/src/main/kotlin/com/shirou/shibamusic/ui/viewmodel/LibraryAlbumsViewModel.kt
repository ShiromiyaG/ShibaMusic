package com.shirou.shibamusic.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.work.WorkInfo
import com.shirou.shibamusic.data.repository.MusicRepository
import com.shirou.shibamusic.ui.model.AlbumItem
import com.shirou.shibamusic.worker.AlbumSyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

data class LibraryAlbumsUiState(
    val sortOption: AlbumSortOption = AlbumSortOption.TITLE_ASC,
    val isSyncing: Boolean = false,
    val error: String? = null
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

    private val sortOptionFlow = MutableStateFlow(AlbumSortOption.TITLE_ASC)

    val albums: Flow<PagingData<AlbumItem>> = sortOptionFlow
        .flatMapLatest { option ->
            val orderClause = buildOrderClause(option)
            musicRepository.observeAlbumsPaged(orderClause)
        }
        .cachedIn(viewModelScope)

    private var hasRequestedSync = false
    private var syncInProgress = false

    init {
        Log.d(TAG, "LibraryAlbumsViewModel initialized")
        observeSyncStatus()
        requestAlbumSync(force = false)
    }

    fun loadAlbums(force: Boolean = false) {
        requestAlbumSync(force)
    }

    fun changeSortOption(option: AlbumSortOption) {
        if (option == _uiState.value.sortOption) return
        sortOptionFlow.value = option
        _uiState.value = _uiState.value.copy(sortOption = option)
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
            isSyncing = true,
            error = null
        )
    }

    private fun observeSyncStatus() {
        albumSyncScheduler
            .observeStatus()
            .onEach { infos ->
                val isRunning = infos.any { info ->
                    info.state == WorkInfo.State.ENQUEUED || info.state == WorkInfo.State.RUNNING
                }
                syncInProgress = isRunning
                _uiState.value = _uiState.value.copy(isSyncing = isRunning)
            }
            .catch { throwable ->
                Log.e(TAG, "Error observing sync status", throwable)
                _uiState.value = _uiState.value.copy(error = throwable.message)
            }
            .launchIn(viewModelScope)
    }

    private fun buildOrderClause(option: AlbumSortOption): String {
        return when (option) {
            AlbumSortOption.TITLE_ASC -> "ORDER BY title COLLATE NOCASE ASC, id COLLATE NOCASE ASC"
            AlbumSortOption.TITLE_DESC -> "ORDER BY title COLLATE NOCASE DESC, id COLLATE NOCASE ASC"
            AlbumSortOption.ARTIST_ASC -> "ORDER BY artist_name COLLATE NOCASE ASC, title COLLATE NOCASE ASC"
            AlbumSortOption.ARTIST_DESC -> "ORDER BY artist_name COLLATE NOCASE DESC, title COLLATE NOCASE ASC"
            AlbumSortOption.YEAR_DESC -> "ORDER BY COALESCE(year, 0) DESC, title COLLATE NOCASE ASC"
            AlbumSortOption.YEAR_ASC -> "ORDER BY COALESCE(year, 0) ASC, title COLLATE NOCASE ASC"
            AlbumSortOption.RECENTLY_ADDED -> "ORDER BY date_added DESC, title COLLATE NOCASE ASC"
        }
    }
}
