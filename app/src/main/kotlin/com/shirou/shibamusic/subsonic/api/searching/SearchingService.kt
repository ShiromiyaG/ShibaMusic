package com.shirou.shibamusic.subsonic.api.searching

import com.shirou.shibamusic.subsonic.base.ApiResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.QueryMap

interface SearchingService {
    @GET("search2")
    fun search2(
        @QueryMap params: Map<String, String>,
        @Query("query") query: String,
        @Query("songCount") songCount: Int,
        @Query("albumCount") albumCount: Int,
        @Query("artistCount") artistCount: Int
    ): Call<ApiResponse>

    @GET("search3")
    fun search3(
        @QueryMap params: Map<String, String>,
        @Query("query") query: String,
        @Query("songCount") songCount: Int,
        @Query("albumCount") albumCount: Int,
        @Query("artistCount") artistCount: Int
    ): Call<ApiResponse>
}
