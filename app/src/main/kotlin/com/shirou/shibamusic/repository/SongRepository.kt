package com.shirou.shibamusic.repository

import androidx.lifecycle.MutableLiveData

import com.shirou.shibamusic.App
import com.shirou.shibamusic.subsonic.base.ApiResponse
import com.shirou.shibamusic.subsonic.models.Child

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

import kotlin.math.min

class SongRepository {

    companion object {
        private const val TAG = "SongRepository"
    }

    fun getStarredSongs(random: Boolean, size: Int): MutableLiveData<List<Child>> {
        val starredSongs = MutableLiveData<List<Child>>(emptyList())

        App.getSubsonicClientInstance(false)
            .albumSongListClient
            .getStarred2()
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    response.body()?.subsonicResponse?.starred2?.songs?.let { songs ->
                        val result = if (!random) {
                            songs
                        } else {
                            songs.shuffled().take(min(size, songs.size))
                        }

                        starredSongs.postValue(result)
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    // Original Java code does nothing on failure
                }
            })

        return starredSongs
    }

    fun getInstantMix(id: String, count: Int): MutableLiveData<List<Child>?> {
        val instantMix = MutableLiveData<List<Child>?>()

        App.getSubsonicClientInstance(false)
            .browsingClient
            .getSimilarSongs2(id, count)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    response.body()?.subsonicResponse?.similarSongs2?.songs?.let { songs ->
                        instantMix.postValue(songs)
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    instantMix.postValue(null)
                }
            })

        return instantMix
    }

    fun getRandomSample(number: Int, fromYear: Int?, toYear: Int?): MutableLiveData<List<Child>> {
        val randomSongsSample = MutableLiveData<List<Child>>(emptyList())

        App.getSubsonicClientInstance(false)
            .albumSongListClient
            .getRandomSongs(number, fromYear, toYear)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    val songs = response.body()
                        ?.subsonicResponse
                        ?.randomSongs
                        ?.songs
                        ?: emptyList()

                    randomSongsSample.postValue(songs)
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    // Original Java code does nothing on failure
                }
            })

        return randomSongsSample
    }

    fun scrobble(id: String, submission: Boolean) {
        App.getSubsonicClientInstance(false)
            .mediaAnnotationClient
            .scrobble(id, submission)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    // Original Java code does nothing
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    // Original Java code does nothing
                }
            })
    }

    fun setRating(id: String, rating: Int) {
        App.getSubsonicClientInstance(false)
            .mediaAnnotationClient
            .setRating(id, rating)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    // Original Java code does nothing
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    // Original Java code does nothing
                }
            })
    }

    fun getSongsByGenre(id: String, page: Int): MutableLiveData<List<Child>> {
        val songsByGenre = MutableLiveData<List<Child>>(emptyList())

        App.getSubsonicClientInstance(false)
            .albumSongListClient
            .getSongsByGenre(id, 100, 100 * page)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    response.body()?.subsonicResponse?.songsByGenre?.songs?.let { songs ->
                        songsByGenre.postValue(songs)
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    // Original Java code does nothing on failure
                }
            })

        return songsByGenre
    }

    fun getSongsByGenres(genresId: ArrayList<String>): MutableLiveData<List<Child>> {
        val songsByGenre = MutableLiveData<List<Child>>(emptyList())

        for (id in genresId) {
            App.getSubsonicClientInstance(false)
                .albumSongListClient
                .getSongsByGenre(id, 500, 0)
                .enqueue(object : Callback<ApiResponse> {
                    override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                        val songs = response.body()
                            ?.subsonicResponse
                            ?.songsByGenre
                            ?.songs
                            ?: emptyList()

                        songsByGenre.postValue(songs)
                    }

                    override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                        // Original Java code does nothing on failure
                    }
                })
        }

        return songsByGenre
    }

    fun getSong(id: String): MutableLiveData<Child?> {
        val song = MutableLiveData<Child?>(null)

        App.getSubsonicClientInstance(false)
            .browsingClient
            .getSong(id)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    response.body()?.subsonicResponse?.song?.let { fetchedSong ->
                        song.postValue(fetchedSong)
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    // Original Java code does nothing on failure
                    // So, the LiveData will remain its initial null value or whatever was set before.
                }
            })

        return song
    }

    fun getSongLyrics(song: Child): MutableLiveData<String?> {
        val lyrics = MutableLiveData<String?>(null)

        val artist = song.artist ?: ""
        val title = song.title ?: ""

        App.getSubsonicClientInstance(false)
            .mediaRetrievalClient
            .getLyrics(artist, title)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    response.body()?.subsonicResponse?.lyrics?.value?.let { lyricsValue ->
                        lyrics.value = lyricsValue
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    // Original Java code does nothing on failure
                }
            })

        return lyrics
    }
}
