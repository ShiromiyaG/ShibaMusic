package com.shirou.shibamusic.repository

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.shirou.shibamusic.data.dao.OfflineTrackDao
import com.shirou.shibamusic.data.model.AudioCodec
import com.shirou.shibamusic.data.model.AudioQuality
import com.shirou.shibamusic.data.model.DownloadProgress
import com.shirou.shibamusic.data.model.DownloadStatus
import com.shirou.shibamusic.data.model.OfflineTrack
import com.shirou.shibamusic.worker.OfflineDownloadWorker
import com.shirou.shibamusic.service.DownloaderManager
import com.shirou.shibamusic.util.Preferences
import com.shirou.shibamusic.util.DownloadUtil
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Date
import java.util.zip.GZIPInputStream
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
            .map { tracks -> tracks.map { ensureOfflineFileNormalized(it) } }
    }
    
    /**
     * Obtém músicas offline de um artista específico
     */
    fun getOfflineTracksByArtist(artist: String): Flow<List<OfflineTrack>> {
        return offlineDao.getTracksByArtist(artist)
            .map { tracks -> tracks.map { ensureOfflineFileNormalized(it) } }
    }
    
    /**
     * Obtém músicas offline de um álbum específico
     */
    fun getOfflineTracksByAlbum(album: String): Flow<List<OfflineTrack>> {
        return offlineDao.getTracksByAlbum(album)
            .map { tracks -> tracks.map { ensureOfflineFileNormalized(it) } }
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
        return offlineDao.getOfflineTrack(trackId)?.let { ensureOfflineFileNormalized(it) }
    }

    suspend fun normalizeOfflineTrack(trackId: String): OfflineTrack? {
        return offlineDao.getOfflineTrack(trackId)?.let { ensureOfflineFileNormalized(it) }
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
    suspend fun removeOfflineTrack(trackId: String) = withContext(Dispatchers.IO) {
        val track = offlineDao.getOfflineTrack(trackId)
        track?.let {
            val file = File(it.localFilePath)
            if (file.exists()) {
                file.delete()
            }

            it.coverArtPath?.let { coverPath ->
                val coverFile = File(coverPath)
                if (coverFile.exists()) {
                    coverFile.delete()
                }
            }

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
    suspend fun getOfflineStorageInfo(): OfflineStorageInfo = withContext(Dispatchers.IO) {
        val trackCount = offlineDao.getOfflineTracksCount()
        val totalSize = offlineDao.getTotalOfflineSize()
        
        OfflineStorageInfo(
            trackCount = trackCount,
            totalSizeBytes = totalSize,
            availableSpaceBytes = getAvailableStorageSpace()
        )
    }
    
    /**
     * Limpa todos os dados offline
     */
    suspend fun clearAllOfflineData() = withContext(Dispatchers.IO) {
        val offlineDir = File(context.filesDir, "offline_music")
        if (offlineDir.exists()) {
            offlineDir.deleteRecursively()
        }
        
        val coversDir = File(context.filesDir, "offline_covers")
        if (coversDir.exists()) {
            coversDir.deleteRecursively()
        }

        // Limpa downloads legados gerenciados pela infraestrutura antiga
        DownloadUtil.eraseDownloadFolder(context)
        DownloaderManager.clearCachedDownloads()
        DownloadRepository().deleteAll()

        offlineDao.deleteAllOfflineTracks()
        DownloadStatus.values().forEach { status ->
            offlineDao.deleteDownloadsByStatus(status)
        }
    }

    /**
     * Cancela um download em andamento
     */
    suspend fun cancelDownload(trackId: String) = withContext(Dispatchers.IO) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelUniqueWork("offline_download_$trackId")

        val musicDir = File(context.filesDir, "offline_music")
        if (musicDir.exists()) {
            musicDir.listFiles { file -> file.name.startsWith(trackId) }?.forEach { it.delete() }
        }

        val coversDir = File(context.filesDir, "offline_covers")
        if (coversDir.exists()) {
            coversDir.listFiles { file -> file.name.startsWith(trackId) }?.forEach { it.delete() }
        }

        offlineDao.deleteDownloadProgress(trackId)
        offlineDao.deleteOfflineTrackById(trackId)
    }
    
    /**
     * Verifica a integridade dos arquivos offline
     */
    suspend fun verifyOfflineIntegrity(): List<String> = withContext(Dispatchers.IO) {
        val corruptedTracks = mutableListOf<String>()
        val tracks = offlineDao.getOfflineTracksSnapshot()
        tracks.forEach { original ->
            val track = ensureOfflineFileNormalized(original)
            val file = File(track.localFilePath)
            if (!file.exists() || file.length() == 0L) {
                corruptedTracks.add(track.id)
                track.coverArtPath?.let { coverPath ->
                    val coverFile = File(coverPath)
                    if (coverFile.exists()) {
                        coverFile.delete()
                    }
                }
                offlineDao.deleteOfflineTrack(track)
                offlineDao.deleteDownloadProgress(track.id)
            }
        }
        
        corruptedTracks
    }
    
    /**
     * Obtém espaço disponível para armazenamento
     */
    private fun getAvailableStorageSpace(): Long {
        return context.filesDir.usableSpace
    }

    private suspend fun ensureOfflineFileNormalized(track: OfflineTrack): OfflineTrack =
        withContext(Dispatchers.IO) {
            var currentTrack = track
            var file = File(track.localFilePath)

            if (!file.exists()) {
                return@withContext currentTrack
            }

            // Check for GZIP encoded files and decompress if needed
            FileInputStream(file).use { input ->
                val header = ByteArray(4)
                val read = input.read(header)
                if (read >= 2 && header[0] == 0x1f.toByte() && header[1] == 0x8b.toByte()) {
                    val tempFile = File(file.parentFile, "${track.id}.${track.quality.fileExtension}.tmp")
                    GZIPInputStream(FileInputStream(file)).use { gzipInput ->
                        FileOutputStream(tempFile).use { output ->
                            gzipInput.copyTo(output)
                        }
                    }
                    file.delete()
                    val finalFile = File(file.parentFile, "${track.id}.${track.quality.fileExtension}")
                    if (tempFile != finalFile) {
                        tempFile.renameTo(finalFile)
                    }
                    file = finalFile
                }
            }

            // Ensure Opus files use .ogg extension
            if (track.codec == AudioCodec.OPUS &&
                file.extension.lowercase() != track.quality.fileExtension
            ) {
                val targetFile = File(file.parentFile, "${track.id}.${track.quality.fileExtension}")
                if (file != targetFile) {
                    val renamed = file.renameTo(targetFile)
                    if (!renamed) {
                        file.copyTo(targetFile, overwrite = true)
                        file.delete()
                    }
                    file = targetFile
                }
            }

            if (file.absolutePath != track.localFilePath || track.fileSize != file.length()) {
                val updatedTrack = track.copy(
                    localFilePath = file.absolutePath,
                    fileSize = file.length()
                )
                offlineDao.updateOfflineTrack(updatedTrack)
                currentTrack = updatedTrack
            }

            currentTrack
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
        
        offlineDao.updateDownloadProgressFields(
            trackId = trackId,
            status = DownloadStatus.COMPLETED,
            progress = 1f,
            bytesDownloaded = fileSize,
            totalBytes = fileSize,
            errorMessage = null,
            updatedAt = Date()
        )
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
