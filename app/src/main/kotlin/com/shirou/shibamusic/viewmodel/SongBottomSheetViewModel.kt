package com.shirou.shibamusic.viewmodel

import android.app.Application
import android.content.Context

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.media3.common.util.UnstableApi

import com.shirou.shibamusic.interfaces.StarCallback
import com.shirou.shibamusic.model.Download
import com.shirou.shibamusic.repository.AlbumRepository
import com.shirou.shibamusic.repository.ArtistRepository
import com.shirou.shibamusic.repository.FavoriteRepository
import com.shirou.shibamusic.repository.SharingRepository
import com.shirou.shibamusic.repository.SongRepository
import com.shirou.shibamusic.subsonic.models.AlbumID3
import com.shirou.shibamusic.subsonic.models.ArtistID3
import com.shirou.shibamusic.subsonic.models.Child
import com.shirou.shibamusic.subsonic.models.Share
import com.shirou.shibamusic.util.DownloadUtil
import com.shirou.shibamusic.util.MappingUtil
import com.shirou.shibamusic.util.NetworkUtil
import com.shirou.shibamusic.util.Preferences

import java.util.Collections
import java.util.Date

@UnstableApi
class SongBottomSheetViewModel(application: Application) : AndroidViewModel(application) {
    private val songRepository: SongRepository
    private val albumRepository: AlbumRepository
    private val artistRepository: ArtistRepository
    private val favoriteRepository: FavoriteRepository
    private val sharingRepository: SharingRepository

    var song: Child? = null

    private val instantMix = MutableLiveData<List<Child>?>(null)

    init {
        songRepository = SongRepository()
        albumRepository = AlbumRepository()
        artistRepository = ArtistRepository()
        favoriteRepository = FavoriteRepository()
        sharingRepository = SharingRepository()
    }

    fun setFavorite(context: Context) {
        val media = song ?: return

        if (media.starred != null) {
            if (NetworkUtil.isOffline()) {
                removeFavoriteOffline(media)
            } else {
                removeFavoriteOnline(media)
            }
        } else {
            if (NetworkUtil.isOffline()) {
                setFavoriteOffline(media)
            } else {
                setFavoriteOnline(context, media)
            }
        }
    }

    private fun removeFavoriteOffline(media: Child) {
        favoriteRepository.starLater(media.id, null, null, false)
        media.starred = null
    }

    private fun songIdOrNull(media: Child?): String? = media?.id?.takeIf { it.isNotBlank() }

    private fun removeFavoriteOnline(media: Child) {
        val songId = songIdOrNull(media) ?: run {
            media.starred = null
            return
        }

        favoriteRepository.unstar(songId, null, null, object : StarCallback {
            override fun onError() {
                favoriteRepository.starLater(songId, null, null, false)
            }
        })
        media.starred = null
    }

    private fun setFavoriteOffline(media: Child) {
        val songId = songIdOrNull(media) ?: return

        favoriteRepository.starLater(songId, null, null, true)
        media.starred = Date()
    }

    private fun setFavoriteOnline(context: Context, media: Child) {
        val songId = songIdOrNull(media) ?: return

        favoriteRepository.star(songId, null, null, object : StarCallback {
            override fun onError() {
                favoriteRepository.starLater(songId, null, null, true)
            }
        })
        media.starred = Date()

        if (Preferences.isStarredSyncEnabled()) {
            DownloadUtil.getDownloadTracker(context).download(
                MappingUtil.mapDownload(media),
                Download(media)
            )
        }
    }

    fun getAlbum(): LiveData<AlbumID3> {
        val albumId = song?.albumId
        if (albumId.isNullOrBlank()) {
            return MutableLiveData<AlbumID3>()
        }
        return albumRepository.getAlbum(albumId)
    }

    fun getArtist(): LiveData<ArtistID3> {
        val artistId = song?.artistId
        if (artistId.isNullOrBlank()) {
            return MutableLiveData<ArtistID3>()
        }
        return artistRepository.getArtist(artistId)
    }

    fun getInstantMix(owner: LifecycleOwner, media: Child): LiveData<List<Child>?> {
        instantMix.value = Collections.emptyList()

        songRepository.getInstantMix(media.id, 20).observe(owner, instantMix::postValue)

        return instantMix
    }

    fun shareTrack(): MutableLiveData<Share?> {
        val media = song ?: return MutableLiveData<Share?>(null)
        return sharingRepository.createShare(media.id, media.title, null)
    }
}
