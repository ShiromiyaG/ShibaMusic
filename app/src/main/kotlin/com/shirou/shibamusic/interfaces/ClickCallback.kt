package com.shirou.shibamusic.interfaces

import android.os.Bundle

import androidx.annotation.Keep

@Keep
interface ClickCallback {
    fun onMediaClick(bundle: Bundle) {}
    fun onMediaLongClick(bundle: Bundle) {}
    fun onAlbumClick(bundle: Bundle) {}
    fun onAlbumLongClick(bundle: Bundle) {}
    fun onArtistClick(bundle: Bundle) {}
    fun onArtistLongClick(bundle: Bundle) {}
    fun onGenreClick(bundle: Bundle) {}
    fun onPlaylistClick(bundle: Bundle) {}
    fun onPlaylistLongClick(bundle: Bundle) {}
    fun onYearClick(bundle: Bundle) {}
    fun onServerClick(bundle: Bundle) {}
    fun onServerLongClick(bundle: Bundle) {}
    fun onPodcastEpisodeClick(bundle: Bundle) {}
    fun onPodcastEpisodeAltClick(bundle: Bundle) {}
    fun onPodcastEpisodeLongClick(bundle: Bundle) {}
    fun onPodcastChannelClick(bundle: Bundle) {}
    fun onPodcastChannelLongClick(bundle: Bundle) {}
    fun onInternetRadioStationClick(bundle: Bundle) {}
    fun onInternetRadioStationLongClick(bundle: Bundle) {}
    fun onMusicFolderClick(bundle: Bundle) {}
    fun onMusicDirectoryClick(bundle: Bundle) {}
    fun onMusicIndexClick(bundle: Bundle) {}
    fun onDownloadGroupLongClick(bundle: Bundle) {}
    fun onShareClick(bundle: Bundle) {}
    fun onShareLongClick(bundle: Bundle) {}
}
