package com.shirou.shibamusic.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.shirou.shibamusic.model.RecentSearch
import com.shirou.shibamusic.repository.SearchingRepository
import com.shirou.shibamusic.subsonic.models.SearchResult2
import com.shirou.shibamusic.subsonic.models.SearchResult3

class SearchViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "SearchViewModel"

    var query: String = ""
        set(value) {
            field = value
            if (value.isNotEmpty()) {
                insertNewSearch(value)
            }
        }

    private val searchingRepository: SearchingRepository

    init {
        searchingRepository = SearchingRepository()
    }

    fun search2(title: String): LiveData<SearchResult2> {
        return searchingRepository.search2(title)
    }

    fun search3(title: String): LiveData<SearchResult3> {
        return searchingRepository.search3(title)
    }

    fun insertNewSearch(search: String) {
        searchingRepository.insert(RecentSearch(search))
    }

    fun deleteRecentSearch(search: String) {
        searchingRepository.delete(RecentSearch(search))
    }

    fun getSearchSuggestion(query: String): LiveData<List<String>> {
        return searchingRepository.getSuggestions(query)
    }

    fun getRecentSearchSuggestion(): List<String> {
        return ArrayList(searchingRepository.getRecentSearchSuggestion())
    }
}
