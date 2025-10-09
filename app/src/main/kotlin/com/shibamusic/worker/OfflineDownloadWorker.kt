package com.shibamusic.worker

import android.content.Context
import android.net.Uri
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
 * Suporta transcodificação para Opus nas qualidades 128 e 320 kbps
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
        private const val KEY_BASE_URL = "base_url"
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
            baseUrl: String,
            coverArtUrl: String? = null,
            quality: AudioQuality = AudioQuality.MEDIUM
        ): OneTimeWorkRequest {
            val inputData = Data.Builder()
                .putString(KEY_TRACK_ID, trackId)
                .putString(KEY_TITLE, title)
                .putString(KEY_ARTIST, artist)
                .putString(KEY_ALBUM, album)
                .putLong(KEY_DURATION, duration)
                .putString(KEY_BASE_URL, baseUrl)
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
            val baseUrl = inputData.getString(KEY_BASE_URL) ?: return@withContext Result.failure()
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
            
            // Constrói URL de download com transcodificação
            val downloadUrl = buildDownloadUrl(baseUrl, trackId, quality)
            
            // Executa o download
            val downloadResult = downloadTrack(
                trackId = trackId,
                downloadUrl = downloadUrl,
                quality = quality
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
                        .putString("codec", quality.codec.displayName)
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
                    originalUrl = downloadUrl,
                    localFilePath = downloadResult.absolutePath,
                    fileSize = downloadResult.length(),
                    quality = quality,
                    coverArtPath = coverArtPath
                )
                
                Result.success(
                    Data.Builder()
                        .putString("track_id", trackId)
                        .putString("status", "completed")
                        .putString("codec", quality.codec.displayName)
                        .putLong("file_size", downloadResult.length())
                        .build()
                )
            } else {
                // Falha no download
                updateDownloadProgress(
                    trackId = trackId,
                    status = DownloadStatus.FAILED,
                    progress = 0f,
                    errorMessage = "Falha ao baixar arquivo de áudio ${quality.codec.displayName}"
                )
                
                Result.failure(
                    Data.Builder()
                        .putString("track_id", trackId)
                        .putString("error", "Falha ao baixar arquivo de áudio ${quality.codec.displayName}")
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
     * Constrói a URL de download com parâmetros de transcodificação para Opus
     */
    private fun buildDownloadUrl(baseUrl: String, trackId: String, quality: AudioQuality): String {
        val uri = Uri.parse(baseUrl).buildUpon()
            .appendPath("rest")
            .appendPath("stream")
            .appendQueryParameter("id", trackId)
            .appendQueryParameter("v", "1.16.1")
            .appendQueryParameter("c", "ShibaMusic")
        
        // Adiciona parâmetros de transcodificação
        when (quality) {
            AudioQuality.LOW, AudioQuality.MEDIUM -> {
                // Para Opus 128kbps e 320kbps
                uri.appendQueryParameter("format", quality.getTranscodeFormat())
                uri.appendQueryParameter("maxBitRate", quality.getBitrateString())
            }
            AudioQuality.HIGH -> {
                // Para FLAC, usa download direto sem transcodificação
                uri.appendQueryParameter("format", "raw")
            }
        }
        
        return uri.build().toString()
    }
    
    /**
     * Executa o download de uma música com suporte a Opus
     */
    private suspend fun downloadTrack(
        trackId: String,
        downloadUrl: String,
        quality: AudioQuality,
        onProgress: suspend (Float) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        try {
            val connection = URL(downloadUrl).openConnection() as HttpURLConnection
            
            // Adiciona cabeçalhos
            connection.setRequestProperty("User-Agent", "ShibaMusic/1.0")
            connection.setRequestProperty("Accept", quality.codec.mimeType)
            connection.connect()
            
            val fileLength = connection.contentLength
            
            // Cria diretório de músicas offline
            val musicDir = File(context.filesDir, "offline_music")
            if (!musicDir.exists()) musicDir.mkdirs()
            
            // Usa extensão correta baseada na qualidade
            val outputFile = File(musicDir, "$trackId.${quality.fileExtension}")
            
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
                createdAt = Date(),
                updatedAt = Date()
            )
            
            offlineDao.insertDownloadProgress(downloadProgress)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}