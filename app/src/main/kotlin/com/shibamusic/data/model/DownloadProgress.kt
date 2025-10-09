package com.shibamusic.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "download_progress")
data class DownloadProgress(
    @PrimaryKey val trackId: String,
    val status: DownloadStatus,
    val progress: Float,
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val errorMessage: String?,
    val createdAt: Date,
    val updatedAt: Date
)
