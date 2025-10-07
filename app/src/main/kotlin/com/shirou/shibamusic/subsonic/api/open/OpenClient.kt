package com.shirou.shibamusic.subsonic.api.open

import android.util.Log
import com.shirou.shibamusic.subsonic.RetrofitClient
import com.shirou.shibamusic.subsonic.Subsonic
import com.shirou.shibamusic.subsonic.base.ApiResponse
import retrofit2.Call

class OpenClient(private val subsonic: Subsonic) {

    private val openService: OpenService =
        RetrofitClient(subsonic).retrofit.create(OpenService::class.java)

    fun getLyricsBySongId(id: String): Call<ApiResponse> {
        Log.d(TAG, "getLyricsBySongId()")
        return openService.getLyricsBySongId(subsonic.params, id)
    }

    companion object {
        private const val TAG = "OpenClient"
    }
}
