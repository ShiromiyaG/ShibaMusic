package com.shirou.shibamusic.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.shirou.shibamusic.repository.PodcastRepository
import com.shirou.shibamusic.subsonic.models.InternetRadioStation

class PodcastChannelEditorViewModel(application: Application) : AndroidViewModel(application) {

    private val podcastRepository: PodcastRepository = PodcastRepository()

    private var toEdit: InternetRadioStation? = null

    fun createChannel(url: String) {
        podcastRepository.createPodcastChannel(url)
    }

    companion object {
        private const val TAG = "RadioEditorViewModel"
    }
}
