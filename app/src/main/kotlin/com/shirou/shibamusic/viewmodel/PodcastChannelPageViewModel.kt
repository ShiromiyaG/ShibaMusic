package com.shirou.shibamusic.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.shirou.shibamusic.repository.PodcastRepository
import com.shirou.shibamusic.subsonic.models.PodcastChannel
import com.shirou.shibamusic.subsonic.models.PodcastEpisode

class PodcastChannelPageViewModel(application: Application) : AndroidViewModel(application) {

    private val podcastRepository = PodcastRepository()

    lateinit var podcastChannel: PodcastChannel

    fun getPodcastChannelEpisodes(): LiveData<List<PodcastChannel>> {
        val channelId = podcastChannel.id ?: return MutableLiveData(emptyList())
        return podcastRepository.getPodcastChannels(true, channelId)
    }

    fun requestPodcastEpisodeDownload(podcastEpisode: PodcastEpisode) {
        val episodeId = podcastEpisode.id ?: return
        podcastRepository.downloadPodcastEpisode(episodeId)
    }
}
