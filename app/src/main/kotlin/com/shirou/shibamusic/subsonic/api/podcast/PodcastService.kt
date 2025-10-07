package com.shirou.shibamusic.subsonic.api.podcast

import com.shirou.shibamusic.subsonic.base.ApiResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.QueryMap

interface PodcastService {
    @GET("getPodcasts")
    fun getPodcasts(
        @QueryMap params: Map<String, String>,
        @Query("includeEpisodes") includeEpisodes: Boolean,
        @Query("id") id: String?
    ): Call<ApiResponse>

    @GET("getNewestPodcasts")
    fun getNewestPodcasts(
        @QueryMap params: Map<String, String>,
        @Query("count") count: Int
    ): Call<ApiResponse>

    @GET("refreshPodcasts")
    fun refreshPodcasts(@QueryMap params: Map<String, String>): Call<ApiResponse>

    @GET("createPodcastChannel")
    fun createPodcastChannel(
        @QueryMap params: Map<String, String>,
        @Query("url") url: String
    ): Call<ApiResponse>

    @GET("deletePodcastChannel")
    fun deletePodcastChannel(
        @QueryMap params: Map<String, String>,
        @Query("id") id: String
    ): Call<ApiResponse>

    @GET("deletePodcastEpisode")
    fun deletePodcastEpisode(
        @QueryMap params: Map<String, String>,
        @Query("id") id: String
    ): Call<ApiResponse>

    @GET("downloadPodcastEpisode")
    fun downloadPodcastEpisode(
        @QueryMap params: Map<String, String>,
        @Query("id") id: String
    ): Call<ApiResponse>
}
