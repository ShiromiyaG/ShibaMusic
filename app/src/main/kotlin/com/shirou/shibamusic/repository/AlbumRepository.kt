package com.shirou.shibamusic.repository

import androidx.lifecycle.MutableLiveData
import com.shirou.shibamusic.App
import com.shirou.shibamusic.interfaces.DecadesCallback
import com.shirou.shibamusic.interfaces.MediaCallback
import com.shirou.shibamusic.subsonic.base.ApiResponse
import com.shirou.shibamusic.subsonic.models.AlbumID3
import com.shirou.shibamusic.subsonic.models.AlbumInfo
import com.shirou.shibamusic.subsonic.models.Child
import java.util.Calendar
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AlbumRepository {

    fun getAlbums(type: String, size: Int, fromYear: Int?, toYear: Int?): MutableLiveData<List<AlbumID3>> {
    val listLiveAlbums = MutableLiveData(emptyList<AlbumID3>())

        App.getSubsonicClientInstance(false)
            .albumSongListClient
            .getAlbumList2(type, size, 0, fromYear, toYear)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful) {
                        response.body()?.subsonicResponse?.albumList2?.albums?.let { albums ->
                            listLiveAlbums.value = albums
                        }
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    // Original Java code leaves this empty, maintaining behavior
                }
            })

        return listLiveAlbums
    }

    fun getStarredAlbums(random: Boolean, size: Int): MutableLiveData<List<AlbumID3>> {
    val starredAlbums = MutableLiveData(emptyList<AlbumID3>())

        App.getSubsonicClientInstance(false)
            .albumSongListClient
            .getStarred2()
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful) {
                        response.body()?.subsonicResponse?.starred2?.albums?.let { albums ->
                            val result = if (random) {
                                if (size < 0) albums.shuffled() else albums.shuffled().take(size)
                            } else {
                                albums
                            }

                            starredAlbums.value = result
                        }
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    // Original Java code leaves this empty, maintaining behavior
                }
            })

        return starredAlbums
    }

    fun setRating(id: String, rating: Int) {
        App.getSubsonicClientInstance(false)
            .mediaAnnotationClient
            .setRating(id, rating)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    // Original Java code leaves this empty, maintaining behavior
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    // Original Java code leaves this empty, maintaining behavior
                }
            })
    }

    fun getAlbumTracks(id: String): MutableLiveData<List<Child>> {
    val albumTracks = MutableLiveData(emptyList<Child>())
        val tracks = mutableListOf<Child>()

        App.getSubsonicClientInstance(false)
            .browsingClient
            .getAlbum(id)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful) {
                        response.body()?.subsonicResponse?.album?.songs?.let { songs ->
                            tracks.addAll(songs)
                        }
                    }
                    albumTracks.value = tracks.toList()
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    // Original Java code leaves this empty, maintaining behavior
                }
            })

        return albumTracks
    }

    fun getArtistAlbums(id: String): MutableLiveData<List<AlbumID3>> {
    val artistsAlbum = MutableLiveData(emptyList<AlbumID3>())

        App.getSubsonicClientInstance(false)
            .browsingClient
            .getArtist(id)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful) {
                        response.body()?.subsonicResponse?.artist?.albums?.let { albums ->
                            val sortedAlbums = albums.sortedByDescending { it.year }
                            artistsAlbum.value = sortedAlbums
                        }
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    // Original Java code leaves this empty, maintaining behavior
                }
            })

        return artistsAlbum
    }

    fun getAlbum(id: String): MutableLiveData<AlbumID3> {
        val album = MutableLiveData<AlbumID3>()

        App.getSubsonicClientInstance(false)
            .browsingClient
            .getAlbum(id)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful) {
                        response.body()?.subsonicResponse?.album?.let { albumID3 ->
                            album.value = albumID3
                        }
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    // Original Java code leaves this empty, maintaining behavior
                }
            })

        return album
    }

    fun getAlbumInfo(id: String): MutableLiveData<AlbumInfo> {
        val albumInfo = MutableLiveData<AlbumInfo>()

        App.getSubsonicClientInstance(false)
            .browsingClient
            .getAlbumInfo2(id)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful) {
                        response.body()?.subsonicResponse?.albumInfo?.let { info ->
                            albumInfo.value = info
                        }
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    // Original Java code leaves this empty, maintaining behavior
                }
            })

        return albumInfo
    }

    fun getInstantMix(album: AlbumID3, count: Int, callback: MediaCallback) {
        val albumId = album.id ?: run {
            callback.onLoadMedia(emptyList<Child>())
            return
        }

        App.getSubsonicClientInstance(false)
            .browsingClient
            .getSimilarSongs2(albumId, count)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    val songs = mutableListOf<Child>()
                    if (response.isSuccessful) {
                        response.body()?.subsonicResponse?.similarSongs2?.songs?.let { s ->
                            songs.addAll(s)
                        }
                    }
                    callback.onLoadMedia(songs)
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    callback.onLoadMedia(emptyList<Child>())
                }
            })
    }

    fun getDecades(): MutableLiveData<List<Int>> {
        val decades = MutableLiveData<List<Int>>().apply { value = emptyList() }

        getFirstAlbum { first: Int ->
            getLastAlbum { last: Int ->
                if (first != -1 && last != -1) {
                    val decadeList = mutableListOf<Int>()

                    var startDecade = first - (first % 10)
                    val lastDecade = last - (last % 10)

                    while (startDecade <= lastDecade) {
                        decadeList.add(startDecade)
                        startDecade += 10
                    }

                    decades.value = decadeList
                }
            }
        }

        return decades
    }

    private fun getFirstAlbum(callback: DecadesCallback) {
        App.getSubsonicClientInstance(false)
            .albumSongListClient
            .getAlbumList2("byYear", 1, 0, 1900, Calendar.getInstance().get(Calendar.YEAR))
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful) {
                        response.body()?.subsonicResponse?.albumList2?.albums?.firstOrNull()?.year?.let { year ->
                            callback.onLoadYear(year)
                        } ?: callback.onLoadYear(-1)
                    } else {
                        callback.onLoadYear(-1)
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    callback.onLoadYear(-1)
                }
            })
    }

    private fun getLastAlbum(callback: DecadesCallback) {
        App.getSubsonicClientInstance(false)
            .albumSongListClient
            .getAlbumList2("byYear", 1, 0, Calendar.getInstance().get(Calendar.YEAR), 1900)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful) {
                        response.body()?.subsonicResponse?.albumList2?.albums?.firstOrNull()?.year?.let { year ->
                            callback.onLoadYear(year)
                        } ?: callback.onLoadYear(-1)
                    } else {
                        callback.onLoadYear(-1)
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    callback.onLoadYear(-1)
                }
            })
    }
}
