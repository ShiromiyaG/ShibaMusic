package com.shirou.shibamusic.repository

import androidx.lifecycle.MutableLiveData

import com.shirou.shibamusic.App
import com.shirou.shibamusic.subsonic.base.ApiResponse
import com.shirou.shibamusic.subsonic.models.ArtistID3
import com.shirou.shibamusic.subsonic.models.ArtistInfo2
import com.shirou.shibamusic.subsonic.models.Child
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ArtistRepository {
    fun getStarredArtists(random: Boolean, size: Int): MutableLiveData<List<ArtistID3>> {
        val starredArtists = MutableLiveData(emptyList<ArtistID3>())

        App.getSubsonicClientInstance(false)
            .albumSongListClient
            .getStarred2()
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    response.body()?.subsonicResponse?.starred2?.artists?.let { initialArtists ->
                        if (initialArtists.isNotEmpty()) {
                            if (!random) {
                                getArtistInfo(initialArtists, starredArtists)
                            } else {
                                val mutableArtists = initialArtists.toMutableList()
                                mutableArtists.shuffle()
                                getArtistInfo(mutableArtists.subList(0, minOf(size, mutableArtists.size)), starredArtists)
                            }
                        }
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    // No-op, as in original Java
                }
            })

        return starredArtists
    }

    fun getArtists(random: Boolean, size: Int): MutableLiveData<List<ArtistID3>> {
        val listLiveArtists = MutableLiveData(emptyList<ArtistID3>())

        App.getSubsonicClientInstance(false)
            .browsingClient
            .getArtists()
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful) {
                        val artists = mutableListOf<ArtistID3>()

                        response.body()?.subsonicResponse?.artists?.indices?.forEach { index ->
                            index?.artists?.let { indexArtists ->
                                artists.addAll(indexArtists)
                            }
                        }

                        if (random) {
                            if (artists.isNotEmpty()) {
                                artists.shuffle()
                            }
                            val sublistSize = if (artists.size / size > 0) size else artists.size
                            getArtistInfo(artists.subList(0, sublistSize), listLiveArtists)
                        } else {
                            listLiveArtists.value = artists
                        }
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    // No-op
                }
            })

        return listLiveArtists
    }

    /*
     * Metodo che mi restituisce le informazioni essenziali dell'artista (cover, numero di album...)
     */
    fun getArtistInfo(artists: List<ArtistID3>, list: MutableLiveData<List<ArtistID3>>) {
        if (list.value == null) {
            list.value = emptyList()
        }

        artists.forEach { artist ->
            val artistId = artist.id ?: return@forEach
            App.getSubsonicClientInstance(false)
                .browsingClient
                .getArtist(artistId)
                .enqueue(object : Callback<ApiResponse> {
                    override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                        response.body()?.subsonicResponse?.artist?.let { fetchedArtist ->
                            addToMutableLiveData(list, fetchedArtist)
                        }
                    }

                    override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                        // No-op
                    }
                })
        }
    }

    fun getArtistInfo(id: String): MutableLiveData<ArtistID3> {
        val artistLiveData = MutableLiveData<ArtistID3>()

        App.getSubsonicClientInstance(false)
            .browsingClient
            .getArtist(id)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    response.body()?.subsonicResponse?.artist?.let { artist ->
                        artistLiveData.value = artist
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    // No-op
                }
            })

        return artistLiveData
    }

    fun getArtistFullInfo(id: String): MutableLiveData<ArtistInfo2> {
        val artistFullInfo = MutableLiveData<ArtistInfo2>()

        App.getSubsonicClientInstance(false)
            .browsingClient
            .getArtistInfo2(id)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    response.body()?.subsonicResponse?.artistInfo2?.let { info ->
                        artistFullInfo.value = info
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    // No-op
                }
            })

        return artistFullInfo
    }

    fun setRating(id: String, rating: Int) {
        App.getSubsonicClientInstance(false)
            .mediaAnnotationClient
            .setRating(id, rating)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    // No-op
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    // No-op
                }
            })
    }

    fun getArtist(id: String): MutableLiveData<ArtistID3> {
        val artistLiveData = MutableLiveData<ArtistID3>()

        App.getSubsonicClientInstance(false)
            .browsingClient
            .getArtist(id)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    response.body()?.subsonicResponse?.artist?.let { artist ->
                        artistLiveData.value = artist
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    // No-op
                }
            })

        return artistLiveData
    }

    fun getInstantMix(artist: ArtistID3, count: Int): MutableLiveData<List<Child>> {
        val instantMix = MutableLiveData(emptyList<Child>())

        val artistId = artist.id ?: return instantMix

        App.getSubsonicClientInstance(false)
            .browsingClient
            .getSimilarSongs2(artistId, count)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    response.body()?.subsonicResponse?.similarSongs2?.songs?.let { songs ->
                        instantMix.value = songs
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    // No-op
                }
            })

        return instantMix
    }

    fun getRandomSong(artist: ArtistID3, count: Int): MutableLiveData<List<Child>> {
        val randomSongs = MutableLiveData(emptyList<Child>())

        val artistName = artist.name ?: return randomSongs

        App.getSubsonicClientInstance(false)
            .browsingClient
            .getTopSongs(artistName, count)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    val songs: MutableList<Child>? = response.body()?.subsonicResponse?.topSongs?.songs?.toMutableList()

                    if (songs != null) {
                        if (songs.isNotEmpty()) {
                            songs.shuffle()
                        }
                        randomSongs.value = songs
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    // No-op
                }
            })

        return randomSongs
    }

    fun getTopSongs(artistName: String, count: Int): MutableLiveData<List<Child>> {
        val topSongs = MutableLiveData(emptyList<Child>())

        App.getSubsonicClientInstance(false)
            .browsingClient
            .getTopSongs(artistName, count)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    response.body()?.subsonicResponse?.topSongs?.songs?.let { songs ->
                        topSongs.value = songs
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    // No-op
                }
            })

        return topSongs
    }

    private fun addToMutableLiveData(liveData: MutableLiveData<List<ArtistID3>>, artist: ArtistID3) {
        liveData.value = liveData.value.orEmpty() + artist
    }
}
