package com.shirou.shibamusic.data.repository

import androidx.paging.PagingData
import com.shirou.shibamusic.ui.model.*
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for music data
 * Provides abstraction layer between data sources and ViewModels
 */
interface MusicRepository {
    
    // Songs
    suspend fun getAllSongs(): List<SongItem>
    suspend fun getSongById(id: String): SongItem?
    suspend fun searchSongs(query: String): List<SongItem>
    fun observeSongs(): Flow<List<SongItem>>
    fun observeSongsPaged(orderClause: String): Flow<PagingData<SongItem>>
    
    // Albums
    suspend fun getAllAlbums(): List<AlbumItem>
    suspend fun getAlbumById(id: String): AlbumItem?
    suspend fun getAlbumSongs(albumId: String): List<SongItem>
    suspend fun searchAlbums(query: String): List<AlbumItem>
    fun observeAlbums(): Flow<List<AlbumItem>>
    fun observeAlbumsPaged(orderClause: String): Flow<PagingData<AlbumItem>>
    
    // Artists
    suspend fun getAllArtists(): List<ArtistItem>
    suspend fun getArtistById(id: String): ArtistItem?
    suspend fun getArtistSongs(artistId: String): List<SongItem>
    suspend fun getArtistAlbums(artistId: String): List<AlbumItem>
    suspend fun searchArtists(query: String): List<ArtistItem>
    fun observeArtists(): Flow<List<ArtistItem>>
    fun observeArtistsPaged(orderClause: String): Flow<PagingData<ArtistItem>>
    
    // Playlists
    suspend fun getAllPlaylists(): List<PlaylistItem>
    suspend fun getPlaylistById(id: String): PlaylistItem?
    suspend fun getPlaylistSongs(playlistId: String): List<SongItem>
    suspend fun createPlaylist(name: String, description: String): PlaylistItem
    suspend fun updatePlaylist(playlist: PlaylistItem)
    suspend fun deletePlaylist(playlistId: String)
    suspend fun addSongToPlaylist(playlistId: String, songId: String)
    suspend fun removeSongFromPlaylist(playlistId: String, songId: String)
    fun observePlaylists(): Flow<List<PlaylistItem>>
    fun observePlaylistSongs(playlistId: String): Flow<List<SongItem>>
    fun observePlaylistsPaged(orderClause: String): Flow<PagingData<PlaylistItem>>
    
    // Favorites
    suspend fun getFavoriteSongs(): List<SongItem>
    suspend fun getFavoriteAlbums(): List<AlbumItem>
    suspend fun getFavoriteArtists(): List<ArtistItem>
    suspend fun toggleFavorite(songId: String)
    suspend fun toggleSongFavorite(songId: String, isFavorite: Boolean)
    suspend fun toggleAlbumFavorite(albumId: String, isFavorite: Boolean)
    suspend fun toggleArtistFavorite(artistId: String, isFavorite: Boolean)
    suspend fun isFavorite(songId: String): Boolean
    fun observeFavorites(): Flow<List<SongItem>>
    fun observeFavoriteSongs(): Flow<List<SongItem>>
    fun observeFavoriteAlbums(): Flow<List<AlbumItem>>
    fun observeFavoriteArtists(): Flow<List<ArtistItem>>
    
    // Recent
    suspend fun getRecentlyPlayed(): List<SongItem>
    suspend fun getRecentlyPlayedSongs(limit: Int = 20): List<SongItem>
    suspend fun addToRecent(songId: String)
    suspend fun recordSongPlay(songId: String)
    
    // Search
    suspend fun searchAll(query: String): SearchResults
    
    // Data Management
    suspend fun clearAllData()
}
