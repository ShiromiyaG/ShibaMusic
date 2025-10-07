package com.shirou.shibamusic.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shirou.shibamusic.App
import com.shirou.shibamusic.R
import com.shirou.shibamusic.data.repository.MusicRepository
import com.shirou.shibamusic.data.repository.SyncRepository
import com.shirou.shibamusic.ui.model.AlbumItem
import com.shirou.shibamusic.ui.model.SongItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = false,
    val isSyncing: Boolean = false,
    val recentlyAddedAlbums: List<AlbumItem> = emptyList(),
    val favoriteAlbums: List<AlbumItem> = emptyList(),
    val mostPlayedSongs: List<SongItem> = emptyList(),
    val allSongs: List<SongItem> = emptyList(),
    val allAlbums: List<AlbumItem> = emptyList(),
    val error: String? = null,
    val syncMessage: String? = null
)

private data class HomeCollections(
    val albums: List<AlbumItem>,
    val recentlyAdded: List<AlbumItem>,
    val favoriteAlbums: List<AlbumItem>,
    val mostPlayedSongs: List<SongItem>
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val syncRepository: SyncRepository
) : ViewModel() {
    
    private val chronologyRepository = com.shirou.shibamusic.repository.ChronologyRepository()
    
    companion object {
        private const val TAG = "ShibaMusicHome"
    }
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    init {
        Log.d(TAG, "HomeViewModel initialized")
        // Ensure serverId is set
        if (com.shirou.shibamusic.util.Preferences.getServerId().isNullOrEmpty()) {
            val server = com.shirou.shibamusic.util.Preferences.getServer()
            if (!server.isNullOrEmpty()) {
                com.shirou.shibamusic.util.Preferences.setServerId(server)
                Log.d(TAG, "Set serverId to: $server")
            }
        }
        checkAndSyncIfNeeded()
    }
    
    /**
     * Check if we need to sync from server
     * Syncs if there's no local data or if server has new content
     */
    private fun checkAndSyncIfNeeded() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                val localSongs = musicRepository.getAllSongs()
                Log.d(TAG, "Found ${localSongs.size} songs in local database")
                
                // If we have data, show it immediately
                if (localSongs.isNotEmpty()) {
                    val collections = loadHomeCollections()

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        recentlyAddedAlbums = collections.recentlyAdded,
                        favoriteAlbums = collections.favoriteAlbums,
                        mostPlayedSongs = collections.mostPlayedSongs,
                        allSongs = localSongs,
                        allAlbums = collections.albums
                    )

                    // Check if server has new content in background
                    checkForNewContent()
                } else {
                    // No data - start sync immediately
                    Log.d(TAG, "No data in database, starting sync...")
                    syncFromServer()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in checkAndSyncIfNeeded", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load data: ${e.message}"
                )
            }
        }
    }
    
    private fun checkForNewContent() {
        viewModelScope.launch {
            try {
                val response = App.getSubsonicClientInstance(false)
                    .mediaLibraryScanningClient
                    .getScanStatus()
                    .execute()
                
                if (response.isSuccessful) {
                    val scanStatus = response.body()?.subsonicResponse?.scanStatus
                    val serverCount = scanStatus?.count ?: 0
                    val localCount = musicRepository.getAllSongs().size
                    
                    Log.d(TAG, "Server songs: $serverCount, Local songs: $localCount")
                    
                    // If server has more songs, sync
                    if (serverCount > localCount) {
                        Log.d(TAG, "New content detected, syncing...")
                        syncFromServer()
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not check for new content", e)
            }
        }
    }
    
    fun loadHomeData() {
        Log.d(TAG, ">>>>> loadHomeData() called")
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // First try to load from local database
                Log.d(TAG, "Loading songs from database...")
                val localSongs = musicRepository.getAllSongs()
                Log.d(TAG, "Found ${localSongs.size} songs in database")
                
                // If database is empty, start initial sync
                if (localSongs.isEmpty()) {
                    Log.d(TAG, "Database is empty! Starting initial sync...")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isSyncing = true,
                        syncMessage = App.getContext().getString(R.string.home_sync_loading)
                    )
                    
                    // Perform initial sync
                    syncRepository.performInitialSync().onSuccess { stats ->
                        // Reload data after sync
                        val songs = musicRepository.getAllSongs()
                        val albums = musicRepository.getAllAlbums()
                        
                        _uiState.value = _uiState.value.copy(
                            isSyncing = false,
                            allSongs = songs,
                            allAlbums = albums,
                            syncMessage = App.getContext().getString(
                                R.string.home_sync_result,
                                stats.albumCount,
                                stats.songCount
                            ),
                            error = null
                        )
                    }.onFailure { error ->
                        _uiState.value = _uiState.value.copy(
                            isSyncing = false,
                            error = App.getContext().getString(
                                R.string.home_sync_error,
                                error.message ?: ""
                            ),
                            syncMessage = null
                        )
                    }
                } else {
                    Log.d(TAG, "Database has songs! Loading UI data...")
                    
                    val collections = loadHomeCollections()
                    Log.d(TAG, "Loaded ${collections.recentlyAdded.size} recent albums")
                    Log.d(TAG, "Loaded ${collections.favoriteAlbums.size} favorite albums")
                    Log.d(TAG, "Updating UI state with ${localSongs.size} songs and ${collections.albums.size} albums")

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        recentlyAddedAlbums = collections.recentlyAdded,
                        favoriteAlbums = collections.favoriteAlbums,
                        mostPlayedSongs = collections.mostPlayedSongs,
                        allSongs = localSongs,
                        allAlbums = collections.albums,
                        error = null
                    )
                    Log.d(TAG, "UI state updated successfully!")
                }
            } catch (e: Exception) {
                val errorMessage = e.message?.let {
                    App.getContext().getString(R.string.home_load_failed_with_reason, it)
                } ?: App.getContext().getString(R.string.home_load_failed)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isSyncing = false,
                    error = errorMessage
                )
            }
        }
    }

    fun refresh() {
        loadHomeData()
    }

    /**
     * Sync data from Navidrome server
     */
    private suspend fun syncFromServer() {
        _uiState.value = _uiState.value.copy(
            isSyncing = true,
            syncMessage = App.getContext().getString(R.string.home_sync_in_progress)
        )

        Log.d(TAG, "Starting sync from Navidrome server...")

        syncRepository.performInitialSync().onSuccess { stats ->
            Log.d(TAG, "Sync successful! Albums: ${stats.albumCount}, Songs: ${stats.songCount}")
            // Reload data from database
            val (songs, collections) = coroutineScope {
                val songsDeferred = async { musicRepository.getAllSongs() }
                val collectionsDeferred = async { loadHomeCollections() }
                songsDeferred.await() to collectionsDeferred.await()
            }

            _uiState.value = _uiState.value.copy(
                isSyncing = false,
                isLoading = false,
                allSongs = songs,
                allAlbums = collections.albums,
                recentlyAddedAlbums = collections.recentlyAdded,
                favoriteAlbums = collections.favoriteAlbums,
                mostPlayedSongs = collections.mostPlayedSongs,
                syncMessage = App.getContext().getString(
                    R.string.home_sync_success,
                    stats.albumCount,
                    stats.songCount
                ),
                error = null
            )
            
            Log.d(TAG, "UI updated with ${songs.size} songs and ${collections.albums.size} albums")
        }.onFailure { error ->
            Log.e(TAG, "Sync failed: ${error.message}", error)
            _uiState.value = _uiState.value.copy(
                isSyncing = false,
                isLoading = false,
                error = App.getContext().getString(
                    R.string.home_sync_error,
                    error.message ?: ""
                ),
                syncMessage = null
            )
        }
    }
    
    fun syncNow() {
        viewModelScope.launch {
            syncFromServer()
        }
    }
    
    private suspend fun getRecentlyAddedAlbums(albumsOverride: List<AlbumItem>? = null): List<AlbumItem> {
        return try {
            val allAlbums = albumsOverride ?: run {
                Log.d(TAG, "Loading recently added albums from database...")
                musicRepository.getAllAlbums()
            }
            if (allAlbums.isEmpty()) {
                Log.d(TAG, "No albums in database")
                return emptyList()
            }
            
            val result = allAlbums
                .sortedByDescending { it.dateAdded }
                .take(10)
            
            Log.d(TAG, "Returning ${result.size} recently added albums")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error loading recently added albums", e)
            emptyList()
        }
    }

    private suspend fun loadHomeCollections(albumsOverride: List<AlbumItem>? = null): HomeCollections = coroutineScope {
        val albumsDeferred = async { albumsOverride ?: musicRepository.getAllAlbums() }
        val favoriteAlbumsDeferred = async { musicRepository.getFavoriteAlbums() }
        val mostPlayedDeferred = async { getMostPlayedSongs() }

        val albums = albumsDeferred.await()
        val recentlyAddedDeferred = async { getRecentlyAddedAlbums(albums) }

        HomeCollections(
            albums = albums,
            recentlyAdded = recentlyAddedDeferred.await(),
            favoriteAlbums = favoriteAlbumsDeferred.await(),
            mostPlayedSongs = mostPlayedDeferred.await()
        )
    }

    private suspend fun getMostPlayedSongs(limit: Int = 20): List<SongItem> {
        return try {
            val serverId = com.shirou.shibamusic.util.Preferences.getServerId().orEmpty()
            if (serverId.isBlank()) {
                Log.w(TAG, "Cannot load most played songs: serverId is blank")
                return emptyList()
            }

            val chronology = chronologyRepository.getLastPlayed(serverId, limit)
            val songIds = chronology.map { it.id }.distinct()
            if (songIds.isEmpty()) {
                return emptyList()
            }

            songIds.mapNotNull { musicRepository.getSongById(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading most played songs", e)
            emptyList()
        }
    }
}
