package com.shirou.shibamusic.subsonic.api.searching

import android.util.Log
import com.shirou.shibamusic.subsonic.RetrofitClient
import com.shirou.shibamusic.subsonic.Subsonic
import com.shirou.shibamusic.subsonic.base.ApiResponse
import retrofit2.Call

class SearchingClient(private val subsonic: Subsonic) {

    private val searchingService: SearchingService =
        RetrofitClient(subsonic).retrofit.create(SearchingService::class.java)

    fun search2(query: String, songCount: Int, albumCount: Int, artistCount: Int): Call<ApiResponse> {
        Log.d(TAG, "search2()")
        return searchingService.search2(subsonic.params, query, songCount, albumCount, artistCount)
    }

    fun search3(query: String, songCount: Int, albumCount: Int, artistCount: Int): Call<ApiResponse> {
        Log.d(TAG, "search3()")
        return searchingService.search3(subsonic.params, query, songCount, albumCount, artistCount)
    }

    companion object {
        private const val TAG = "SearchingClient"
    }
}
