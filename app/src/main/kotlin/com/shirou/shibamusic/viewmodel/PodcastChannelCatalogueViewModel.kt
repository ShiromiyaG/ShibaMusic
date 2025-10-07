package com.shirou.shibamusic.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.shirou.shibamusic.repository.PodcastRepository
import com.shirou.shibamusic.subsonic.models.PodcastChannel

class PodcastChannelCatalogueViewModel(application: Application) : AndroidViewModel(application) {
    private val podcastRepository = PodcastRepository()

    private val _podcastChannels = MutableLiveData<List<PodcastChannel>>(emptyList())

    fun getPodcastChannels(owner: LifecycleOwner): LiveData<List<PodcastChannel>> {
        if (_podcastChannels.value.isNullOrEmpty()) {
            podcastRepository.getPodcastChannels(false, null).observe(owner) {
                _podcastChannels.postValue(it)
            }
        }
        return _podcastChannels
    }
}
