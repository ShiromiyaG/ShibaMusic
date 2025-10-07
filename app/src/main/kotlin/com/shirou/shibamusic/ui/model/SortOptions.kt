package com.shirou.shibamusic.ui.model

/**
 * Sort options for different content types
 */

/**
 * Sort options for songs
 */
enum class SongSortOption(val displayName: String) {
    TITLE_ASC("Title (A-Z)"),
    TITLE_DESC("Title (Z-A)"),
    ARTIST_ASC("Artist (A-Z)"),
    ARTIST_DESC("Artist (Z-A)"),
    ALBUM_ASC("Album (A-Z)"),
    ALBUM_DESC("Album (Z-A)"),
    DURATION_ASC("Duration (Short-Long)"),
    DURATION_DESC("Duration (Long-Short)"),
    DATE_ADDED_DESC("Recently Added"),
    DATE_ADDED_ASC("Oldest First"),
    PLAY_COUNT_DESC("Most Played"),
    PLAY_COUNT_ASC("Least Played")
}

/**
 * Sort options for albums
 */
enum class AlbumSortOption(val displayName: String) {
    NAME_ASC("Title (A-Z)"),
    NAME_DESC("Title (Z-A)"),
    TITLE_ASC("Title (A-Z)"),  // Alias for NAME_ASC
    TITLE_DESC("Title (Z-A)"),  // Alias for NAME_DESC
    ARTIST_ASC("Artist (A-Z)"),
    ARTIST_DESC("Artist (Z-A)"),
    YEAR_DESC("Year (New-Old)"),
    YEAR_ASC("Year (Old-New)"),
    DATE_DESC("Year (New-Old)"),  // Alias for YEAR_DESC
    DATE_ASC("Year (Old-New)"),   // Alias for YEAR_ASC
    SONG_COUNT_DESC("Most Songs"),
    SONG_COUNT_ASC("Fewest Songs"),
    DATE_ADDED_DESC("Recently Added"),
    DATE_ADDED_ASC("Oldest First")
}

/**
 * Sort options for artists
 */
enum class ArtistSortOption(val displayName: String) {
    NAME_ASC("Name (A-Z)"),
    NAME_DESC("Name (Z-A)"),
    ALBUM_COUNT_DESC("Most Albums"),
    ALBUM_COUNT_ASC("Fewest Albums"),
    SONG_COUNT_DESC("Most Songs"),
    SONG_COUNT_ASC("Fewest Songs"),
    DATE_DESC("Date (Newest First)"),
    DATE_ASC("Date (Oldest First)")
}

/**
 * Sort options for playlists
 */
enum class PlaylistSortOption(val displayName: String) {
    NAME_ASC("Name (A-Z)"),
    NAME_DESC("Name (Z-A)"),
    DATE_CREATED_DESC("Recently Created"),
    DATE_CREATED_ASC("Oldest First"),
    DATE_UPDATED_DESC("Recently Updated"),
    DATE_UPDATED_ASC("Least Recently Updated"),
    SONG_COUNT_DESC("Most Songs"),
    SONG_COUNT_ASC("Fewest Songs")
}
