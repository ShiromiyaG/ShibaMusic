package com.shirou.shibamusic.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

import com.shirou.shibamusic.repository.PodcastRepository
import com.shirou.shibamusic.subsonic.models.PodcastChannel
import com.shirou.shibamusic.subsonic.models.PodcastEpisode

class PodcastViewModel(application: Application) : AndroidViewModel(application) {
    private val podcastRepository: PodcastRepository = PodcastRepository()

    private val newestPodcastEpisodes = MutableLiveData<List<PodcastEpisode>>(emptyList())
    private val podcastChannels = MutableLiveData<List<PodcastChannel>>(emptyList())

    fun getNewestPodcastEpisodes(owner: LifecycleOwner): LiveData<List<PodcastEpisode>> {
        if (newestPodcastEpisodes.value.isNullOrEmpty()) {
            podcastRepository.getNewestPodcastEpisodes(20).observe(owner, newestPodcastEpisodes::postValue)
        }
        return newestPodcastEpisodes
    }

    fun getPodcastChannels(owner: LifecycleOwner): LiveData<List<PodcastChannel>> {
        if (podcastChannels.value.isNullOrEmpty()) {
            podcastRepository.getPodcastChannels(false, null).observe(owner, podcastChannels::postValue)
        }
        return podcastChannels
    }

    fun refreshNewestPodcastEpisodes(owner: LifecycleOwner) {
        podcastRepository.getNewestPodcastEpisodes(20).observe(owner, newestPodcastEpisodes::postValue)
    }

    fun refreshPodcastChannels(owner: LifecycleOwner) {
        podcastRepository.getPodcastChannels(false, null).observe(owner, podcastChannels::postValue)
    }
}
