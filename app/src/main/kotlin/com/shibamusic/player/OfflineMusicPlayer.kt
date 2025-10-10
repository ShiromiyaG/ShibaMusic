package com.shibamusic.player

import android.content.Context
import android.net.Uri
import androidx.media3.common.*
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.shibamusic.data.model.OfflineTrack
import com.shibamusic.repository.OfflineRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Player responsável pela reprodução de músicas offline
 * Suporta gapless playback e gerenciamento de playlist offline
 */
@Singleton
class OfflineMusicPlayer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val offlineRepository: OfflineRepository
) {
    
    private var exoPlayer: ExoPlayer? = null
    private val _currentTrack = MutableStateFlow<OfflineTrack?>(null)
    private val _isPlaying = MutableStateFlow(false)
    private val _playbackPosition = MutableStateFlow(0L)
    private val _playlist = MutableStateFlow<List<OfflineTrack>>(emptyList())
    private val _currentIndex = MutableStateFlow(0)
    
    // Estados públicos
    val currentTrack: StateFlow<OfflineTrack?> = _currentTrack.asStateFlow()
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    val playbackPosition: StateFlow<Long> = _playbackPosition.asStateFlow()
    val playlist: StateFlow<List<OfflineTrack>> = _playlist.asStateFlow()
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()
    
    // Listener para mudanças de estado
    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) {
            when (state) {
                Player.STATE_READY -> {
                    updatePlaybackPosition()
                }
                Player.STATE_ENDED -> {
                    playNext()
                }
            }
        }
        
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
            if (isPlaying) {
                startPositionUpdates()
            }
        }
        
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            updateCurrentTrack()
        }
    }
    
    init {
        initializePlayer()
    }
    
    private fun initializePlayer() {
        exoPlayer = ExoPlayer.Builder(context)
            .build()
            .apply {
                addListener(playerListener)
                // Habilita gapless playback
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                        .build(),
                    true
                )
            }
    }
    
    /**
     * Define a playlist offline para reprodução
     */
    fun setPlaylist(tracks: List<OfflineTrack>, startIndex: Int = 0) {
        val normalizedTracks = tracks.map { track ->
            runBlocking(Dispatchers.IO) {
                offlineRepository.normalizeOfflineTrack(track.id)
            } ?: track
        }

        _playlist.value = normalizedTracks
        _currentIndex.value = startIndex

        val mediaItems = normalizedTracks.map { track ->
            createMediaItemFromOfflineTrack(track)
        }

        exoPlayer?.let { player ->
            player.setMediaItems(mediaItems, startIndex, 0L)
            player.prepare()
            updateCurrentTrack()
        }
    }
    
    /**
     * Reproduz uma música offline específica
     */
    suspend fun playOfflineTrack(trackId: String) {
        val track = offlineRepository.getOfflineTrack(trackId)
        if (track != null) {
            setPlaylist(listOf(track), 0)
            play()
        }
    }
    
    /**
     * Reproduz todas as músicas offline de um artista
     */
    fun playArtistOffline(artist: String) {
        offlineRepository.getOfflineTracksByArtist(artist)
            .onEach { tracks ->
                if (tracks.isNotEmpty()) {
                    setPlaylist(tracks)
                    play()
                }
            }
    }
    
    /**
     * Reproduz todas as músicas offline de um álbum
     */
    fun playAlbumOffline(album: String) {
        offlineRepository.getOfflineTracksByAlbum(album)
            .onEach { tracks ->
                if (tracks.isNotEmpty()) {
                    setPlaylist(tracks)
                    play()
                }
            }
    }
    
    /**
     * Inicia a reprodução
     */
    fun play() {
        exoPlayer?.play()
    }
    
    /**
     * Pausa a reprodução
     */
    fun pause() {
        exoPlayer?.pause()
    }
    
    /**
     * Para a reprodução
     */
    fun stop() {
        exoPlayer?.stop()
        _currentTrack.value = null
        _isPlaying.value = false
        _playbackPosition.value = 0L
    }
    
    /**
     * Vai para a próxima música
     */
    fun playNext() {
        exoPlayer?.let { player ->
            if (player.hasNextMediaItem()) {
                player.seekToNextMediaItem()
                _currentIndex.value = player.currentMediaItemIndex
            } else {
                // Reinicia a playlist se chegou ao final
                player.seekTo(0, 0L)
                _currentIndex.value = 0
            }
        }
    }
    
    /**
     * Vai para a música anterior
     */
    fun playPrevious() {
        exoPlayer?.let { player ->
            if (player.hasPreviousMediaItem()) {
                player.seekToPreviousMediaItem()
                _currentIndex.value = player.currentMediaItemIndex
            } else {
                // Vai para a última música se está na primeira
                val lastIndex = _playlist.value.size - 1
                player.seekTo(lastIndex, 0L)
                _currentIndex.value = lastIndex
            }
        }
    }
    
    /**
     * Navega para uma posição específica da música
     */
    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
    }
    
    /**
     * Define o modo de repetição
     */
    fun setRepeatMode(repeatMode: Int) {
        exoPlayer?.repeatMode = repeatMode
    }
    
    /**
     * Define o modo shuffle
     */
    fun setShuffleModeEnabled(enabled: Boolean) {
        exoPlayer?.shuffleModeEnabled = enabled
    }
    
    /**
     * Verifica se existe uma música sendo reproduzida
     */
    fun hasCurrentTrack(): Boolean {
        return _currentTrack.value != null
    }
    
    /**
     * Obtém a duração total da música atual
     */
    fun getCurrentTrackDuration(): Long {
        return exoPlayer?.duration ?: 0L
    }
    
    private fun createMediaItemFromOfflineTrack(track: OfflineTrack): MediaItem {
        val file = File(track.localFilePath)
        val uri = Uri.fromFile(file)
        val mimeType = track.codec.playbackMimeType
        
        return MediaItem.Builder()
            .setMediaId(track.id)
            .setUri(uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(track.title)
                    .setArtist(track.artist)
                    .setAlbumTitle(track.album)
                    .build()
            )
            .apply {
                setMimeType(mimeType)
            }
            .build()
    }
    
    private fun updateCurrentTrack() {
        val currentIndex = exoPlayer?.currentMediaItemIndex ?: -1
        if (currentIndex >= 0 && currentIndex < _playlist.value.size) {
            _currentTrack.value = _playlist.value[currentIndex]
            _currentIndex.value = currentIndex
        }
    }
    
    private fun updatePlaybackPosition() {
        exoPlayer?.let { player ->
            _playbackPosition.value = player.currentPosition
        }
    }
    
    private fun startPositionUpdates() {
        // Implementar atualizações periódicas de posição se necessário
    }
    
    /**
     * Libera recursos do player
     */
    fun release() {
        exoPlayer?.release()
        exoPlayer = null
    }
}
