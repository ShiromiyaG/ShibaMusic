package com.shirou.shibamusic.subsonic.api.albumsonglist

import android.util.Log
import com.shirou.shibamusic.subsonic.RetrofitClient
import com.shirou.shibamusic.subsonic.Subsonic
import com.shirou.shibamusic.subsonic.base.ApiResponse
import retrofit2.Call

class AlbumSongListClient(private val subsonic: Subsonic) {

    private val albumSongListService: AlbumSongListService =
        RetrofitClient(subsonic).retrofit.create(AlbumSongListService::class.java)

    fun getAlbumList(type: String, size: Int, offset: Int): Call<ApiResponse> {
        Log.d(TAG, "getAlbumList()")
        return albumSongListService.getAlbumList(subsonic.params, type, size, offset)
    }

    fun getAlbumList2(type: String, size: Int, offset: Int, fromYear: Int?, toYear: Int?): Call<ApiResponse> {
        Log.d(TAG, "getAlbumList2()")
        return albumSongListService.getAlbumList2(subsonic.params, type, size, offset, fromYear, toYear)
    }

    fun getRandomSongs(size: Int, fromYear: Int?, toYear: Int?): Call<ApiResponse> {
        Log.d(TAG, "getRandomSongs()")
        return albumSongListService.getRandomSongs(subsonic.params, size, fromYear, toYear)
    }

    fun getSongsByGenre(genre: String, count: Int, offset: Int): Call<ApiResponse> {
        Log.d(TAG, "getSongsByGenre()")
        return albumSongListService.getSongsByGenre(subsonic.params, genre, count, offset)
    }

    fun getNowPlaying(): Call<ApiResponse> {
        Log.d(TAG, "getNowPlaying()")
        return albumSongListService.getNowPlaying(subsonic.params)
    }

    fun getStarred(): Call<ApiResponse> {
        Log.d(TAG, "getStarred()")
        return albumSongListService.getStarred(subsonic.params)
    }

    fun getStarred2(): Call<ApiResponse> {
        Log.d(TAG, "getStarred2()")
        return albumSongListService.getStarred2(subsonic.params)
    }

    companion object {
        private const val TAG = "AlbumSongListClient"
    }
}
