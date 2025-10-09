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
import com.shirou.shibamusic.data.database.entity.AlbumEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AlbumDao {

    @Query("SELECT * FROM albums ORDER BY title COLLATE NOCASE")
    suspend fun getAllAlbums(): List<AlbumEntity>

    @Query("SELECT * FROM albums ORDER BY title COLLATE NOCASE")
    fun observeAllAlbums(): Flow<List<AlbumEntity>>

    @Query("SELECT * FROM albums WHERE id = :albumId LIMIT 1")
    suspend fun getAlbumById(albumId: String): AlbumEntity?

    @Query(
        "SELECT * FROM albums " +
            "WHERE title LIKE '%' || :query || '%' " +
            "OR artist_name LIKE '%' || :query || '%' " +
            "OR genre LIKE '%' || :query || '%' " +
            "ORDER BY title COLLATE NOCASE"
    )
    suspend fun searchAlbums(query: String): List<AlbumEntity>

    @Query("SELECT * FROM albums WHERE artist_id = :artistId ORDER BY year DESC, title COLLATE NOCASE")
    suspend fun getAlbumsByArtistId(artistId: String): List<AlbumEntity>

    @Query("SELECT * FROM albums WHERE is_favorite = 1 ORDER BY title COLLATE NOCASE")
    suspend fun getFavoriteAlbums(): List<AlbumEntity>

    @Query("SELECT * FROM albums WHERE is_favorite = 1 ORDER BY title COLLATE NOCASE")
    fun observeFavoriteAlbums(): Flow<List<AlbumEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlbum(album: AlbumEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlbums(albums: List<AlbumEntity>)

    @Update
    suspend fun updateAlbum(album: AlbumEntity)

    @Query("UPDATE albums SET song_count = :songCount, duration_ms = :durationMs WHERE id = :albumId")
    suspend fun updateAlbumStats(albumId: String, songCount: Int, durationMs: Long)

    @Query("UPDATE albums SET is_favorite = :isFavorite WHERE id = :albumId")
    suspend fun updateFavoriteStatus(albumId: String, isFavorite: Boolean)

    @Query("UPDATE albums SET play_count = play_count + 1 WHERE id = :albumId")
    suspend fun incrementPlayCountInternal(albumId: String)

    @Query("DELETE FROM albums")
    suspend fun deleteAllAlbums()

    @Transaction
    suspend fun incrementPlayCount(albumId: String?) {
        albumId?.let { incrementPlayCountInternal(it) }
    }

    @RawQuery(observedEntities = [AlbumEntity::class])
    fun pagingAlbums(query: SupportSQLiteQuery): PagingSource<Int, AlbumEntity>
}
