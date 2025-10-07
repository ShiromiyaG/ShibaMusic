package com.shirou.shibamusic.repository

import androidx.lifecycle.MutableLiveData
import com.shirou.shibamusic.App
import com.shirou.shibamusic.subsonic.base.ApiResponse
import com.shirou.shibamusic.subsonic.models.InternetRadioStation
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RadioRepository {
    fun getInternetRadioStations(): MutableLiveData<List<InternetRadioStation>> {
        val liveData = MutableLiveData<List<InternetRadioStation>>(emptyList())

        App.getSubsonicClientInstance(false)
            .internetRadioClient
            .getInternetRadioStations()
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful) {
                        response.body()?.subsonicResponse?.internetRadioStations?.internetRadioStations?.let { stations ->
                            liveData.postValue(stations)
                        }
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    // The original Java code has an empty onFailure, so we keep it as is.
                }
            })

        return liveData
    }

    fun createInternetRadioStation(name: String, streamURL: String, homepageURL: String?) {
        App.getSubsonicClientInstance(false)
            .internetRadioClient
            .createInternetRadioStation(streamURL, name, homepageURL.orEmpty())
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    // The original Java code has an empty onResponse, so we keep it as is.
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    // The original Java code has an empty onFailure, so we keep it as is.
                }
            })
    }

    fun updateInternetRadioStation(id: String, name: String, streamURL: String, homepageURL: String?) {
        App.getSubsonicClientInstance(false)
            .internetRadioClient
            .updateInternetRadioStation(id, streamURL, name, homepageURL.orEmpty())
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    // The original Java code has an empty onResponse, so we keep it as is.
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    // The original Java code has an empty onFailure, so we keep it as is.
                }
            })
    }

    fun deleteInternetRadioStation(id: String) {
        App.getSubsonicClientInstance(false)
            .internetRadioClient
            .deleteInternetRadioStation(id)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    // The original Java code has an empty onResponse, so we keep it as is.
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    // The original Java code has an empty onFailure, so we keep it as is.
                }
            })
    }
}
