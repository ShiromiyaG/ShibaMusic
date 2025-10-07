package com.shirou.shibamusic.util

import android.net.Uri
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import com.shirou.shibamusic.App
import com.shirou.shibamusic.glide.CustomGlideRequest
import com.shirou.shibamusic.model.Download
import com.shirou.shibamusic.repository.DownloadRepository
import com.shirou.shibamusic.subsonic.models.Child
import com.shirou.shibamusic.subsonic.models.ArtistID3
import com.shirou.shibamusic.subsonic.models.InternetRadioStation
import com.shirou.shibamusic.subsonic.models.PodcastEpisode

@OptIn(UnstableApi::class)
object MappingUtil {

    fun mapMediaItems(items: List<Child>): List<MediaItem> {
        return items.map { mapMediaItem(it) }
    }

    fun mapMediaItem(media: Child): MediaItem {
        val uri = getUri(media)
        val artworkUri = media.coverArtId
            ?.let { CustomGlideRequest.createUrl(it, Preferences.getImageSize()) }
            ?.let(Uri::parse)

        val bundle = Bundle().apply {
            putString("id", media.id)
            putString("parentId", media.parentId)
            putBoolean("isDir", media.isDir)
            putString("title", media.title)
            putString("album", media.album)
            putString("artist", media.artist)
            putInt("track", media.track ?: 0)
            putInt("year", media.year ?: 0)
            putString("genre", media.genre)
            putString("coverArtId", media.coverArtId)
            putLong("size", media.size ?: 0)
            putString("contentType", media.contentType)
            putString("suffix", media.suffix)
            putString("transcodedContentType", media.transcodedContentType)
            putString("transcodedSuffix", media.transcodedSuffix)
            putInt("duration", media.duration ?: 0)
            putInt("bitrate", media.bitrate ?: 0)
            putInt("samplingRate", media.samplingRate ?: 0)
            putInt("bitDepth", media.bitDepth ?: 0)
            putString("path", media.path)
            putBoolean("isVideo", media.isVideo)
            putInt("userRating", media.userRating ?: 0)
            putDouble("averageRating", media.averageRating ?: 0.0)
            putLong("playCount", media.playCount ?: 0)
            putInt("discNumber", media.discNumber ?: 0)
            putLong("created", media.created?.time ?: 0)
            putLong("starred", media.starred?.time ?: 0)
            putString("albumId", media.albumId)
            putString("artistId", media.artistId)
            putString("type", Constants.MEDIA_TYPE_MUSIC)
            putLong("bookmarkPosition", media.bookmarkPosition ?: 0)
            putInt("originalWidth", media.originalWidth ?: 0)
            putInt("originalHeight", media.originalHeight ?: 0)
            putString("uri", uri.toString())
        }

        return MediaItem.Builder()
            .setMediaId(media.id)
            .setMediaMetadata(
                MediaMetadata.Builder().apply {
                    setTitle(media.title)
                    setTrackNumber(media.track ?: 0)
                    setDiscNumber(media.discNumber ?: 0)
                    setReleaseYear(media.year ?: 0)
                    setAlbumTitle(media.album)
                    setArtist(media.artist)
                    setArtworkUri(artworkUri)
                    setExtras(bundle)
                    setIsBrowsable(false)
                    setIsPlayable(true)
                }.build()
            )
            .setRequestMetadata(
                MediaItem.RequestMetadata.Builder().apply {
                    setMediaUri(uri)
                    setExtras(bundle)
                }.build()
            )
            .setMimeType(MimeTypes.BASE_TYPE_AUDIO)
            .setUri(uri)
            .build()
    }

    fun mapDownloads(items: List<Child>): List<MediaItem> {
        return items.map { mapDownload(it) }
    }

    fun mapDownload(media: Child): MediaItem {
        val downloadUri = if (Preferences.preferTranscodedDownload()) {
            MusicUtil.getTranscodedDownloadUri(media.id)
        } else {
            MusicUtil.getDownloadUri(media.id)
        }

        return MediaItem.Builder()
            .setMediaId(media.id)
            .setMediaMetadata(
                MediaMetadata.Builder().apply {
                    setTitle(media.title)
                    setTrackNumber(media.track ?: 0)
                    setDiscNumber(media.discNumber ?: 0)
                    setReleaseYear(media.year ?: 0)
                    setAlbumTitle(media.album)
                    setArtist(media.artist)
                    setIsBrowsable(false)
                    setIsPlayable(true)
                }.build()
            )
            .setRequestMetadata(
                MediaItem.RequestMetadata.Builder().apply {
                    setMediaUri(downloadUri)
                }.build()
            )
            .setMimeType(MimeTypes.BASE_TYPE_AUDIO)
            .setUri(downloadUri)
            .build()
    }

