package com.shirou.shibamusic.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.shirou.shibamusic.repository.PlaylistRepository
import com.shirou.shibamusic.subsonic.models.Child
import com.shirou.shibamusic.subsonic.models.Playlist

class PlaylistPageViewModel(application: Application) : AndroidViewModel(application) {

    private val playlistRepository: PlaylistRepository = PlaylistRepository()

    lateinit var playlist: Playlist

    fun getPlaylistSongLiveList(): LiveData<List<Child>> {
        return playlistRepository.getPlaylistSongs(playlist.id)
    }

    fun isPinned(owner: LifecycleOwner): LiveData<Boolean> {
        val isPinnedLive = MutableLiveData<Boolean>()

        playlistRepository.getPinnedPlaylists().observe(owner) { playlists ->
            isPinnedLive.postValue(playlists.any { obj -> obj.id == playlist.id })
        }

        return isPinnedLive
    }

    fun setPinned(isNowPinned: Boolean) {
        if (isNowPinned) {
            playlistRepository.insert(playlist)
        } else {
            playlistRepository.delete(playlist)
        }
    }
}
