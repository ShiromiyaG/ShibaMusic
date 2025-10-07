package com.shirou.shibamusic.subsonic.api.medialibraryscanning

import android.util.Log
import com.shirou.shibamusic.subsonic.RetrofitClient
import com.shirou.shibamusic.subsonic.Subsonic
import com.shirou.shibamusic.subsonic.base.ApiResponse
import retrofit2.Call

class MediaLibraryScanningClient(private val subsonic: Subsonic) {

    private val mediaLibraryScanningService: MediaLibraryScanningService =
        RetrofitClient(subsonic).retrofit.create(MediaLibraryScanningService::class.java)

    fun startScan(): Call<ApiResponse> {
        Log.d(TAG, "startScan()")
        return mediaLibraryScanningService.startScan(subsonic.params)
    }

    fun getScanStatus(): Call<ApiResponse> {
        Log.d(TAG, "getScanStatus()")
        return mediaLibraryScanningService.getScanStatus(subsonic.params)
    }

    companion object {
        private const val TAG = "MediaLibraryScanningClient"
    }
}
