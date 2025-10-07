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
import com.shirou.shibamusic.model.Queue
import com.shirou.shibamusic.repository.AlbumRepository
import com.shirou.shibamusic.repository.ArtistRepository
import com.shirou.shibamusic.repository.FavoriteRepository
import com.shirou.shibamusic.repository.OpenRepository
import com.shirou.shibamusic.repository.QueueRepository
import com.shirou.shibamusic.repository.SongRepository
import com.shirou.shibamusic.subsonic.models.AlbumID3
import com.shirou.shibamusic.subsonic.models.ArtistID3
import com.shirou.shibamusic.subsonic.models.Child
import com.shirou.shibamusic.subsonic.models.LyricsList
import com.shirou.shibamusic.subsonic.models.PlayQueue
import com.shirou.shibamusic.util.Constants
import com.shirou.shibamusic.util.DownloadUtil
import com.shirou.shibamusic.util.MappingUtil
import com.shirou.shibamusic.util.NetworkUtil
import com.shirou.shibamusic.util.OpenSubsonicExtensionsUtil
import com.shirou.shibamusic.util.Preferences
import java.util.Date
import kotlin.OptIn

@OptIn(UnstableApi::class)
class PlayerBottomSheetViewModel(application: Application) : AndroidViewModel(application) {
    private val songRepository: SongRepository
    private val albumRepository: AlbumRepository
    private val artistRepository: ArtistRepository
    private val queueRepository: QueueRepository
    private val favoriteRepository: FavoriteRepository
    private val openRepository: OpenRepository

    private val lyricsLiveData = MutableLiveData<String?>(null)
    private val lyricsListLiveData = MutableLiveData<LyricsList?>(null)
    private val descriptionLiveData = MutableLiveData<String?>(null)
    private val _liveMedia = MutableLiveData<Child?>(null)
    private val _liveAlbum = MutableLiveData<AlbumID3?>(null)
    private val _liveArtist = MutableLiveData<ArtistID3?>(null)
    private val _instantMix = MutableLiveData<List<Child>?>(null)
    private var lyricsSyncState = true

    private fun songIdOrNull(media: Child): String? = media.id.takeIf { it.isNotBlank() }

    init {
        songRepository = SongRepository()
        albumRepository = AlbumRepository()
        artistRepository = ArtistRepository()
        queueRepository = QueueRepository()
        favoriteRepository = FavoriteRepository()
        openRepository = OpenRepository()
    }

    val queueSong: LiveData<List<Queue>>
        get() = queueRepository.getLiveQueue()

    fun setFavorite(context: Context?, media: Child?) {
        media?.let {
            if (it.starred != null) {
                if (NetworkUtil.isOffline()) {
                    removeFavoriteOffline(it)
                } else {
                    removeFavoriteOnline(it)
                }
            } else {
                if (NetworkUtil.isOffline()) {
                    setFavoriteOffline(it)
                } else {
                    setFavoriteOnline(context, it)
                }
            }
        }
    }

    private fun removeFavoriteOffline(media: Child) {
        val songId = songIdOrNull(media)
        if (songId == null) {
            media.starred = null
            return
        }

        favoriteRepository.starLater(songId, null, null, false)
        media.starred = null
    }

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

    private fun setFavoriteOnline(context: Context?, media: Child) {
        val songId = songIdOrNull(media) ?: return

        favoriteRepository.star(songId, null, null, object : StarCallback {
            override fun onError() {
                favoriteRepository.starLater(songId, null, null, true)
            }
        })

        media.starred = Date()

        if (Preferences.isStarredSyncEnabled()) {
            context?.let { ctx ->
                DownloadUtil.getDownloadTracker(ctx).download(
                    MappingUtil.mapDownload(media),
                    Download(media)
                )
            }
        }
    }

    val liveLyrics: LiveData<String?>
        get() = lyricsLiveData

    val liveLyricsList: LiveData<LyricsList?>
        get() = lyricsListLiveData

    fun refreshMediaInfo(owner: LifecycleOwner, media: Child) {
        if (OpenSubsonicExtensionsUtil.isSongLyricsExtensionAvailable) {
            openRepository.getLyricsBySongId(media.id).observe(owner) { lyricsListLiveData.postValue(it) }
            lyricsLiveData.postValue(null)
        } else {
            songRepository.getSongLyrics(media).observe(owner) { lyricsLiveData.postValue(it) }
            lyricsListLiveData.postValue(null)
        }
    }

    val liveMedia: LiveData<Child?>
        get() = _liveMedia

    fun setLiveMedia(owner: LifecycleOwner, mediaType: String?, mediaId: String) {
        mediaType?.let { type ->
            when (type) {
                Constants.MEDIA_TYPE_MUSIC -> {
                    songRepository.getSong(mediaId).observe(owner) { _liveMedia.postValue(it) }
                    descriptionLiveData.postValue(null)
                }
                Constants.MEDIA_TYPE_PODCAST -> {
                    _liveMedia.postValue(null)
                }
            }
        }
    }

    val liveAlbum: LiveData<AlbumID3?>
        get() = _liveAlbum

    fun setLiveAlbum(owner: LifecycleOwner, mediaType: String?, albumId: String) {
        mediaType?.let { type ->
            when (type) {
                Constants.MEDIA_TYPE_MUSIC -> {
                    albumRepository.getAlbum(albumId).observe(owner) { _liveAlbum.postValue(it) }
                }
                Constants.MEDIA_TYPE_PODCAST -> {
                    _liveAlbum.postValue(null)
                }
            }
        }
    }

    val liveArtist: LiveData<ArtistID3?>
        get() = _liveArtist

    fun setLiveArtist(owner: LifecycleOwner, mediaType: String?, artistId: String) {
        mediaType?.let { type ->
            when (type) {
                Constants.MEDIA_TYPE_MUSIC -> {
                    artistRepository.getArtist(artistId).observe(owner) { _liveArtist.postValue(it) }
                }
                Constants.MEDIA_TYPE_PODCAST -> {
                    _liveArtist.postValue(null)
                }
            }
        }
    }

    fun setLiveDescription(description: String?) {
        descriptionLiveData.postValue(description)
    }

    val liveDescription: LiveData<String?>
        get() = descriptionLiveData

    fun getMediaInstantMix(owner: LifecycleOwner, media: Child): LiveData<List<Child>?> {
        _instantMix.value = emptyList()

        songRepository.getInstantMix(media.id, 20).observe(owner) { _instantMix.postValue(it) }

        return _instantMix
    }

    val playQueue: LiveData<PlayQueue?>
        get() = queueRepository.getPlayQueue()

    fun savePlayQueue(): Boolean {
        val media = _liveMedia.value ?: return false
        val queue = queueRepository.getMedia()
        val ids = queue.map { it.id }

        queueRepository.savePlayQueue(ids, media.id, 0)
        return true
    }

    fun changeSyncLyricsState() {
        lyricsSyncState = !lyricsSyncState
    }

    val syncLyricsState: Boolean
        get() = lyricsSyncState
}
