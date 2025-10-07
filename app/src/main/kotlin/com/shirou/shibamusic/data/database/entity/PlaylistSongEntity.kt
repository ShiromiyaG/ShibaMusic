package com.shirou.shibamusic.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

/**
 * Junction table between playlists and songs with ordering support.
 */
@Entity(
    tableName = "playlist_songs",
    primaryKeys = ["playlist_id", "song_id"]
)
data class PlaylistSongEntity(
    @ColumnInfo(name = "playlist_id")
    val playlistId: String,
    @ColumnInfo(name = "song_id")
    val songId: String,
    @ColumnInfo(name = "position")
    val position: Int,
    @ColumnInfo(name = "date_added")
    val dateAdded: Long = System.currentTimeMillis()
)
