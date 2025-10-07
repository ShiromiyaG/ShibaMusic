package com.shirou.shibamusic.data.repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.shirou.shibamusic.App
import com.shirou.shibamusic.data.database.dao.ArtistDao
import com.shirou.shibamusic.data.database.entity.ArtistEntity
import com.shirou.shibamusic.subsonic.base.ApiResponse
import com.shirou.shibamusic.subsonic.models.ArtistID3
import com.shirou.shibamusic.subsonic.models.Child
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for Artist operations with automatic database persistence
 * Migrated from Java to Kotlin with improvements
 */
@Singleton
class ArtistRepository @Inject constructor(
    private val artistDao: ArtistDao
) {
    
    companion object {
        private const val TAG = "ArtistRepository"
    }
    
    /**
     * Get starred/favorite artists from server
     * @param random If true, shuffles and returns limited results
     * @param size Number of random artists to return
     */
    fun getStarredArtists(random: Boolean = false, size: Int = 20): LiveData<List<ArtistID3>> {
        val liveData = MutableLiveData<List<ArtistID3>>(emptyList())
        
        App.getSubsonicClientInstance(false)
            .albumSongListClient
            .getStarred2()
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    val body = response.body()
                    val artists = body?.subsonicResponse?.starred2?.artists.orEmpty()

                    if (response.isSuccessful && artists.isNotEmpty()) {
                        val resultArtists = if (random) {
                            artists.shuffled().take(size)
                        } else {
                            artists
                        }
                        getArtistInfo(resultArtists, liveData)
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    Log.e(TAG, "Failed to get starred artists", t)
                }
            })
        
        return liveData
    }
    
    /**
     * Get all artists from server
     * @param random If true, shuffles and returns limited results
     * @param size Number of random artists to return
     */
    fun getArtists(random: Boolean = false, size: Int = 20): LiveData<List<ArtistID3>> {
        val liveData = MutableLiveData<List<ArtistID3>>()
        
        App.getSubsonicClientInstance(false)
            .browsingClient
            .getArtists()
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    val body = response.body()
                    if (body == null || !response.isSuccessful) {
                        return
                    }

                    val artists = mutableListOf<ArtistID3>()

                    body.subsonicResponse?.artists?.indices?.forEach { index ->
                        index.artists?.let { artists.addAll(it) }
                    }

                    if (random && artists.isNotEmpty()) {
                        val shuffled = artists.shuffled()
                        val limited = shuffled.take(minOf(size, artists.size))
                        getArtistInfo(limited, liveData)
                    } else {
                        liveData.value = artists
                        saveArtistsToDatabase(artists)
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    Log.e(TAG, "Failed to get artists", t)
                }
            })
        
        return liveData
    }
    
    /**
     * Get detailed artist info for a list of artists
     * Fetches full artist details with album count, cover, etc.
     */
    private fun getArtistInfo(artists: List<ArtistID3>, liveData: MutableLiveData<List<ArtistID3>>) {
        val currentList = liveData.value ?: emptyList()
        liveData.value = currentList
        
        artists.forEach { artist ->
            val artistId = artist.id
            if (artistId.isNullOrBlank()) {
                Log.w(TAG, "Skipping artist without id while fetching details: ${artist.name}")
                return@forEach
            }

            App.getSubsonicClientInstance(false)
                .browsingClient
                .getArtist(artistId)
                .enqueue(
                    object : Callback<ApiResponse> {
                        override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                            if (response.isSuccessful && response.body()?.subsonicResponse?.artist != null) {
                                val detailedArtist = response.body()?.subsonicResponse?.artist
                                detailedArtist?.let {
                                    addToMutableLiveData(liveData, it)
                                    saveArtistToDatabase(it)
                                }
                            }
                        }

                        override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                            Log.e(TAG, "Failed to get artist info for $artistId", t)
                        }
                    }
                )
        }
    }

    
    /**
     * Get artist info by ID
     */
    fun getArtistInfo(id: String): LiveData<ArtistID3> {
        val liveData = MutableLiveData<ArtistID3>()
        
        App.getSubsonicClientInstance(false)
            .browsingClient
            .getArtist(id)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful && response.body()?.subsonicResponse?.artist != null) {
                        val artist = response.body()?.subsonicResponse?.artist
                        artist?.let { 
                            liveData.value = it
                            saveArtistToDatabase(it) 
                        }
                    }
                }
                
                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    Log.e(TAG, "Failed to get artist info for $id", t)
                }
            })
        
        return liveData
    }
    
    /**
     * Get instant mix (similar songs) for an artist
     */
    fun getInstantMix(artist: ArtistID3, count: Int = 50): LiveData<List<Child>> {
        val liveData = MutableLiveData<List<Child>>()

        val artistId = artist.id
        if (artistId.isNullOrBlank()) {
            liveData.value = emptyList()
            return liveData
        }

        App.getSubsonicClientInstance(false)
            .browsingClient
            .getSimilarSongs2(artistId, count)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful && response.body()?.subsonicResponse?.similarSongs2 != null) {
                        liveData.value = response.body()?.subsonicResponse?.similarSongs2?.songs ?: emptyList()
                    }
                }
                
                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    Log.e(TAG, "Failed to get instant mix for artist $artistId", t)
                }
            })
        
        return liveData
    }
    
    /**
     * Get random songs from an artist (top songs shuffled)
     */
    fun getRandomSong(artist: ArtistID3, count: Int = 50): LiveData<List<Child>> {
        val liveData = MutableLiveData<List<Child>>()

        val artistName = artist.name
        if (artistName.isNullOrBlank()) {
            liveData.value = emptyList()
            return liveData
        }

        App.getSubsonicClientInstance(false)
            .browsingClient
            .getTopSongs(artistName, count)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful && response.body()?.subsonicResponse?.topSongs?.songs != null) {
                        val songs = response.body()?.subsonicResponse?.topSongs?.songs ?: emptyList()
                        liveData.value = if (songs.isNotEmpty()) songs.shuffled() else emptyList()
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    Log.e(TAG, "Failed to get random songs for artist $artistName", t)
                }
            })

        return liveData
    }
    
    /**
     * Get top songs for an artist
     */
    fun getTopSongs(artistName: String, count: Int = 50): LiveData<List<Child>> {
        val liveData = MutableLiveData<List<Child>>(emptyList())
        
        App.getSubsonicClientInstance(false)
            .browsingClient
            .getTopSongs(artistName, count)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful && response.body()?.subsonicResponse?.topSongs?.songs != null) {
                        liveData.value = response.body()?.subsonicResponse?.topSongs?.songs ?: emptyList()
                    }
                }
                
                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    Log.e(TAG, "Failed to get top songs for artist $artistName", t)
                }
            })
        
        return liveData
    }
    
    // ========== Database Operations ==========
    
    /**
     * Save artists to local database
     */
    private fun saveArtistsToDatabase(artists: List<ArtistID3>) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val entities = artists.map { it.toEntity() }
                artistDao.insertArtists(entities)
                Log.d(TAG, "Saved ${entities.size} artists to database")
            } catch (e: Exception) {
                Log.e(TAG, "Error saving artists to database", e)
            }
        }
    }
    
    /**
     * Save single artist to local database
     */
    private fun saveArtistToDatabase(artist: ArtistID3) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                artistDao.insertArtist(artist.toEntity())
                Log.d(TAG, "Saved artist ${artist.name} to database")
            } catch (e: Exception) {
                Log.e(TAG, "Error saving artist to database", e)
            }
        }
    }
    
    /**
     * Helper to add artist to MutableLiveData list
     */
    private fun addToMutableLiveData(liveData: MutableLiveData<List<ArtistID3>>, artist: ArtistID3) {
        val currentList = liveData.value?.toMutableList() ?: mutableListOf()
        currentList.add(artist)
        liveData.value = currentList
    }
    
    // ========== Extension Functions ==========
    
    /**
     * Convert ArtistID3 API model to database entity
     */
    private fun ArtistID3.toEntity(): ArtistEntity {
        return ArtistEntity(
            id = this.id ?: "",
            name = this.name ?: "",
            imageUrl = this.coverArtId,
            albumCount = this.albumCount,
            songCount = 0, // Not provided by API directly
            isFavorite = this.starred != null
        )
    }
}
