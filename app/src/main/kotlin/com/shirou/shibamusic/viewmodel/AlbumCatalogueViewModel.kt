package com.shirou.shibamusic.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.shirou.shibamusic.App
import com.shirou.shibamusic.interfaces.MediaCallback
import com.shirou.shibamusic.subsonic.base.ApiResponse
import com.shirou.shibamusic.subsonic.models.AlbumID3
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.ArrayList

class AlbumCatalogueViewModel(application: Application) : AndroidViewModel(application) {

    private val _albumList = MutableLiveData<List<AlbumID3>>(mutableListOf())
    val albumList: LiveData<List<AlbumID3>> get() = _albumList

    private val _loading = MutableLiveData(true)
    val loadingStatus: LiveData<Boolean> get() = _loading

    private var page = 0
    private var status = Status.STOPPED

    fun loadAlbums() {
        page = 0
        status = Status.RUNNING
        _albumList.value = mutableListOf()
        loadAlbums(500)
    }

    fun stopLoading() {
        status = Status.STOPPED
    }

    private fun loadAlbums(size: Int) {
        retrieveAlbums(object : MediaCallback {
            override fun onError(exception: Exception) {
                // The original Java code had an empty onError block.
            }

            override fun onLoadMedia(media: List<*>) {
                if (status == Status.STOPPED) {
                    _loading.value = false
                    return
                }

                val currentAlbumList = (_albumList.value ?: emptyList()).toMutableList()
                // The Java code performs an unchecked cast (List<AlbumID3>) media.
                // To maintain strict semantic equivalence (including potential ClassCastException),
                // we use 'as' instead of 'filterIsInstance'.
                val albumId3List = media as List<AlbumID3>
                currentAlbumList.addAll(albumId3List)
                _albumList.value = currentAlbumList

                if (media.size == size) {
                    loadAlbums(size)
                    _loading.value = true
                } else {
                    status = Status.STOPPED
                    _loading.value = false
                }
            }
        }, size, size * page++)
    }

    private fun retrieveAlbums(callback: MediaCallback, size: Int, offset: Int) {
        App.getSubsonicClientInstance(false)
            .albumSongListClient // Converted getter to property access
            .getAlbumList2("alphabeticalByName", size, offset, null, null)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    response.body()?.subsonicResponse?.albumList2?.albums?.let { albums ->
                        // The Java code creates a new ArrayList from the collection.
                        val albumList = ArrayList(albums)
                        callback.onLoadMedia(albumList)
                    }
                    // If any part of the chain is null or not successful, onLoadMedia is not called,
                    // which matches the Java behavior.
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    // t.message can be null, but Java's new Exception(null) is valid.
                    callback.onError(Exception(t.message))
                }
            })
    }

    private enum class Status {
        RUNNING,
        STOPPED
    }
}
