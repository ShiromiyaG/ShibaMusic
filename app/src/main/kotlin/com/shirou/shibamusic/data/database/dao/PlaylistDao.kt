package com.shirou.shibamusic.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.shirou.shibamusic.data.database.entity.PlaylistEntity
import com.shirou.shibamusic.data.database.entity.PlaylistSongEntity
import com.shirou.shibamusic.data.database.entity.SongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {

    @Query("SELECT * FROM playlists ORDER BY date_modified DESC")
    suspend fun getAllPlaylists(): List<PlaylistEntity>

    @Query("SELECT * FROM playlists ORDER BY date_modified DESC")
    fun observeAllPlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE id = :playlistId LIMIT 1")
    suspend fun getPlaylistById(playlistId: String): PlaylistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity)

    @Update
    suspend fun updatePlaylist(playlist: PlaylistEntity)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylistById(playlistId: String)

    @Query("DELETE FROM playlists")
    suspend fun deleteAllPlaylists()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistSong(entity: PlaylistSongEntity)

    @Update
    suspend fun updatePlaylistSongRefs(entities: List<PlaylistSongEntity>)

    @Query("DELETE FROM playlist_songs WHERE playlist_id = :playlistId AND song_id = :songId")
    suspend fun deletePlaylistSong(playlistId: String, songId: String)

    @Query("DELETE FROM playlist_songs WHERE playlist_id = :playlistId")
    suspend fun deleteSongsForPlaylist(playlistId: String)

    @Query("SELECT MAX(position) FROM playlist_songs WHERE playlist_id = :playlistId")
    suspend fun getMaxPosition(playlistId: String): Int?

    @Query("SELECT * FROM playlist_songs WHERE playlist_id = :playlistId ORDER BY position")
    suspend fun getPlaylistSongRefs(playlistId: String): List<PlaylistSongEntity>

    @Query(
        "SELECT songs.* FROM songs " +
            "INNER JOIN playlist_songs ON songs.id = playlist_songs.song_id " +
            "WHERE playlist_songs.playlist_id = :playlistId " +
            "ORDER BY playlist_songs.position"
    )
    suspend fun getSongsInPlaylist(playlistId: String): List<SongEntity>

    @Query(
        "SELECT songs.* FROM songs " +
            "INNER JOIN playlist_songs ON songs.id = playlist_songs.song_id " +
            "WHERE playlist_songs.playlist_id = :playlistId " +
            "ORDER BY playlist_songs.position"
    )
    fun observeSongsInPlaylist(playlistId: String): Flow<List<SongEntity>>

    @Transaction
    suspend fun addSongToPlaylist(playlistId: String, songId: String) {
        val nextPosition = (getMaxPosition(playlistId) ?: -1) + 1
        insertPlaylistSong(PlaylistSongEntity(playlistId, songId, nextPosition))
    }

    @Transaction
    suspend fun addSongToPlaylist(playlistId: String, songId: String, position: Int) {
        insertPlaylistSong(PlaylistSongEntity(playlistId, songId, position))
    }

    @Transaction
    suspend fun removeSongFromPlaylistAndReorder(playlistId: String, songId: String) {
        deletePlaylistSong(playlistId, songId)
        val reordered = getPlaylistSongRefs(playlistId).mapIndexed { index, entity ->
            entity.copy(position = index)
        }
        if (reordered.isNotEmpty()) {
            updatePlaylistSongRefs(reordered)
        }
    }
}
