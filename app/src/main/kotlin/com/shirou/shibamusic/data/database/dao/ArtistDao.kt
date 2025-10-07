package com.shirou.shibamusic.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.shirou.shibamusic.data.database.entity.ArtistEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ArtistDao {

    @Query("SELECT * FROM artists ORDER BY name COLLATE NOCASE")
    suspend fun getAllArtists(): List<ArtistEntity>

    @Query("SELECT * FROM artists ORDER BY name COLLATE NOCASE")
    fun observeAllArtists(): Flow<List<ArtistEntity>>

    @Query("SELECT * FROM artists WHERE id = :artistId LIMIT 1")
    suspend fun getArtistById(artistId: String): ArtistEntity?

    @Query(
        "SELECT * FROM artists " +
            "WHERE name LIKE '%' || :query || '%' " +
            "OR genre LIKE '%' || :query || '%' " +
            "ORDER BY name COLLATE NOCASE"
    )
    suspend fun searchArtists(query: String): List<ArtistEntity>

    @Query("SELECT * FROM artists WHERE is_favorite = 1 ORDER BY name COLLATE NOCASE")
    suspend fun getFavoriteArtists(): List<ArtistEntity>

    @Query("SELECT * FROM artists WHERE is_favorite = 1 ORDER BY name COLLATE NOCASE")
    fun observeFavoriteArtists(): Flow<List<ArtistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArtist(artist: ArtistEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArtists(artists: List<ArtistEntity>)

    @Query("UPDATE artists SET is_favorite = :isFavorite WHERE id = :artistId")
    suspend fun updateFavoriteStatus(artistId: String, isFavorite: Boolean)

    @Query("DELETE FROM artists")
    suspend fun deleteAllArtists()
}
