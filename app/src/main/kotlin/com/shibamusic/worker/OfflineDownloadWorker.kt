package com.shibamusic.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.shibamusic.data.dao.OfflineTrackDao
import com.shibamusic.data.model.*
import com.shibamusic.repository.OfflineRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

/**
 * Worker para gerenciar downloads de músicas em background
 * Usado como alternativa ao serviço para downloads mais robustos
 */
@HiltWorker
class OfflineDownloadWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted private val workerParams: WorkerParameters,
    private val offlineDao: OfflineTrackDao,
    private val offlineRepository: OfflineRepository
) : CoroutineWorker(context, workerParams) {
    
    companion object {
        private const val KEY_TRACK_ID = "track_id"
        private const val KEY_TITLE = "title"
        private const val KEY_ARTIST = "artist"
        private const val KEY_ALBUM = "album"
        private const val KEY_DURATION = "duration"
        private const val KEY_ORIGINAL_URL = "original_url"
        private const val KEY_COVER_ART_URL = "cover_art_url"
        private const val KEY_QUALITY = "quality"
        
        /**
         * Cria uma requisição de download para ser executada pelo WorkManager
         */
        fun createDownloadRequest(
            trackId: String,
            title: String,
            artist: String,
            album: String,
            duration: Long,
            originalUrl: String,
            coverArtUrl: String? = null,
            quality: AudioQuality = AudioQuality.MEDIUM
        ): OneTimeWorkRequest {
            val inputData = Data.Builder()
                .putString(KEY_TRACK_ID, trackId)
                .putString(KEY_TITLE, title)
                .putString(KEY_ARTIST, artist)
                .putString(KEY_ALBUM, album)
                .putLong(KEY_DURATION, duration)
                .putString(KEY_ORIGINAL_URL, originalUrl)
                .putString(KEY_COVER_ART_URL, coverArtUrl)
                .putString(KEY_QUALITY, quality.name)
                .build()
            
            return OneTimeWorkRequestBuilder<OfflineDownloadWorker>()
                .setInputData(inputData)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .setRequiresBatteryNotLow(true)
                        .build()
                )
                .addTag("offline_download")
                .addTag(trackId)
                .build()
        }
    }
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val trackId = inputData.getString(KEY_TRACK_ID) ?: return@withContext Result.failure()
            val title = inputData.getString(KEY_TITLE) ?: return@withContext Result.failure()
            val artist = inputData.getString(KEY_ARTIST) ?: return@withContext Result.failure()
            val album = inputData.getString(KEY_ALBUM) ?: return@withContext Result.failure()
            val duration = inputData.getLong(KEY_DURATION, 0L)
            val originalUrl = inputData.getString(KEY_ORIGINAL_URL) ?: return@withContext Result.failure()
            val coverArtUrl = inputData.getString(KEY_COVER_ART_URL)
            val qualityName = inputData.getString(KEY_QUALITY) ?: AudioQuality.MEDIUM.name
            val quality = AudioQuality.valueOf(qualityName)
            
            // Verifica se já está baixado
            if (offlineRepository.isTrackAvailableOffline(trackId)) {
                return@withContext Result.success()
            }
            
            // Atualiza status para downloading
            updateDownloadProgress(
                trackId = trackId,
                status = DownloadStatus.DOWNLOADING,
                progress = 0f
            )
            
            // Executa o download
            val downloadResult = downloadTrack(
                trackId = trackId,
                originalUrl = originalUrl
            ) { progress ->
                // Atualiza progresso durante o download
                updateDownloadProgress(
                    trackId = trackId,
                    status = DownloadStatus.DOWNLOADING,
                    progress = progress
                )
                
                // Atualiza progresso no WorkManager
                setProgressAsync(
                    Data.Builder()
                        .putString("track_id", trackId)
                        .putFloat("progress", progress)
                        .build()
                )
            }
            
            if (downloadResult != null) {
                // Download de capa se disponível
                val coverArtPath = coverArtUrl?.let { downloadCoverArt(trackId, it) }
                
                // Completa o download
                offlineRepository.completeDownload(
                    trackId = trackId,
                    title = title,
                    artist = artist,
                    album = album,
                    duration = duration,
                    originalUrl = originalUrl,
                    localFilePath = downloadResult.absolutePath,
                    fileSize = downloadResult.length(),
                    quality = quality,
                    coverArtPath = coverArtPath
                )
                
                Result.success(
                    Data.Builder()
                        .putString("track_id", trackId)
                        .putString("status", "completed")
                        .build()
                )
            } else {
                // Falha no download
                updateDownloadProgress(
                    trackId = trackId,
                    status = DownloadStatus.FAILED,
                    progress = 0f,
                    errorMessage = "Falha ao baixar arquivo de áudio"
                )
                
                Result.failure(
                    Data.Builder()
                        .putString("track_id", trackId)
                        .putString("error", "Falha ao baixar arquivo de áudio")
                        .build()
                )
            }
            
        } catch (e: Exception) {
            val trackId = inputData.getString(KEY_TRACK_ID)
            
            trackId?.let {
                updateDownloadProgress(
                    trackId = it,
                    status = DownloadStatus.FAILED,
                    progress = 0f,
                    errorMessage = e.message
                )
            }
            
            Result.failure(
                Data.Builder()
                    .putString("track_id", trackId)
                    .putString("error", e.message)
                    .build()
            )
        }
    }
    
    /**
     * Executa o download de uma música
     */
    private suspend fun downloadTrack(
        trackId: String,
        originalUrl: String,
        onProgress: suspend (Float) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        try {
            val connection = URL(originalUrl).openConnection() as HttpURLConnection
            connection.connect()
            
            val fileLength = connection.contentLength
            
            // Cria diretório de músicas offline
            val musicDir = File(context.filesDir, "offline_music")
            if (!musicDir.exists()) musicDir.mkdirs()
            
            val outputFile = File(musicDir, "$trackId.mp3")
            
            connection.inputStream.use { input ->
                FileOutputStream(outputFile).use { output ->
                    val buffer = ByteArray(8192) // Buffer maior para melhor performance
                    var total = 0L
                    var count: Int
                    
                    while (input.read(buffer).also { count = it } != -1) {
                        if (isStopped) {
                            // Worker foi cancelado
                            return@withContext null
                        }
                        
                        total += count
                        output.write(buffer, 0, count)
                        
                        // Calcula e reporta progresso
                        val progress = if (fileLength > 0) {
                            (total.toFloat() / fileLength.toFloat())
                        } else 0f
                        
                        onProgress(progress)
                    }
                }
            }
            
            connection.disconnect()
            outputFile
            
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Faz download da capa do álbum
     */
    private suspend fun downloadCoverArt(
        trackId: String,
        coverArtUrl: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            val connection = URL(coverArtUrl).openConnection() as HttpURLConnection
            connection.connect()
            
            // Cria diretório para capas
            val coversDir = File(context.filesDir, "offline_covers")
            if (!coversDir.exists()) coversDir.mkdirs()
            
            val outputFile = File(coversDir, "$trackId.jpg")
            
            connection.inputStream.use { input ->
                FileOutputStream(outputFile).use { output ->
                    input.copyTo(output)
                }
            }
            
            connection.disconnect()
            outputFile.absolutePath
            
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Atualiza o progresso do download no banco de dados
     */
    private suspend fun updateDownloadProgress(
        trackId: String,
        status: DownloadStatus,
        progress: Float,
        errorMessage: String? = null
    ) {
        try {
            val downloadProgress = DownloadProgress(
                trackId = trackId,
                status = status,
                progress = progress,
                bytesDownloaded = 0L, // Será atualizado pelo repositório
                totalBytes = 0L,
                errorMessage = errorMessage,
                updatedAt = Date()
            )
            
            offlineDao.insertDownloadProgress(downloadProgress)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}