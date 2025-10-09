package com.shibamusic.repository

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.shibamusic.data.dao.OfflineTrackDao
import com.shibamusic.data.model.AudioQuality
import com.shibamusic.data.model.DownloadProgress
import com.shibamusic.data.model.DownloadStatus
import com.shibamusic.data.model.OfflineTrack
import com.shibamusic.worker.OfflineDownloadWorker
import com.shirou.shibamusic.util.Preferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import java.io.File
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repositório responsável pelo gerenciamento de músicas offline
 */
@Singleton
class OfflineRepository @Inject constructor(
    private val offlineDao: OfflineTrackDao,
    @ApplicationContext private val context: Context
) {
    
    /**
     * Obtém todas as músicas offline disponíveis
     */
    fun getAllOfflineTracks(): Flow<List<OfflineTrack>> {
        return offlineDao.getAllOfflineTracks()
    }
    
    /**
     * Obtém músicas offline de um artista específico
     */
    fun getOfflineTracksByArtist(artist: String): Flow<List<OfflineTrack>> {
        return offlineDao.getTracksByArtist(artist)
    }
    
    /**
     * Obtém músicas offline de um álbum específico
     */
    fun getOfflineTracksByAlbum(album: String): Flow<List<OfflineTrack>> {
        return offlineDao.getTracksByAlbum(album)
    }
    
    /**
     * Verifica se uma música está disponível offline
     */
    suspend fun isTrackAvailableOffline(trackId: String): Boolean {
        return offlineDao.isTrackDownloaded(trackId)
    }
    
    /**
     * Obtém uma música offline específica
     */
    suspend fun getOfflineTrack(trackId: String): OfflineTrack? {
        return offlineDao.getOfflineTrack(trackId)
    }
    
    /**
     * Inicia o download de uma música para uso offline
     */
    suspend fun downloadTrack(
        trackId: String,
        title: String,
        artist: String,
        album: String,
        duration: Long,
        coverArtUrl: String? = null,
        quality: AudioQuality = AudioQuality.MEDIUM
    ) {
        if (offlineDao.isTrackDownloaded(trackId)) {
            return
        }

    val baseUrl = Preferences.getInUseServerAddress() ?: return

    val workManager = WorkManager.getInstance(context)
    val workRequest = OfflineDownloadWorker.createDownloadRequest(
                trackId = trackId,
                title = title,
                artist = artist,
                album = album,
                duration = duration,
        baseUrl = baseUrl,
                coverArtUrl = coverArtUrl,
                quality = quality
            )

        enqueueWork(
            workManager = workManager,
            workRequest = workRequest,
            uniqueWorkName = "offline_download_$trackId"
        )

        val progress = DownloadProgress(
            trackId = trackId,
            status = DownloadStatus.PENDING,
            progress = 0f,
            bytesDownloaded = 0L,
            totalBytes = 0L,
            errorMessage = null,
            createdAt = Date(),
            updatedAt = Date()
        )
        offlineDao.insertDownloadProgress(progress)
    }

    private fun enqueueWork(
        workManager: WorkManager,
        workRequest: OneTimeWorkRequest,
        uniqueWorkName: String
    ) {
        workManager.enqueueUniqueWork(
            uniqueWorkName,
            ExistingWorkPolicy.KEEP,
            workRequest
        )
    }
    
    /**
     * Remove uma música offline
     */
    suspend fun removeOfflineTrack(trackId: String) {
        val track = offlineDao.getOfflineTrack(trackId)
        track?.let {
            // Remove arquivo físico
            val file = File(it.localFilePath)
            if (file.exists()) {
                file.delete()
            }
            
            // Remove capa se existir
            it.coverArtPath?.let { coverPath ->
                val coverFile = File(coverPath)
                if (coverFile.exists()) {
                    coverFile.delete()
                }
            }
            
            // Remove do banco de dados
            offlineDao.deleteOfflineTrack(it)
            offlineDao.deleteDownloadProgress(trackId)
        }
    }
    
    /**
     * Obtém o progresso de download de uma música
     */
    suspend fun getDownloadProgress(trackId: String): DownloadProgress? {
        return offlineDao.getDownloadProgress(trackId)
    }
    
    /**
     * Obtém todos os downloads ativos
     */
    fun getActiveDownloads(): Flow<List<DownloadProgress>> {
        return offlineDao.getActiveDownloads(
            listOf(
                DownloadStatus.PENDING,
                DownloadStatus.DOWNLOADING
            )
        )
    }
    
    /**
     * Obtém informações sobre o armazenamento offline
     */
    suspend fun getOfflineStorageInfo(): OfflineStorageInfo {
        val trackCount = offlineDao.getOfflineTracksCount()
        val totalSize = offlineDao.getTotalOfflineSize()
        
        return OfflineStorageInfo(
            trackCount = trackCount,
            totalSizeBytes = totalSize,
            availableSpaceBytes = getAvailableStorageSpace()
        )
    }
    
    /**
     * Limpa todos os dados offline
     */
    suspend fun clearAllOfflineData() {
        // Remove todos os arquivos
        val offlineDir = File(context.filesDir, "offline_music")
        if (offlineDir.exists()) {
            offlineDir.deleteRecursively()
        }
        
        // Limpa banco de dados
        offlineDao.deleteAllOfflineTracks()
        offlineDao.deleteDownloadsByStatus(DownloadStatus.COMPLETED)
    }
    
    /**
     * Verifica a integridade dos arquivos offline
     */
    suspend fun verifyOfflineIntegrity(): List<String> {
        val corruptedTracks = mutableListOf<String>()
        val tracks = offlineDao.getAllOfflineTracks().first()
        tracks.forEach { track ->
            val file = File(track.localFilePath)
            if (!file.exists() || file.length() == 0L) {
                corruptedTracks.add(track.id)
                offlineDao.deleteOfflineTrack(track)
            }
        }
        
        return corruptedTracks
    }
    
    /**
     * Obtém espaço disponível para armazenamento
     */
    private fun getAvailableStorageSpace(): Long {
        return context.filesDir.usableSpace
    }
    
    /**
     * Finaliza um download com sucesso
     */
    suspend fun completeDownload(
        trackId: String,
        title: String,
        artist: String,
        album: String,
        duration: Long,
        originalUrl: String,
        localFilePath: String,
        fileSize: Long,
        quality: AudioQuality,
        coverArtPath: String? = null
    ) {
        val offlineTrack = OfflineTrack(
            id = trackId,
            title = title,
            artist = artist,
            album = album,
            duration = duration,
            localFilePath = localFilePath,
            originalUrl = originalUrl,
            coverArtPath = coverArtPath,
            downloadedAt = Date(),
            fileSize = fileSize,
            quality = quality,
            codec = quality.codec
        )
        
        offlineDao.insertOfflineTrack(offlineTrack)
        
        // Atualiza progresso para concluído
        val completedProgress = DownloadProgress(
            trackId = trackId,
            status = DownloadStatus.COMPLETED,
            progress = 1f,
            bytesDownloaded = fileSize,
            totalBytes = fileSize,
            errorMessage = null,
            createdAt = Date(),
            updatedAt = Date()
        )
        offlineDao.updateDownloadProgress(completedProgress)
    }
}

/**
 * Classe para informações sobre armazenamento offline
 */
data class OfflineStorageInfo(
    val trackCount: Int,
    val totalSizeBytes: Long,
    val availableSpaceBytes: Long
) {
    val totalSizeMB: Double get() = totalSizeBytes / (1024.0 * 1024.0)
    val availableSpaceMB: Double get() = availableSpaceBytes / (1024.0 * 1024.0)
}