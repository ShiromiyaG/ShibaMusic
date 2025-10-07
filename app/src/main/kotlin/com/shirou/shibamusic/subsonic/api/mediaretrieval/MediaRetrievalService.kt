package com.shirou.shibamusic.subsonic.api.mediaretrieval

import com.shirou.shibamusic.subsonic.base.ApiResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.QueryMap

interface MediaRetrievalService {
    @GET("stream")
    fun stream(
        @QueryMap params: Map<String, String>,
        @Query("id") id: String,
        @Query("maxBitRate") maxBitRate: Int?,
        @Query("format") format: String?
    ): Call<ApiResponse>

    @GET("download")
    fun download(
        @QueryMap params: Map<String, String>,
        @Query("id") id: String
    ): Call<ApiResponse>

    @GET("getLyrics")
    fun getLyrics(
        @QueryMap params: Map<String, String>,
        @Query("artist") artist: String,
        @Query("title") title: String
    ): Call<ApiResponse>
}
