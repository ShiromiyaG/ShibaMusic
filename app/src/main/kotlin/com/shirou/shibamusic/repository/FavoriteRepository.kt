package com.shirou.shibamusic.repository

import com.shirou.shibamusic.App
import com.shirou.shibamusic.database.AppDatabase
import com.shirou.shibamusic.database.dao.FavoriteDao
import com.shirou.shibamusic.interfaces.StarCallback
import com.shirou.shibamusic.model.Favorite
import com.shirou.shibamusic.subsonic.base.ApiResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class FavoriteRepository {
    private val favoriteDao: FavoriteDao = AppDatabase.getInstance().favoriteDao()

    fun star(id: String?, albumId: String?, artistId: String?, starCallback: StarCallback) {
        val songId = id?.takeUnless { it.isBlank() }
        val cleanedAlbumId = albumId?.takeUnless { it.isBlank() }
        val cleanedArtistId = artistId?.takeUnless { it.isBlank() }

        if (songId == null && cleanedAlbumId == null && cleanedArtistId == null) {
            starCallback.onError()
            return
        }

        App.getSubsonicClientInstance(false)
            .mediaAnnotationClient
            .star(songId, cleanedAlbumId, cleanedArtistId)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful) {
                        starCallback.onSuccess()
                    } else {
                        starCallback.onError()
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    starCallback.onError()
                }
            })
    }

    fun unstar(id: String?, albumId: String?, artistId: String?, starCallback: StarCallback) {
        val songId = id?.takeUnless { it.isBlank() }
        val cleanedAlbumId = albumId?.takeUnless { it.isBlank() }
        val cleanedArtistId = artistId?.takeUnless { it.isBlank() }

        if (songId == null && cleanedAlbumId == null && cleanedArtistId == null) {
            starCallback.onError()
            return
        }

        App.getSubsonicClientInstance(false)
            .mediaAnnotationClient
            .unstar(songId, cleanedAlbumId, cleanedArtistId)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful) {
                        starCallback.onSuccess()
                    } else {
                        starCallback.onError()
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    starCallback.onError()
                }
            })
    }

    fun getFavorites(): List<Favorite> {
        var favorites: List<Favorite> = emptyList()

        val getAllThreadSafe = GetAllThreadSafe(favoriteDao)
        val thread = Thread(getAllThreadSafe)
        thread.start()

        try {
            thread.join()
            favorites = getAllThreadSafe.favorites
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        return favorites
    }

    private class GetAllThreadSafe(private val favoriteDao: FavoriteDao) : Runnable {
        var favorites: List<Favorite> = emptyList()
            private set

        override fun run() {
            favorites = favoriteDao.getAll()
        }
    }

    fun starLater(id: String?, albumId: String?, artistId: String?, toStar: Boolean) {
        val insert = InsertThreadSafe(favoriteDao, Favorite(System.currentTimeMillis(), id, albumId, artistId, toStar))
        Thread(insert).start()
    }

    private class InsertThreadSafe(private val favoriteDao: FavoriteDao, private val favorite: Favorite) : Runnable {
        override fun run() {
            favoriteDao.insert(favorite)
        }
    }

    fun delete(favorite: Favorite) {
        val delete = DeleteThreadSafe(favoriteDao, favorite)
        Thread(delete).start()
    }

    private class DeleteThreadSafe(private val favoriteDao: FavoriteDao, private val favorite: Favorite) : Runnable {
        override fun run() {
            favoriteDao.delete(favorite)
        }
    }
}
