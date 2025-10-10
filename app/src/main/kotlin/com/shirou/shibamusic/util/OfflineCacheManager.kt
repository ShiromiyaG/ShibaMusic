package com.shirou.shibamusic.util

import android.content.Context
import com.shirou.shibamusic.data.dao.OfflineTrackDao
import com.shirou.shibamusic.data.model.OfflineTrack
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gerenciador de cache para funcionalidades offline
 * Responsável pela limpeza, organização e otimização do armazenamento
 */
@Singleton
class OfflineCacheManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val offlineDao: OfflineTrackDao
) {
    
    companion object {
        private const val MUSIC_DIR_NAME = "offline_music"
        private const val COVERS_DIR_NAME = "offline_covers"
        
        // Limites de cache (podem ser configurados pelo usuário)
        private const val MAX_CACHE_SIZE_MB = 1024L // 1GB por padrão
        private const val MAX_TRACKS_COUNT = 500
        
        // Tempo de retenção para limpeza automática
        private const val RETENTION_DAYS = 30L
    }
    
    private val musicDir: File
        get() = File(context.filesDir, MUSIC_DIR_NAME)
    
    private val coversDir: File
        get() = File(context.filesDir, COVERS_DIR_NAME)
    
    /**
     * Inicializa os diretórios de cache
     */
    fun initializeCacheDirectories() {
        if (!musicDir.exists()) {
            musicDir.mkdirs()
        }
        if (!coversDir.exists()) {
            coversDir.mkdirs()
        }
    }
    
    /**
     * Obtém informações sobre o cache atual
     */
    suspend fun getCacheInfo(): CacheInfo = withContext(Dispatchers.IO) {
        val musicFiles = musicDir.listFiles() ?: emptyArray()
        val coverFiles = coversDir.listFiles() ?: emptyArray()
        
        val musicSizeBytes = musicFiles.sumOf { it.length() }
        val coversSizeBytes = coverFiles.sumOf { it.length() }
        val totalSizeBytes = musicSizeBytes + coversSizeBytes
        
        val tracksInDb = offlineDao.getOfflineTracksCount()
        val availableSpace = context.filesDir.usableSpace
        
        CacheInfo(
            totalSizeBytes = totalSizeBytes,
            musicSizeBytes = musicSizeBytes,
            coversSizeBytes = coversSizeBytes,
            tracksCount = tracksInDb,
            filesOnDisk = musicFiles.size,
            availableSpaceBytes = availableSpace,
            isOverLimit = totalSizeBytes > (MAX_CACHE_SIZE_MB * 1024 * 1024) || tracksInDb > MAX_TRACKS_COUNT
        )
    }
    
    /**
     * Verifica a integridade do cache e remove arquivos órfãos
     */
    suspend fun verifyAndCleanIntegrity(): IntegrityReport = withContext(Dispatchers.IO) {
        val report = IntegrityReport()
        
        // Verifica arquivos de música
        val musicFiles = musicDir.listFiles() ?: emptyArray()
        val coverFiles = coversDir.listFiles() ?: emptyArray()
        
        // Obtém todas as músicas do banco
        val offlineTracks = mutableListOf<OfflineTrack>()
        offlineDao.getAllOfflineTracks().collect { tracks ->
            offlineTracks.addAll(tracks)
        }
        
        val validTrackIds = offlineTracks.map { it.id }.toSet()
        
        // Remove arquivos de música órfãos
        musicFiles.forEach { file ->
            val trackId = file.nameWithoutExtension
            if (trackId !in validTrackIds) {
                if (file.delete()) {
                    report.orphanedMusicFilesRemoved++
                    report.freedSpaceBytes += file.length()
                }
            }
        }
        
        // Remove arquivos de capa órfãos
        coverFiles.forEach { file ->
            val trackId = file.nameWithoutExtension
            if (trackId !in validTrackIds) {
                if (file.delete()) {
                    report.orphanedCoverFilesRemoved++
                    report.freedSpaceBytes += file.length()
                }
            }
        }
        
        // Remove entradas do banco sem arquivo correspondente
        offlineTracks.forEach { track ->
            val musicFile = File(track.localFilePath)
            if (!musicFile.exists()) {
                offlineDao.deleteOfflineTrack(track)
                report.corruptedDatabaseEntriesRemoved++
            }
        }
        
        report
    }
    
    /**
     * Executa limpeza automática baseada em políticas
     */
    suspend fun performAutomaticCleanup(): CleanupReport = withContext(Dispatchers.IO) {
        val report = CleanupReport()
        val cacheInfo = getCacheInfo()
        
        if (!cacheInfo.isOverLimit) {
            return@withContext report
        }
        
        // Obtém músicas ordenadas por data de download (mais antigas primeiro)
        val offlineTracks = mutableListOf<OfflineTrack>()
        offlineDao.getAllOfflineTracks().collect { tracks ->
            offlineTracks.addAll(tracks.sortedBy { it.downloadedAt })
        }
        
        var currentSize = cacheInfo.totalSizeBytes
        val targetSize = (MAX_CACHE_SIZE_MB * 1024 * 1024 * 0.8).toLong() // Remove até 80% do limite
        
        // Remove músicas antigas até atingir o tamanho alvo
        for (track in offlineTracks) {
            if (currentSize <= targetSize && offlineTracks.size - report.tracksRemoved <= MAX_TRACKS_COUNT) {
                break
            }
            
            val removed = removeTrackFiles(track)
            if (removed > 0) {
                offlineDao.deleteOfflineTrack(track)
                report.tracksRemoved++
                report.freedSpaceBytes += removed
                currentSize -= removed
            }
        }
        
        report
    }
    
    /**
     * Remove músicas baseadas em critérios de retenção
     */
    suspend fun cleanupByRetentionPolicy(): CleanupReport = withContext(Dispatchers.IO) {
        val report = CleanupReport()
        val cutoffDate = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -RETENTION_DAYS.toInt())
        }.time
        
        val offlineTracks = mutableListOf<OfflineTrack>()
        offlineDao.getAllOfflineTracks().collect { tracks ->
            offlineTracks.addAll(tracks.filter { it.downloadedAt.before(cutoffDate) })
        }
        
        for (track in offlineTracks) {
            val removed = removeTrackFiles(track)
            if (removed > 0) {
                offlineDao.deleteOfflineTrack(track)
                report.tracksRemoved++
                report.freedSpaceBytes += removed
            }
        }
        
        report
    }
    
    /**
     * Remove todos os dados de cache
     */
    suspend fun clearAllCache(): Long = withContext(Dispatchers.IO) {
        var totalFreed = 0L
        
        // Remove todos os arquivos de música
        musicDir.listFiles()?.forEach { file ->
            totalFreed += file.length()
            file.delete()
        }
        
        // Remove todos os arquivos de capa
        coversDir.listFiles()?.forEach { file ->
            totalFreed += file.length()
            file.delete()
        }
        
        // Remove entradas do banco
        offlineDao.deleteAllOfflineTracks()
        
        totalFreed
    }
    
    /**
     * Otimiza o armazenamento reorganizando arquivos
     */
    suspend fun optimizeStorage(): OptimizationReport = withContext(Dispatchers.IO) {
        val report = OptimizationReport()
        
        // Remove fragmentação (se necessário para o sistema de arquivos)
        // Por enquanto, apenas executa verificação de integridade
        val integrityReport = verifyAndCleanIntegrity()
        
        report.integrityIssuesFixed = integrityReport.orphanedMusicFilesRemoved + 
                                     integrityReport.orphanedCoverFilesRemoved + 
                                     integrityReport.corruptedDatabaseEntriesRemoved
        report.spaceReclaimed = integrityReport.freedSpaceBytes
        
        report
    }
    
    /**
     * Remove arquivos associados a uma música
     */
    private fun removeTrackFiles(track: OfflineTrack): Long {
        var totalRemoved = 0L
        
        // Remove arquivo de música
        val musicFile = File(track.localFilePath)
        if (musicFile.exists()) {
            totalRemoved += musicFile.length()
            musicFile.delete()
        }
        
        // Remove arquivo de capa se existir
        track.coverArtPath?.let { coverPath ->
            val coverFile = File(coverPath)
            if (coverFile.exists()) {
                totalRemoved += coverFile.length()
                coverFile.delete()
            }
        }
        
        return totalRemoved
    }
    
    /**
     * Verifica se há espaço suficiente para um download
     */
    fun hasEnoughSpace(requiredSizeBytes: Long): Boolean {
        val availableSpace = context.filesDir.usableSpace
        return availableSpace > (requiredSizeBytes * 1.2) // Margem de 20%
    }
    
    /**
     * Verifica se os limites de cache seriam excedidos
     */
    suspend fun wouldExceedLimits(additionalSizeBytes: Long): Boolean {
        val currentInfo = getCacheInfo()
        val newSize = currentInfo.totalSizeBytes + additionalSizeBytes
        val newCount = currentInfo.tracksCount + 1
        
        return newSize > (MAX_CACHE_SIZE_MB * 1024 * 1024) || newCount > MAX_TRACKS_COUNT
    }
}

