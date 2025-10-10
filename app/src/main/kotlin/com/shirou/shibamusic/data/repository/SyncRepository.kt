package com.shirou.shibamusic.data.repository

import android.util.Log
import com.shirou.shibamusic.App
import com.shirou.shibamusic.data.database.dao.AlbumDao
import com.shirou.shibamusic.data.database.dao.ArtistDao
import com.shirou.shibamusic.data.database.dao.PlaylistDao
import com.shirou.shibamusic.data.database.dao.SongDao
import com.shirou.shibamusic.data.database.entity.AlbumEntity
import com.shirou.shibamusic.data.database.entity.ArtistEntity
import com.shirou.shibamusic.data.database.entity.PlaylistEntity
import com.shirou.shibamusic.data.database.entity.SongEntity
import com.shirou.shibamusic.di.IoDispatcher
import com.shirou.shibamusic.subsonic.Subsonic
import com.shirou.shibamusic.subsonic.base.ApiResponse
import com.shirou.shibamusic.subsonic.models.AlbumID3
import com.shirou.shibamusic.subsonic.models.AlbumWithSongsID3
import com.shirou.shibamusic.subsonic.models.ArtistID3
import com.shirou.shibamusic.subsonic.models.ArtistWithAlbumsID3
import com.shirou.shibamusic.subsonic.models.ArtistsID3
import com.shirou.shibamusic.subsonic.models.Child
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import java.util.concurrent.ConcurrentHashMap

/**
 * Repository for syncing data from Navidrome server to local database
 * Rewritten to properly handle Retrofit callbacks with coroutines
 */
