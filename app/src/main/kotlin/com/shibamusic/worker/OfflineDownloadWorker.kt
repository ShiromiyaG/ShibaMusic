package com.shibamusic.worker

import android.content.Context
import android.net.Uri
import android.os.SystemClock
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.shibamusic.data.dao.OfflineTrackDao
import com.shibamusic.data.model.*
import com.shibamusic.repository.OfflineRepository
import com.shirou.shibamusic.App
import com.shirou.shibamusic.util.Util
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import java.util.zip.GZIPInputStream

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
                progress = 0f,
                bytesDownloaded = 0L,
                totalBytes = 0L
            )
            
            // Constrói URL de download com transcodificação
            val downloadUrl = buildDownloadUrl(baseUrl, trackId, quality)
            
            // Executa o download
            val downloadResult = downloadTrack(
                trackId = trackId,
                downloadUrl = downloadUrl,
                quality = quality
            ) { bytesDownloaded, totalBytes, progress ->
                updateDownloadProgress(
                    trackId = trackId,
                    status = DownloadStatus.DOWNLOADING,
                    progress = progress,
                    bytesDownloaded = bytesDownloaded,
                    totalBytes = totalBytes
                )
                
                setProgressAsync(
                    Data.Builder()
                        .putString("track_id", trackId)
                        .putFloat("progress", progress)
                        .putLong("bytes_downloaded", bytesDownloaded)
                        .putLong("total_bytes", totalBytes)
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

                setProgressAsync(
                    Data.Builder()
                        .putString("track_id", trackId)
                        .putFloat("progress", 1f)
                        .putLong("bytes_downloaded", downloadResult.length())
                        .putLong("total_bytes", downloadResult.length())
                        .putString("codec", quality.codec.displayName)
                        .build()
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
                    bytesDownloaded = 0L,
                    totalBytes = 0L,
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
                    bytesDownloaded = 0L,
                    totalBytes = 0L,
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
        val subsonicClient = App.getSubsonicClientInstance(false)
        val params = subsonicClient.params

        val uriBuilder = if (baseUrl.isNotBlank()) {
            Uri.parse(baseUrl).buildUpon()
                .appendPath("rest")
                .appendPath("stream")
        } else {
            Uri.parse(subsonicClient.url).buildUpon()
                .appendPath("stream")
        }

        val authKeys = listOf("u", "p", "s", "t", "v", "c")
        authKeys.forEach { key ->
            params[key]?.let { value ->
                val paramValue = if (key == "u") Util.encode(value) else value
                uriBuilder.appendQueryParameter(key, paramValue)
            }
        }

        when (quality) {
            AudioQuality.LOW, AudioQuality.MEDIUM -> {
                uriBuilder.appendQueryParameter("format", quality.getTranscodeFormat())
                uriBuilder.appendQueryParameter("maxBitRate", quality.getBitrateString())
            }
            AudioQuality.HIGH -> {
                uriBuilder.appendQueryParameter("format", quality.getTranscodeFormat())
            }
        }

        uriBuilder.appendQueryParameter("id", trackId)

        return uriBuilder.build().toString()
    }
    /**
     * Executa o download de uma música com suporte a Opus
     */
    private suspend fun downloadTrack(
        trackId: String,
        downloadUrl: String,
        quality: AudioQuality,
        onProgress: suspend (bytesDownloaded: Long, totalBytes: Long, progress: Float) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        var outputFile: File? = null
        return@withContext try {
            connection = URL(downloadUrl).openConnection() as HttpURLConnection
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            connection.setRequestProperty("User-Agent", "ShibaMusic/1.0")
            connection.setRequestProperty("Accept", quality.codec.downloadMimeType)
            connection.setRequestProperty("Accept-Encoding", "identity")
            connection.connect()
            
            val totalBytes = connection.contentLengthLong.takeIf { it > 0 } ?: -1L
            
            val musicDir = File(context.filesDir, "offline_music")
            if (!musicDir.exists()) musicDir.mkdirs()
            
            outputFile = File(musicDir, "$trackId.${quality.fileExtension}")
            
            var totalDownloaded = 0L
            var lastProgress = 0f
            var lastUpdateAt = SystemClock.elapsedRealtime()
            onProgress(0L, totalBytes, 0f)
            
            val rawInputStream = connection.inputStream
            val inputStream = if (connection.contentEncoding.equals("gzip", true)) {
                GZIPInputStream(rawInputStream)
            } else {
                rawInputStream
            }

            inputStream.use { input ->
                FileOutputStream(outputFile).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var count: Int
                    
                    while (input.read(buffer).also { count = it } != -1) {
                        if (isStopped) {
                            outputFile?.delete()
                            connection?.disconnect()
                            return@withContext null
                        }
                        
                        output.write(buffer, 0, count)
                        totalDownloaded += count
                        
                        val progress = if (totalBytes > 0) {
                            (totalDownloaded.toDouble() / totalBytes.toDouble()).toFloat()
                        } else 0f
                        
                        val now = SystemClock.elapsedRealtime()
                        val shouldEmit = (progress - lastProgress) >= 0.02f || (now - lastUpdateAt) >= 500L
                        if (shouldEmit) {
                            onProgress(totalDownloaded, totalBytes, progress.coerceIn(0f, 1f))
                            lastProgress = progress
                            lastUpdateAt = now
                        }
                    }
                }
            }
            
            onProgress(totalDownloaded, totalBytes, 1f)
            connection?.disconnect()
            outputFile
        } catch (e: Exception) {
            outputFile?.delete()
            connection?.disconnect()
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
        bytesDownloaded: Long,
        totalBytes: Long,
        errorMessage: String? = null
    ) {
        try {
            val now = Date()
            val updatedRows = offlineDao.updateDownloadProgressFields(
                trackId = trackId,
                status = status,
                progress = progress,
                bytesDownloaded = bytesDownloaded,
                totalBytes = totalBytes,
                errorMessage = errorMessage,
                updatedAt = now
            )
            
            if (updatedRows == 0) {
                offlineDao.insertDownloadProgress(
                    DownloadProgress(
                        trackId = trackId,
                        status = status,
                        progress = progress,
                        bytesDownloaded = bytesDownloaded,
                        totalBytes = totalBytes,
                        errorMessage = errorMessage,
                        createdAt = now,
                        updatedAt = now
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
