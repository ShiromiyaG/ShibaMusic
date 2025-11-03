package com.shirou.shibamusic.ui.viewmodel

import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.work.WorkInfo
import com.shirou.shibamusic.R
import com.shirou.shibamusic.data.repository.MusicRepository
import com.shirou.shibamusic.ui.model.AlbumItem
import com.shirou.shibamusic.util.Preferences
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

enum class AlbumSortOption(
    @StringRes val labelResId: Int,
    val orderClause: String
) {
    TITLE_ASC(
        R.string.sort_title_asc,
        "ORDER BY title COLLATE NOCASE ASC, id COLLATE NOCASE ASC"
    ),
    TITLE_DESC(
        R.string.sort_title_desc,
        "ORDER BY title COLLATE NOCASE DESC, id COLLATE NOCASE ASC"
    ),
    ARTIST_ASC(
        R.string.sort_artist_asc,
        "ORDER BY artist_name COLLATE NOCASE ASC, title COLLATE NOCASE ASC"
    ),
    ARTIST_DESC(
        R.string.sort_artist_desc,
        "ORDER BY artist_name COLLATE NOCASE DESC, title COLLATE NOCASE ASC"
    ),
    YEAR_DESC(
        R.string.sort_year_desc,
        "ORDER BY COALESCE(year, 0) DESC, title COLLATE NOCASE ASC"
    ),
    YEAR_ASC(
        R.string.sort_year_asc,
        "ORDER BY COALESCE(year, 0) ASC, title COLLATE NOCASE ASC"
    ),
    RECENTLY_ADDED(
        R.string.sort_recently_added,
        "ORDER BY date_added DESC, title COLLATE NOCASE ASC"
    )
}

@HiltViewModel
class LibraryAlbumsViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val albumSyncScheduler: AlbumSyncScheduler
) : ViewModel() {
    
    companion object {
        private const val TAG = "LibraryAlbums"
    }
    
    private val initialSort = Preferences.getLibraryAlbumSort()
        ?.let { stored -> runCatching { AlbumSortOption.valueOf(stored) }.getOrNull() }
        ?: AlbumSortOption.TITLE_ASC

    private val _uiState = MutableStateFlow(LibraryAlbumsUiState(sortOption = initialSort))
    val uiState: StateFlow<LibraryAlbumsUiState> = _uiState.asStateFlow()

    private val sortOptionFlow = MutableStateFlow(initialSort)

    val albums: Flow<PagingData<AlbumItem>> = sortOptionFlow
        .flatMapLatest { option ->
            musicRepository.observeAlbumsPaged(option.orderClause)
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
        Preferences.setLibraryAlbumSort(option.name)
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

}
