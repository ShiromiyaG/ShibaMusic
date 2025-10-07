package com.shirou.shibamusic.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shirou.shibamusic.data.repository.MusicRepository
import com.shirou.shibamusic.media.MediaController
import com.shirou.shibamusic.ui.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for player screen and mini player
 * 
 * Manages:
 * - Playback state
 * - Player controls
 * - Queue management
 * - Favorites
 */
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val mediaController: MediaController,
    private val repository: MusicRepository
) : ViewModel() {
    
    // ==================== State ====================
    
    /**
     * Complete player state
     */
    val playerState: StateFlow<PlayerState> = mediaController.playerState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PlayerState()
        )
    
    /**
     * Now playing song
     */
    val nowPlaying: StateFlow<SongItem?> = playerState
        .map { it.nowPlaying }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
    
    /**
     * Is playing state
     */
    val isPlaying: StateFlow<Boolean> = playerState
        .map { it.isPlaying }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )
    
    /**
     * Queue
     */
    val queue: StateFlow<List<SongItem>> = playerState
        .map { it.queue }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    /**
     * UI State for player screen
     */
    data class PlayerUiState(
        val nowPlaying: SongItem? = null,
        val queue: List<SongItem> = emptyList(),
        val isPlaying: Boolean = false,
        val currentPosition: Long = 0L,
        val duration: Long = 0L,
        val repeatMode: RepeatMode = RepeatMode.OFF,
        val shuffleMode: Boolean = false,
        val hasNext: Boolean = false,
        val hasPrevious: Boolean = false,
        val isFavorite: Boolean = false,
        val error: String? = null
    )
    
    val uiState: StateFlow<PlayerUiState> = playerState
        .map { state ->
            PlayerUiState(
                nowPlaying = state.nowPlaying,
                queue = state.queue,
                isPlaying = state.isPlaying,
                currentPosition = state.progress.currentPosition,
                duration = state.progress.duration,
                repeatMode = state.repeatMode,
                shuffleMode = state.shuffleMode,
                hasNext = state.hasNext,
                hasPrevious = state.hasPrevious,
                isFavorite = state.nowPlaying?.isFavorite ?: false
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PlayerUiState()
        )
    
    // ==================== Initialization ====================
    
    init {
        viewModelScope.launch {
            mediaController.connect()
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        mediaController.disconnect()
    }
    
    // ==================== Playback Control ====================
    
    /**
     * Play or pause current song
     */
    fun playPause() {
        mediaController.playPause()
    }
    
    /**
     * Play
     */
    fun play() {
        mediaController.play()
    }
    
    /**
     * Pause
     */
    fun pause() {
        mediaController.pause()
    }
    
    /**
     * Skip to next song
     */
    fun skipToNext() {
        mediaController.skipToNext()
    }
    
    /**
     * Skip to previous song
     */
    fun skipToPrevious() {
        mediaController.skipToPrevious()
    }
    
    /**
     * Seek to position
     */
    fun seekTo(positionMs: Long) {
        mediaController.seekTo(positionMs)
        
        // Record play progress
        nowPlaying.value?.let { song ->
            viewModelScope.launch {
                repository.recordSongPlay(song.id)
            }
        }
    }
    
    /**
     * Seek to specific song in queue
     */
    fun seekToSong(index: Int) {
        mediaController.seekToSong(index)
    }
    
    // ==================== Queue Management ====================
    
    /**
     * Play a single song
     */
    fun playSong(song: SongItem) {
        mediaController.playSong(song)
        recordPlay(song.id)
    }
    
    /**
     * Play list of songs starting at index
     */
    fun playSongs(songs: List<SongItem>, startIndex: Int = 0) {
        mediaController.playSongs(songs, startIndex)
        if (songs.isNotEmpty()) {
            recordPlay(songs[startIndex].id)
        }
    }
    
    /**
     * Play album
     */
    fun playAlbum(albumId: String) {
        viewModelScope.launch {
            val songs = repository.getAlbumSongs(albumId)
            if (songs.isNotEmpty()) {
                playSongs(songs)
            }
        }
    }
    
    /**
     * Play album shuffled
     */
    fun shuffleAlbum(albumId: String) {
        viewModelScope.launch {
            val songs = repository.getAlbumSongs(albumId).shuffled()
            if (songs.isNotEmpty()) {
                playSongs(songs)
                mediaController.setShuffleMode(true)
            }
        }
    }
    
    /**
     * Play artist's songs
     */
    fun playArtist(artistId: String) {
        viewModelScope.launch {
            val songs = repository.getArtistSongs(artistId)
            if (songs.isNotEmpty()) {
                playSongs(songs)
            }
        }
    }
    
    /**
     * Play playlist
     */
    fun playPlaylist(playlistId: String) {
        viewModelScope.launch {
            val songs = repository.getPlaylistSongs(playlistId)
            if (songs.isNotEmpty()) {
                playSongs(songs)
            }
        }
    }
    
    /**
     * Add song to queue
     */
    fun addToQueue(song: SongItem) {
        mediaController.addToQueue(song)
    }
    
    /**
     * Add songs to queue
     */
    fun addToQueue(songs: List<SongItem>) {
        mediaController.addToQueue(songs)
    }
    
    /**
     * Play next (add after current)
     */
    fun playNext(song: SongItem) {
        mediaController.playNext(song)
    }
    
    /**
     * Remove from queue
     */
    fun removeFromQueue(index: Int) {
        mediaController.removeFromQueue(index)
    }
    
    /**
     * Clear queue
     */
    fun clearQueue() {
        mediaController.clearQueue()
    }
    
    // ==================== Modes ====================
    
    /**
     * Toggle shuffle mode
     */
    fun toggleShuffle() {
        mediaController.toggleShuffle()
    }
    
    /**
     * Toggle repeat mode (OFF -> ONE -> ALL -> OFF)
     */
    fun toggleRepeatMode() {
        mediaController.toggleRepeatMode()
    }
    
    // ==================== Favorites ====================
    
    /**
     * Toggle favorite for current song
     */
    fun toggleFavorite() {
        nowPlaying.value?.let { song ->
            viewModelScope.launch {
                repository.toggleSongFavorite(song.id, !song.isFavorite)
            }
        }
    }
    
    /**
     * Toggle favorite for specific song
     */
    fun toggleFavorite(songId: String, isFavorite: Boolean) {
        viewModelScope.launch {
            repository.toggleSongFavorite(songId, isFavorite)
        }
    }
    
    // ==================== Helpers ====================
    
    /**
     * Record song play in database
     */
    private fun recordPlay(songId: String) {
        viewModelScope.launch {
            repository.recordSongPlay(songId)
        }
    }
    
    /**
     * Get progress as percentage (0.0 to 1.0)
     */
    fun getProgressPercentage(): Float {
        val state = uiState.value
        return if (state.duration > 0) {
            state.currentPosition / state.duration.toFloat()
        } else {
            0f
        }
    }
    
    /**
     * Format time in milliseconds to MM:SS
     */
    fun formatTime(ms: Long): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%d:%02d", minutes, seconds)
    }
}
