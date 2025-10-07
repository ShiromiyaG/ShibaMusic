package com.shirou.shibamusic.data.repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.shirou.shibamusic.App
import com.shirou.shibamusic.data.database.dao.SongDao
import com.shirou.shibamusic.data.database.entity.SongEntity
import com.shirou.shibamusic.subsonic.base.ApiResponse
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
 * Repository responsible for fetching songs from the Subsonic API
 * and persisting them into the local Room database.
 */
@Singleton
class SongRepository @Inject constructor(
    private val songDao: SongDao
) {

    companion object {
        private const val TAG = "SongRepository"
    }

    /**
     * Fetch users' starred songs. Optionally returns a random subset.
     */
    fun getStarredSongs(random: Boolean = false, size: Int = 20): LiveData<List<Child>> {
        val liveData = MutableLiveData<List<Child>>(emptyList())

        App.getSubsonicClientInstance(false)
            .albumSongListClient
            .getStarred2()
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (!response.isSuccessful) {
                        liveData.postValue(emptyList())
                        return
                    }

                    val songs = response.body()
                        ?.subsonicResponse
                        ?.starred2
                        ?.songs
                        ?: emptyList()

                    val result = when {
                        !random -> songs
                        size < 0 -> songs.shuffled()
                        songs.isEmpty() -> songs
                        else -> songs.shuffled().take(size)
                    }

                    liveData.postValue(result)
                    saveSongsToDatabase(result)
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    Log.e(TAG, "Failed to fetch starred songs", t)
                    liveData.postValue(emptyList())
                }
            })

        return liveData
    }

    /**
     * Retrieve an instant mix of similar songs for the provided song id.
     */
    fun getInstantMix(id: String, count: Int = 50): LiveData<List<Child>> {
        val liveData = MutableLiveData<List<Child>>(emptyList())

        App.getSubsonicClientInstance(false)
            .browsingClient
            .getSimilarSongs2(id, count)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (!response.isSuccessful) {
                        liveData.postValue(emptyList())
                        return
                    }

                    val songs = response.body()
                        ?.subsonicResponse
                        ?.similarSongs2
                        ?.songs
                        ?: emptyList()

                    liveData.postValue(songs)
                    saveSongsToDatabase(songs)
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    Log.e(TAG, "Failed to fetch instant mix for $id", t)
                    liveData.postValue(emptyList())
                }
            })

        return liveData
    }

    /**
     * Fetch top songs for the provided artist name.
     */
    fun getTopSongs(artistName: String, count: Int = 50): LiveData<List<Child>> {
        val liveData = MutableLiveData<List<Child>>(emptyList())

        App.getSubsonicClientInstance(false)
            .browsingClient
            .getTopSongs(artistName, count)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (!response.isSuccessful) {
                        liveData.postValue(emptyList())
                        return
                    }

                    val songs = response.body()
                        ?.subsonicResponse
                        ?.topSongs
                        ?.songs
                        ?: emptyList()

                    liveData.postValue(songs)
                    saveSongsToDatabase(songs)
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    Log.e(TAG, "Failed to fetch top songs for $artistName", t)
                    liveData.postValue(emptyList())
                }
            })

        return liveData
    }

    /**
     * Fetch a random sample of songs, optionally filtered by year range.
     */
    fun getRandomSample(count: Int, fromYear: Int? = null, toYear: Int? = null): LiveData<List<Child>> {
        val liveData = MutableLiveData<List<Child>>(emptyList())

        App.getSubsonicClientInstance(false)
            .albumSongListClient
            .getRandomSongs(count, fromYear, toYear)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (!response.isSuccessful) {
                        liveData.postValue(emptyList())
                        return
                    }

                    val songs = response.body()
                        ?.subsonicResponse
                        ?.randomSongs
                        ?.songs
                        ?: emptyList()

                    liveData.postValue(songs)
                    saveSongsToDatabase(songs)
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    Log.e(TAG, "Failed to fetch random songs", t)
                    liveData.postValue(emptyList())
                }
            })

        return liveData
    }

    private fun saveSongsToDatabase(songs: List<Child>) {
        if (songs.isEmpty()) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val entities = songs.mapNotNull { it.toEntityOrNull() }
                if (entities.isNotEmpty()) {
                    songDao.insertSongs(entities)
                    Log.d(TAG, "Persisted ${entities.size} songs to database")
                }
            } catch (t: Throwable) {
                Log.e(TAG, "Unable to persist songs", t)
            }
        }
    }

    private fun saveSongToDatabase(song: Child) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                song.toEntityOrNull()?.let { songDao.insertSong(it) }
            } catch (t: Throwable) {
                Log.e(TAG, "Unable to persist song ${song.id}", t)
            }
        }
    }

    private fun Child.toEntityOrNull(): SongEntity? {
        val childId = id.takeIf { it.isNotBlank() } ?: return null

        return SongEntity(
            id = childId,
            title = title.orEmpty(),
            artistId = artistId.orEmpty(),
            artistName = artist.orEmpty(),
            albumId = albumId.orEmpty(),
            albumName = album.orEmpty(),
            albumArtUrl = coverArtId,
            durationMs = (duration ?: 0) * 1000L,
            trackNumber = track,
            discNumber = discNumber,
            year = year,
            genre = genre,
            path = path,
            isFavorite = starred != null,
            playCount = 0,
            lastPlayedTimestamp = null,
            dateAdded = System.currentTimeMillis(),
            dateModified = System.currentTimeMillis()
        )
    }
}
