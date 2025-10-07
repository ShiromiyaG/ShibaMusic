package com.shirou.shibamusic.media

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaBrowser
import androidx.media3.session.SessionToken
import com.shirou.shibamusic.glide.CustomGlideRequest
import com.shirou.shibamusic.repository.QueueRepository
import com.shirou.shibamusic.service.MediaService
import com.shirou.shibamusic.subsonic.models.Child
import com.shirou.shibamusic.ui.model.*
import com.shirou.shibamusic.util.Constants
import com.shirou.shibamusic.util.Preferences
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * MediaController provides a Kotlin/Coroutines friendly wrapper around Media3 MediaBrowser
 * 
 * This class bridges the existing Java-based MediaService with the new Kotlin architecture,
 * providing:
 * - Flow-based reactive updates
 * - Suspend functions for operations
 * - Type-safe song models instead of raw MediaItems
 * - Simplified API surface
 */
@UnstableApi
@Singleton
class MediaController @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private var mediaBrowserFuture: ListenableFuture<MediaBrowser>? = null
    private var mediaBrowser: MediaBrowser? = null
    private val queueRepository = QueueRepository()
    private val chronologyRepository = com.shirou.shibamusic.repository.ChronologyRepository()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    /**
     * Initialize connection to MediaService
     * Must be called before using other methods
     */
    suspend fun connect(): Boolean = suspendCoroutine { continuation ->
        if (mediaBrowser != null) {
            continuation.resume(true)
            return@suspendCoroutine
        }

        if (mediaBrowserFuture == null) {
            val sessionToken = SessionToken(
                context,
                ComponentName(context, MediaService::class.java)
            )

            mediaBrowserFuture = MediaBrowser.Builder(context, sessionToken).buildAsync()
        }

        mediaBrowserFuture?.addListener({
            try {
                mediaBrowser = mediaBrowserFuture?.get()
                continuation.resume(mediaBrowser != null)
            } catch (e: Exception) {
                continuation.resume(false)
            }
        }, MoreExecutors.directExecutor())
    }

    /** Ensure a connection exists before interacting with the browser */
    suspend fun ensureConnected(): Boolean {
        return mediaBrowser != null || connect()
    }
    
    /**
     * Release connection to MediaService
     */
    fun disconnect() {
        mediaBrowserFuture?.let { MediaBrowser.releaseFuture(it) }
        mediaBrowser = null
        mediaBrowserFuture = null
    }
    
    // ==================== Player State Flows ====================
    
    /**
     * Observe complete player state
     */
    val playerState: Flow<PlayerState> = callbackFlow {
        val browser = mediaBrowser ?: run {
            close()
            return@callbackFlow
        }

        val listener = object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                trySend(getCurrentPlayerState(player))
            }
        }

        browser.addListener(listener)
        trySend(getCurrentPlayerState(browser))

        awaitClose {
            browser.removeListener(listener)
        }
    }.distinctUntilChanged()
    
    /**
     * Observe now playing song
     */
    val nowPlaying: Flow<SongItem?> = playerState.map { it.nowPlaying }
    
    /**
     * Observe is playing state
     */
    val isPlaying: Flow<Boolean> = playerState.map { it.isPlaying }
    
    /**
     * Observe playback progress
     */
    val progress: Flow<PlaybackProgress> = playerState.map { it.progress }
    
    /**
     * Observe queue
     */
    val queue: Flow<List<SongItem>> = playerState.map { it.queue }
    
    /**
     * Observe repeat mode
     */
    val repeatMode: Flow<RepeatMode> = playerState.map { it.repeatMode }
    
    /**
     * Observe shuffle mode
     */
    val shuffleMode: Flow<Boolean> = playerState.map { it.shuffleMode }
    
    // ==================== Playback Control ====================
    
    /**
     * Play or pause
     */
    fun playPause() {
        mediaBrowser?.let { browser ->
            if (browser.isPlaying) {
                browser.pause()
            } else {
                browser.play()
            }
        }
    }
    
    /**
     * Play
     */
    fun play() {
        mediaBrowser?.play()
    }
    
    /**
     * Pause
     */
    fun pause() {
        mediaBrowser?.pause()
    }
    
    /**
     * Skip to next song
     */
    fun skipToNext() {
        mediaBrowser?.let { browser ->
            browser.seekToNext()
            browser.currentMediaItem?.toSongItem()?.let { saveToChronology(it) }
        }
    }
    
    /**
     * Skip to previous song
     */
    fun skipToPrevious() {
        mediaBrowser?.let { browser ->
            browser.seekToPrevious()
            browser.currentMediaItem?.toSongItem()?.let { saveToChronology(it) }
        }
    }
    
    /**
     * Seek to position in milliseconds
     */
    fun seekTo(positionMs: Long) {
        mediaBrowser?.seekTo(positionMs)
    }
    
    /**
     * Seek to specific song in queue
     */
    fun seekToSong(index: Int) {
        mediaBrowser?.let { browser ->
            browser.seekTo(index, 0)
            browser.getMediaItemAt(index).toSongItem()?.let { saveToChronology(it) }
        }
    }
    
    // ==================== Queue Management ====================
    
    /**
     * Play a song immediately, clearing the queue
     */
    fun playSong(song: SongItem) {
        mediaBrowser?.let { browser ->
            queueRepository.insert(song.toChild(), true, 0)
            saveToChronology(song)
            browser.clearMediaItems()
            browser.setMediaItem(song.toMediaItem())
            browser.prepare()
            browser.play()
        }
    }
    
    /**
     * Play a list of songs starting at index
     */
    fun playSongs(songs: List<SongItem>, startIndex: Int = 0) {
        mediaBrowser?.let { browser ->
            queueRepository.insertAll(songs.map { it.toChild() }, true, 0)
            if (songs.isNotEmpty() && startIndex in songs.indices) {
                saveToChronology(songs[startIndex])
            }
            browser.clearMediaItems()
            browser.setMediaItems(songs.map { it.toMediaItem() })
            browser.seekTo(startIndex, 0)
            browser.prepare()
            browser.play()
        }
    }
    
    /**
     * Add song to end of queue
     */
    fun addToQueue(song: SongItem) {
        mediaBrowser?.let { browser ->
            val insertIndex = browser.mediaItemCount
            queueRepository.insert(song.toChild(), false, insertIndex)
            browser.addMediaItem(song.toMediaItem())
        }
    }
    
    /**
     * Add multiple songs to end of queue
     */
    fun addToQueue(songs: List<SongItem>) {
        mediaBrowser?.let { browser ->
            val insertIndex = browser.mediaItemCount
            queueRepository.insertAll(songs.map { it.toChild() }, false, insertIndex)
            browser.addMediaItems(songs.map { it.toMediaItem() })
        }
    }
    
    /**
     * Play next (add after current song)
     */
    fun playNext(song: SongItem) {
        mediaBrowser?.let { browser ->
            val nextIndex = browser.currentMediaItemIndex + 1
            val targetIndex = if (nextIndex < 0) 0 else nextIndex
            queueRepository.insert(song.toChild(), false, targetIndex)
            browser.addMediaItem(targetIndex, song.toMediaItem())
        }
    }
    
    /**
     * Remove song from queue
     */
    fun removeFromQueue(index: Int) {
        mediaBrowser?.let { browser ->
            queueRepository.delete(index)
            browser.removeMediaItem(index)
        }
    }
    
    /**
     * Clear entire queue
     */
    fun clearQueue() {
        queueRepository.deleteAll()
        mediaBrowser?.clearMediaItems()
    }
    
    // ==================== Modes ====================
    
    /**
     * Toggle shuffle mode
     */
    fun toggleShuffle() {
        mediaBrowser?.let { browser ->
            browser.shuffleModeEnabled = !browser.shuffleModeEnabled
        }
    }
    
    /**
     * Set shuffle mode
     */
    fun setShuffleMode(enabled: Boolean) {
        mediaBrowser?.shuffleModeEnabled = enabled
    }
    
    /**
     * Cycle repeat mode (OFF -> ONE -> ALL -> OFF)
     */
    fun toggleRepeatMode() {
        mediaBrowser?.let { browser ->
            browser.repeatMode = when (browser.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ONE
                Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_OFF
                else -> Player.REPEAT_MODE_OFF
            }
        }
    }
    
    /**
     * Set repeat mode
     */
    fun setRepeatMode(mode: RepeatMode) {
        mediaBrowser?.repeatMode = when (mode) {
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
        }
    }
    
    // ==================== Helper Methods ====================
    
    /**
     * Convert current player state to PlayerState model
     */
    private fun getCurrentPlayerState(player: Player): PlayerState {
        val currentSong = player.currentMediaItem?.toSongItem()
        return PlayerState(
            nowPlaying = currentSong,
            queue = (0 until player.mediaItemCount).mapNotNull {
                player.getMediaItemAt(it).toSongItem()
            },
            currentIndex = player.currentMediaItemIndex,
            playbackState = when (player.playbackState) {
                Player.STATE_IDLE -> PlaybackState.IDLE
                Player.STATE_BUFFERING -> PlaybackState.BUFFERING
                Player.STATE_READY -> PlaybackState.READY
                Player.STATE_ENDED -> PlaybackState.ENDED
                else -> PlaybackState.IDLE
            },
            isPlaying = player.isPlaying,
            playWhenReady = player.playWhenReady,
            progress = PlaybackProgress(
                currentPosition = player.currentPosition,
                duration = player.duration.takeIf { it > 0 } ?: 0,
                bufferedPosition = player.bufferedPosition
            ),
            repeatMode = when (player.repeatMode) {
                Player.REPEAT_MODE_ONE -> RepeatMode.ONE
                Player.REPEAT_MODE_ALL -> RepeatMode.ALL
                else -> RepeatMode.OFF
            },
            shuffleMode = player.shuffleModeEnabled,
            isFavorite = currentSong?.isFavorite == true,
            hasNext = player.hasNextMediaItem(),
            hasPrevious = player.hasPreviousMediaItem()
        )
    }
    
    /**
     * Convert SongItem to MediaItem
     */
    private fun SongItem.toMediaItem(): MediaItem {
        val streamUri = getStreamUri(id)
        val artworkUri = albumArtUrl?.let { android.net.Uri.parse(it) }
        val extras = Bundle().apply {
            albumArtUrl?.takeIf { it.isNotBlank() }?.let { putString("albumArtUrl", it) }
            albumId?.takeIf { it.isNotBlank() }?.let { putString("albumId", it) }
            artistId?.takeIf { it.isNotBlank() }?.let { putString("artistId", it) }
        }
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artistName)
            .setAlbumTitle(albumName)
            .setArtworkUri(artworkUri)
            .setExtras(extras)
            .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
            .setIsBrowsable(false)
            .setIsPlayable(true)
            .build()

        return MediaItem.Builder()
            .setMediaId(id)
            .setUri(streamUri)
            .setMediaMetadata(metadata)
            .setRequestMetadata(
                MediaItem.RequestMetadata.Builder()
                    .setMediaUri(android.net.Uri.parse(streamUri))
                    .setExtras(extras)
                    .build()
            )
            .build()
    }

    private fun SongItem.toChild(): Child {
        return Child(id = id).apply {
            title = this@toChild.title
            album = this@toChild.albumName
            artist = this@toChild.artistName
            track = this@toChild.trackNumber
            year = this@toChild.year
            genre = this@toChild.genre
            albumId = this@toChild.albumId
            artistId = this@toChild.artistId
            val songDuration = this@toChild.duration
            duration = if (songDuration > 0) (songDuration / 1000).toInt() else null
            playCount = this@toChild.playCount.toLong()
            path = this@toChild.path
            type = Constants.MEDIA_TYPE_MUSIC
        }
    }
    
    private fun saveToChronology(song: SongItem) {
        android.util.Log.d("MediaController", "Saving to chronology: ${song.title}")
        val chronology = com.shirou.shibamusic.model.Chronology(song.id).apply {
            title = song.title
            artist = song.artistName
            album = song.albumName
            albumId = song.albumId
            artistId = song.artistId
            server = Preferences.getServerId()
            timestamp = System.currentTimeMillis()
        }
        scope.launch {
            chronologyRepository.insertSync(chronology)
            android.util.Log.d("MediaController", "Saved to chronology: ${song.title}")
        }
    }
    
    /**
     * Build stream URI for a song ID
     * Based on MusicUtil.getStreamUri() from the original code
     */
    private fun getStreamUri(songId: String): String {
        val subsonic = com.shirou.shibamusic.App.getSubsonicClientInstance(false)
        val params = subsonic.params
        val baseUrl = subsonic.url
        
        val uri = StringBuilder().apply {
            append(baseUrl)
            append("stream")
            
            params["u"]?.let { append("?u=").append(com.shirou.shibamusic.util.Util.encode(it)) }
            params["p"]?.let { append("&p=").append(it) }
            params["s"]?.let { append("&s=").append(it) }
            params["t"]?.let { append("&t=").append(it) }
            params["v"]?.let { append("&v=").append(it) }
            params["c"]?.let { append("&c=").append(it) }
            
            // Add bitrate and format preferences
            if (!com.shirou.shibamusic.util.Preferences.isServerPrioritized()) {
                append("&maxBitRate=").append(com.shirou.shibamusic.util.MusicUtil.getBitratePreference())
                append("&format=").append(com.shirou.shibamusic.util.MusicUtil.getTranscodingFormatPreference())
            }
            
            if (com.shirou.shibamusic.util.Preferences.askForEstimateContentLength()) {
                append("&estimateContentLength=true")
            }
            
            append("&id=").append(songId)
        }
        
        return uri.toString()
    }
    
    /**
     * Convert MediaItem to SongItem (basic mapping)
     * Note: This is a simplified version. Full implementation should fetch from database
     */
    private fun MediaItem.toSongItem(): SongItem? {
        val metadata = mediaMetadata
        val songId = mediaId.takeIf { it.isNotBlank() } ?: return null
        val title = metadata.title?.toString().orEmpty().ifBlank { songId }
        val artistName = metadata.artist?.toString().orEmpty().ifBlank { "Unknown Artist" }
        val albumName = metadata.albumTitle?.toString()
        val albumArtUrl = metadata.artworkUri?.toString()
            ?: metadata.extras?.getString("albumArtUrl")
            ?: requestMetadata.extras?.getString("albumArtUrl")
            ?: run {
                val coverArtId = metadata.extras?.getString("coverArtId")
                    ?: requestMetadata.extras?.getString("coverArtId")
                coverArtId?.let { id ->
                    CustomGlideRequest.createUrl(id, Preferences.getImageSize())
                }
            }
        val albumId = metadata.extras?.getString("albumId")
            ?: requestMetadata.extras?.getString("albumId")
        val artistId = metadata.extras?.getString("artistId")
            ?: requestMetadata.extras?.getString("artistId")

        return SongItem(
            id = songId,
            title = title,
            artistName = artistName,
            albumName = albumName,
            albumId = albumId?.takeUnless { it.isBlank() },
            artistId = artistId?.takeUnless { it.isBlank() },
            albumArtUrl = albumArtUrl
        )
    }
}
