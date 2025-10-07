package com.shirou.shibamusic.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

import com.shirou.shibamusic.repository.PlaylistRepository
import com.shirou.shibamusic.repository.SharingRepository
import com.shirou.shibamusic.subsonic.models.Child
import com.shirou.shibamusic.subsonic.models.Playlist
import com.shirou.shibamusic.subsonic.models.Share

import java.util.ArrayList

class PlaylistEditorViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "PlaylistEditorViewModel"

    private val playlistRepository = PlaylistRepository()
    private val sharingRepository = SharingRepository()

    // Maps to Java's `private ArrayList<Child> toAdd;` and its public getters/setters.
    var songsToAdd: ArrayList<Child>? = null

    // Maps to Java's `private Playlist toEdit;` and its public getters/setters, with custom logic in setter.
    var playlistToEdit: Playlist? = null
        set(value) {
            field = value // Assign to the backing field
            // Replicate Java's setter logic
            if (value != null) {
                // `value.id` is assumed non-null based on Java's usage.
                _songLiveList = playlistRepository.getPlaylistSongs(value.id)
            } else {
                _songLiveList = MutableLiveData()
            }
        }

    // This is the internal mutable LiveData instance.
    // Maps to Java's `private MutableLiveData<List<Child>> songLiveList = new MutableLiveData<>();`
    private var _songLiveList: MutableLiveData<List<Child>> = MutableLiveData()

    // Publicly exposed LiveData, providing read-only access to the internal mutable LiveData.
    // Maps to Java's `public LiveData<List<Child>> getPlaylistSongLiveList()`
    val playlistSongLiveList: LiveData<List<Child>>
        get() = _songLiveList

    fun createPlaylist(name: String) {
        // `Lists.transform(toAdd, Child::getId)` is converted to a Kotlin `map` operation.
        // `songsToAdd` is nullable, so a safe call `?.map` and Elvis operator `?: emptyList()` are used.
        // `Child.id` is assumed non-null from its typical usage.
        val songIds = songsToAdd?.map { it.id } ?: emptyList()
        playlistRepository.createPlaylist(null, name, ArrayList(songIds))
    }

    fun updatePlaylist(name: String) {
        // Java's `toEdit.getId()` implies `playlistToEdit` is non-null when this method is called.
        // Using `!!` mirrors Java's implicit `NullPointerException` if it were null.
        playlistRepository.updatePlaylist(playlistToEdit!!.id, name, getPlaylistSongIds())
    }

    fun deletePlaylist() {
        // Java's `if (toEdit != null)` is converted to a safe call `?.let`.
        playlistToEdit?.let { playlistRepository.deletePlaylist(it.id) }
    }

    fun removeFromPlaylistSongLiveList(position: Int) {
        val currentSongs = _songLiveList.value?.toMutableList() ?: return
        if (position in currentSongs.indices) {
            currentSongs.removeAt(position)
            _songLiveList.postValue(currentSongs)
        }
    }

    fun orderPlaylistSongLiveListAfterSwap(songs: List<Child>) {
        _songLiveList.postValue(songs)
    }

    private fun getPlaylistSongIds(): ArrayList<String> {
        // Kotlin equivalent of checking for null/empty list and then mapping.
        // `_songLiveList.value` is nullable, so safe calls `?.map` are used.
        // `Child.id` is assumed non-null.
        // `toCollection(ArrayList())` is used to ensure the return type is `ArrayList<String>` as in Java.
        // If `_songLiveList.value` is null, an empty `ArrayList` is returned.
        return _songLiveList.value?.map { it.id }?.toCollection(ArrayList()) ?: ArrayList()
    }

    fun sharePlaylist(): MutableLiveData<Share?> {
        // Java's `toEdit.getId(), toEdit.getName()` implies `playlistToEdit` is non-null.
        // Using `!!` mirrors Java's implicit `NullPointerException` if it were null.
        // `playlistToEdit.id` and `playlistToEdit.name` are assumed non-null.
        return sharingRepository.createShare(playlistToEdit!!.id, playlistToEdit!!.name, null)
    }
}
