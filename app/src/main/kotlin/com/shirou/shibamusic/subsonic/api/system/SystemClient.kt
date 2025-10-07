package com.shirou.shibamusic.subsonic.api.system

import android.util.Log
import com.shirou.shibamusic.subsonic.RetrofitClient
import com.shirou.shibamusic.subsonic.Subsonic
import com.shirou.shibamusic.subsonic.base.ApiResponse
import retrofit2.Call

class SystemClient(private val subsonic: Subsonic) {

    private val systemService: SystemService =
        RetrofitClient(subsonic).retrofit.create(SystemService::class.java)

    fun ping(): Call<ApiResponse> {
        Log.d(TAG, "ping()")
        return systemService.ping(subsonic.params)
    }

    fun getLicense(): Call<ApiResponse> {
        Log.d(TAG, "getLicense()")
        return systemService.getLicense(subsonic.params)
    }

    fun getOpenSubsonicExtensions(): Call<ApiResponse> {
        Log.d(TAG, "getOpenSubsonicExtensions()")
        return systemService.getOpenSubsonicExtensions(subsonic.params)
    }

    companion object {
        private const val TAG = "SystemClient"
    }
}
