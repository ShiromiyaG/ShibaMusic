package com.shirou.shibamusic.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.shirou.shibamusic.repository.PlaylistRepository
import com.shirou.shibamusic.subsonic.models.Playlist

class PlaylistCatalogueViewModel(application: Application) : AndroidViewModel(application) {

    private val playlistRepository: PlaylistRepository = PlaylistRepository()

    var type: String? = null

    private val playlistList: MutableLiveData<List<Playlist>?> = MutableLiveData(null)

    fun getPlaylistList(owner: LifecycleOwner): LiveData<List<Playlist>?> {
        if (playlistList.value == null) {
            playlistRepository.getPlaylists(false, -1).observe(owner, playlistList::postValue)
        }
        return playlistList
    }
}
