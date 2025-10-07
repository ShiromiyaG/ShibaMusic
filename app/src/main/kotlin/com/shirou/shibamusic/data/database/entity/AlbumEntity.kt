package com.shirou.shibamusic.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents an album stored locally in Room.
 */
@Entity(tableName = "albums")
data class AlbumEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "artist_id")
    val artistId: String,
    @ColumnInfo(name = "artist_name")
    val artistName: String,
    @ColumnInfo(name = "album_art_url")
    val albumArtUrl: String? = null,
    @ColumnInfo(name = "year")
    val year: Int? = null,
    @ColumnInfo(name = "genre")
    val genre: String? = null,
    @ColumnInfo(name = "song_count")
    val songCount: Int = 0,
    @ColumnInfo(name = "duration_ms")
    val durationMs: Long = 0,
    @ColumnInfo(name = "is_favorite")
    val isFavorite: Boolean = false,
    @ColumnInfo(name = "play_count")
    val playCount: Int = 0,
    @ColumnInfo(name = "date_added")
    val dateAdded: Long = System.currentTimeMillis()
)
