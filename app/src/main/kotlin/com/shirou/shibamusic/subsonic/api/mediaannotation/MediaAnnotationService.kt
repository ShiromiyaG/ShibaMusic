package com.shirou.shibamusic.subsonic.api.mediaannotation

import com.shirou.shibamusic.subsonic.base.ApiResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.QueryMap

interface MediaAnnotationService {
    @GET("star")
    fun starSong(
        @QueryMap params: Map<String, String>,
        @Query("id") id: String
    ): Call<ApiResponse>

    @GET("star")
    fun starAlbum(
        @QueryMap params: Map<String, String>,
        @Query("albumId") albumId: String
    ): Call<ApiResponse>

    @GET("star")
    fun starArtist(
        @QueryMap params: Map<String, String>,
        @Query("artistId") artistId: String
    ): Call<ApiResponse>

    @GET("unstar")
    fun unstarSong(
        @QueryMap params: Map<String, String>,
        @Query("id") id: String
    ): Call<ApiResponse>

    @GET("unstar")
    fun unstarAlbum(
        @QueryMap params: Map<String, String>,
        @Query("albumId") albumId: String
    ): Call<ApiResponse>

    @GET("unstar")
    fun unstarArtist(
        @QueryMap params: Map<String, String>,
        @Query("artistId") artistId: String
    ): Call<ApiResponse>

    @GET("setRating")
    fun setRating(
        @QueryMap params: Map<String, String>,
        @Query("id") id: String,
        @Query("rating") rating: Int
    ): Call<ApiResponse>

    @GET("scrobble")
    fun scrobble(
        @QueryMap params: Map<String, String>,
        @Query("id") id: String,
        @Query("submission") submission: Boolean?
    ): Call<ApiResponse>
}
