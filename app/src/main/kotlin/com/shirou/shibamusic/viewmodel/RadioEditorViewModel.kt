package com.shirou.shibamusic.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.shirou.shibamusic.repository.RadioRepository
import com.shirou.shibamusic.subsonic.models.InternetRadioStation

class RadioEditorViewModel(application: Application) : AndroidViewModel(application) {

    private val radioRepository: RadioRepository = RadioRepository()

    var radioToEdit: InternetRadioStation? = null

    fun createRadio(name: String, streamURL: String, homepageURL: String?) {
        radioRepository.createInternetRadioStation(name, streamURL, homepageURL)
    }

    fun updateRadio(name: String, streamURL: String, homepageURL: String?) {
        val radio = radioToEdit ?: return
        val radioId = radio.id ?: return
        radioRepository.updateInternetRadioStation(radioId, name, streamURL, homepageURL)
    }

    fun deleteRadio() {
        val radioId = radioToEdit?.id ?: return
        radioRepository.deleteInternetRadioStation(radioId)
    }

    companion object {
        private const val TAG = "RadioEditorViewModel"
    }
}
