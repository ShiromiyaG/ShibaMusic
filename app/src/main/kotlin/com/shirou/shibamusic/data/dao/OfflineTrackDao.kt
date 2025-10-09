package com.shirou.shibamusic.data.dao

import androidx.room.*
import com.shirou.shibamusic.data.model.DownloadProgress
import com.shirou.shibamusic.data.model.OfflineTrack
import kotlinx.coroutines.flow.Flow

/**
 * DAO para operações com músicas offline
 */
@Dao
interface OfflineTrackDao {
    
    // Músicas offline
    @Query("SELECT * FROM offline_tracks ORDER BY downloadedAt DESC")
    fun getAllOfflineTracks(): Flow<List<OfflineTrack>>
    
    @Query("SELECT * FROM offline_tracks WHERE id = :trackId LIMIT 1")
    suspend fun getOfflineTrack(trackId: String): OfflineTrack?
    
    @Query("SELECT * FROM offline_tracks WHERE artist = :artist ORDER BY album, title")
    fun getOfflineTracksByArtist(artist: String): Flow<List<OfflineTrack>>
    
    @Query("SELECT * FROM offline_tracks WHERE album = :album ORDER BY title")
    fun getOfflineTracksByAlbum(album: String): Flow<List<OfflineTrack>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOfflineTrack(track: OfflineTrack)
    
    @Update
    suspend fun updateOfflineTrack(track: OfflineTrack)
    
    @Delete
    suspend fun deleteOfflineTrack(track: OfflineTrack)
    
    @Query("DELETE FROM offline_tracks WHERE id = :trackId")
    suspend fun deleteOfflineTrackById(trackId: String)
    
    @Query("DELETE FROM offline_tracks")
    suspend fun deleteAllOfflineTracks()
    
    // Download progress
    @Query("SELECT * FROM download_progress WHERE status != 'COMPLETED' AND status != 'FAILED'")
    fun getActiveDownloads(): Flow<List<DownloadProgress>>
    
    @Query("SELECT * FROM download_progress WHERE trackId = :trackId LIMIT 1")
    suspend fun getDownloadProgress(trackId: String): DownloadProgress?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownloadProgress(progress: DownloadProgress)
    
    @Update
    suspend fun updateDownloadProgress(progress: DownloadProgress)
    
    @Query("DELETE FROM download_progress WHERE trackId = :trackId")
    suspend fun deleteDownloadProgress(trackId: String)
    
    // Consultas de estatísticas
    @Query("SELECT COUNT(*) FROM offline_tracks")
    suspend fun getTotalOfflineTracksCount(): Int
    
    @Query("SELECT SUM(fileSize) FROM offline_tracks")
    suspend fun getTotalOfflineSize(): Long?
    
    @Query("SELECT COUNT(*) FROM offline_tracks WHERE codec = 'OPUS'")
    suspend fun getOpusTracksCount(): Int
    
    @Query("SELECT COUNT(*) FROM offline_tracks WHERE codec = 'FLAC'")
    suspend fun getFlacTracksCount(): Int
    
    @Query("SELECT * FROM offline_tracks WHERE downloadedAt < :beforeDate ORDER BY downloadedAt ASC")
    suspend fun getTracksOlderThan(beforeDate: Long): List<OfflineTrack>
}