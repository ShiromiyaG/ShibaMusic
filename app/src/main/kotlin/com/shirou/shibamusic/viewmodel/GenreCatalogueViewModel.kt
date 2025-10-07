package com.shirou.shibamusic.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.shirou.shibamusic.repository.GenreRepository
import com.shirou.shibamusic.subsonic.models.Genre

class GenreCatalogueViewModel(application: Application) : AndroidViewModel(application) {
    private val genreRepository = GenreRepository()

    fun getGenreList(): LiveData<List<Genre>> = genreRepository.getGenres(false, -1)
}
