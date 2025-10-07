package com.shirou.shibamusic.subsonic.api.internetradio

import com.shirou.shibamusic.subsonic.base.ApiResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.QueryMap

interface InternetRadioService {
    @GET("getInternetRadioStations")
    fun getInternetRadioStations(@QueryMap params: Map<String, String>): Call<ApiResponse>

    @GET("createInternetRadioStation")
    fun createInternetRadioStation(
        @QueryMap params: Map<String, String>,
        @Query("streamUrl") streamUrl: String,
        @Query("name") name: String,
        @Query("homepageUrl") homepageUrl: String
    ): Call<ApiResponse>

    @GET("updateInternetRadioStation")
    fun updateInternetRadioStation(
        @QueryMap params: Map<String, String>,
        @Query("id") id: String,
        @Query("streamUrl") streamUrl: String,
        @Query("name") name: String,
        @Query("homepageUrl") homepageUrl: String
    ): Call<ApiResponse>

    @GET("deleteInternetRadioStation")
    fun deleteInternetRadioStation(
        @QueryMap params: Map<String, String>,
        @Query("id") id: String
    ): Call<ApiResponse>
}
