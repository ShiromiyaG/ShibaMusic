package com.shirou.shibamusic.data.database.mapper

import com.shirou.shibamusic.data.database.entity.AlbumEntity
import com.shirou.shibamusic.data.database.entity.ArtistEntity
import com.shirou.shibamusic.data.database.entity.PlaylistEntity
import com.shirou.shibamusic.data.database.entity.SongEntity
import com.shirou.shibamusic.ui.model.AlbumItem
import com.shirou.shibamusic.ui.model.ArtistItem
import com.shirou.shibamusic.ui.model.PlaylistItem
import com.shirou.shibamusic.ui.model.SongItem

// ===== Song Mappers =====

fun SongEntity.toSongItem(): SongItem = SongItem(
    id = id,
    title = title,
    artistName = artistName,
    artistId = artistId,
    albumName = albumName,
    albumId = albumId,
    albumArtUrl = resolveCoverArt(albumArtUrl),
    duration = durationMs,
    trackNumber = trackNumber,
    year = year,
    genre = genre,
    isFavorite = isFavorite,
    playCount = playCount,
    lastPlayed = lastPlayedTimestamp,
    path = path,
    dateAdded = dateAdded
)

fun List<SongEntity>.toSongItems(): List<SongItem> = map { it.toSongItem() }

// ===== Album Mappers =====

fun AlbumEntity.toAlbumItem(): AlbumItem = AlbumItem(
    id = id,
    title = title,
    artistName = artistName,
    artistId = artistId,
    albumArtUrl = resolveCoverArt(albumArtUrl),
    year = year,
    songCount = songCount,
    duration = durationMs,
    isFavorite = isFavorite,
    genre = genre,
    dateAdded = dateAdded
)

fun List<AlbumEntity>.toAlbumItems(): List<AlbumItem> = map { it.toAlbumItem() }

// ===== Artist Mappers =====

fun ArtistEntity.toArtistItem(): ArtistItem = ArtistItem(
    id = id,
    name = name,
    albumCount = albumCount,
    songCount = songCount,
    imageUrl = resolveCoverArt(imageUrl),
    isFavorite = isFavorite,
    genre = genre
)

private fun resolveCoverArt(rawValue: String?): String? {
    if (rawValue.isNullOrBlank()) {
        return null
    }

    val trimmed = rawValue.trim()
    if (trimmed.startsWith("http", ignoreCase = true) || trimmed.startsWith("file:", ignoreCase = true)) {
        return trimmed
    }

    return com.shirou.shibamusic.glide.CustomGlideRequest.createUrl(
        trimmed,
        com.shirou.shibamusic.util.Preferences.getImageSize()
    ) ?: trimmed
}

fun List<ArtistEntity>.toArtistItems(): List<ArtistItem> = map { it.toArtistItem() }

// ===== Playlist Mappers =====

fun PlaylistEntity.toPlaylistItem(): PlaylistItem = PlaylistItem(
    id = id,
    name = name,
    description = description,
    songCount = songCount,
    duration = durationMs,
    thumbnailUrl = coverUrl,
    createdAt = dateCreated,
    updatedAt = dateModified
)

fun List<PlaylistEntity>.toPlaylistItems(): List<PlaylistItem> = map { it.toPlaylistItem() }

fun PlaylistItem.toPlaylistEntity(): PlaylistEntity = PlaylistEntity(
    id = id,
    name = name,
    description = description,
    coverUrl = thumbnailUrl,
    songCount = songCount,
    durationMs = duration,
    dateCreated = createdAt,
    dateModified = updatedAt
)
