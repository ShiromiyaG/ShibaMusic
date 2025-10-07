package com.shirou.shibamusic.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.shirou.shibamusic.repository.SongRepository
import com.shirou.shibamusic.subsonic.models.Child

class StarredSyncViewModel(application: Application) : AndroidViewModel(application) {
    private val songRepository: SongRepository

    private val starredTracks: MutableLiveData<List<Child>?> = MutableLiveData(null)

    init {
        songRepository = SongRepository()
    }

    fun getStarredTracks(owner: LifecycleOwner): LiveData<List<Child>?> {
        songRepository.getStarredSongs(false, -1).observe(owner, starredTracks::postValue)
        return starredTracks
    }
}
