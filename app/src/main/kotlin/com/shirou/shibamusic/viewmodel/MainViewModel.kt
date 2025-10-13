package com.shirou.shibamusic.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.shirou.shibamusic.github.models.LatestRelease
import com.shirou.shibamusic.repository.QueueRepository
import com.shirou.shibamusic.repository.SystemRepository
import com.shirou.shibamusic.subsonic.models.OpenSubsonicExtension
import com.shirou.shibamusic.subsonic.models.SubsonicResponse

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val systemRepository: SystemRepository = SystemRepository()

    fun isQueueLoaded(): Boolean {
        val queueRepository = QueueRepository()
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
        private const val TAG = "SearchViewModel"
    }
}
