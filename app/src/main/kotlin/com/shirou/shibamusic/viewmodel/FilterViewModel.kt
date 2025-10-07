package com.shirou.shibamusic.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.shirou.shibamusic.repository.GenreRepository
import com.shirou.shibamusic.subsonic.models.Genre

class FilterViewModel(application: Application) : AndroidViewModel(application) {

    private val genreRepository: GenreRepository = GenreRepository()

    private val selectedFiltersID: ArrayList<String> = ArrayList()
    private val selectedFilters: ArrayList<String> = ArrayList()

    val filters: ArrayList<String>
        get() = selectedFiltersID

    val filterNames: ArrayList<String>
        get() = selectedFilters

    fun getGenreList(): LiveData<List<Genre>> = genreRepository.getGenres(false, -1)

    fun addFilter(filterID: String, filterName: String) {
        selectedFiltersID.add(filterID)
        selectedFilters.add(filterName)
    }

    fun removeFilter(filterID: String, filterName: String) {
        selectedFiltersID.remove(filterID)
        selectedFilters.remove(filterName)
    }
}
