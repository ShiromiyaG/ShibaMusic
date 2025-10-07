package com.shirou.shibamusic.service

import android.content.ComponentName
import androidx.lifecycle.Observer
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaBrowser
import androidx.media3.session.SessionToken
import com.shirou.shibamusic.App
import com.shirou.shibamusic.interfaces.MediaIndexCallback
import com.shirou.shibamusic.model.Chronology
import com.shirou.shibamusic.repository.ChronologyRepository
import com.shirou.shibamusic.repository.QueueRepository
import com.shirou.shibamusic.repository.SongRepository
import com.shirou.shibamusic.subsonic.models.Child
import com.shirou.shibamusic.subsonic.models.InternetRadioStation
import com.shirou.shibamusic.subsonic.models.PodcastEpisode
import com.shirou.shibamusic.util.MappingUtil
import com.shirou.shibamusic.util.Preferences
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlin.OptIn

object MediaManager {

    private const val TAG = "MediaManager"

    private fun ListenableFuture<MediaBrowser>?.executeOnBrowser(action: (MediaBrowser) -> Unit) {
        this?.addListener(Runnable {
            runCatching {
                if (isDone) {
                    action(get())
                }
            }.onFailure { e ->
                e.printStackTrace()
            }
        }, MoreExecutors.directExecutor())
    }

    fun reset(mediaBrowserFuture: ListenableFuture<MediaBrowser>?) {
        mediaBrowserFuture.executeOnBrowser { mediaBrowser ->
            mediaBrowser.run {
                if (isPlaying) {
                    pause()
                }
                stop()
                clearMediaItems()
                clearDatabase()
            }
        }
    }

    fun hide(mediaBrowserFuture: ListenableFuture<MediaBrowser>?) {
        mediaBrowserFuture.executeOnBrowser { mediaBrowser ->
            if (mediaBrowser.isPlaying) {
                mediaBrowser.pause()
            }
        }
    }

    fun check(mediaBrowserFuture: ListenableFuture<MediaBrowser>?) {
        mediaBrowserFuture.executeOnBrowser { mediaBrowser ->
            if (mediaBrowser.mediaItemCount < 1) {
                val media = getQueueRepository().getMedia()
                if (!media.isNullOrEmpty()) {
                    init(mediaBrowserFuture, media)
                }
            }
        }
    }

    fun init(mediaBrowserFuture: ListenableFuture<MediaBrowser>?, media: List<Child>) {
        mediaBrowserFuture.executeOnBrowser { mediaBrowser ->
            mediaBrowser.run {
                clearMediaItems()
                setMediaItems(MappingUtil.mapMediaItems(media))
                seekTo(
                    getQueueRepository().getLastPlayedMediaIndex(),
                    getQueueRepository().getLastPlayedMediaTimestamp()
                )
                prepare()
            }
        }
    }

    fun startQueue(mediaBrowserFuture: ListenableFuture<MediaBrowser>?, media: List<Child>, startIndex: Int) {
        mediaBrowserFuture.executeOnBrowser { mediaBrowser ->
            mediaBrowser.run {
                clearMediaItems()
                setMediaItems(MappingUtil.mapMediaItems(media))
                prepare()
                seekTo(startIndex, 0)
                play()
                enqueueDatabase(media, true, 0)
            }
        }
    }

    fun startQueue(mediaBrowserFuture: ListenableFuture<MediaBrowser>?, media: Child) {
        mediaBrowserFuture.executeOnBrowser { mediaBrowser ->
            mediaBrowser.run {
                clearMediaItems()
                setMediaItem(MappingUtil.mapMediaItem(media))
                prepare()
                play()
                enqueueDatabase(media, true, 0)
            }
        }
    }

    fun startRadio(mediaBrowserFuture: ListenableFuture<MediaBrowser>?, internetRadioStation: InternetRadioStation) {
        mediaBrowserFuture.executeOnBrowser { mediaBrowser ->
            mediaBrowser.run {
                clearMediaItems()
                setMediaItem(MappingUtil.mapInternetRadioStation(internetRadioStation))
                prepare()
                play()
            }
        }
    }

    fun startPodcast(mediaBrowserFuture: ListenableFuture<MediaBrowser>?, podcastEpisode: PodcastEpisode) {
        mediaBrowserFuture.executeOnBrowser { mediaBrowser ->
            mediaBrowser.run {
                clearMediaItems()
                setMediaItem(MappingUtil.mapMediaItem(podcastEpisode))
                prepare()
                play()
            }
        }
    }

    fun enqueue(mediaBrowserFuture: ListenableFuture<MediaBrowser>?, media: List<Child>, playImmediatelyAfter: Boolean) {
        mediaBrowserFuture.executeOnBrowser { mediaBrowser ->
            mediaBrowser.run {
                val nextMediaItemIndex = nextMediaItemIndex
                val mediaItemCount = mediaItemCount
                if (playImmediatelyAfter && nextMediaItemIndex != -1) {
                    enqueueDatabase(media, false, nextMediaItemIndex)
                    addMediaItems(nextMediaItemIndex, MappingUtil.mapMediaItems(media))
                } else {
                    enqueueDatabase(media, false, mediaItemCount)
                    addMediaItems(MappingUtil.mapMediaItems(media))
                }
            }
        }
    }

