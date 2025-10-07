package com.shirou.shibamusic.repository

import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.shirou.shibamusic.App
import com.shirou.shibamusic.R
import com.shirou.shibamusic.database.AppDatabase
import com.shirou.shibamusic.database.dao.PlaylistDao
import com.shirou.shibamusic.subsonic.base.ApiResponse
import com.shirou.shibamusic.subsonic.models.Child
import com.shirou.shibamusic.subsonic.models.Playlist
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PlaylistRepository {
    @androidx.media3.common.util.UnstableApi
    private val playlistDao: PlaylistDao = AppDatabase.getInstance().playlistDao()

    fun getPlaylists(random: Boolean, size: Int): MutableLiveData<List<Playlist>> {
        val liveData = MutableLiveData<List<Playlist>>(emptyList())

        App.getSubsonicClientInstance(false)
            .playlistClient
            .getPlaylists()
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    val playlists = response.body()
                        ?.subsonicResponse
                        ?.playlists
                        ?.playlists
                        ?: return

                    val result = if (random) {
                        playlists.shuffled().take(size)
                    } else {
                        playlists
                    }

                    liveData.postValue(result)
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    // Keep empty list on failure to mirror legacy behavior
                }
            })

        return liveData
    }

    fun getPlaylistSongs(id: String): MutableLiveData<List<Child>> {
        val liveData = MutableLiveData<List<Child>>(emptyList())

        App.getSubsonicClientInstance(false)
            .playlistClient
            .getPlaylist(id)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    val songs = response.body()
                        ?.subsonicResponse
                        ?.playlist
                        ?.entries
                        ?: return

                    liveData.postValue(songs)
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    // Keep empty list on failure to mirror legacy behavior
                }
            })

        return liveData
    }

    fun addSongToPlaylist(playlistId: String, songsId: List<String>) {
        App.getSubsonicClientInstance(false)
            .playlistClient
            .updatePlaylist(playlistId, null, true, songsId, null)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    val context = App.getContext()
                    Toast.makeText(
                        context,
                        context.getString(R.string.playlist_chooser_dialog_toast_add_success),
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    val context = App.getContext()
                    Toast.makeText(
                        context,
                        context.getString(R.string.playlist_chooser_dialog_toast_add_failure),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    fun createPlaylist(playlistId: String?, name: String, songsId: List<String>?) {
        App.getSubsonicClientInstance(false)
            .playlistClient
            .createPlaylist(
                playlistId.orEmpty(),
                name,
                songsId.orEmpty()
            )
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    // Handle response if needed, currently empty as per Java code
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    // Handle failure if needed, currently empty as per Java code
                }
            })
    }

    fun updatePlaylist(playlistId: String, name: String, songsId: List<String>?) {
        App.getSubsonicClientInstance(false)
            .playlistClient
            .deletePlaylist(playlistId)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    createPlaylist(null, name, songsId)
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    // Handle failure if needed, currently empty as per Java code
                }
            })
    }

    fun updatePlaylist(
        playlistId: String,
        name: String?,
        isPublic: Boolean,
        songIdToAdd: List<String>?,
        songIndexToRemove: List<Int>?
    ) {
        App.getSubsonicClientInstance(false)
            .playlistClient
            .updatePlaylist(
                playlistId,
                name.orEmpty(),
                isPublic,
                songIdToAdd.orEmpty(),
                songIndexToRemove.orEmpty()
            )
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    // Handle response if needed, currently empty as per Java code
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    // Handle failure if needed, currently empty as per Java code
                }
            })
    }

    fun deletePlaylist(playlistId: String) {
        App.getSubsonicClientInstance(false)
            .playlistClient
            .deletePlaylist(playlistId)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    // Handle response if needed, currently empty as per Java code
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    // Handle failure if needed, currently empty as per Java code
                }
            })
    }

    @androidx.media3.common.util.UnstableApi
    fun getPinnedPlaylists(): LiveData<List<Playlist>> {
        return playlistDao.getAll()
    }

    @androidx.media3.common.util.UnstableApi
    fun insert(playlist: Playlist) {
        val insert = InsertThreadSafe(playlistDao, playlist)
        Thread(insert).start()
    }

    @androidx.media3.common.util.UnstableApi
    fun delete(playlist: Playlist) {
        val delete = DeleteThreadSafe(playlistDao, playlist)
        Thread(delete).start()
    }

    private class InsertThreadSafe(private val playlistDao: PlaylistDao, private val playlist: Playlist) : Runnable {
        override fun run() {
            playlistDao.insert(playlist)
        }
    }

    private class DeleteThreadSafe(private val playlistDao: PlaylistDao, private val playlist: Playlist) : Runnable {
        override fun run() {
            playlistDao.delete(playlist)
        }
    }
}
