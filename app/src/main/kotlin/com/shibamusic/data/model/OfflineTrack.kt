package com.shibamusic.data.model

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
    val isComplete: Boolean = true
)

/**
 * Enumeração para definir a qualidade do áudio
 */
enum class AudioQuality(val bitrate: Int) {
    LOW(128),
    MEDIUM(320),
    HIGH(LOSSLESS)
    
    companion object {
        const val LOSSLESS = -1 // Indica qualidade lossless
    }
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