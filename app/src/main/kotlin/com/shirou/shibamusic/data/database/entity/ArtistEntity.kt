package com.shirou.shibamusic.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents an artist stored locally in Room.
 */
@Entity(
    tableName = "artists",
    indices = [
        Index(value = ["name"])
    ]
)
data class ArtistEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "image_url")
    val imageUrl: String? = null,
    @ColumnInfo(name = "album_count")
    val albumCount: Int = 0,
    @ColumnInfo(name = "song_count")
    val songCount: Int = 0,
    @ColumnInfo(name = "is_favorite")
    val isFavorite: Boolean = false,
    @ColumnInfo(name = "genre")
    val genre: String? = null,
    @ColumnInfo(name = "date_added")
    val dateAdded: Long = System.currentTimeMillis()
)
