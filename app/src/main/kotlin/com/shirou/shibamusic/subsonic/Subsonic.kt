package com.shirou.shibamusic.subsonic

import com.shirou.shibamusic.subsonic.api.albumsonglist.AlbumSongListClient
import com.shirou.shibamusic.subsonic.api.bookmarks.BookmarksClient
import com.shirou.shibamusic.subsonic.api.browsing.BrowsingClient
import com.shirou.shibamusic.subsonic.api.internetradio.InternetRadioClient
import com.shirou.shibamusic.subsonic.api.mediaannotation.MediaAnnotationClient
import com.shirou.shibamusic.subsonic.api.medialibraryscanning.MediaLibraryScanningClient
import com.shirou.shibamusic.subsonic.api.mediaretrieval.MediaRetrievalClient
import com.shirou.shibamusic.subsonic.api.open.OpenClient
import com.shirou.shibamusic.subsonic.api.playlist.PlaylistClient
import com.shirou.shibamusic.subsonic.api.podcast.PodcastClient
import com.shirou.shibamusic.subsonic.api.searching.SearchingClient
import com.shirou.shibamusic.subsonic.api.sharing.SharingClient
import com.shirou.shibamusic.subsonic.api.system.SystemClient
import com.shirou.shibamusic.subsonic.base.Version

class Subsonic(private val preferences: SubsonicPreferences) {

    companion object {
        private val API_MAX_VERSION = Version.of("1.15.0")
    }

    val apiVersion: Version = API_MAX_VERSION

    val systemClient: SystemClient by lazy { SystemClient(this) }
    val browsingClient: BrowsingClient by lazy { BrowsingClient(this) }
    val mediaRetrievalClient: MediaRetrievalClient by lazy { MediaRetrievalClient(this) }
    val playlistClient: PlaylistClient by lazy { PlaylistClient(this) }
    val searchingClient: SearchingClient by lazy { SearchingClient(this) }
    val albumSongListClient: AlbumSongListClient by lazy { AlbumSongListClient(this) }
    val mediaAnnotationClient: MediaAnnotationClient by lazy { MediaAnnotationClient(this) }
    val podcastClient: PodcastClient by lazy { PodcastClient(this) }
    val mediaLibraryScanningClient: MediaLibraryScanningClient by lazy { MediaLibraryScanningClient(this) }
    val bookmarksClient: BookmarksClient by lazy { BookmarksClient(this) }
    val internetRadioClient: InternetRadioClient by lazy { InternetRadioClient(this) }
    val sharingClient: SharingClient by lazy { SharingClient(this) }
    val openClient: OpenClient by lazy { OpenClient(this) }

    val url: String
        get() {
            val base = preferences.serverUrl.orEmpty()
            return ("$base/rest/").replace("//rest", "/rest")
        }

    val params: Map<String, String>
        get() {
            val parameters = mutableMapOf<String, String>()
            parameters["u"] = preferences.username.orEmpty()

            preferences.authentication?.password?.let { parameters["p"] = it }
            preferences.authentication?.salt?.let { parameters["s"] = it }
            preferences.authentication?.token?.let { parameters["t"] = it }

            parameters["v"] = apiVersion.versionString
            parameters["c"] = preferences.clientName
            parameters["f"] = "json"

            return parameters
        }
}
