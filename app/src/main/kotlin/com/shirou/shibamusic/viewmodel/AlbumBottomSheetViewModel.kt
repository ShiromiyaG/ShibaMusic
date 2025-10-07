package com.shirou.shibamusic.viewmodel

import android.app.Application
import android.content.Context

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer

import com.shirou.shibamusic.model.Download
import com.shirou.shibamusic.interfaces.StarCallback
import com.shirou.shibamusic.repository.AlbumRepository
import com.shirou.shibamusic.repository.ArtistRepository
import com.shirou.shibamusic.repository.FavoriteRepository
import com.shirou.shibamusic.repository.SharingRepository
import com.shirou.shibamusic.subsonic.models.AlbumID3
import com.shirou.shibamusic.subsonic.models.ArtistID3
import com.shirou.shibamusic.subsonic.models.Child
import com.shirou.shibamusic.subsonic.models.Share
import com.shirou.shibamusic.util.DownloadUtil
import com.shirou.shibamusic.util.MappingUtil
import com.shirou.shibamusic.util.NetworkUtil
import com.shirou.shibamusic.util.Preferences

import java.util.Date

class AlbumBottomSheetViewModel(application: Application) : AndroidViewModel(application) {
    private val albumRepository: AlbumRepository = AlbumRepository()
    private val artistRepository: ArtistRepository = ArtistRepository()
    private val favoriteRepository: FavoriteRepository = FavoriteRepository()
    private val sharingRepository: SharingRepository = SharingRepository()

    lateinit var album: AlbumID3

    private fun albumIdOrNull(): String? = album.id?.takeIf { it.isNotBlank() }

    fun getArtist(): LiveData<ArtistID3> = artistRepository.getArtist(album.artistId ?: "")

    fun getAlbumTracks(): MutableLiveData<List<Child>> = albumRepository.getAlbumTracks(album.id ?: "")

    fun setFavorite(context: Context) {
        if (album.starred != null) {
            if (NetworkUtil.isOffline()) {
                removeFavoriteOffline()
            } else {
                removeFavoriteOnline()
            }
        } else {
            if (NetworkUtil.isOffline()) {
                setFavoriteOffline()
            } else {
                setFavoriteOnline(context)
            }
        }
    }

    fun shareAlbum(): MutableLiveData<Share?> = sharingRepository.createShare(album.id ?: "", album.name, null)

    private fun removeFavoriteOffline() {
        val albumId = albumIdOrNull() ?: run {
            album.starred = null
            return
        }

        favoriteRepository.starLater(null, albumId, null, false)
        album.starred = null
    }

    private fun removeFavoriteOnline() {
        val albumId = albumIdOrNull() ?: run {
            album.starred = null
            return
        }

        favoriteRepository.unstar(null, albumId, null, object : StarCallback {
            override fun onError() {
                favoriteRepository.starLater(null, albumId, null, false)
            }
        })
        album.starred = null
    }

    private fun setFavoriteOffline() {
        val albumId = albumIdOrNull() ?: return

        favoriteRepository.starLater(null, albumId, null, true)
        album.starred = Date()
    }

    private fun setFavoriteOnline(context: Context) {
        val albumId = albumIdOrNull() ?: return

        favoriteRepository.star(null, albumId, null, object : StarCallback {
            override fun onError() {
                favoriteRepository.starLater(null, albumId, null, true)
            }
        })

        album.starred = Date()
        if (Preferences.isStarredAlbumsSyncEnabled()) {
            val albumRepository = AlbumRepository()
            val tracksLiveData = albumRepository.getAlbumTracks(albumId)

            tracksLiveData.observeForever(object : Observer<List<Child>> {
                override fun onChanged(songs: List<Child>) {
                    if (songs.isNotEmpty()) {
                        DownloadUtil.getDownloadTracker(context).download(
                            MappingUtil.mapDownloads(songs),
                            songs.map { child -> Download(child.id ?: "") }
                        )
                    }
                    tracksLiveData.removeObserver(this)
                }
            })
        }
    }
}
