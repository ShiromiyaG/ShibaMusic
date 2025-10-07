package com.shirou.shibamusic.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.shirou.shibamusic.repository.PlaylistRepository
import com.shirou.shibamusic.subsonic.models.Child
import com.shirou.shibamusic.subsonic.models.Playlist

class PlaylistChooserViewModel(application: Application) : AndroidViewModel(application) {

    private val playlistRepository = PlaylistRepository()

    private val _playlists = MutableLiveData<List<Playlist>?>(null)
    val playlists: LiveData<List<Playlist>?> get() = _playlists

    var songsToAdd: MutableList<Child> = mutableListOf()

    fun getPlaylistList(owner: LifecycleOwner): LiveData<List<Playlist>?> {
        playlistRepository.getPlaylists(false, -1).observe(owner) {
            _playlists.postValue(it)
        }
        return _playlists
    }

    fun addSongsToPlaylist(playlistId: String) {
        playlistRepository.addSongToPlaylist(playlistId, ArrayList(songsToAdd.map { it.id }))
    }
}
