package com.shirou.shibamusic.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.shirou.shibamusic.repository.PodcastRepository
import com.shirou.shibamusic.subsonic.models.PodcastChannel

class PodcastChannelBottomSheetViewModel(application: Application) : AndroidViewModel(application) {

    private val podcastRepository: PodcastRepository = PodcastRepository()

    var podcastChannel: PodcastChannel? = null

    fun deletePodcastChannel() {
        val channelId = podcastChannel?.id ?: return
        podcastRepository.deletePodcastChannel(channelId)
    }
}
