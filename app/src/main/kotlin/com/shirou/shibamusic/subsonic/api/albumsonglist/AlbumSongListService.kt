package com.shirou.shibamusic.subsonic.api.albumsonglist

import com.shirou.shibamusic.subsonic.base.ApiResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.QueryMap

interface AlbumSongListService {
    @GET("getAlbumList")
    fun getAlbumList(
        @QueryMap params: Map<String, String>,
        @Query("type") type: String,
        @Query("size") size: Int,
        @Query("offset") offset: Int
    ): Call<ApiResponse>

    @GET("getAlbumList2")
    fun getAlbumList2(
        @QueryMap params: Map<String, String>,
        @Query("type") type: String,
        @Query("size") size: Int,
        @Query("offset") offset: Int,
        @Query("fromYear") fromYear: Int?,
        @Query("toYear") toYear: Int?
    ): Call<ApiResponse>

    @GET("getRandomSongs")
    fun getRandomSongs(
        @QueryMap params: Map<String, String>,
        @Query("size") size: Int,
        @Query("fromYear") fromYear: Int?,
        @Query("toYear") toYear: Int?
    ): Call<ApiResponse>

    @GET("getSongsByGenre")
    fun getSongsByGenre(
        @QueryMap params: Map<String, String>,
        @Query("genre") genre: String,
        @Query("count") count: Int,
        @Query("offset") offset: Int
    ): Call<ApiResponse>

    @GET("getNowPlaying")
    fun getNowPlaying(@QueryMap params: Map<String, String>): Call<ApiResponse>

    @GET("getStarred")
    fun getStarred(@QueryMap params: Map<String, String>): Call<ApiResponse>

    @GET("getStarred2")
    fun getStarred2(@QueryMap params: Map<String, String>): Call<ApiResponse>
}