@Singleton
class SyncRepository @Inject constructor(
    private val albumDao: AlbumDao,
    private val artistDao: ArtistDao,
    private val playlistDao: PlaylistDao,
    private val songDao: SongDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    companion object {
        private const val TAG = "ShibaMusicSync"
    }
    
    init {
        Log.d(TAG, "SyncRepository initialized")
    }

    private val artistDetailCache = ConcurrentHashMap<String, CachedArtistDetail>()
    private val albumSongCache = ConcurrentHashMap<String, CachedAlbumSongs>()
    
    /**
     * Extension function to convert AlbumID3 to AlbumEntity
     */
    private fun AlbumID3.toEntity(): AlbumEntity {
        return AlbumEntity(
            id = this.id ?: "",
            title = this.name ?: "",
            artistId = this.artistId ?: "",
            artistName = this.artist ?: "",
            albumArtUrl = this.coverArtId,
            year = this.year,
            genre = this.genre,
            songCount = this.songCount ?: 0,
            durationMs = (this.duration ?: 0).toLong() * 1000,
            isFavorite = this.starred != null,
            playCount = this.playCount?.toInt() ?: 0,
            dateAdded = this.created?.time ?: System.currentTimeMillis()
        )
    }
    
    /**
     * Extension function to convert Child to SongEntity
     */
    private fun Child.toEntityOrNull(): SongEntity? {
        return try {
            SongEntity(
                id = this.id,
                title = this.title ?: "",
                artistId = this.artistId ?: "",
                artistName = this.artist ?: "",
                albumId = this.albumId ?: "",
                albumName = this.album ?: "",
                albumArtUrl = this.coverArtId,
                durationMs = (this.duration ?: 0).toLong() * 1000,
                trackNumber = this.track,
                discNumber = this.discNumber,
                year = this.year,
                genre = this.genre,
                path = this.path,
                isFavorite = this.starred != null,
                playCount = this.playCount?.toInt() ?: 0,
                lastPlayedTimestamp = null,
                dateAdded = this.created?.time ?: System.currentTimeMillis()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error converting Child to SongEntity", e)
            null
        }
    }

    private fun ArtistID3.toEntity(
        albumCountOverride: Int? = null,
        songCount: Int = 0
    ): ArtistEntity {
        return ArtistEntity(
            id = this.id ?: "",
            name = this.name ?: "",
            imageUrl = this.coverArtId,
            albumCount = albumCountOverride ?: this.albumCount,
            songCount = songCount,
            isFavorite = this.starred != null
        )
    }

    private fun ArtistWithAlbumsID3.toEntity(
        albumCountOverride: Int? = null,
        songCount: Int = 0
    ): ArtistEntity {
        val resolvedAlbumCount = albumCountOverride ?: this.albums?.size ?: this.albumCount
        return ArtistEntity(
            id = this.id ?: "",
            name = this.name ?: "",
            imageUrl = this.coverArtId,
            albumCount = resolvedAlbumCount,
            songCount = songCount,
            isFavorite = this.starred != null
        )
    }
    
    suspend fun syncAllAlbumsPaged(
        type: String = "alphabeticalByName",
        pageSize: Int = DEFAULT_PAGE_SIZE,
        maxPages: Int = DEFAULT_MAX_PAGES,
        throttleMs: Long = DEFAULT_THROTTLE_MS
    ): AlbumSyncReport = withContext(ioDispatcher) {
        var fetchedCount = 0
        var insertedCount = 0
        var currentPage = 0

        while (currentPage < maxPages) {
            val offset = currentPage * pageSize
            
            Log.d(TAG, "Fetching albums page=$currentPage offset=$offset pageSize=$pageSize")

            val response = try {
                App.getSubsonicClientInstance(false)
                    .albumSongListClient
                    .getAlbumList2(type, pageSize, offset, null, null)
                    .execute()
            } catch (e: Exception) {
                Log.e(TAG, "Exception fetching page $currentPage", e)
                break
            }

            if (!response.isSuccessful) {
                Log.e(TAG, "Failed to fetch albums page=$currentPage code=${response.code()}")
                break
            }

            val albums = response.body()
                ?.subsonicResponse
                ?.albumList2
                ?.albums
                .orEmpty()
            
            Log.d(TAG, "Page $currentPage returned ${albums.size} albums")

            if (albums.isEmpty()) {
                Log.d(TAG, "No more albums, stopping at page $currentPage")
                break
            }

            val entities = albums.mapNotNull { album ->
                try {
                    album.toEntity()
                } catch (error: Exception) {
                    Log.e(TAG, "Error converting album ${album.id}", error)
                    null
                }
            }

            if (entities.isNotEmpty()) {
                albumDao.insertAlbums(entities)
                insertedCount += entities.size
            }

            fetchedCount += albums.size
            currentPage += 1

            if (throttleMs > 0) {
                delay(throttleMs)
            }
        }
        
        Log.d(TAG, "Album sync complete: fetched=$fetchedCount inserted=$insertedCount pages=$currentPage")

        AlbumSyncReport(
            fetched = fetchedCount,
            inserted = insertedCount,
            pages = currentPage
        )
    }

    suspend fun syncArtistsAndAlbumsDeep(
        artistLimit: Int? = null,
        albumLimitPerArtist: Int? = null,
        syncSongs: Boolean = true,
        throttleMs: Long = DEFAULT_THROTTLE_MS
    ): ArtistAlbumSyncReport = withContext(ioDispatcher) {
        val client = App.getSubsonicClientInstance(false)

        val artistsResponse = client
            .browsingClient
            .getArtists()
            .execute()

        if (!artistsResponse.isSuccessful) {
            throw IOException(
                "Failed to fetch artists code=${artistsResponse.code()} message=${artistsResponse.message()}"
            )
        }

        val allArtists = artistsResponse.body()
            ?.subsonicResponse
            ?.artists
            ?.indices
            .orEmpty()
            .flatMap { index -> index.artists.orEmpty() }
            .filter { !it.id.isNullOrBlank() }

        val selectedArtists = when {
            artistLimit == null || artistLimit <= 0 -> allArtists
            artistLimit >= allArtists.size -> allArtists
            else -> allArtists.take(artistLimit)
        }

        if (selectedArtists.isEmpty()) {
            return@withContext ArtistAlbumSyncReport(artists = 0, albums = 0, songs = 0)
        }

        artistDao.insertArtists(selectedArtists.map { artist -> artist.toEntity() })

        val semaphore = Semaphore(permits = ARTIST_SYNC_CONCURRENCY)

        val reports = coroutineScope {
            selectedArtists.map { artist ->
                async {
                    semaphore.withPermit {
                        syncArtistDeep(
                            client = client,
                            artist = artist,
                            albumLimitPerArtist = albumLimitPerArtist,
                            syncSongs = syncSongs,
                            throttleMs = throttleMs
                        )
                    }
                }
            }.awaitAll()
        }

        reports.fold(
            ArtistAlbumSyncReport(
                artists = 0,
                albums = 0,
                songs = 0
            )
        ) { acc, report ->
            ArtistAlbumSyncReport(
                artists = acc.artists + report.artists,
                albums = acc.albums + report.albums,
                songs = acc.songs + report.songs
            )
        }
    }

    private suspend fun syncArtistDeep(
        client: Subsonic,
        artist: ArtistID3,
        albumLimitPerArtist: Int?,
        syncSongs: Boolean,
        throttleMs: Long
    ): ArtistAlbumSyncReport {
        val artistId = artist.id ?: return ArtistAlbumSyncReport(artists = 0, albums = 0, songs = 0)

        val now = System.currentTimeMillis()
        val cachedArtist = artistDetailCache[artistId]
        val artistDetail: ArtistWithAlbumsID3? =
            if (cachedArtist != null && cachedArtist.isFresh(now)) {
                cachedArtist.detail
            } else {
                throttleForNetwork(throttleMs, ARTIST_SYNC_CONCURRENCY)

                val artistDetailResponse = try {
                    client.browsingClient.getArtist(artistId).execute()
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (error: Exception) {
                    Log.e(TAG, "Error fetching artist $artistId", error)
                    return ArtistAlbumSyncReport(artists = 0, albums = 0, songs = 0)
                }

                if (!artistDetailResponse.isSuccessful) {
                    Log.e(
                        TAG,
                        "Failed to fetch artist $artistId: ${artistDetailResponse.code()} ${artistDetailResponse.message()}"
                    )
                    return ArtistAlbumSyncReport(artists = 0, albums = 0, songs = 0)
                }

                val detail = artistDetailResponse.body()?.subsonicResponse?.artist
                artistDetailCache[artistId] = CachedArtistDetail(detail, now)
                pruneArtistCache()
                detail
            }
        val albums = artistDetail?.albums.orEmpty().filter { !it.id.isNullOrBlank() }

        val limitedAlbums = when {
            albumLimitPerArtist == null || albumLimitPerArtist <= 0 -> albums
            albumLimitPerArtist >= albums.size -> albums
            else -> albums.take(albumLimitPerArtist)
        }

        val albumEntities = limitedAlbums.mapNotNull { album ->
            try {
                album.toEntity()
            } catch (error: Exception) {
                Log.e(TAG, "Error converting album ${album.id}", error)
                null
            }
        }

        if (albumEntities.isNotEmpty()) {
            albumDao.insertAlbums(albumEntities)
        }

        val albumResults = if (syncSongs && limitedAlbums.isNotEmpty()) {
            val albumSemaphore = Semaphore(permits = ALBUM_SYNC_CONCURRENCY)
            coroutineScope {
                limitedAlbums.mapNotNull { album ->
                    val albumId = album.id ?: return@mapNotNull null
                    async {
                        albumSemaphore.withPermit {
                            fetchAlbumSongs(client, albumId, throttleMs)
                        }
                    }
                }.awaitAll()
            }
        } else {
            emptyList()
        }

        val totalSongsForArtist = albumResults.sumOf { it.songCount }

        artistDetail?.let { detail ->
            artistDao.insertArtist(
                detail.toEntity(
                    albumCountOverride = limitedAlbums.size,
                    songCount = totalSongsForArtist
                )
            )
        }

        return ArtistAlbumSyncReport(
            artists = 1,
            albums = albumEntities.size,
            songs = totalSongsForArtist
        )
    }

    private suspend fun fetchAlbumSongs(
        client: Subsonic,
        albumId: String,
        throttleMs: Long
    ): AlbumSongSyncResult {
        val now = System.currentTimeMillis()
        val cached = albumSongCache[albumId]
        if (cached != null && cached.isFresh(now)) {
            return cached.result
        }

        throttleForNetwork(throttleMs, ALBUM_SYNC_CONCURRENCY)

        val albumResponse = try {
            client.browsingClient.getAlbum(albumId).execute()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            Log.e(TAG, "Error fetching album $albumId", error)
            return AlbumSongSyncResult(albumId = albumId, songCount = 0, totalDuration = 0)
        }

        if (!albumResponse.isSuccessful) {
            Log.e(
                TAG,
                "Failed to fetch album $albumId: ${albumResponse.code()} ${albumResponse.message()}"
            )
            return AlbumSongSyncResult(albumId = albumId, songCount = 0, totalDuration = 0)
        }

        val albumWithSongs: AlbumWithSongsID3? = albumResponse.body()?.subsonicResponse?.album
        val songEntities = albumWithSongs?.songs.orEmpty().mapNotNull { it.toEntityOrNull() }

        if (songEntities.isEmpty()) {
            return AlbumSongSyncResult(albumId = albumId, songCount = 0, totalDuration = 0)
        }

        songDao.insertSongs(songEntities)
        val totalDuration = songEntities.sumOf { it.durationMs }
        albumDao.updateAlbumStats(albumId, songEntities.size, totalDuration)

        val result = AlbumSongSyncResult(
            albumId = albumId,
            songCount = songEntities.size,
            totalDuration = totalDuration
        )
        albumSongCache[albumId] = CachedAlbumSongs(result, System.currentTimeMillis())
        pruneAlbumSongCache()
        return result
    }

    private suspend fun throttleForNetwork(throttleMs: Long, concurrency: Int) {
        if (throttleMs <= 0) return
        val adjustedDelay = throttleMs / concurrency.coerceAtLeast(1)
        if (adjustedDelay > 0) {
            delay(adjustedDelay)
        }
    }

    private fun pruneArtistCache(now: Long = System.currentTimeMillis()) {
        if (artistDetailCache.size <= ARTIST_CACHE_MAX) return
        val iterator = artistDetailCache.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (!entry.value.isFresh(now)) {
                iterator.remove()
            }
        }
        if (artistDetailCache.size > ARTIST_CACHE_MAX) {
            val surplus = artistDetailCache.entries
                .sortedBy { it.value.timestamp }
                .take(artistDetailCache.size - ARTIST_CACHE_MAX)
            surplus.forEach { artistDetailCache.remove(it.key) }
        }
    }

    private fun pruneAlbumSongCache(now: Long = System.currentTimeMillis()) {
        if (albumSongCache.size <= ALBUM_SONG_CACHE_MAX) return
        val iterator = albumSongCache.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (!entry.value.isFresh(now)) {
                iterator.remove()
            }
        }
        if (albumSongCache.size > ALBUM_SONG_CACHE_MAX) {
            val surplus = albumSongCache.entries
                .sortedBy { it.value.timestamp }
                .take(albumSongCache.size - ALBUM_SONG_CACHE_MAX)
            surplus.forEach { albumSongCache.remove(it.key) }
        }
    }

    /**
     * Sync albums from server directly using Retrofit
     */
    private suspend fun syncAlbums(type: String, size: Int): Result<List<AlbumEntity>> = suspendCoroutine { continuation ->
        try {
            Log.d(TAG, ">>> Syncing albums from server (type: $type, size: $size)")
            
            App.getSubsonicClientInstance(false)
                .albumSongListClient
                .getAlbumList2(type, size, 0, null, null)
                .enqueue(object : Callback<ApiResponse> {
                    override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                        try {
                            if (response.isSuccessful && 
                                response.body()?.subsonicResponse?.albumList2?.albums != null) {
                                
                                val albums = response.body()!!.subsonicResponse.albumList2!!.albums!!
                                
                                // Convert to entities
                                val albumEntities = albums.mapNotNull { 
                                    try { it.toEntity() } catch (e: Exception) { 
                                        Log.e(TAG, "Error converting album", e)
                                        null 
                                    }
                                }
                                
                                Log.d(TAG, "Converted ${albumEntities.size} albums")
                                continuation.resume(Result.success(albumEntities))
                            } else {
                                Log.e(TAG, "Failed to sync albums: empty response")
                                continuation.resume(Result.success(emptyList()))
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error processing album response", e)
                            continuation.resumeWithException(e)
                        }
                    }

                    override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                        Log.e(TAG, "Error syncing albums", t)
                        continuation.resumeWithException(t)
                    }
                })
        } catch (e: Exception) {
            Log.e(TAG, "Error initiating album sync", e)
            continuation.resumeWithException(e)
        }
    }
    
    /**
     * Sync playlists from server
     */
    private suspend fun syncPlaylists(): Result<List<PlaylistEntity>> = suspendCoroutine { continuation ->
        try {
            Log.d(TAG, "Syncing playlists from server")
            
            App.getSubsonicClientInstance(false)
                .playlistClient
                .getPlaylists()
                .enqueue(object : Callback<ApiResponse> {
                    override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                        try {
                            if (response.isSuccessful &&
                                response.body()?.subsonicResponse?.playlists?.playlists != null) {
                                
                                val playlists = response.body()!!.subsonicResponse.playlists!!.playlists!!

                                val playlistEntities = playlists.mapNotNull { playlist ->
                                    val playlistId = playlist.id
                                    if (playlistId.isNullOrEmpty()) {
                                        Log.w(TAG, "Skipping playlist with missing id")
                                        return@mapNotNull null
                                    }

                                    try {
                                        val name = playlist.name ?: playlistId
                                        val durationSeconds = playlist.duration.coerceAtLeast(0L)
                                        PlaylistEntity(
                                            id = playlistId,
                                            name = name,
                                            description = playlist.comment,
                                            coverUrl = playlist.coverArtId,
                                            songCount = playlist.songCount.coerceAtLeast(0),
                                            durationMs = durationSeconds * 1000,
                                            dateCreated = playlist.created?.time ?: System.currentTimeMillis(),
                                            dateModified = playlist.changed?.time ?: System.currentTimeMillis(),
                                            isFavorite = false
                                        )
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Error converting playlist $playlistId", e)
                                        null
                                    }
                                }
                                
                                Log.d(TAG, "Converted ${playlistEntities.size} playlists")
                                continuation.resume(Result.success(playlistEntities))
                            } else {
                                Log.e(TAG, "Failed to sync playlists: empty response")
                                continuation.resume(Result.success(emptyList()))
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error processing playlist response", e)
                            continuation.resumeWithException(e)
                        }
                    }

                    override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                        Log.e(TAG, "Error syncing playlists", t)
                        continuation.resumeWithException(t)
                    }
                })
        } catch (e: Exception) {
            Log.e(TAG, "Error initiating playlist sync", e)
            continuation.resumeWithException(e)
        }
    }
    
    /**
     * Sync random songs from server directly using Retrofit
     */
    private suspend fun syncRandomSongs(size: Int): Result<List<SongEntity>> = suspendCoroutine { continuation ->
        try {
            Log.d(TAG, "Syncing random songs from server (size: $size)")
            
            App.getSubsonicClientInstance(false)
                .albumSongListClient
                .getRandomSongs(size, null, null)
                .enqueue(object : Callback<ApiResponse> {
                    override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                        try {
                            if (response.isSuccessful && 
                                response.body()?.subsonicResponse?.randomSongs?.songs != null) {
                                
                                val songs = response.body()!!.subsonicResponse.randomSongs!!.songs!!
                                
                                // Convert to entities
                                val songEntities = songs.mapNotNull { it.toEntityOrNull() }
                                
                                Log.d(TAG, "Converted ${songEntities.size} songs")
                                continuation.resume(Result.success(songEntities))
                            } else {
                                Log.e(TAG, "Failed to sync songs: empty response")
                                continuation.resume(Result.success(emptyList()))
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error processing song response", e)
                            continuation.resumeWithException(e)
                        }
                    }

                    override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                        Log.e(TAG, "Error syncing songs", t)
                        continuation.resumeWithException(t)
                    }
                })
        } catch (e: Exception) {
            Log.e(TAG, "Error initiating song sync", e)
            continuation.resumeWithException(e)
        }
    }
    
    /**
     * Full initial sync after login
     * Directly calls Subsonic API and saves to Room database
     */
    suspend fun performInitialSync(): Result<SyncStats> = withContext(ioDispatcher) {
        try {
            Log.d(TAG, ">>>>> STARTING INITIAL SYNC <<<<<")
            
            val stats = SyncStats()
            
            // Sync albums using paged sync
            val albumReport = syncAllAlbumsPaged()
            stats.albumCount = albumReport.inserted
            Log.d(TAG, "Synced ${stats.albumCount} albums")
            
            // Skip old sync method
            val albumResult = Result.success(emptyList<AlbumEntity>())

            
            // Sync random songs
            val songResult = syncRandomSongs(200)
            if (songResult.isSuccess) {
                val songEntities = songResult.getOrNull() ?: emptyList()
                songEntities.forEach { entity ->
                    try {
                        songDao.insertSong(entity)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error inserting song: ${entity.id}", e)
                    }
                }
                stats.songCount = songEntities.size
                Log.d(TAG, "Synced ${stats.songCount} songs")
            }
            
            // Extract and sync artists from albums and songs
            val artistMap = mutableMapOf<String, ArtistEntity>()
            
            albumResult.getOrNull()?.forEach { album ->
                if (album.artistId.isNotBlank()) {
                    if (!artistMap.containsKey(album.artistId)) {
                        artistMap[album.artistId] = ArtistEntity(
                            id = album.artistId,
                            name = album.artistName,
                            imageUrl = album.albumArtUrl,
                            albumCount = 0,
                            songCount = 0,
                            isFavorite = false
                        )
                    }
                }
            }
            
            songResult.getOrNull()?.forEach { song ->
                if (song.artistId.isNotBlank()) {
                    artistMap[song.artistId] = ArtistEntity(
                        id = song.artistId,
                        name = song.artistName,
                        imageUrl = null,
                        albumCount = 0,
                        songCount = 0,
                        isFavorite = false
                    )
                }
            }
            
            artistMap.values.forEach { artist ->
                try {
                    val albumCount = albumDao.getAlbumsByArtistId(artist.id).size
                    val songCount = songDao.getSongsByArtistId(artist.id).size
                    
                    // Try to get artist info with image
                    var artistImageUrl = artist.imageUrl
                    try {
                        val artistInfoResponse = App.getSubsonicClientInstance(false)
                            .browsingClient
                            .getArtistInfo2(artist.id)
                            .execute()
                        
                        if (artistInfoResponse.isSuccessful) {
                            val artistInfo = artistInfoResponse.body()?.subsonicResponse?.artistInfo2
                            artistImageUrl = artistInfo?.largeImageUrl ?: artistInfo?.mediumImageUrl ?: artistInfo?.smallImageUrl ?: artist.imageUrl
                            Log.d(TAG, "Artist ${artist.name} image: $artistImageUrl")
                        } else {
                            Log.w(TAG, "Failed to fetch artist info for ${artist.name}: ${artistInfoResponse.code()}")
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Could not fetch artist info for ${artist.name}", e)
                    }
                    
                    artistDao.insertArtist(artist.copy(
                        albumCount = albumCount,
                        songCount = songCount,
                        imageUrl = artistImageUrl
                    ))
                } catch (e: Exception) {
                    Log.e(TAG, "Error inserting artist: ${artist.id}", e)
                }
            }
            stats.artistCount = artistMap.size
            Log.d(TAG, "Synced ${stats.artistCount} artists")
            
            // Sync playlists
            val playlistResult = syncPlaylists()
            if (playlistResult.isSuccess) {
                val playlistEntities = playlistResult.getOrNull() ?: emptyList()
                playlistEntities.forEach { entity ->
                    try {
                        playlistDao.insertPlaylist(entity)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error inserting playlist: ${entity.id}", e)
                    }
                }
                stats.playlistCount = playlistEntities.size
                Log.d(TAG, "Synced ${stats.playlistCount} playlists")
            }
            
            Log.d(TAG, "Initial sync completed: albums=${stats.albumCount}, songs=${stats.songCount}, artists=${stats.artistCount}, playlists=${stats.playlistCount}")
            Result.success(stats)
        } catch (e: Exception) {
            Log.e(TAG, "Error during initial sync", e)
            Result.failure(e)
        }
    }
}

