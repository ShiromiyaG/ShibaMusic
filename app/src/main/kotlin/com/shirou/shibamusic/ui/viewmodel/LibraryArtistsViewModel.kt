package com.shirou.shibamusic.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.shirou.shibamusic.data.repository.MusicRepository
import com.shirou.shibamusic.ui.model.ArtistItem
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

data class LibraryArtistsUiState(
    val sortOption: ArtistSortOption = ArtistSortOption.NAME_ASC,
    val isSyncing: Boolean = false,
    val error: String? = null
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
    
    companion object {
        private const val TAG = "LibraryArtists"
    }
    
    private val _uiState = MutableStateFlow(LibraryArtistsUiState())
    val uiState: StateFlow<LibraryArtistsUiState> = _uiState.asStateFlow()

    private val sortOptionFlow = MutableStateFlow(ArtistSortOption.NAME_ASC)

    val artists: Flow<PagingData<ArtistItem>> = sortOptionFlow
        .flatMapLatest { option ->
            val orderClause = buildOrderClause(option)
            musicRepository.observeArtistsPaged(orderClause)
        }
        .cachedIn(viewModelScope)

    private var hasRequestedSync = false
    private var syncInProgress = false
    
    init {
        observeSyncStatus()
        requestAlbumSync(force = false)
    }
    
    fun loadArtists(force: Boolean = false) {
        requestAlbumSync(force = force)
    }
    
    fun changeSortOption(option: ArtistSortOption) {
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
        Log.d(TAG, "Requesting artist sync (force=$force)")
        albumSyncScheduler.enqueueAlbumSync(force = force)

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
                _uiState.value = _uiState.value.copy(error = throwable.message)
            }
            .launchIn(viewModelScope)
    }

    private fun buildOrderClause(option: ArtistSortOption): String {
        return when (option) {
            ArtistSortOption.NAME_ASC -> "ORDER BY name COLLATE NOCASE ASC, id COLLATE NOCASE ASC"
            ArtistSortOption.NAME_DESC -> "ORDER BY name COLLATE NOCASE DESC, id COLLATE NOCASE ASC"
            ArtistSortOption.ALBUM_COUNT_DESC -> "ORDER BY album_count DESC, name COLLATE NOCASE ASC"
            ArtistSortOption.ALBUM_COUNT_ASC -> "ORDER BY album_count ASC, name COLLATE NOCASE ASC"
            ArtistSortOption.SONG_COUNT_DESC -> "ORDER BY song_count DESC, name COLLATE NOCASE ASC"
            ArtistSortOption.SONG_COUNT_ASC -> "ORDER BY song_count ASC, name COLLATE NOCASE ASC"
        }
    }
}
