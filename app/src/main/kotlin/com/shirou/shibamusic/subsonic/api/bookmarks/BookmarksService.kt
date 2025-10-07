package com.shirou.shibamusic.subsonic.api.bookmarks

import com.shirou.shibamusic.subsonic.base.ApiResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.QueryMap

interface BookmarksService {
    @GET("getPlayQueue")
    fun getPlayQueue(@QueryMap params: Map<String, String>): Call<ApiResponse>

    @GET("savePlayQueue")
    fun savePlayQueue(
        @QueryMap params: Map<String, String>,
        @Query("id") ids: List<String>,
        @Query("current") current: String,
        @Query("position") position: Long
    ): Call<ApiResponse>
}
