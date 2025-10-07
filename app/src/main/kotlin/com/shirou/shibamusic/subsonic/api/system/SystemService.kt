package com.shirou.shibamusic.subsonic.api.system

import com.shirou.shibamusic.subsonic.base.ApiResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.QueryMap

interface SystemService {
    @GET("ping")
    fun ping(@QueryMap params: Map<String, String>): Call<ApiResponse>

    @GET("getLicense")
    fun getLicense(@QueryMap params: Map<String, String>): Call<ApiResponse>

    @GET("getOpenSubsonicExtensions")
    fun getOpenSubsonicExtensions(@QueryMap params: Map<String, String>): Call<ApiResponse>
}
