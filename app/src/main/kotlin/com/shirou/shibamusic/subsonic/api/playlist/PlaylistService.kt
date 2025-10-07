package com.shirou.shibamusic.subsonic.api.playlist

import com.shirou.shibamusic.subsonic.base.ApiResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.QueryMap

interface PlaylistService {
    @GET("getPlaylists")
    fun getPlaylists(@QueryMap params: Map<String, String>): Call<ApiResponse>

    @GET("getPlaylist")
    fun getPlaylist(
        @QueryMap params: Map<String, String>,
        @Query("id") id: String
    ): Call<ApiResponse>

    @GET("createPlaylist")
    fun createPlaylist(
        @QueryMap params: Map<String, String>,
        @Query("playlistId") playlistId: String,
        @Query("name") name: String,
        @Query("songId") songIds: List<String>
    ): Call<ApiResponse>

    @GET("updatePlaylist")
    fun updatePlaylist(
        @QueryMap params: Map<String, String>,
        @Query("playlistId") playlistId: String,
        @Query("name") name: String?,
        @Query("public") isPublic: Boolean,
        @Query("songIdToAdd") songIdsToAdd: List<String>?,
        @Query("songIndexToRemove") songIndicesToRemove: List<Int>?
    ): Call<ApiResponse>

    @GET("deletePlaylist")
    fun deletePlaylist(
        @QueryMap params: Map<String, String>,
        @Query("id") id: String
    ): Call<ApiResponse>
}
