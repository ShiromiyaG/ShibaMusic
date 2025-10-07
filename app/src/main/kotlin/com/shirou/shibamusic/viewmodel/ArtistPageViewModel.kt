package com.shirou.shibamusic.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.shirou.shibamusic.repository.AlbumRepository
import com.shirou.shibamusic.repository.ArtistRepository
import com.shirou.shibamusic.subsonic.models.AlbumID3
import com.shirou.shibamusic.subsonic.models.ArtistID3
import com.shirou.shibamusic.subsonic.models.ArtistInfo2
import com.shirou.shibamusic.subsonic.models.Child

class ArtistPageViewModel(application: Application) : AndroidViewModel(application) {
    private val albumRepository = AlbumRepository()
    private val artistRepository = ArtistRepository()

    var artist: ArtistID3? = null

    private val emptyAlbums = MutableLiveData<List<AlbumID3>>(emptyList())
    private val emptySongs = MutableLiveData<List<Child>>(emptyList())
    private val emptyArtistInfo = MutableLiveData<ArtistInfo2?>(null)

    fun getAlbumList(): LiveData<List<AlbumID3>> {
        val artistId = artist?.id
        return if (artistId.isNullOrEmpty()) {
            emptyAlbums
        } else {
            albumRepository.getArtistAlbums(artistId)
        }
    }

    fun getArtistInfo(id: String?): LiveData<ArtistInfo2?> {
        return if (id.isNullOrEmpty()) {
            emptyArtistInfo
        } else {
            artistRepository.getArtistFullInfo(id)
        }
    }

    fun getArtistTopSongList(): LiveData<List<Child>> {
        val artistName = artist?.name
        return if (artistName.isNullOrEmpty()) {
            emptySongs
        } else {
            artistRepository.getTopSongs(artistName, 20)
        }
    }

    fun getArtistShuffleList(): LiveData<List<Child>> {
        val artistData = artist
        return if (artistData == null) {
            emptySongs
        } else {
            artistRepository.getRandomSong(artistData, 50)
        }
    }

    fun getArtistInstantMix(): LiveData<List<Child>> {
        val artistData = artist
        return if (artistData == null) {
            emptySongs
        } else {
            artistRepository.getInstantMix(artistData, 20)
        }
    }
}
