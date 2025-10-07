package com.shirou.shibamusic.viewmodel

import android.app.Application
import android.app.Activity

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer

import com.shirou.shibamusic.repository.AlbumRepository
import com.shirou.shibamusic.subsonic.models.AlbumID3
import com.shirou.shibamusic.subsonic.models.Child

import java.util.concurrent.atomic.AtomicInteger

class StarredAlbumsSyncViewModel(application: Application) : AndroidViewModel(application) {
    private val albumRepository = AlbumRepository()

    private val starredAlbums = MutableLiveData<List<AlbumID3>>(emptyList())
    private val starredAlbumSongs = MutableLiveData<List<Child>>(emptyList())

    fun getStarredAlbums(owner: LifecycleOwner): LiveData<List<AlbumID3>> {
        albumRepository.getStarredAlbums(false, -1).observe(owner, starredAlbums::postValue)
        return starredAlbums
    }

    fun getAllStarredAlbumSongs(): LiveData<List<Child>> {
        val liveAlbums = albumRepository.getStarredAlbums(false, -1)
        val observer = object : Observer<List<AlbumID3>> {
            override fun onChanged(albums: List<AlbumID3>) {
                if (albums.isNotEmpty()) {
                    collectAllAlbumSongs(albums) { songs -> starredAlbumSongs.postValue(songs) }
                } else {
                    starredAlbumSongs.postValue(emptyList())
                }
                liveAlbums.removeObserver(this)
            }
        }
        liveAlbums.observeForever(observer)
        return starredAlbumSongs
    }

    fun getStarredAlbumSongs(activity: Activity): LiveData<List<Child>> {
        val liveAlbums = albumRepository.getStarredAlbums(false, -1)
        liveAlbums.observe(activity as LifecycleOwner) { albums ->
            if (albums.isNotEmpty()) {
                collectAllAlbumSongs(albums) { songs -> starredAlbumSongs.postValue(songs) }
            } else {
                starredAlbumSongs.postValue(emptyList())
            }
        }
        return starredAlbumSongs
    }

    private fun collectAllAlbumSongs(albums: List<AlbumID3>, callback: (List<Child>) -> Unit) {
        val albumIds = albums.mapNotNull { it.id }
        if (albumIds.isEmpty()) {
            callback(emptyList())
            return
        }

        val allSongs = mutableListOf<Child>()
        val pending = AtomicInteger(albumIds.size)

        albumIds.forEach { albumId ->
            val albumTracks = albumRepository.getAlbumTracks(albumId)
            val observer = object : Observer<List<Child>> {
                override fun onChanged(songs: List<Child>) {
                    allSongs.addAll(songs)
                    albumTracks.removeObserver(this)
                    if (pending.decrementAndGet() == 0) {
                        callback(allSongs)
                    }
                }
            }
            albumTracks.observeForever(observer)
        }
    }
}
