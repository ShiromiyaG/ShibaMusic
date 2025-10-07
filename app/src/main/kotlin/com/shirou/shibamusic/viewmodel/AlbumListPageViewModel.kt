package com.shirou.shibamusic.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.shirou.shibamusic.repository.AlbumRepository
import com.shirou.shibamusic.repository.DownloadRepository
import com.shirou.shibamusic.subsonic.models.AlbumID3
import com.shirou.shibamusic.subsonic.models.ArtistID3
import com.shirou.shibamusic.util.Constants
import java.util.Calendar

class AlbumListPageViewModel(application: Application) : AndroidViewModel(application) {
    private val albumRepository: AlbumRepository = AlbumRepository()
    private val downloadRepository: DownloadRepository = DownloadRepository()

    lateinit var title: String
    var artist: ArtistID3? = null

    private var albumList: MutableLiveData<List<AlbumID3>> = MutableLiveData(emptyList())

    var maxNumber: Int = 500

    fun getAlbumList(owner: LifecycleOwner): LiveData<List<AlbumID3>> {
        if (albumList.value == null) {
            albumList.value = emptyList()
        }

        when (title) {
            Constants.ALBUM_RECENTLY_PLAYED ->
                albumRepository.getAlbums("recent", maxNumber, null, null).observe(owner) { albums ->
                    albumList.value = albums
                }
            Constants.ALBUM_MOST_PLAYED ->
                albumRepository.getAlbums("frequent", maxNumber, null, null).observe(owner) { albums ->
                    albumList.value = albums
                }
            Constants.ALBUM_RECENTLY_ADDED ->
                albumRepository.getAlbums("newest", maxNumber, null, null).observe(owner) { albums ->
                    albumList.value = albums
                }
            Constants.ALBUM_STARRED ->
                albumList = albumRepository.getStarredAlbums(false, -1)
            Constants.ALBUM_NEW_RELEASES -> {
                val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                albumRepository.getAlbums("byYear", maxNumber, currentYear, currentYear).observe(owner) { albums ->
                    val sortedAlbums = albums
                        .orEmpty()
                        .sortedByDescending { it.created }
                        .take(20)
                    albumList.postValue(sortedAlbums)
                }
            }
            Constants.ALBUM_FROM_ARTIST -> {
                val artistId = artist?.id
                if (artistId.isNullOrEmpty()) {
                    albumList.value = emptyList()
                } else {
                    albumRepository.getArtistAlbums(artistId).observe(owner) { albums ->
                        albumList.value = albums
                    }
                }
            }
        }

        return albumList
    }
}
