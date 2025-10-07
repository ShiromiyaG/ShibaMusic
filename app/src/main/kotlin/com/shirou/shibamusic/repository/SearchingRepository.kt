package com.shirou.shibamusic.repository

import androidx.lifecycle.MutableLiveData

import com.shirou.shibamusic.App
import com.shirou.shibamusic.database.AppDatabase
import com.shirou.shibamusic.database.dao.RecentSearchDao
import com.shirou.shibamusic.model.RecentSearch
import com.shirou.shibamusic.subsonic.base.ApiResponse
import com.shirou.shibamusic.subsonic.models.SearchResult2
import com.shirou.shibamusic.subsonic.models.SearchResult3

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SearchingRepository {
    private val recentSearchDao: RecentSearchDao = AppDatabase.getInstance().recentSearchDao()

    fun search2(query: String): MutableLiveData<SearchResult2> {
        val result = MutableLiveData<SearchResult2>()

        App.getSubsonicClientInstance(false)
            .searchingClient
            .search2(query, 20, 20, 20)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful) {
                        response.body()?.subsonicResponse?.searchResult2?.let {
                            result.postValue(it)
                        }
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    // Original Java code has an empty onFailure, so we maintain this behavior.
                }
            })
        return result
    }

    fun search3(query: String): MutableLiveData<SearchResult3> {
        val result = MutableLiveData<SearchResult3>()

        App.getSubsonicClientInstance(false)
            .searchingClient
            .search3(query, 20, 20, 20)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful) {
                        response.body()?.subsonicResponse?.searchResult3?.let {
                            result.postValue(it)
                        }
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    // Original Java code has an empty onFailure, so we maintain this behavior.
                }
            })
        return result
    }

    fun getSuggestions(query: String): MutableLiveData<List<String>> {
        val suggestions = MutableLiveData<List<String>>(emptyList())

        App.getSubsonicClientInstance(false)
            .searchingClient
            .search3(query, 5, 5, 5)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful) {
                        response.body()?.subsonicResponse?.searchResult3?.let { searchResult3 ->
                            val newSuggestions = buildList {
                                searchResult3.artists?.mapNotNullTo(this) { it.name }
                                searchResult3.albums?.mapNotNullTo(this) { it.name }
                                searchResult3.songs?.mapNotNullTo(this) { it.title }
                            }.distinct()

                            suggestions.postValue(newSuggestions)
                        }
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    // Original Java code has an empty onFailure, so we maintain this behavior.
                }
            })
        return suggestions
    }

    fun insert(recentSearch: RecentSearch) {
        // For simple Runnable tasks, a lambda is more idiomatic Kotlin.
        Thread {
            recentSearchDao.insert(recentSearch)
        }.start()
    }

    fun delete(recentSearch: RecentSearch) {
        // For simple Runnable tasks, a lambda is more idiomatic Kotlin.
        Thread {
            recentSearchDao.delete(recentSearch)
        }.start()
    }

    fun getRecentSearchSuggestion(): List<String> {
        val suggestionsThread = RecentThreadSafe(recentSearchDao)
        val thread = Thread(suggestionsThread)
        thread.start()

        try {
            thread.join()
        } catch (e: InterruptedException) {
            e.printStackTrace()
            Thread.currentThread().interrupt() // Restore the interrupted status as per best practices
        }
        return suggestionsThread.recent
    }

    // This nested class is kept because its state (the 'recent' list) needs to be
    // accessed after the thread has completed its execution, which is not easily
    // achievable with a simple lambda without shared mutable state complexity.
    private class RecentThreadSafe(
        private val recentSearchDao: RecentSearchDao
    ) : Runnable {
        var recent: List<String> = emptyList() // `var` is used as the list is reassigned within `run`

        override fun run() {
            recent = recentSearchDao.getRecent() // Assuming getRecent() is a method on the DAO
        }
    }
}