/**
 * Informações sobre o estado atual do cache
 */
data class CacheInfo(
    val totalSizeBytes: Long,
    val musicSizeBytes: Long,
    val coversSizeBytes: Long,
    val tracksCount: Int,
    val filesOnDisk: Int,
    val availableSpaceBytes: Long,
    val isOverLimit: Boolean
) {
    val totalSizeMB: Double get() = totalSizeBytes / (1024.0 * 1024.0)
    val availableSpaceMB: Double get() = availableSpaceBytes / (1024.0 * 1024.0)
}

/**
 * Relatório de verificação de integridade
 */
data class IntegrityReport(
    var orphanedMusicFilesRemoved: Int = 0,
    var orphanedCoverFilesRemoved: Int = 0,
    var corruptedDatabaseEntriesRemoved: Int = 0,
    var freedSpaceBytes: Long = 0L
)

/**
 * Relatório de limpeza
 */
data class CleanupReport(
    var tracksRemoved: Int = 0,
    var freedSpaceBytes: Long = 0L
) {
    val freedSpaceMB: Double get() = freedSpaceBytes / (1024.0 * 1024.0)
}

/**
 * Relatório de otimização
 */
data class OptimizationReport(
    var integrityIssuesFixed: Int = 0,
    var spaceReclaimed: Long = 0L
) {
    val spaceReclaimedMB: Double get() = spaceReclaimed / (1024.0 * 1024.0)
}