package com.shirou.shibamusic.subsonic.api.open

import com.shirou.shibamusic.subsonic.base.ApiResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.QueryMap

interface OpenService {
    @GET("getLyricsBySongId")
    fun getLyricsBySongId(
        @QueryMap params: Map<String, String>,
        @Query("id") id: String
    ): Call<ApiResponse>
}
