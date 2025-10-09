package com.shibamusic.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "offline_tracks")
data class OfflineTrack(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val localFilePath: String,
    val originalUrl: String,
    val coverArtPath: String?,
    val downloadedAt: Date,
    val fileSize: Long,
    val quality: AudioQuality,
    val codec: AudioCodec,
    val isComplete: Boolean = true
)