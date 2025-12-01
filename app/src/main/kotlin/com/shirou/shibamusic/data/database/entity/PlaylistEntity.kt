package com.shirou.shibamusic.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a playlist stored locally in Room.
 */
@Entity(
    tableName = "playlists",
    indices = [
        Index(value = ["name"]),
        Index(value = ["date_modified"]),
        // Índices compostos para ordenação otimizada na Library
        Index(value = ["name", "id"]),
        Index(value = ["date_created"]),
        Index(value = ["song_count"])
    ]
)
data class PlaylistEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "description")
    val description: String? = null,
    @ColumnInfo(name = "cover_url")
    val coverUrl: String? = null,
    @ColumnInfo(name = "song_count")
    val songCount: Int = 0,
    @ColumnInfo(name = "duration_ms")
    val durationMs: Long = 0,
    @ColumnInfo(name = "date_created")
    val dateCreated: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "date_modified")
    val dateModified: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "is_favorite")
    val isFavorite: Boolean = false
)
