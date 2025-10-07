package com.shirou.shibamusic.subsonic.api.sharing

import android.util.Log
import com.shirou.shibamusic.subsonic.RetrofitClient
import com.shirou.shibamusic.subsonic.Subsonic
import com.shirou.shibamusic.subsonic.base.ApiResponse
import retrofit2.Call

class SharingClient(private val subsonic: Subsonic) {

    private val sharingService: SharingService =
        RetrofitClient(subsonic).retrofit.create(SharingService::class.java)

    fun getShares(): Call<ApiResponse> {
        Log.d(TAG, "getShares()")
        return sharingService.getShares(subsonic.params)
    }

    fun createShare(id: String, description: String?, expires: Long?): Call<ApiResponse> {
        Log.d(TAG, "createShare()")
        return sharingService.createShare(subsonic.params, id, description, expires)
    }

    fun updateShare(id: String, description: String?, expires: Long?): Call<ApiResponse> {
        Log.d(TAG, "updateShare()")
        return sharingService.updateShare(subsonic.params, id, description, expires)
    }

    fun deleteShare(id: String): Call<ApiResponse> {
        Log.d(TAG, "deleteShare()")
        return sharingService.deleteShare(subsonic.params, id)
    }

    companion object {
        private const val TAG = "SharingClient"
    }
}