    fun mapInternetRadioStation(internetRadioStation: InternetRadioStation): MediaItem {
        val streamUrl = internetRadioStation.streamUrl
        val uri = when {
            streamUrl.isNullOrEmpty() -> Uri.EMPTY
            else -> Uri.parse(streamUrl)
        }

        val bundle = Bundle().apply {
            putString("id", internetRadioStation.id)
            putString("title", internetRadioStation.name)
            putString("uri", uri.toString())
            putString("type", Constants.MEDIA_TYPE_RADIO)
        }

        return MediaItem.Builder()
            .setMediaId(internetRadioStation.id ?: "")
            .setMediaMetadata(
                MediaMetadata.Builder().apply {
                    setTitle(internetRadioStation.name)
                    setExtras(bundle)
                    setIsBrowsable(false)
                    setIsPlayable(true)
                }.build()
            )
            .setRequestMetadata(
                MediaItem.RequestMetadata.Builder().apply {
                    setMediaUri(uri)
                    setExtras(bundle)
                }.build()
            )
            // .setMimeType(MimeTypes.BASE_TYPE_AUDIO) // Original code had this commented out
            .setUri(uri)
            .build()
    }

    fun mapMediaItem(podcastEpisode: PodcastEpisode): MediaItem {
        val uri = getUri(podcastEpisode)
        val artworkUri = podcastEpisode.coverArtId
            ?.let { CustomGlideRequest.createUrl(it, Preferences.getImageSize()) }
            ?.let(Uri::parse)

        val bundle = Bundle().apply {
            putString("id", podcastEpisode.id)
            putString("parentId", podcastEpisode.parentId)
            putBoolean("isDir", podcastEpisode.isDir)
            putString("title", podcastEpisode.title)
            putString("album", podcastEpisode.album)
            putString("artist", podcastEpisode.artist)
            putInt("year", podcastEpisode.year ?: 0)
            putString("coverArtId", podcastEpisode.coverArtId)
            putLong("size", podcastEpisode.size ?: 0)
            putString("contentType", podcastEpisode.contentType)
            putString("suffix", podcastEpisode.suffix)
            putInt("duration", podcastEpisode.duration ?: 0)
            putInt("bitrate", podcastEpisode.bitrate ?: 0)
            putBoolean("isVideo", podcastEpisode.isVideo)
            putLong("created", podcastEpisode.created?.time ?: 0)
            putString("artistId", podcastEpisode.artistId)
            putString("description", podcastEpisode.description)
            putString("type", Constants.MEDIA_TYPE_PODCAST)
            putString("uri", uri.toString())
        }

        return MediaItem.Builder()
            .setMediaId(podcastEpisode.id ?: "")
            .setMediaMetadata(
                MediaMetadata.Builder().apply {
                    setTitle(podcastEpisode.title)
                    setReleaseYear(podcastEpisode.year ?: 0)
                    setAlbumTitle(podcastEpisode.album)
                    setArtist(podcastEpisode.artist)
                    setArtworkUri(artworkUri)
                    setExtras(bundle)
                    setIsBrowsable(false)
                    setIsPlayable(true)
                }.build()
            )
            .setRequestMetadata(
                MediaItem.RequestMetadata.Builder().apply {
                    setMediaUri(uri)
                    setExtras(bundle)
                }.build()
            )
            .setMimeType(MimeTypes.BASE_TYPE_AUDIO)
            .setUri(uri)
            .build()
    }

    private fun getUri(media: Child): Uri {
        return if (DownloadUtil.getDownloadTracker(App.getContext()).isDownloaded(media.id)) {
            getDownloadUri(media.id)
        } else {
            MusicUtil.getStreamUri(media.id)
        }
    }

    private fun getUri(podcastEpisode: PodcastEpisode): Uri {
        val streamId = podcastEpisode.streamId ?: podcastEpisode.id ?: return Uri.EMPTY
        return if (DownloadUtil.getDownloadTracker(App.getContext()).isDownloaded(streamId)) {
            getDownloadUri(streamId)
        } else {
            MusicUtil.getStreamUri(streamId)
        }
    }

    private fun getDownloadUri(id: String): Uri {
        val download = DownloadRepository().getDownload(id)
        return download?.downloadUri?.takeIf { it.isNotEmpty() }?.let { Uri.parse(it) }
            ?: MusicUtil.getDownloadUri(id)
    }

    fun mapDownloadsToArtists(downloads: List<Download>): List<ArtistID3> {
        if (downloads.isEmpty()) return emptyList()

        return downloads.mapNotNull { download ->
            val artistId = download.artistId
            val artistName = download.artist

            if (artistId.isNullOrEmpty() && artistName.isNullOrEmpty()) {
                null
            } else {
                ArtistID3().apply {
                    id = artistId ?: artistName ?: ""
                    name = artistName ?: artistId
                    coverArtId = download.coverArtId
                }
            }
        }
    }
}
