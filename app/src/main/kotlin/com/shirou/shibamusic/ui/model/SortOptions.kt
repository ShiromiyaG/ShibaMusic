package com.shirou.shibamusic.ui.model

import com.shirou.shibamusic.R

/**
 * Sort options for different content types
 */

/**
 * Sort options for songs
 */
enum class SongSortOption(val stringResId: Int) {
    TITLE_ASC(R.string.sort_title_asc),
    TITLE_DESC(R.string.sort_title_desc),
    ARTIST_ASC(R.string.sort_artist_asc),
    ARTIST_DESC(R.string.sort_artist_desc),
    ALBUM_ASC(R.string.sort_album_asc),
    ALBUM_DESC(R.string.sort_album_desc),
    DURATION_ASC(R.string.sort_duration_asc),
    DURATION_DESC(R.string.sort_duration_desc),
    DATE_ADDED_DESC(R.string.sort_recently_added),
    DATE_ADDED_ASC(R.string.sort_oldest_first),
    PLAY_COUNT_DESC(R.string.sort_most_played),
    PLAY_COUNT_ASC(R.string.sort_least_played)
}

/**
 * Sort options for albums
 */
enum class AlbumSortOption(val stringResId: Int) {
    NAME_ASC(R.string.sort_title_asc),
    NAME_DESC(R.string.sort_title_desc),
    TITLE_ASC(R.string.sort_title_asc),  // Alias for NAME_ASC
    TITLE_DESC(R.string.sort_title_desc),  // Alias for NAME_DESC
    ARTIST_ASC(R.string.sort_artist_asc),
    ARTIST_DESC(R.string.sort_artist_desc),
    YEAR_DESC(R.string.sort_year_desc),
    YEAR_ASC(R.string.sort_year_asc),
    DATE_DESC(R.string.sort_year_desc),  // Alias for YEAR_DESC
    DATE_ASC(R.string.sort_year_asc),   // Alias for YEAR_ASC
    SONG_COUNT_DESC(R.string.sort_most_songs),
    SONG_COUNT_ASC(R.string.sort_fewest_songs),
    DATE_ADDED_DESC(R.string.sort_recently_added),
    DATE_ADDED_ASC(R.string.sort_oldest_first)
}

/**
 * Sort options for artists
 */
enum class ArtistSortOption(val stringResId: Int) {
    NAME_ASC(R.string.sort_name_asc),
    NAME_DESC(R.string.sort_name_desc),
    ALBUM_COUNT_DESC(R.string.sort_most_albums),
    ALBUM_COUNT_ASC(R.string.sort_fewest_albums),
    SONG_COUNT_DESC(R.string.sort_most_songs),
    SONG_COUNT_ASC(R.string.sort_fewest_songs),
    DATE_DESC(R.string.sort_date_newest),
    DATE_ASC(R.string.sort_date_oldest)
}

/**
 * Sort options for playlists
 */
enum class PlaylistSortOption(val stringResId: Int) {
    NAME_ASC(R.string.sort_name_asc),
    NAME_DESC(R.string.sort_name_desc),
    DATE_CREATED_DESC(R.string.sort_recently_created),
    DATE_CREATED_ASC(R.string.sort_oldest_first),
    DATE_UPDATED_DESC(R.string.sort_recently_updated),
    DATE_UPDATED_ASC(R.string.sort_least_recently_updated),
    SONG_COUNT_DESC(R.string.sort_most_songs),
    SONG_COUNT_ASC(R.string.sort_fewest_songs)
}
