package com.shirou.shibamusic.data.database.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.room.Update
import androidx.sqlite.db.SupportSQLiteQuery
import com.shirou.shibamusic.data.database.entity.SongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {

    @Query("SELECT * FROM songs ORDER BY title COLLATE NOCASE")
    suspend fun getAllSongs(): List<SongEntity>

    @Query("SELECT * FROM songs ORDER BY title COLLATE NOCASE")
    fun observeAllSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE id = :songId LIMIT 1")
    suspend fun getSongById(songId: String): SongEntity?

    @Query(
        "SELECT * FROM songs " +
            "WHERE title LIKE '%' || :query || '%' " +
            "OR artist_name LIKE '%' || :query || '%' " +
            "OR album_name LIKE '%' || :query || '%' " +
            "ORDER BY title COLLATE NOCASE"
    )
    suspend fun searchSongs(query: String): List<SongEntity>

    @Query("SELECT * FROM songs WHERE album_id = :albumId ORDER BY track_number")
    suspend fun getSongsByAlbumId(albumId: String): List<SongEntity>

    @Query("SELECT * FROM songs WHERE artist_id = :artistId ORDER BY title COLLATE NOCASE")
    suspend fun getSongsByArtistId(artistId: String): List<SongEntity>

    @Query("SELECT * FROM songs WHERE is_favorite = 1 ORDER BY title COLLATE NOCASE")
    suspend fun getFavoriteSongs(): List<SongEntity>

    @Query("SELECT * FROM songs WHERE is_favorite = 1 ORDER BY title COLLATE NOCASE")
    fun observeFavoriteSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE last_played_timestamp IS NOT NULL ORDER BY last_played_timestamp DESC LIMIT :limit")
    suspend fun getRecentlyPlayedSongs(limit: Int): List<SongEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: SongEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<SongEntity>)

    @Update
    suspend fun updateSong(song: SongEntity)

    @Query("UPDATE songs SET is_favorite = :isFavorite, date_modified = :timestamp WHERE id = :songId")
    suspend fun updateFavoriteStatus(songId: String, isFavorite: Boolean, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE songs SET play_count = play_count + 1, last_played_timestamp = :timestamp, date_modified = :timestamp WHERE id = :songId")
    suspend fun incrementPlayCount(songId: String, timestamp: Long)

    @Query("UPDATE songs SET play_count = play_count + 1 WHERE id = :songId")
    suspend fun incrementPlayCount(songId: String)

    @Query("SELECT COUNT(*) FROM songs")
    suspend fun getSongCount(): Int

    @Query("DELETE FROM songs WHERE id = :songId")
    suspend fun deleteSongById(songId: String)

    @Query("DELETE FROM songs")
    suspend fun deleteAllSongs()

    /**
     * Wrapper to increment play count with the current timestamp.
     */
    @Transaction
    suspend fun incrementPlayCountWithTimestamp(songId: String) {
        incrementPlayCount(songId, System.currentTimeMillis())
    }

    @RawQuery(observedEntities = [SongEntity::class])
    fun pagingSongs(query: SupportSQLiteQuery): PagingSource<Int, SongEntity>
}
