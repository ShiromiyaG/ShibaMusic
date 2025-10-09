package com.shibamusic.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.shirou.shibamusic.R
import com.shibamusic.data.dao.OfflineTrackDao
import com.shibamusic.data.model.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import javax.inject.Inject

/**
 * Serviço responsável pelo download e gerenciamento de músicas offline
 * Suporta transcodificação para Opus em qualidades 128 e 320 kbps
 */
@AndroidEntryPoint
class OfflineDownloadService : Service() {
    
    @Inject
    lateinit var offlineDao: OfflineTrackDao
    
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "offline_download_channel"
        
        fun startDownload(
            context: Context, 
            trackId: String, 
            baseUrl: String, 
            quality: AudioQuality = AudioQuality.MEDIUM
        ) {
            val intent = Intent(context, OfflineDownloadService::class.java).apply {
                putExtra("track_id", trackId)
                putExtra("base_url", baseUrl)
                putExtra("quality", quality.name)
                action = "START_DOWNLOAD"
            }
            context.startForegroundService(intent)
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val trackId = intent?.getStringExtra("track_id")
        val baseUrl = intent?.getStringExtra("base_url")
        val qualityName = intent?.getStringExtra("quality") ?: AudioQuality.MEDIUM.name
        val quality = AudioQuality.valueOf(qualityName)
        
        if (trackId != null && baseUrl != null) {
            when (intent.action) {
                "START_DOWNLOAD" -> startDownload(trackId, baseUrl, quality)
                "CANCEL_DOWNLOAD" -> cancelDownload(trackId)
            }
        }
        
        return START_NOT_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Downloads Offline",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notificações de download de músicas offline"
            }
            
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
    
    private fun startDownload(trackId: String, baseUrl: String, quality: AudioQuality) {
        serviceScope.launch {
            try {
                // Cria entrada de progresso
                val progress = DownloadProgress(
                    trackId = trackId,
                    status = DownloadStatus.DOWNLOADING,
                    progress = 0f,
                    bytesDownloaded = 0L,
                    totalBytes = 0L
                )
                offlineDao.insertDownloadProgress(progress)
                
                startForeground(NOTIFICATION_ID, createDownloadNotification(trackId, 0f, quality))
                
                val downloadUrl = buildDownloadUrl(baseUrl, trackId, quality)
                
                downloadTrack(trackId, downloadUrl, quality) { currentProgress ->
                    // Atualiza progresso no banco
                    serviceScope.launch {
                        val updatedProgress = progress.copy(
                            progress = currentProgress,
                            updatedAt = Date()
                        )
                        offlineDao.updateDownloadProgress(updatedProgress)
                    }
                    
                    // Atualiza notificação
                    val notification = createDownloadNotification(trackId, currentProgress, quality)
                    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.notify(NOTIFICATION_ID, notification)
                }
                
            } catch (e: Exception) {
                handleDownloadError(trackId, e)
            } finally {
                stopSelf()
            }
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
        
        // Adiciona parâmetros de transcodificação para Opus
        when (quality) {
            AudioQuality.LOW, AudioQuality.MEDIUM -> {
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
    
    private suspend fun downloadTrack(
        trackId: String, 
        url: String, 
        quality: AudioQuality,
        onProgress: (Float) -> Unit
    ) {
        val connection = URL(url).openConnection() as HttpURLConnection
        
        try {
            // Adiciona cabeçalhos de autenticação se necessário
            connection.setRequestProperty("User-Agent", "ShibaMusic/1.0")
            connection.connect()
            
            val fileLength = connection.contentLength
            
            // Cria diretório de músicas offline
            val musicDir = File(filesDir, "offline_music")
            if (!musicDir.exists()) musicDir.mkdirs()
            
            // Usa extensão correta baseada na qualidade
            val outputFile = File(musicDir, "$trackId.${quality.fileExtension}")
            
            connection.inputStream.use { input ->
                FileOutputStream(outputFile).use { output ->
                    val buffer = ByteArray(8192) // Buffer maior para melhor performance
                    var total = 0L
                    var count: Int
                    
                    while (input.read(buffer).also { count = it } != -1) {
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
            
            // Marca como concluído
            val completedProgress = DownloadProgress(
                trackId = trackId,
                status = DownloadStatus.COMPLETED,
                progress = 1f,
                bytesDownloaded = outputFile.length(),
                totalBytes = fileLength.toLong(),
                updatedAt = Date()
            )
            offlineDao.updateDownloadProgress(completedProgress)
            
        } finally {
            connection.disconnect()
        }
    }
    
    private suspend fun handleDownloadError(trackId: String, error: Exception) {
        val errorProgress = DownloadProgress(
            trackId = trackId,
            status = DownloadStatus.FAILED,
            progress = 0f,
            bytesDownloaded = 0L,
            totalBytes = 0L,
            errorMessage = error.message,
            updatedAt = Date()
        )
        offlineDao.updateDownloadProgress(errorProgress)
    }
    
    private fun cancelDownload(trackId: String) {
        serviceScope.launch {
            offlineDao.deleteDownloadProgress(trackId)
        }
        stopSelf()
    }
    
    private fun createDownloadNotification(
        trackId: String, 
        progress: Float, 
        quality: AudioQuality
    ): android.app.Notification {
        val cancelIntent = Intent(this, OfflineDownloadService::class.java).apply {
            action = "CANCEL_DOWNLOAD"
            putExtra("track_id", trackId)
        }
        
        val cancelPendingIntent = PendingIntent.getService(
            this, 0, cancelIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val qualityText = when (quality) {
            AudioQuality.LOW -> "Opus 128kbps"
            AudioQuality.MEDIUM -> "Opus 320kbps"
            AudioQuality.HIGH -> "FLAC Lossless"
        }
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Baixando música")
            .setContentText("Progresso: ${(progress * 100).toInt()}% ($qualityText)")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, (progress * 100).toInt(), false)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancelar", cancelPendingIntent)
            .setOngoing(true)
            .build()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}