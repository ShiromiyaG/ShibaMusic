package com.shirou.shibamusic.subsonic.api.playlist

import android.util.Log
import com.shirou.shibamusic.subsonic.RetrofitClient
import com.shirou.shibamusic.subsonic.Subsonic
import com.shirou.shibamusic.subsonic.base.ApiResponse
import retrofit2.Call

class PlaylistClient(private val subsonic: Subsonic) {

    private val playlistService: PlaylistService =
        RetrofitClient(subsonic).retrofit.create(PlaylistService::class.java)

    fun getPlaylists(): Call<ApiResponse> {
        Log.d(TAG, "getPlaylists()")
        return playlistService.getPlaylists(subsonic.params)
    }

    fun getPlaylist(id: String): Call<ApiResponse> {
        Log.d(TAG, "getPlaylist()")
        return playlistService.getPlaylist(subsonic.params, id)
    }

    fun createPlaylist(playlistId: String, name: String, songIds: List<String>): Call<ApiResponse> {
        Log.d(TAG, "createPlaylist()")
        return playlistService.createPlaylist(subsonic.params, playlistId, name, songIds)
    }

    fun updatePlaylist(
        playlistId: String,
        name: String?,
        isPublic: Boolean,
        songIdsToAdd: List<String>?,
        songIndicesToRemove: List<Int>?
    ): Call<ApiResponse> {
        Log.d(TAG, "updatePlaylist()")
        return playlistService.updatePlaylist(
            subsonic.params,
            playlistId,
            name,
            isPublic,
            songIdsToAdd,
            songIndicesToRemove
        )
    }

    fun deletePlaylist(id: String): Call<ApiResponse> {
        Log.d(TAG, "deletePlaylist()")
        return playlistService.deletePlaylist(subsonic.params, id)
    }

    companion object {
        private const val TAG = "PlaylistClient"
    }
}
