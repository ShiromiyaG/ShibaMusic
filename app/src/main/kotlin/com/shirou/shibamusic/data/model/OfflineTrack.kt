package com.shirou.shibamusic.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Entidade que representa uma música disponível para reprodução offline
 */
@Entity(tableName = "offline_tracks")
data class OfflineTrack(
    @PrimaryKey
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val localFilePath: String,
    val originalUrl: String,
    val coverArtPath: String?,
    val downloadedAt: Date,
    val fileSize: Long,
    val quality: AudioQuality = AudioQuality.MEDIUM,
    val codec: AudioCodec = AudioCodec.OPUS,
    val isComplete: Boolean = true
)

/**
 * Enumeração para definir a qualidade do áudio
 */
enum class AudioQuality(
    val bitrate: Int,
    val codec: AudioCodec,
    val fileExtension: String
) {
    LOW(128, AudioCodec.OPUS, "opus"),
    MEDIUM(320, AudioCodec.OPUS, "opus"),
    HIGH(-1, AudioCodec.FLAC, "flac"); // -1 indica lossless
    
    /**
     * Retorna a URL de transcodificação para o servidor Subsonic
     */
    fun getTranscodeFormat(): String {
        return when (this) {
            LOW -> "opus"
            MEDIUM -> "opus" 
            HIGH -> "flac"
        }
    }
    
    /**
     * Retorna o bitrate como string para requisições
     */
    fun getBitrateString(): String {
        return if (bitrate == -1) "0" else bitrate.toString()
    }
    
    companion object {
        const val LOSSLESS = -1 // Indica qualidade lossless
    }
}

/**
 * Enumeração para codecs de áudio suportados
 */
enum class AudioCodec(val mimeType: String, val displayName: String) {
    OPUS("audio/opus", "Opus"),
    MP3("audio/mpeg", "MP3"),
    FLAC("audio/flac", "FLAC"),
    OGG("audio/ogg", "OGG")
}

/**
 * Estado do download de uma música
 */
enum class DownloadStatus {
    PENDING,
    DOWNLOADING,
    COMPLETED,
    FAILED,
    PAUSED
}

/**
 * Entidade para acompanhar o progresso de downloads
 */
@Entity(tableName = "download_progress")
data class DownloadProgress(
    @PrimaryKey
    val trackId: String,
    val status: DownloadStatus,
    val progress: Float, // 0.0 a 1.0
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val errorMessage: String? = null,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)

/**
 * Informações de armazenamento offline
 */
data class OfflineStorageInfo(
    val totalTracks: Int,
    val totalSizeBytes: Long,
    val availableSpaceBytes: Long,
    val opusTracksCount: Int = 0,
    val flacTracksCount: Int = 0
) {
    val totalSizeMB: Int
        get() = (totalSizeBytes / (1024 * 1024)).toInt()
    
    val availableSpaceMB: Int
        get() = (availableSpaceBytes / (1024 * 1024)).toInt()
}