    fun enqueue(mediaBrowserFuture: ListenableFuture<MediaBrowser>?, media: Child, playImmediatelyAfter: Boolean) {
        mediaBrowserFuture.executeOnBrowser { mediaBrowser ->
            mediaBrowser.run {
                val nextMediaItemIndex = nextMediaItemIndex
                val mediaItemCount = mediaItemCount
                if (playImmediatelyAfter && nextMediaItemIndex != -1) {
                    enqueueDatabase(media, false, nextMediaItemIndex)
                    addMediaItem(nextMediaItemIndex, MappingUtil.mapMediaItem(media))
                } else {
                    enqueueDatabase(media, false, mediaItemCount)
                    addMediaItem(MappingUtil.mapMediaItem(media))
                }
            }
        }
    }

    fun shuffle(mediaBrowserFuture: ListenableFuture<MediaBrowser>?, media: List<Child>, startIndex: Int, endIndex: Int) {
        mediaBrowserFuture.executeOnBrowser { mediaBrowser ->
            mediaBrowser.run {
                removeMediaItems(startIndex, endIndex + 1)
                addMediaItems(MappingUtil.mapMediaItems(media).subList(startIndex, endIndex + 1))
                swapDatabase(media)
            }
        }
    }

    fun swap(mediaBrowserFuture: ListenableFuture<MediaBrowser>?, media: List<Child>, from: Int, to: Int) {
        mediaBrowserFuture.executeOnBrowser { mediaBrowser ->
            mediaBrowser.run {
                moveMediaItem(from, to)
                swapDatabase(media)
            }
        }
    }

    fun remove(mediaBrowserFuture: ListenableFuture<MediaBrowser>?, media: MutableList<Child>, toRemove: Int) {
        mediaBrowserFuture.executeOnBrowser { mediaBrowser ->
            mediaBrowser.run {
                if (mediaItemCount > 1 && currentMediaItemIndex != toRemove) {
                    removeMediaItem(toRemove)
                    removeDatabase(media, toRemove)
                } else {
                    removeDatabase(media, -1)
                }
            }
        }
    }

    fun removeRange(mediaBrowserFuture: ListenableFuture<MediaBrowser>?, media: MutableList<Child>, fromItem: Int, toItem: Int) {
        mediaBrowserFuture.executeOnBrowser { mediaBrowser ->
            mediaBrowser.run {
                removeMediaItems(fromItem, toItem)
                removeRangeDatabase(media, fromItem, toItem)
            }
        }
    }

    fun getCurrentIndex(mediaBrowserFuture: ListenableFuture<MediaBrowser>?, callback: MediaIndexCallback) {
        mediaBrowserFuture.executeOnBrowser { mediaBrowser ->
            callback.onRecovery(mediaBrowser.currentMediaItemIndex)
        }
    }

    fun setLastPlayedTimestamp(mediaItem: MediaItem?) {
        mediaItem?.let {
            getQueueRepository().setLastPlayedTimestamp(it.mediaId)
        }
    }

    fun setPlayingPausedTimestamp(mediaItem: MediaItem?, ms: Long) {
        mediaItem?.let {
            getQueueRepository().setPlayingPausedTimestamp(it.mediaId, ms)
        }
    }

    fun scrobble(mediaItem: MediaItem?, submission: Boolean) {
        if (Preferences.isScrobblingEnabled()) {
            mediaItem?.let { item ->
                val id = item.mediaMetadata?.extras?.getString("id")
                if (id != null) {
                    getSongRepository().scrobble(id, submission)
                }
            }
        }
    }

    @OptIn(UnstableApi::class)
    fun continuousPlay(mediaItem: MediaItem?) {
        if (Preferences.isContinuousPlayEnabled() && Preferences.isInstantMixUsable()) {
            mediaItem?.let { item ->
                Preferences.setLastInstantMix()

                val instantMixLiveData = getSongRepository().getInstantMix(item.mediaId, 10)

                val observer = object : Observer<List<Child>?> {
                    override fun onChanged(media: List<Child>?) {
                        media?.let { nonNullMedia ->
                            val context = App.getContext()
                            val mediaBrowserFuture = MediaBrowser.Builder(
                                context,
                                SessionToken(context, ComponentName(context, MediaService::class.java))
                            ).buildAsync()

                            enqueue(mediaBrowserFuture, nonNullMedia, true)
                        }
                        instantMixLiveData.removeObserver(this)
                    }
                }
                instantMixLiveData.observeForever(observer)
            }
        }
    }

    fun saveChronology(mediaItem: MediaItem?) {
        mediaItem?.let {
            getChronologyRepository().insert(Chronology(it))
        }
    }

    private fun getQueueRepository(): QueueRepository {
        return QueueRepository()
    }

    private fun getSongRepository(): SongRepository {
        return SongRepository()
    }

    private fun getChronologyRepository(): ChronologyRepository {
        return ChronologyRepository()
    }

    private fun enqueueDatabase(media: List<Child>, reset: Boolean, afterIndex: Int) {
        getQueueRepository().insertAll(media, reset, afterIndex)
    }

    private fun enqueueDatabase(media: Child, reset: Boolean, afterIndex: Int) {
        getQueueRepository().insert(media, reset, afterIndex)
    }

    private fun swapDatabase(media: List<Child>) {
        getQueueRepository().insertAll(media, true, 0)
    }

    private fun removeDatabase(media: MutableList<Child>, toRemove: Int) {
        if (toRemove != -1) {
            media.removeAt(toRemove)
            getQueueRepository().insertAll(media, true, 0)
        }
    }

    private fun removeRangeDatabase(media: MutableList<Child>, fromItem: Int, toItem: Int) {
        val toRemove = media.subList(fromItem, toItem)
        media.removeAll(toRemove)

        getQueueRepository().insertAll(media, true, 0)
    }

    fun clearDatabase() {
        getQueueRepository().deleteAll()
    }
}
