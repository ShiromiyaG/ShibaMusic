package com.shirou.shibamusic.subsonic.api.mediaretrieval

import android.util.Log
import com.shirou.shibamusic.subsonic.RetrofitClient
import com.shirou.shibamusic.subsonic.Subsonic
import com.shirou.shibamusic.subsonic.base.ApiResponse
import retrofit2.Call

class MediaRetrievalClient(private val subsonic: Subsonic) {

    private val mediaRetrievalService: MediaRetrievalService =
        RetrofitClient(subsonic).retrofit.create(MediaRetrievalService::class.java)

    fun stream(id: String, maxBitRate: Int?, format: String?): Call<ApiResponse> {
        Log.d(TAG, "stream()")
        return mediaRetrievalService.stream(subsonic.params, id, maxBitRate, format)
    }

    fun download(id: String): Call<ApiResponse> {
        Log.d(TAG, "download()")
        return mediaRetrievalService.download(subsonic.params, id)
    }

    fun getLyrics(artist: String, title: String): Call<ApiResponse> {
        Log.d(TAG, "getLyrics()")
        return mediaRetrievalService.getLyrics(subsonic.params, artist, title)
    }

    companion object {
        private const val TAG = "MediaRetrievalClient"
    }
}
