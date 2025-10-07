package com.shirou.shibamusic.data.repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.shirou.shibamusic.App
import com.shirou.shibamusic.data.database.dao.AlbumDao
import com.shirou.shibamusic.data.database.dao.SongDao
import com.shirou.shibamusic.data.database.entity.AlbumEntity
import com.shirou.shibamusic.data.database.entity.SongEntity
import com.shirou.shibamusic.di.IoDispatcher
import com.shirou.shibamusic.subsonic.base.ApiResponse
import com.shirou.shibamusic.subsonic.models.AlbumID3
import com.shirou.shibamusic.subsonic.models.Child
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for Album operations - migrated from Java
 * Handles both server API calls and local database operations
 */
@Singleton
class AlbumRepository @Inject constructor(
    private val albumDao: AlbumDao,
    private val songDao: SongDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    companion object {
        private const val TAG = "AlbumRepository"
    }
    
    /**
     * Get albums from server
     * @param type: "alphabeticalByName", "frequent", "recent", "newest", "random", etc
     */
    fun getAlbums(
        type: String,
        size: Int,
        fromYear: Int? = null,
        toYear: Int? = null
    ): LiveData<List<AlbumID3>> {
        val liveData = MutableLiveData<List<AlbumID3>>(emptyList())
        
        App.getSubsonicClientInstance(false)
            .albumSongListClient
            .getAlbumList2(type, size, 0, fromYear, toYear)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful) {
                        val albums = response.body()
                            ?.subsonicResponse
                            ?.albumList2
                            ?.albums
                            ?: emptyList()
                        liveData.value = albums
                        
                        // Save to database
                        saveAlbumsToDatabase(albums)
                    }
                }
                
                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    Log.e(TAG, "Failed to get albums", t)
                }
            })
        
        return liveData
    }
    
    /**
     * Get starred (favorite) albums
     */
    fun getStarredAlbums(random: Boolean = false, size: Int = 100): LiveData<List<AlbumID3>> {
        val liveData = MutableLiveData<List<AlbumID3>>(emptyList())
        
        App.getSubsonicClientInstance(false)
            .albumSongListClient
            .getStarred2()
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful) {
                        var albums = response.body()
                            ?.subsonicResponse
                            ?.starred2
                            ?.albums
                            ?: emptyList()
                        
                        if (random && albums.isNotEmpty()) {
                            albums = albums.shuffled().take(size)
                        }
                        
                        liveData.value = albums
                    }
                }
                
                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    Log.e(TAG, "Failed to get starred albums", t)
                }
            })
        
        return liveData
    }
    
    /**
     * Get album tracks (songs)
     */
    fun getAlbumTracks(albumId: String): LiveData<List<Child>> {
        val liveData = MutableLiveData<List<Child>>(emptyList())
        
        App.getSubsonicClientInstance(false)
            .browsingClient
            .getAlbum(albumId)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful) {
                        val songs = response.body()
                            ?.subsonicResponse
                            ?.album
                            ?.songs
                            ?: emptyList()
                        
                        liveData.value = songs
                        
                        // Save songs to database
                        saveSongsToDatabase(songs)
                    }
                }
                
                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    Log.e(TAG, "Failed to get album tracks", t)
                }
            })
        
        return liveData
    }
    
    /**
     * Get artist albums
     */
    fun getArtistAlbums(artistId: String): LiveData<List<AlbumID3>> {
        val liveData = MutableLiveData<List<AlbumID3>>(emptyList())
        
        App.getSubsonicClientInstance(false)
            .browsingClient
            .getArtist(artistId)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful) {
                        val albums = response.body()
                            ?.subsonicResponse
                            ?.artist
                            ?.albums
                            ?: emptyList()
                        
                        // Sort by year (newest first)
                        val sortedAlbums = albums.sortedByDescending { it.year }
                        liveData.value = sortedAlbums
                        
                        // Save to database
                        saveAlbumsToDatabase(albums)
                    }
                }
                
                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    Log.e(TAG, "Failed to get artist albums", t)
                }
            })
        
        return liveData
    }
    
    /**
     * Set rating for album
     */
    fun setRating(albumId: String, rating: Int) {
        App.getSubsonicClientInstance(false)
            .mediaAnnotationClient
            .setRating(albumId, rating)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    Log.d(TAG, "Rating set successfully")
                }
                
                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    Log.e(TAG, "Failed to set rating", t)
                }
            })
    }
    
    // Private helper methods
    
    private fun saveAlbumsToDatabase(albums: List<AlbumID3>) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val entities = albums.map { it.toEntity() }
                albumDao.insertAlbums(entities)
                Log.d(TAG, "Saved ${entities.size} albums to database")
            } catch (e: Exception) {
                Log.e(TAG, "Error saving albums to database", e)
            }
        }
    }
    
    private fun saveSongsToDatabase(songs: List<Child>) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val entities = songs.map { it.toEntity() }
                songDao.insertSongs(entities)
                Log.d(TAG, "Saved ${entities.size} songs to database")
            } catch (e: Exception) {
                Log.e(TAG, "Error saving songs to database", e)
            }
        }
    }
}

// Extension functions for conversion
private fun AlbumID3.toEntity() = AlbumEntity(
    id = this.id ?: "",
    title = this.name ?: "Unknown Album",
    artistId = this.artistId ?: "",
    artistName = this.artist ?: "Unknown Artist",
    albumArtUrl = this.coverArtId,
    songCount = this.songCount ?: 0,
    durationMs = (this.duration ?: 0).toLong() * 1000,
    year = this.year,
    genre = this.genre
)

private fun Child.toEntity() = SongEntity(
    id = this.id,
    title = this.title ?: "Unknown Title",
    artistId = this.artistId ?: "",
    artistName = this.artist ?: "Unknown Artist",
    albumId = this.albumId ?: "",
    albumName = this.album ?: "Unknown Album",
    albumArtUrl = this.coverArtId,
    durationMs = (this.duration ?: 0).toLong() * 1000,
    trackNumber = this.track,
    discNumber = this.discNumber,
    year = this.year,
    genre = this.genre,
    path = this.path
)
