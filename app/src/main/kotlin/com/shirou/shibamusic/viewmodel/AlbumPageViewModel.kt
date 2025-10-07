package com.shirou.shibamusic.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

import com.shirou.shibamusic.repository.AlbumRepository
import com.shirou.shibamusic.repository.ArtistRepository
import com.shirou.shibamusic.subsonic.models.AlbumID3
import com.shirou.shibamusic.subsonic.models.AlbumInfo
import com.shirou.shibamusic.subsonic.models.ArtistID3
import com.shirou.shibamusic.subsonic.models.Child

class AlbumPageViewModel(application: Application) : AndroidViewModel(application) {

    private val albumRepository: AlbumRepository = AlbumRepository()
    private val artistRepository: ArtistRepository = ArtistRepository()

    private var albumId: String? = null
    private var artistId: String? = null

    private val _album = MutableLiveData<AlbumID3?>(null)
    private val emptyAlbumTracks = MutableLiveData<List<Child>>(emptyList())
    private val emptyArtist = MutableLiveData<ArtistID3?>(null)
    private val emptyAlbumInfo = MutableLiveData<AlbumInfo?>(null)

    val albumSongLiveList: LiveData<List<Child>>
        get() = albumId?.let { albumRepository.getAlbumTracks(it) } ?: emptyAlbumTracks

    val album: LiveData<AlbumID3?>
        get() = _album

    fun setAlbum(owner: LifecycleOwner, album: AlbumID3?) {
        val albumId = album?.id
        this.albumId = albumId
        this.artistId = album?.artistId
        _album.postValue(album)

        if (albumId.isNullOrEmpty()) {
            return
        }

        albumRepository.getAlbum(albumId).observe(owner) { fetchedAlbum ->
            fetchedAlbum?.let { _album.value = it }
        }
    }

    val artist: LiveData<ArtistID3?>
        get() = artistId?.let { artistRepository.getArtistInfo(it) } ?: emptyArtist

    val albumInfo: LiveData<AlbumInfo?>
        get() = albumId?.let { albumRepository.getAlbumInfo(it) } ?: emptyAlbumInfo
}
