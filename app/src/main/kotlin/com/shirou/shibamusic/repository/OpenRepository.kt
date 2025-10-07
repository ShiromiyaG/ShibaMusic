package com.shirou.shibamusic.repository

import androidx.lifecycle.MutableLiveData
import com.shirou.shibamusic.App
import com.shirou.shibamusic.subsonic.base.ApiResponse
import com.shirou.shibamusic.subsonic.models.LyricsList
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class OpenRepository {
    fun getLyricsBySongId(id: String): MutableLiveData<LyricsList> {
        val lyricsList = MutableLiveData<LyricsList>()

        App.getSubsonicClientInstance(false)
            .openClient
            .getLyricsBySongId(id)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (!response.isSuccessful) {
                        return
                    }

                    response.body()
                        ?.subsonicResponse
                        ?.lyricsList
                        ?.let { lyricsList.postValue(it) }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    // No action was taken in the original Java code on failure.
                }
            })

        return lyricsList
    }
}
