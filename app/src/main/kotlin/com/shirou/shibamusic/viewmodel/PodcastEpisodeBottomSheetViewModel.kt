package com.shirou.shibamusic.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.shirou.shibamusic.repository.PodcastRepository
import com.shirou.shibamusic.subsonic.models.PodcastEpisode

class PodcastEpisodeBottomSheetViewModel(application: Application) : AndroidViewModel(application) {

    private val podcastRepository: PodcastRepository = PodcastRepository()

    var podcastEpisode: PodcastEpisode? = null

    fun deletePodcastEpisode() {
        val episodeId = podcastEpisode?.id ?: return
        podcastRepository.deletePodcastEpisode(episodeId)
    }
}
