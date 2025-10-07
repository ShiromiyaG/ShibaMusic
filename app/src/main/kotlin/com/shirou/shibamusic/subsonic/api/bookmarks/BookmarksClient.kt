package com.shirou.shibamusic.subsonic.api.bookmarks

import android.util.Log
import com.shirou.shibamusic.subsonic.RetrofitClient
import com.shirou.shibamusic.subsonic.Subsonic
import com.shirou.shibamusic.subsonic.base.ApiResponse
import retrofit2.Call

class BookmarksClient(private val subsonic: Subsonic) {

    private val bookmarksService: BookmarksService =
        RetrofitClient(subsonic).retrofit.create(BookmarksService::class.java)

    fun getPlayQueue(): Call<ApiResponse> {
        Log.d(TAG, "getPlayQueue()")
        return bookmarksService.getPlayQueue(subsonic.params)
    }

    fun savePlayQueue(ids: List<String>, current: String, position: Long): Call<ApiResponse> {
        Log.d(TAG, "savePlayQueue()")
        return bookmarksService.savePlayQueue(subsonic.params, ids, current, position)
    }

    companion object {
        private const val TAG = "BookmarksClient"
    }
}
