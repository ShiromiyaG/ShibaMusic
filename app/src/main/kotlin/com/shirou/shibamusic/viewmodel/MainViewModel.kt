package com.shirou.shibamusic.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.shirou.shibamusic.github.models.LatestRelease
import com.shirou.shibamusic.helper.ThemeHelper
import com.shirou.shibamusic.repository.QueueRepository
import com.shirou.shibamusic.repository.SystemRepository
import com.shirou.shibamusic.repository.UserPreferencesRepository
import com.shirou.shibamusic.subsonic.models.OpenSubsonicExtension
import com.shirou.shibamusic.subsonic.models.SubsonicResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val systemRepository: SystemRepository,
    private val queueRepository: QueueRepository,
    application: Application
) : AndroidViewModel(application) {

    val theme: StateFlow<String> = userPreferencesRepository.theme
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ThemeHelper.DEFAULT_MODE
        )

    fun isQueueLoaded(): Boolean {
        return queueRepository.count() != 0
    }

    fun ping(): LiveData<SubsonicResponse?> {
        return systemRepository.ping()
    }

    fun getOpenSubsonicExtensions(): LiveData<List<OpenSubsonicExtension>?> {
        return systemRepository.getOpenSubsonicExtensions()
    }

    fun checkShibaMusicUpdate(): LiveData<LatestRelease?> {
        return systemRepository.checkShibaMusicUpdate(getApplication())
    }

    companion object {
        private const val TAG = "MainViewModel"
    }
}
