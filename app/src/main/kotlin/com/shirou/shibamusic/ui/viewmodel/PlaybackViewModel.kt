package com.shirou.shibamusic.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shirou.shibamusic.data.repository.MusicRepository
import com.shirou.shibamusic.media.MediaController
import com.shirou.shibamusic.ui.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing music playback across the app
 * Provides a single source of truth for playback state
 */
@HiltViewModel
class PlaybackViewModel @Inject constructor(
    private val mediaController: MediaController,
    private val musicRepository: MusicRepository
) : ViewModel() {
    
    private val _playbackState = MutableStateFlow(PlayerState())
    val playbackState: StateFlow<PlayerState> = _playbackState.asStateFlow()
    private val chronologyRepository = com.shirou.shibamusic.repository.ChronologyRepository()
    
    companion object {
        private const val TAG = "PlaybackViewModel"
    }
    
    init {
        viewModelScope.launch {
            try {
                mediaController.ensureConnected()
                mediaController.playerState.collect { state ->
                    _playbackState.value = state
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error observing playback state", e)
            }
        }
    }

    private fun launchWithController(action: MediaController.() -> Unit) {
        viewModelScope.launch {
            try {
                if (mediaController.ensureConnected()) {
                    mediaController.action()
                } else {
                    Log.e(TAG, "MediaService not connected; action skipped")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error executing media action", e)
            }
        }
    }
    
    // ==================== Playback Control ====================
    
    /**
     * Play a single song
     */
    fun playSong(song: SongItem) {
        Log.d(TAG, "Playing song: ${song.title}")
        saveToChronology(song)
        launchWithController { 
            Log.d(TAG, "MediaController.playSong called for: ${song.title}")
            playSong(song) 
        }
    }
    
    private fun saveToChronology(song: SongItem) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Saving to chronology: ${song.title}")
                val serverId = com.shirou.shibamusic.util.Preferences.getServerId() ?: ""
                Log.d(TAG, "Using serverId: '$serverId'")
                val chronology = com.shirou.shibamusic.model.Chronology(song.id).apply {
                    title = song.title
                    artist = song.artistName
                    album = song.albumName
                    albumId = song.albumId
                    artistId = song.artistId
                    server = serverId
                    timestamp = System.currentTimeMillis()
                }
                chronologyRepository.insertSync(chronology)
                Log.d(TAG, "Saved to chronology: ${song.title}")
            } catch (e: Exception) {
                Log.e(TAG, "Error saving to chronology", e)
            }
        }
    }
    
    /**
     * Play a list of songs starting at index
     */
    fun playSongs(songs: List<SongItem>, startIndex: Int = 0) {
        Log.d(TAG, "Playing ${songs.size} songs, starting at index $startIndex")
        if (songs.isNotEmpty() && startIndex in songs.indices) {
            saveToChronology(songs[startIndex])
        }
        launchWithController { playSongs(songs, startIndex) }
    }
    
    /**
     * Play all songs from an album
     */
    fun playAlbum(albumId: String) {
        viewModelScope.launch {
            try {
                val songs = musicRepository.getAlbumSongs(albumId)
                Log.d(TAG, "Playing album with ${songs.size} songs")
                if (songs.isNotEmpty()) {
                    saveToChronology(songs[0])
                }
                if (mediaController.ensureConnected()) {
                    mediaController.playSongs(songs)
                } else {
                    Log.e(TAG, "Unable to connect to MediaService to play album")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error playing album", e)
            }
        }
    }
    
    /**
     * Play all songs from an artist
     */
    fun playArtist(artistId: String) {
        viewModelScope.launch {
            try {
                val songs = musicRepository.getArtistSongs(artistId)
                Log.d(TAG, "Playing artist with ${songs.size} songs")
                if (songs.isNotEmpty()) {
                    saveToChronology(songs[0])
                }
                if (mediaController.ensureConnected()) {
                    mediaController.playSongs(songs)
                } else {
                    Log.e(TAG, "Unable to connect to MediaService to play artist")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error playing artist", e)
            }
        }
    }

    /**
     * Play all songs from a playlist
     */
    fun playPlaylist(playlistId: String) {
        viewModelScope.launch {
            try {
                val songs = musicRepository.getPlaylistSongs(playlistId)
                Log.d(TAG, "Playing playlist with ${songs.size} songs")
                if (songs.isNotEmpty()) {
                    saveToChronology(songs[0])
                }
                if (mediaController.ensureConnected()) {
                    mediaController.playSongs(songs)
                } else {
                    Log.e(TAG, "Unable to connect to MediaService to play playlist")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error playing playlist", e)
            }
        }
    }
    
    /**
     * Play or pause current song
     */
    fun playPause() {
        launchWithController { playPause() }
    }
    
    /**
     * Skip to next song
     */
    fun skipToNext() {
        val nextSong = playbackState.value.queue.getOrNull(playbackState.value.currentIndex + 1)
        nextSong?.let { saveToChronology(it) }
        launchWithController { skipToNext() }
    }
    
    /**
     * Skip to previous song
     */
    fun skipToPrevious() {
        val prevSong = playbackState.value.queue.getOrNull(playbackState.value.currentIndex - 1)
        prevSong?.let { saveToChronology(it) }
        launchWithController { skipToPrevious() }
    }
    
    /**
     * Seek to position in milliseconds
     */
    fun seekTo(positionMs: Long) {
        launchWithController { seekTo(positionMs) }
    }
    
    /**
     * Seek to specific song in queue
     */
    fun seekToSong(index: Int) {
        launchWithController { seekToSong(index) }
    }
    
    // ==================== Queue Management ====================
    
    /**
     * Add song to end of queue
     */
    fun addToQueue(song: SongItem) {
        Log.d(TAG, "Adding to queue: ${song.title}")
        launchWithController { addToQueue(song) }
    }
    
    /**
     * Add songs to end of queue
     */
    fun addToQueue(songs: List<SongItem>) {
        Log.d(TAG, "Adding ${songs.size} songs to queue")
        launchWithController { addToQueue(songs) }
    }
    
    /**
     * Play next (add after current song)
     */
    fun playNext(song: SongItem) {
        Log.d(TAG, "Playing next: ${song.title}")
        launchWithController { playNext(song) }
    }
    
    /**
     * Remove song from queue
     */
    fun removeFromQueue(index: Int) {
        launchWithController { removeFromQueue(index) }
    }
    
    /**
     * Clear entire queue
     */
    fun clearQueue() {
        launchWithController { clearQueue() }
    }
    
    // ==================== Modes ====================
    
    /**
     * Toggle shuffle mode
     */
    fun toggleShuffle() {
        launchWithController { toggleShuffle() }
    }
    
    /**
     * Toggle repeat mode
     */
    fun toggleRepeat() {
        launchWithController { toggleRepeatMode() }
    }

    /**
     * Toggle favorite state for the currently playing song
     */
    fun toggleFavorite() {
        val currentSong = playbackState.value.nowPlaying ?: return
        viewModelScope.launch {
            try {
                musicRepository.toggleSongFavorite(currentSong.id, !currentSong.isFavorite)
            } catch (e: Exception) {
                Log.e(TAG, "Error toggling favorite", e)
            }
        }
    }
    
    /**
     * Shuffle all songs in library
     */
    fun shuffleAll() {
        viewModelScope.launch {
            try {
                val allSongs = musicRepository.getAllSongs().shuffled()
                Log.d(TAG, "Shuffling ${allSongs.size} songs")
                if (allSongs.isNotEmpty()) {
                    saveToChronology(allSongs[0])
                }
                if (mediaController.ensureConnected()) {
                    mediaController.playSongs(allSongs)
                    mediaController.setShuffleMode(true)
                } else {
                    Log.e(TAG, "Unable to connect to MediaService to shuffle all")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error shuffling all songs", e)
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "ViewModel cleared")
    }
}
