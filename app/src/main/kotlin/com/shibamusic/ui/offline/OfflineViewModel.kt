package com.shibamusic.ui.offline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shibamusic.data.model.*
import com.shibamusic.player.OfflineMusicPlayer
import com.shibamusic.repository.OfflineRepository
import com.shibamusic.repository.OfflineStorageInfo
import com.shirou.shibamusic.util.Preferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel para gerenciar funcionalidades offline
 */
@HiltViewModel
class OfflineViewModel @Inject constructor(
    private val offlineRepository: OfflineRepository,
    private val offlineMusicPlayer: OfflineMusicPlayer
) : ViewModel() {
    
    // Estados da interface
    private val _uiState = MutableStateFlow(OfflineUiState())
    val uiState: StateFlow<OfflineUiState> = _uiState.asStateFlow()
    
    // Músicas offline
    val offlineTracks = offlineRepository.getAllOfflineTracks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    // Downloads ativos
    val activeDownloads = offlineRepository.getActiveDownloads()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    // Estados do player
    val currentTrack = offlineMusicPlayer.currentTrack
    val isPlaying = offlineMusicPlayer.isPlaying
    val playbackPosition = offlineMusicPlayer.playbackPosition
    val playlist = offlineMusicPlayer.playlist
    
    init {
        loadOfflineStorageInfo()
    }
    
    /**
     * Inicia o download de uma música
     */
    fun downloadTrack(
        trackId: String,
        title: String,
        artist: String,
        album: String,
        duration: Long,
        coverArtUrl: String? = null,
        quality: AudioQuality? = null
    ) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }

                val resolvedQuality = quality ?: Preferences.getOfflineDownloadQuality()
                
                offlineRepository.downloadTrack(
                    trackId = trackId,
                    title = title,
                    artist = artist,
                    album = album,
                    duration = duration,
                    coverArtUrl = coverArtUrl,
                    quality = resolvedQuality
                )
                
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        message = "Download iniciado para $title"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = "Erro ao iniciar download: ${e.message}"
                    )
                }
            }
        }
    }
    
    /**
     * Remove uma música offline
     */
    fun removeOfflineTrack(trackId: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                
                offlineRepository.removeOfflineTrack(trackId)
                loadOfflineStorageInfo()
                
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        message = "Música removida do modo offline"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = "Erro ao remover música: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Cancela um download em andamento
     */
    fun cancelDownload(trackId: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }

                offlineRepository.cancelDownload(trackId)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        message = "Download cancelado"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Erro ao cancelar download: ${e.message}"
                    )
                }
            }
        }
    }
    
    /**
     * Reproduz uma música offline
     */
    fun playOfflineTrack(trackId: String) {
        viewModelScope.launch {
            try {
                offlineMusicPlayer.playOfflineTrack(trackId)
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(error = "Erro ao reproduzir música: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Reproduz toda a biblioteca offline
     */
    fun playAllOffline() {
        viewModelScope.launch {
            val tracks = offlineTracks.value
            if (tracks.isNotEmpty()) {
                offlineMusicPlayer.setPlaylist(tracks)
                offlineMusicPlayer.play()
            } else {
                _uiState.update { 
                    it.copy(message = "Nenhuma música offline disponível")
                }
            }
        }
    }
    
    /**
     * Reproduz músicas de um artista offline
     */
    fun playArtistOffline(artist: String) {
        offlineMusicPlayer.playArtistOffline(artist)
    }
    
    /**
     * Reproduz músicas de um álbum offline
     */
    fun playAlbumOffline(album: String) {
        offlineMusicPlayer.playAlbumOffline(album)
    }
    
    /**
     * Controles do player
     */
    fun play() = offlineMusicPlayer.play()
    fun pause() = offlineMusicPlayer.pause()
    fun stop() = offlineMusicPlayer.stop()
    fun playNext() = offlineMusicPlayer.playNext()
    fun playPrevious() = offlineMusicPlayer.playPrevious()
    fun seekTo(positionMs: Long) = offlineMusicPlayer.seekTo(positionMs)
    
    /**
     * Obtém progresso de download de uma música
     */
    fun getDownloadProgress(trackId: String): Flow<DownloadProgress?> {
        return flow {
            emit(offlineRepository.getDownloadProgress(trackId))
        }
    }
    
    /**
     * Verifica se uma música está disponível offline
     */
    suspend fun isTrackAvailableOffline(trackId: String): Boolean {
        return offlineRepository.isTrackAvailableOffline(trackId)
    }
    
    /**
     * Limpa todos os dados offline
     */
    fun clearAllOfflineData() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                
                offlineRepository.clearAllOfflineData()
                loadOfflineStorageInfo()
                
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        message = "Todos os dados offline foram removidos"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = "Erro ao limpar dados: ${e.message}"
                    )
                }
            }
        }
    }
    
    /**
     * Verifica integridade dos arquivos offline
     */
    fun verifyOfflineIntegrity() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                
                val corruptedTracks = offlineRepository.verifyOfflineIntegrity()
                loadOfflineStorageInfo()
                
                val message = if (corruptedTracks.isEmpty()) {
                    "Todos os arquivos offline estão íntegros"
                } else {
                    "${corruptedTracks.size} arquivo(s) corrompido(s) foram removidos"
                }
                
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        message = message
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = "Erro ao verificar integridade: ${e.message}"
                    )
                }
            }
        }
    }
    
    /**
     * Carrega informações de armazenamento offline
     */
    private fun loadOfflineStorageInfo() {
        viewModelScope.launch {
            try {
                val storageInfo = offlineRepository.getOfflineStorageInfo()
                _uiState.update { it.copy(storageInfo = storageInfo) }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(error = "Erro ao carregar informações: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Limpa mensagens de erro e sucesso
     */
    fun clearMessages() {
        _uiState.update { it.copy(error = null, message = null) }
    }
    
    override fun onCleared() {
        super.onCleared()
        // Não liberamos o player aqui pois ele pode estar sendo usado em outros lugares
    }
}

/**
 * Estado da interface offline
 */
data class OfflineUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val storageInfo: OfflineStorageInfo? = null
)
