package com.shirou.shibamusic.subsonic.api.browsing

import com.shirou.shibamusic.subsonic.base.ApiResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.QueryMap

interface BrowsingService {
    @GET("getMusicFolders")
    fun getMusicFolders(@QueryMap params: Map<String, String>): Call<ApiResponse>

    @GET("getIndexes")
    fun getIndexes(
        @QueryMap params: Map<String, String>,
        @Query("musicFolderId") musicFolderId: String?,
        @Query("ifModifiedSince") ifModifiedSince: Long?
    ): Call<ApiResponse>

    @GET("getMusicDirectory")
    fun getMusicDirectory(
        @QueryMap params: Map<String, String>,
        @Query("id") id: String
    ): Call<ApiResponse>

    @GET("getGenres")
    fun getGenres(@QueryMap params: Map<String, String>): Call<ApiResponse>

    @GET("getArtists")
    fun getArtists(@QueryMap params: Map<String, String>): Call<ApiResponse>

    @GET("getArtist")
    fun getArtist(
        @QueryMap params: Map<String, String>,
        @Query("id") id: String
    ): Call<ApiResponse>

    @GET("getAlbum")
    fun getAlbum(
        @QueryMap params: Map<String, String>,
        @Query("id") id: String
    ): Call<ApiResponse>

    @GET("getSong")
    fun getSong(
        @QueryMap params: Map<String, String>,
        @Query("id") id: String
    ): Call<ApiResponse>

    @GET("getVideos")
    fun getVideos(@QueryMap params: Map<String, String>): Call<ApiResponse>

    @GET("getVideoInfo")
    fun getVideoInfo(
        @QueryMap params: Map<String, String>,
        @Query("id") id: String
    ): Call<ApiResponse>

    @GET("getArtistInfo")
    fun getArtistInfo(
        @QueryMap params: Map<String, String>,
        @Query("id") id: String
    ): Call<ApiResponse>

    @GET("getArtistInfo2")
    fun getArtistInfo2(
        @QueryMap params: Map<String, String>,
        @Query("id") id: String
    ): Call<ApiResponse>

    @GET("getAlbumInfo")
    fun getAlbumInfo(
        @QueryMap params: Map<String, String>,
        @Query("id") id: String
    ): Call<ApiResponse>

    @GET("getAlbumInfo2")
    fun getAlbumInfo2(
        @QueryMap params: Map<String, String>,
        @Query("id") id: String
    ): Call<ApiResponse>

    @GET("getSimilarSongs")
    fun getSimilarSongs(
        @QueryMap params: Map<String, String>,
        @Query("id") id: String,
        @Query("count") count: Int
    ): Call<ApiResponse>

    @GET("getSimilarSongs2")
    fun getSimilarSongs2(
        @QueryMap params: Map<String, String>,
        @Query("id") id: String,
        @Query("count") count: Int
    ): Call<ApiResponse>

    @GET("getTopSongs")
    fun getTopSongs(
        @QueryMap params: Map<String, String>,
        @Query("artist") artist: String,
        @Query("count") count: Int
    ): Call<ApiResponse>
}
