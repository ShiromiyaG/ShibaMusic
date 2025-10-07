package com.shirou.shibamusic.repository

import androidx.lifecycle.MutableLiveData
import com.shirou.shibamusic.App
import com.shirou.shibamusic.subsonic.base.ApiResponse
import com.shirou.shibamusic.subsonic.models.Directory
import com.shirou.shibamusic.subsonic.models.Indexes
import com.shirou.shibamusic.subsonic.models.MusicFolder
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DirectoryRepository {

    companion object {
        private const val TAG = "DirectoryRepository"
    }

    fun getMusicFolders(): MutableLiveData<List<MusicFolder>> {
        val liveMusicFolders = MutableLiveData<List<MusicFolder>>()

        App.getSubsonicClientInstance(false)
            .browsingClient
            .getMusicFolders()
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful) {
                        response.body()?.subsonicResponse?.musicFolders?.musicFolders?.let {
                            liveMusicFolders.value = it
                        }
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    // No operation in original Java code
                }
            })

        return liveMusicFolders
    }

    fun getIndexes(musicFolderId: String?, ifModifiedSince: Long?): MutableLiveData<Indexes> {
        val liveIndexes = MutableLiveData<Indexes>()

        App.getSubsonicClientInstance(false)
            .browsingClient
            .getIndexes(musicFolderId, ifModifiedSince)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful) {
                        response.body()?.subsonicResponse?.indexes?.let {
                            liveIndexes.value = it
                        }
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    // No operation in original Java code
                }
            })

        return liveIndexes
    }

    fun getMusicDirectory(id: String): MutableLiveData<Directory> {
        val liveMusicDirectory = MutableLiveData<Directory>()

        App.getSubsonicClientInstance(false)
            .browsingClient
            .getMusicDirectory(id)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful) {
                        response.body()?.subsonicResponse?.directory?.let {
                            liveMusicDirectory.value = it
                        }
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    t.printStackTrace()
                }
            })

        return liveMusicDirectory
    }
}
