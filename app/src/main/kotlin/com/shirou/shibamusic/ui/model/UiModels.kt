package com.shirou.shibamusic.ui.model

/**
 * Central UI models for the ShibaMusic app
 * These models represent the data displayed in the UI
 */

/**
 * Represents a song in the UI
 */
data class SongItem(
    val id: String,
    val title: String,
    val artistName: String,
    val artistId: String? = null,
    val albumName: String? = null,
    val albumId: String? = null,
    val albumArtUrl: String? = null,
    val duration: Long = 0,
    val trackNumber: Int? = null,
    val year: Int? = null,
    val genre: String? = null,
    val isFavorite: Boolean = false,
    val playCount: Int = 0,
    val lastPlayed: Long? = null,
    val path: String? = null,
    val dateAdded: Long = 0
)

/**
 * Represents an album in the UI
 */
data class AlbumItem(
    val id: String,
    val title: String,
    val artistName: String,
    val artistId: String? = null,
    val albumArtUrl: String? = null,
    val year: Int? = null,
    val songCount: Int = 0,
    val duration: Long = 0,
    val isFavorite: Boolean = false,
    val genre: String? = null,
    val dateAdded: Long = 0
)

/**
 * Represents an artist in the UI
 */
data class ArtistItem(
    val id: String,
    val name: String,
    val albumCount: Int = 0,
    val songCount: Int = 0,
    val imageUrl: String? = null,
    val isFavorite: Boolean = false,
    val genre: String? = null
)

/**
 * Detailed artist information for UI consumption
 */
data class ArtistDetailModel(
    val id: String,
    val name: String,
    val imageUrl: String?,
    val albumCount: Int,
    val songCount: Int,
    val bio: String? = null
)

/**
 * Represents a playlist in the UI
 */
data class PlaylistItem(
    val id: String,
    val name: String,
    val description: String? = null,
    val songCount: Int = 0,
    val duration: Long = 0,
    val thumbnailUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Represents album detail with additional information
 */
data class AlbumDetailModel(
    val album: AlbumItem,
    val songs: List<SongItem> = emptyList()
)

/**
 * Helper extension to convert duration in milliseconds to formatted time
 */
fun Long.formatDuration(): String {
    val hours = this / 3600000
    val minutes = (this % 3600000) / 60000
    val seconds = (this % 60000) / 1000
    
    return when {
        hours > 0 -> String.format("%d:%02d:%02d", hours, minutes, seconds)
        else -> String.format("%d:%02d", minutes, seconds)
    }
}

/**
 * Helper to get thumbnail URL with fallback
 */
fun SongItem.getThumbnailUrl(): String? = com.shirou.shibamusic.util.ArtworkUrlHelper.forList(albumArtUrl)

fun SongItem.getPlayerArtworkUrl(): String? = com.shirou.shibamusic.util.ArtworkUrlHelper.forPlayer(albumArtUrl)

/**
 * Helper to get album art URL with fallback
 */
fun AlbumItem.getThumbnailUrl(): String? = com.shirou.shibamusic.util.ArtworkUrlHelper.forList(albumArtUrl)

fun AlbumItem.getPlayerArtworkUrl(): String? = com.shirou.shibamusic.util.ArtworkUrlHelper.forPlayer(albumArtUrl)

/**
 * Helper to get artist image URL with fallback
 */
fun ArtistItem.getThumbnailUrl(): String? = com.shirou.shibamusic.util.ArtworkUrlHelper.forList(imageUrl)
