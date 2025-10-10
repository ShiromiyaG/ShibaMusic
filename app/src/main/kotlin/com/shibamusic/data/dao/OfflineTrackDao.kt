package com.shibamusic.data.dao

import androidx.room.*
import com.shibamusic.data.model.DownloadProgress
import com.shibamusic.data.model.DownloadStatus
import com.shibamusic.data.model.OfflineTrack
import kotlinx.coroutines.flow.Flow
import java.util.Date

/**
 * DAO para gerenciar o acesso aos dados de músicas offline
 */
@Dao
interface OfflineTrackDao {
    
    @Query("SELECT * FROM offline_tracks ORDER BY downloadedAt DESC")
    fun getAllOfflineTracks(): Flow<List<OfflineTrack>>

    @Query("SELECT * FROM offline_tracks ORDER BY downloadedAt DESC")
    suspend fun getOfflineTracksSnapshot(): List<OfflineTrack>
    
    @Query("SELECT * FROM offline_tracks WHERE id = :trackId")
    suspend fun getOfflineTrack(trackId: String): OfflineTrack?
    
    @Query("SELECT * FROM offline_tracks WHERE artist = :artist ORDER BY title")
    fun getTracksByArtist(artist: String): Flow<List<OfflineTrack>>
    
    @Query("SELECT * FROM offline_tracks WHERE album = :album ORDER BY title")
    fun getTracksByAlbum(album: String): Flow<List<OfflineTrack>>
    
    @Query("SELECT EXISTS(SELECT 1 FROM offline_tracks WHERE id = :trackId)")
    suspend fun isTrackDownloaded(trackId: String): Boolean
    
    @Query("SELECT COUNT(*) FROM offline_tracks")
    suspend fun getOfflineTracksCount(): Int
    
    @Query("SELECT SUM(fileSize) FROM offline_tracks")
    suspend fun getTotalOfflineSize(): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOfflineTrack(track: OfflineTrack)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOfflineTracks(tracks: List<OfflineTrack>)
    
    @Update
    suspend fun updateOfflineTrack(track: OfflineTrack)
    
    @Delete
    suspend fun deleteOfflineTrack(track: OfflineTrack)
    
    @Query("DELETE FROM offline_tracks WHERE id = :trackId")
    suspend fun deleteOfflineTrackById(trackId: String)
    
    @Query("DELETE FROM offline_tracks")
    suspend fun deleteAllOfflineTracks()
    
    // Métodos para gerenciar o progresso de download
    @Query("SELECT * FROM download_progress WHERE trackId = :trackId")
    suspend fun getDownloadProgress(trackId: String): DownloadProgress?
    
    @Query("SELECT * FROM download_progress WHERE status = :status")
    fun getDownloadsByStatus(status: DownloadStatus): Flow<List<DownloadProgress>>
    
    @Query("SELECT * FROM download_progress WHERE status IN (:statuses)")
    fun getActiveDownloads(statuses: List<DownloadStatus>): Flow<List<DownloadProgress>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownloadProgress(progress: DownloadProgress)
    
    @Update
    suspend fun updateDownloadProgress(progress: DownloadProgress)

    @Query(
        "UPDATE download_progress SET " +
            "status = :status, " +
            "progress = :progress, " +
            "bytesDownloaded = :bytesDownloaded, " +
            "totalBytes = :totalBytes, " +
            "errorMessage = :errorMessage, " +
            "updatedAt = :updatedAt " +
            "WHERE trackId = :trackId"
    )
    suspend fun updateDownloadProgressFields(
        trackId: String,
        status: DownloadStatus,
        progress: Float,
        bytesDownloaded: Long,
        totalBytes: Long,
        errorMessage: String?,
        updatedAt: Date
    ): Int
    
    @Query("DELETE FROM download_progress WHERE trackId = :trackId")
    suspend fun deleteDownloadProgress(trackId: String)
    
    @Query("DELETE FROM download_progress WHERE status = :status")
    suspend fun deleteDownloadsByStatus(status: DownloadStatus)
}
