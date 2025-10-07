package com.shirou.shibamusic.ui.model

/**
 * Data class representing search results across all music types
 */
data class SearchResults(
    val songs: List<SongItem> = emptyList(),
    val albums: List<AlbumItem> = emptyList(),
    val artists: List<ArtistItem> = emptyList()
) {
    /**
     * Check if search returned any results
     */
    fun isEmpty(): Boolean = songs.isEmpty() && albums.isEmpty() && artists.isEmpty()
    
    /**
     * Check if search returned results
     */
    fun isNotEmpty(): Boolean = !isEmpty()
    
    /**
     * Get total count of all results
     */
    fun totalCount(): Int = songs.size + albums.size + artists.size
}