data class SyncStats(
    var albumCount: Int = 0,
    var songCount: Int = 0,
    var artistCount: Int = 0,
    var playlistCount: Int = 0
)

data class AlbumSyncReport(
    val fetched: Int,
    val inserted: Int,
    val pages: Int
)

data class ArtistAlbumSyncReport(
    val artists: Int,
    val albums: Int,
    val songs: Int
)

data class AlbumSongSyncResult(
    val albumId: String,
    val songCount: Int,
    val totalDuration: Long
)

private data class CachedArtistDetail(
    val detail: ArtistWithAlbumsID3?,
    val timestamp: Long
) {
    fun isFresh(now: Long): Boolean = now - timestamp <= SYNC_CACHE_TTL_MS
}

private data class CachedAlbumSongs(
    val result: AlbumSongSyncResult,
    val timestamp: Long
) {
    fun isFresh(now: Long): Boolean = now - timestamp <= SYNC_CACHE_TTL_MS
}

private const val DEFAULT_PAGE_SIZE = 500
private const val DEFAULT_MAX_PAGES = 500
private const val DEFAULT_THROTTLE_MS = 50L
private const val ARTIST_SYNC_CONCURRENCY = 4
private const val ALBUM_SYNC_CONCURRENCY = 4
private const val SYNC_CACHE_TTL_MS = 5 * 60 * 1000L
private const val ARTIST_CACHE_MAX = 256
private const val ALBUM_SONG_CACHE_MAX = 512
