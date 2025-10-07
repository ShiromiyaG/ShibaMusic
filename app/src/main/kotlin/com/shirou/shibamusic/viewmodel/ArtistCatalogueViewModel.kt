package com.shirou.shibamusic.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.shirou.shibamusic.App
import com.shirou.shibamusic.subsonic.base.ApiResponse
import com.shirou.shibamusic.subsonic.models.ArtistID3
import com.shirou.shibamusic.subsonic.models.IndexID3
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ArtistCatalogueViewModel(application: Application) : AndroidViewModel(application) {

    private val _artistList = MutableLiveData<List<ArtistID3>>(emptyList())
    val artistList: LiveData<List<ArtistID3>> get() = _artistList

    fun loadArtists() {
        App.getSubsonicClientInstance(false)
            .browsingClient
            .getArtists()
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful) {
                        response.body()?.subsonicResponse?.artists?.indices?.let { indices ->
                            val artists = mutableListOf<ArtistID3>()
                            for (index: IndexID3 in indices) {
                                index.artists?.let { artists.addAll(it) }
                            }
                            _artistList.value = artists
                        }
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    // Original Java code has an empty onFailure, keeping it consistent.
                }
            })
    }
}
