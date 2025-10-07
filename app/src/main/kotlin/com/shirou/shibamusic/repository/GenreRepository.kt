package com.shirou.shibamusic.repository

import androidx.lifecycle.MutableLiveData
import com.shirou.shibamusic.App
import com.shirou.shibamusic.subsonic.base.ApiResponse
import com.shirou.shibamusic.subsonic.models.Genre
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class GenreRepository {
    fun getGenres(random: Boolean, size: Int): MutableLiveData<List<Genre>> {
        val genres = MutableLiveData<List<Genre>>(emptyList())

        App.getSubsonicClientInstance(false)
            .browsingClient
            .getGenres()
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful) {
                        val genreListFromResponse = response.body()
                            ?.subsonicResponse
                            ?.genres
                            ?.genres
                            ?: run {
                                genres.postValue(emptyList())
                                return
                            }

                        val genreList = genreListFromResponse.toMutableList()

                        if (genreList.isEmpty()) {
                            genres.postValue(emptyList())
                            return
                        }

                        if (random) {
                            genreList.shuffle()
                        }

                        val result = if (size != -1) {
                            genreList.take(minOf(size, genreList.size))
                        } else {
                            genreList.sortedBy { it.genre }
                        }

                        genres.postValue(result)
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    // Original Java code has an empty onFailure, so we mimic this behavior.
                }
            })

        return genres
    }
}
