package com.shirou.shibamusic.data.repository

import android.util.Log
import com.shirou.shibamusic.App
import com.shirou.shibamusic.data.database.dao.*
import com.shirou.shibamusic.data.database.entity.*
import com.shirou.shibamusic.data.database.mapper.*
import com.shirou.shibamusic.di.IoDispatcher
import com.shirou.shibamusic.subsonic.models.AlbumID3
import com.shirou.shibamusic.subsonic.models.AlbumWithSongsID3
import com.shirou.shibamusic.subsonic.models.Child
import com.shirou.shibamusic.ui.model.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of MusicRepository using Room database
 * 
 * This implementation provides persistent local storage for music data
 * with reactive updates using Flow.
 */
@Singleton
class MusicRepositoryImpl @Inject constructor(
    private val songDao: SongDao,
    private val albumDao: AlbumDao,
    private val artistDao: ArtistDao,
    private val playlistDao: PlaylistDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : MusicRepository {

    companion object {
        private const val TAG = "MusicRepository"
    }
    
    // ==================== Songs ====================
    
    override suspend fun getAllSongs(): List<SongItem> = withContext(ioDispatcher) {
        songDao.getAllSongs().toSongItems()
    }
    
    override suspend fun getSongById(id: String): SongItem? = withContext(ioDispatcher) {
        songDao.getSongById(id)?.toSongItem()
    }
    
    override suspend fun searchSongs(query: String): List<SongItem> = withContext(ioDispatcher) {
        songDao.searchSongs(query).toSongItems()
    }
    
    override fun observeSongs(): Flow<List<SongItem>> {
        return songDao.observeAllSongs().map { entities ->
            entities.toSongItems()
        }
    }
    
    // ==================== Albums ====================
    
    override suspend fun getAllAlbums(): List<AlbumItem> = withContext(ioDispatcher) {
        albumDao.getAllAlbums().toAlbumItems()
    }
    
    override suspend fun getAlbumById(id: String): AlbumItem? = withContext(ioDispatcher) {
        val local = albumDao.getAlbumById(id)?.toAlbumItem()
        if (local != null) {
            return@withContext local
        }

        val (remoteAlbum, _) = fetchAndCacheAlbum(id)
        remoteAlbum
    }
    
    override suspend fun getAlbumSongs(albumId: String): List<SongItem> = withContext(ioDispatcher) {
        val localSongs = songDao.getSongsByAlbumId(albumId)
        if (localSongs.isNotEmpty()) {
            return@withContext localSongs.toSongItems()
        }

        val (_, remoteSongs) = fetchAndCacheAlbum(albumId)
        remoteSongs
    }
    
    override suspend fun searchAlbums(query: String): List<AlbumItem> = withContext(ioDispatcher) {
        albumDao.searchAlbums(query).toAlbumItems()
    }
    
    override fun observeAlbums(): Flow<List<AlbumItem>> {
        return albumDao.observeAllAlbums().map { entities ->
            entities.toAlbumItems()
        }
    }

    private suspend fun fetchAndCachePlaylist(playlistId: String): List<SongItem> {
        return try {
            val response = App.getSubsonicClientInstance(false)
                .playlistClient
                .getPlaylist(playlistId)
                .execute()

            if (!response.isSuccessful) {
                Log.e(TAG, "Failed to fetch playlist $playlistId: ${response.code()} ${response.message()}")
                return emptyList()
            }

            val playlistResponse = response.body()?.subsonicResponse?.playlist
            if (playlistResponse == null) {
                Log.w(TAG, "Playlist $playlistId response body was null")
                return emptyList()
            }

            val songs = playlistResponse.entries.orEmpty()
            val songEntities = songs.mapNotNull { it.toSongEntityOrNull() }
            
            if (songEntities.isNotEmpty()) {
                songDao.insertSongs(songEntities)
                songEntities.forEachIndexed { index, songEntity ->
                    playlistDao.addSongToPlaylist(playlistId, songEntity.id, index)
                }
            }

            songEntities.toSongItems()
        } catch (exception: Exception) {
            Log.e(TAG, "Error fetching playlist $playlistId", exception)
            emptyList()
        }
    }

    private suspend fun fetchAndCacheAlbum(albumId: String): Pair<AlbumItem?, List<SongItem>> {
        return try {
            val response = App.getSubsonicClientInstance(false)
                .browsingClient
                .getAlbum(albumId)
                .execute()

            if (!response.isSuccessful) {
                Log.e(TAG, "Failed to fetch album $albumId: ${response.code()} ${response.message()}")
                return Pair(null, emptyList())
            }

            val albumResponse: AlbumWithSongsID3? = response.body()?.subsonicResponse?.album
            if (albumResponse == null) {
                Log.w(TAG, "Album $albumId response body was null")
                return Pair(null, emptyList())
            }

            val albumEntity = albumResponse.toAlbumEntity()
                ?: run {
                    Log.w(TAG, "Album $albumId missing required data")
                    return Pair(null, emptyList())
                }
            albumDao.insertAlbum(albumEntity)

            val songEntities = albumResponse.songs.orEmpty().mapNotNull { it.toSongEntityOrNull() }
            if (songEntities.isNotEmpty()) {
                songDao.insertSongs(songEntities)
                val totalDuration = songEntities.sumOf { it.durationMs }
                albumDao.updateAlbumStats(albumEntity.id, songEntities.size, totalDuration)
            }

            val albumItem = albumEntity.toAlbumItem()
            val songItems = if (songEntities.isNotEmpty()) {
                songEntities.toSongItems()
            } else {
                songDao.getSongsByAlbumId(albumId).toSongItems()
            }

            albumItem to songItems
        } catch (exception: Exception) {
            Log.e(TAG, "Error fetching album $albumId", exception)
            Pair(null, emptyList())
        }
    }
    
    // ==================== Artists ====================
    
    override suspend fun getAllArtists(): List<ArtistItem> = withContext(ioDispatcher) {
        artistDao.getAllArtists().toArtistItems()
    }
    
    override suspend fun getArtistById(id: String): ArtistItem? = withContext(ioDispatcher) {
        artistDao.getArtistById(id)?.toArtistItem()
    }
    
    override suspend fun getArtistSongs(artistId: String): List<SongItem> = withContext(ioDispatcher) {
        songDao.getSongsByArtistId(artistId).toSongItems()
    }
    
    override suspend fun getArtistAlbums(artistId: String): List<AlbumItem> = withContext(ioDispatcher) {
        albumDao.getAlbumsByArtistId(artistId).toAlbumItems()
    }
    
    override suspend fun searchArtists(query: String): List<ArtistItem> = withContext(ioDispatcher) {
        artistDao.searchArtists(query).toArtistItems()
    }
    
    override fun observeArtists(): Flow<List<ArtistItem>> {
        return artistDao.observeAllArtists().map { entities ->
            entities.toArtistItems()
        }
    }
    
    // ==================== Playlists ====================
    
    override suspend fun getAllPlaylists(): List<PlaylistItem> = withContext(ioDispatcher) {
        playlistDao.getAllPlaylists().toPlaylistItems()
    }
    
    override suspend fun getPlaylistById(id: String): PlaylistItem? = withContext(ioDispatcher) {
        playlistDao.getPlaylistById(id)?.toPlaylistItem()
    }
    
    override suspend fun getPlaylistSongs(playlistId: String): List<SongItem> = withContext(ioDispatcher) {
        val localSongs = playlistDao.getSongsInPlaylist(playlistId)
        if (localSongs.isNotEmpty()) {
            return@withContext localSongs.toSongItems()
        }

        fetchAndCachePlaylist(playlistId)
    }
    
    override suspend fun createPlaylist(name: String, description: String): PlaylistItem = withContext(ioDispatcher) {
        val playlist = PlaylistEntity(
            id = UUID.randomUUID().toString(),
            name = name,
            description = description,
            coverUrl = null,
            songCount = 0,
            durationMs = 0,
            dateCreated = System.currentTimeMillis(),
            dateModified = System.currentTimeMillis(),
            isFavorite = false
        )
        playlistDao.insertPlaylist(playlist)
        playlist.toPlaylistItem()
    }
    
    override suspend fun updatePlaylist(playlist: PlaylistItem) = withContext(ioDispatcher) {
        val entity = playlist.toPlaylistEntity()
        playlistDao.updatePlaylist(entity)
    }
    
    override suspend fun deletePlaylist(playlistId: String) = withContext(ioDispatcher) {
        playlistDao.deletePlaylistById(playlistId)
    }
    
    override suspend fun addSongToPlaylist(playlistId: String, songId: String) = withContext(ioDispatcher) {
        playlistDao.addSongToPlaylist(playlistId, songId)
    }
    
    override suspend fun removeSongFromPlaylist(playlistId: String, songId: String) = withContext(ioDispatcher) {
        playlistDao.removeSongFromPlaylistAndReorder(playlistId, songId)
    }
    
    override fun observePlaylists(): Flow<List<PlaylistItem>> {
        return playlistDao.observeAllPlaylists().map { entities ->
            entities.toPlaylistItems()
        }
    }
    
    override fun observePlaylistSongs(playlistId: String): Flow<List<SongItem>> {
        return playlistDao.observeSongsInPlaylist(playlistId).map { entities ->
            entities.toSongItems()
        }
    }
    
    // ==================== Favorites ====================
    
    override suspend fun getFavoriteSongs(): List<SongItem> = withContext(ioDispatcher) {
        songDao.getFavoriteSongs().toSongItems()
    }
    
    override suspend fun getFavoriteAlbums(): List<AlbumItem> = withContext(ioDispatcher) {
        albumDao.getFavoriteAlbums().toAlbumItems()
    }
    
    override suspend fun getFavoriteArtists(): List<ArtistItem> = withContext(ioDispatcher) {
        artistDao.getFavoriteArtists().toArtistItems()
    }
    
    override suspend fun toggleFavorite(songId: String): Unit = withContext(ioDispatcher) {
        val song = songDao.getSongById(songId)
        song?.let {
            songDao.updateFavoriteStatus(songId, !it.isFavorite)
        }
    }
    
    override suspend fun toggleSongFavorite(songId: String, isFavorite: Boolean) = withContext(ioDispatcher) {
        songDao.updateFavoriteStatus(songId, isFavorite)
    }
    
    override suspend fun toggleAlbumFavorite(albumId: String, isFavorite: Boolean) = withContext(ioDispatcher) {
        albumDao.updateFavoriteStatus(albumId, isFavorite)
    }
    
    override suspend fun toggleArtistFavorite(artistId: String, isFavorite: Boolean) = withContext(ioDispatcher) {
        artistDao.updateFavoriteStatus(artistId, isFavorite)
    }
    
    override suspend fun isFavorite(songId: String): Boolean = withContext(ioDispatcher) {
        songDao.getSongById(songId)?.isFavorite ?: false
    }
    
    override fun observeFavorites(): Flow<List<SongItem>> {
        return songDao.observeFavoriteSongs().map { entities ->
            entities.toSongItems()
        }
    }
    
    override fun observeFavoriteSongs(): Flow<List<SongItem>> {
        return songDao.observeFavoriteSongs().map { entities ->
            entities.toSongItems()
        }
    }
    
    override fun observeFavoriteAlbums(): Flow<List<AlbumItem>> {
        return albumDao.observeFavoriteAlbums().map { entities ->
            entities.toAlbumItems()
        }
    }
    
    override fun observeFavoriteArtists(): Flow<List<ArtistItem>> {
        return artistDao.observeFavoriteArtists().map { entities ->
            entities.toArtistItems()
        }
    }
    
    // ==================== Recently Played ====================
    
    override suspend fun addToRecent(songId: String): Unit = withContext(ioDispatcher) {
        songDao.incrementPlayCount(songId, System.currentTimeMillis())
    }
    
    override suspend fun getRecentlyPlayed(): List<SongItem> = withContext(ioDispatcher) {
        songDao.getRecentlyPlayedSongs(20).toSongItems()
    }
    
    override suspend fun getRecentlyPlayedSongs(limit: Int): List<SongItem> = withContext(ioDispatcher) {
        songDao.getRecentlyPlayedSongs(limit).toSongItems()
    }
    
    override suspend fun recordSongPlay(songId: String): Unit = withContext(ioDispatcher) {
        songDao.incrementPlayCount(songId)
        
        // Update album play count
        val song = songDao.getSongById(songId)
        song?.let {
            albumDao.incrementPlayCount(it.albumId)
        }
    }
    
    // ==================== Search ====================
    
    override suspend fun searchAll(query: String): SearchResults = withContext(ioDispatcher) {
        SearchResults(
            songs = songDao.searchSongs(query).toSongItems(),
            albums = albumDao.searchAlbums(query).toAlbumItems(),
            artists = artistDao.searchArtists(query).toArtistItems()
        )
    }
    
    // ==================== Data Management ====================
    
    /**
     * Insert sample data for testing
     * This should be called once when the app is first installed
     */
    suspend fun insertSampleData() = withContext(ioDispatcher) {
        // Check if data already exists
        val songCount = songDao.getSongCount()
        if (songCount > 0) return@withContext
        
        // Insert sample artists
        val artists = listOf(
            ArtistEntity("artist_1", "Queen", null, 3, 10, false),
            ArtistEntity("artist_2", "Led Zeppelin", null, 3, 8, false),
            ArtistEntity("artist_3", "The Beatles", null, 2, 6, false),
            ArtistEntity("artist_4", "Pink Floyd", null, 2, 5, false),
            ArtistEntity("artist_5", "The Eagles", null, 1, 4, false)
        )
        artistDao.insertArtists(artists)
        
        // Insert sample albums
        val albums = listOf(
            AlbumEntity("album_1", "A Night at the Opera", "artist_1", "Queen", null, 1975, "Rock", 4, 0, false, 0),
            AlbumEntity("album_2", "Led Zeppelin IV", "artist_2", "Led Zeppelin", null, 1971, "Rock", 3, 0, false, 0),
            AlbumEntity("album_3", "Abbey Road", "artist_3", "The Beatles", null, 1969, "Rock", 3, 0, false, 0),
            AlbumEntity("album_4", "The Dark Side of the Moon", "artist_4", "Pink Floyd", null, 1973, "Progressive Rock", 3, 0, false, 0),
            AlbumEntity("album_5", "Hotel California", "artist_5", "The Eagles", null, 1976, "Rock", 2, 0, false, 0)
        )
        albumDao.insertAlbums(albums)
        
        // Insert sample songs
        val songs = listOf(
            SongEntity("song_1", "Bohemian Rhapsody", "artist_1", "Queen", "album_1", "A Night at the Opera", null, 354000, 1, 1, 1975, "Rock", null, false, 0, null),
            SongEntity("song_2", "Love of My Life", "artist_1", "Queen", "album_1", "A Night at the Opera", null, 213000, 2, 1, 1975, "Rock", null, false, 0, null),
            SongEntity("song_3", "Stairway to Heaven", "artist_2", "Led Zeppelin", "album_2", "Led Zeppelin IV", null, 482000, 1, 1, 1971, "Rock", null, false, 0, null),
            SongEntity("song_4", "Black Dog", "artist_2", "Led Zeppelin", "album_2", "Led Zeppelin IV", null, 295000, 2, 1, 1971, "Rock", null, false, 0, null),
            SongEntity("song_5", "Come Together", "artist_3", "The Beatles", "album_3", "Abbey Road", null, 259000, 1, 1, 1969, "Rock", null, false, 0, null),
            SongEntity("song_6", "Something", "artist_3", "The Beatles", "album_3", "Abbey Road", null, 182000, 2, 1, 1969, "Rock", null, false, 0, null),
            SongEntity("song_7", "Time", "artist_4", "Pink Floyd", "album_4", "The Dark Side of the Moon", null, 413000, 1, 1, 1973, "Progressive Rock", null, false, 0, null),
            SongEntity("song_8", "Money", "artist_4", "Pink Floyd", "album_4", "The Dark Side of the Moon", null, 382000, 2, 1, 1973, "Progressive Rock", null, false, 0, null),
            SongEntity("song_9", "Hotel California", "artist_5", "The Eagles", "album_5", "Hotel California", null, 391000, 1, 1, 1976, "Rock", null, false, 0, null),
            SongEntity("song_10", "New Kid in Town", "artist_5", "The Eagles", "album_5", "Hotel California", null, 305000, 2, 1, 1976, "Rock", null, false, 0, null)
        )
        songDao.insertSongs(songs)
    }
    
    /**
     * Clear all data from the database
     */
    override suspend fun clearAllData() = withContext(ioDispatcher) {
        songDao.deleteAllSongs()
        albumDao.deleteAllAlbums()
        artistDao.deleteAllArtists()
        playlistDao.deleteAllPlaylists()
    }
}

private fun AlbumID3.toAlbumEntity(): AlbumEntity? {
    val albumId = id ?: return null
    return AlbumEntity(
        id = albumId,
        title = name.orEmpty(),
        artistId = artistId.orEmpty(),
        artistName = artist.orEmpty(),
        albumArtUrl = coverArtId,
        year = year.takeIf { it != 0 },
        genre = genre,
        songCount = songCount ?: 0,
        durationMs = (duration ?: 0).toLong() * 1000L,
        isFavorite = starred != null,
        playCount = playCount?.toInt() ?: 0,
        dateAdded = created?.time ?: System.currentTimeMillis()
    )
}

private fun Child.toSongEntityOrNull(): SongEntity? {
    val albumId = albumId ?: return null
    return SongEntity(
        id = id,
        title = title.orEmpty(),
        artistId = artistId.orEmpty(),
        artistName = artist.orEmpty(),
        albumId = albumId,
        albumName = album.orEmpty(),
        albumArtUrl = coverArtId,
        durationMs = (duration ?: 0).toLong() * 1000L,
        trackNumber = track,
        discNumber = discNumber,
        year = year,
        genre = genre,
        path = path,
        isFavorite = starred != null,
        playCount = playCount?.toInt() ?: 0,
        lastPlayedTimestamp = created?.time,
        dateAdded = created?.time ?: System.currentTimeMillis(),
        dateModified = System.currentTimeMillis()
    )
}
