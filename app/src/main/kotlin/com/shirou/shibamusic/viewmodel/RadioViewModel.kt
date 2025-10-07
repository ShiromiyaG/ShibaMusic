package com.shirou.shibamusic.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.shirou.shibamusic.repository.RadioRepository
import com.shirou.shibamusic.subsonic.models.InternetRadioStation

class RadioViewModel(application: Application) : AndroidViewModel(application) {
    private val radioRepository: RadioRepository = RadioRepository()

    private val internetRadioStations = MutableLiveData<List<InternetRadioStation>>(emptyList())

    fun getInternetRadioStations(owner: LifecycleOwner): LiveData<List<InternetRadioStation>> {
        radioRepository.getInternetRadioStations().observe(owner, internetRadioStations::postValue)
        return internetRadioStations
    }

    fun refreshInternetRadioStations(owner: LifecycleOwner) {
        radioRepository.getInternetRadioStations().observe(owner, internetRadioStations::postValue)
    }
}
