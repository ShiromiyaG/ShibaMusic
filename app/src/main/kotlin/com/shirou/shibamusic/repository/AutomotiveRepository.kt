package com.shirou.shibamusic.repository

import android.net.Uri
import androidx.lifecycle.Observer
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.LibraryResult
import com.shirou.shibamusic.App
import com.shirou.shibamusic.database.AppDatabase
import com.shirou.shibamusic.database.dao.SessionMediaItemDao
import com.shirou.shibamusic.glide.CustomGlideRequest
import com.shirou.shibamusic.model.Chronology
import com.shirou.shibamusic.model.SessionMediaItem
import com.shirou.shibamusic.subsonic.base.ApiResponse
import com.shirou.shibamusic.subsonic.models.Child
import com.shirou.shibamusic.subsonic.models.InternetRadioStation
import com.shirou.shibamusic.subsonic.models.PodcastEpisode
import com.shirou.shibamusic.util.MappingUtil
import com.shirou.shibamusic.util.MusicUtil
import com.shirou.shibamusic.util.Preferences
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.OptIn

class AutomotiveRepository {
    private val sessionMediaItemDao = AppDatabase.getInstance().sessionMediaItemDao()
    private val chronologyDao = AppDatabase.getInstance().chronologyDao()

    fun getAlbums(prefix: String, type: String, size: Int): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        val listenableFuture = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()

        App.getSubsonicClientInstance(false)
            .albumSongListClient
            .getAlbumList2(type, size, 0, null, null)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful) {
                        response.body()?.subsonicResponse?.albumList2?.albums?.let { albums ->
                            val mediaItems = albums.mapNotNull { album ->
                                val albumId = album.id ?: return@mapNotNull null
                                val artworkUri = CustomGlideRequest
                                    .createUrl(album.coverArtId, Preferences.getImageSize())
                                    ?.let(Uri::parse)

                                val mediaMetadata = MediaMetadata.Builder().apply {
                                    setTitle(album.name)
                                    setAlbumTitle(album.name)
                                    setArtist(album.artist)
                                    setGenre(album.genre)
                                    setIsBrowsable(true)
                                    setIsPlayable(false)
                                    setMediaType(MediaMetadata.MEDIA_TYPE_ALBUM)
                                    artworkUri?.let(::setArtworkUri)
                                }.build()

                                MediaItem.Builder().apply {
                                    setMediaId(prefix + albumId)
                                    setMediaMetadata(mediaMetadata)
                                    setUri("")
                                }.build()
                            }
                            val libraryResult = LibraryResult.ofItemList(ImmutableList.copyOf(mediaItems), null)
                            listenableFuture.set(libraryResult)
                        } ?: listenableFuture.set(LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE))
                    } else {
                        listenableFuture.set(LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE))
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    listenableFuture.setException(t)
                }
            })

        return listenableFuture
    }

    fun getStarredSongs(): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        val listenableFuture = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()

        App.getSubsonicClientInstance(false)
            .albumSongListClient
            .getStarred2()
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful) {
                        response.body()?.subsonicResponse?.starred2?.songs?.let { songs ->
                            setChildrenMetadata(songs)
                            val mediaItems = MappingUtil.mapMediaItems(songs)
                            val libraryResult = LibraryResult.ofItemList(ImmutableList.copyOf(mediaItems), null)
                            listenableFuture.set(libraryResult)
                        } ?: listenableFuture.set(LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE))
                    } else {
                        listenableFuture.set(LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE))
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    listenableFuture.setException(t)
                }
            })

        return listenableFuture
    }

    fun getRandomSongs(count: Int): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        val listenableFuture = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()

        App.getSubsonicClientInstance(false)
            .albumSongListClient
            .getRandomSongs(100, null, null)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful) {
                        response.body()?.subsonicResponse?.randomSongs?.songs?.let { songs ->
                            setChildrenMetadata(songs)
                            val mediaItems = MappingUtil.mapMediaItems(songs)
                            val libraryResult = LibraryResult.ofItemList(ImmutableList.copyOf(mediaItems), null)
                            listenableFuture.set(libraryResult)
                        } ?: listenableFuture.set(LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE))
                    } else {
                        listenableFuture.set(LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE))
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    listenableFuture.setException(t)
                }
            })

        return listenableFuture
    }

    fun getRecentlyPlayedSongs(server: String, count: Int): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        val listenableFuture = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()

        val liveData = chronologyDao.getLastPlayed(server, count)
        lateinit var observer: Observer<List<Chronology>>
        observer = Observer { chronologyList ->
            if (!chronologyList.isNullOrEmpty()) {
                val songs: List<Child> = chronologyList
                setChildrenMetadata(songs)
                val mediaItems = MappingUtil.mapMediaItems(songs)
                val libraryResult = LibraryResult.ofItemList(ImmutableList.copyOf(mediaItems), null)
                listenableFuture.set(libraryResult)
            } else {
                listenableFuture.set(LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE))
            }
            liveData.removeObserver(observer)
        }
        liveData.observeForever(observer)

        return listenableFuture
    }

    fun getStarredAlbums(prefix: String): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        val listenableFuture = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()

        App.getSubsonicClientInstance(false)
            .albumSongListClient
            .getStarred2()
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful) {
                        response.body()?.subsonicResponse?.starred2?.albums?.let { albums ->
                            val mediaItems = albums.mapNotNull { album ->
                                val albumId = album.id ?: return@mapNotNull null
                                val artworkUri = CustomGlideRequest
                                    .createUrl(album.coverArtId, Preferences.getImageSize())
                                    ?.let(Uri::parse)

                                val mediaMetadata = MediaMetadata.Builder().apply {
                                    setTitle(album.name)
                                    setAlbumTitle(album.name)
                                    setArtist(album.artist)
                                    setGenre(album.genre)
                                    setIsBrowsable(true)
                                    setIsPlayable(false)
                                    setMediaType(MediaMetadata.MEDIA_TYPE_ALBUM)
                                    artworkUri?.let(::setArtworkUri)
                                }.build()

                                MediaItem.Builder().apply {
                                    setMediaId(prefix + albumId)
                                    setMediaMetadata(mediaMetadata)
                                    setUri("")
                                }.build()
                            }
                            val libraryResult = LibraryResult.ofItemList(ImmutableList.copyOf(mediaItems), null)
                            listenableFuture.set(libraryResult)
                        } ?: listenableFuture.set(LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE))
                    } else {
                        listenableFuture.set(LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE))
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    // Original Java code had an empty onFailure block here.
                    // To maintain semantic equivalence, we keep it empty.
                }
            })

        return listenableFuture
    }

    fun getStarredArtists(prefix: String): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        val listenableFuture = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()

        App.getSubsonicClientInstance(false)
            .albumSongListClient
            .getStarred2()
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful) {
                        response.body()?.subsonicResponse?.starred2?.artists?.let { artists ->
                            val shuffledArtists = artists.toMutableList().apply { shuffle() }

                            val mediaItems = shuffledArtists.mapNotNull { artist ->
                                val artistId = artist.id ?: return@mapNotNull null
                                val artworkUri = CustomGlideRequest
                                    .createUrl(artist.coverArtId, Preferences.getImageSize())
                                    ?.let(Uri::parse)

                                val mediaMetadata = MediaMetadata.Builder().apply {
                                    setTitle(artist.name)
                                    setIsBrowsable(true)
                                    setIsPlayable(false)
                                    setMediaType(MediaMetadata.MEDIA_TYPE_PLAYLIST)
                                    artworkUri?.let(::setArtworkUri)
                                }.build()

                                MediaItem.Builder().apply {
                                    setMediaId(prefix + artistId)
                                    setMediaMetadata(mediaMetadata)
                                    setUri("")
                                }.build()
                            }
                            val libraryResult = LibraryResult.ofItemList(ImmutableList.copyOf(mediaItems), null)
                            listenableFuture.set(libraryResult)
                        } ?: listenableFuture.set(LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE))
                    } else {
                        listenableFuture.set(LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE))
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    listenableFuture.setException(t)
                }
            })

        return listenableFuture
    }

    fun getMusicFolders(prefix: String): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        val listenableFuture = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()

        App.getSubsonicClientInstance(false)
            .browsingClient
            .getMusicFolders()
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful) {
                        response.body()?.subsonicResponse?.musicFolders?.musicFolders?.let { musicFolders ->
                            val mediaItems = musicFolders.map { musicFolder ->
                                val mediaMetadata = MediaMetadata.Builder().apply {
                                    setTitle(musicFolder.name)
                                    setIsBrowsable(true)
                                    setIsPlayable(false)
                                    setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                                }.build()

                                MediaItem.Builder().apply {
                                    setMediaId(prefix + musicFolder.id)
                                    setMediaMetadata(mediaMetadata)
                                    setUri("")
                                }.build()
                            }
                            val libraryResult = LibraryResult.ofItemList(ImmutableList.copyOf(mediaItems), null)
                            listenableFuture.set(libraryResult)
                        } ?: listenableFuture.set(LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE))
                    } else {
                        listenableFuture.set(LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE))
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    listenableFuture.setException(t)
                }
            })

        return listenableFuture
    }

    fun getIndexes(prefix: String, id: String): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        val listenableFuture = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()

        App.getSubsonicClientInstance(false)
            .browsingClient
            .getIndexes(id, null)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful) {
                        response.body()?.subsonicResponse?.indexes?.let { indexes ->
                            val mediaItems = mutableListOf<MediaItem>()

                            indexes.indices?.let { indices ->
                                for (index in indices) {
                                    index.artists?.let { artists ->
                                        for (artist in artists) {
                                            val artistId = artist.id ?: continue
                                            val mediaMetadata = MediaMetadata.Builder().apply {
                                                setTitle(artist.name)
                                                setIsBrowsable(true)
                                                setMediaType(MediaMetadata.MEDIA_TYPE_ARTIST)
                                            }.build()

                                            MediaItem.Builder().apply {
                                                setMediaId(prefix + artistId)
                                                setMediaMetadata(mediaMetadata)
                                                setUri("")
                                            }.build().also { mediaItems.add(it) }
                                        }
                                    }
                                }
                            }

                            indexes.children?.let { children ->
                                for (song in children) {
                                    val songId = song.id ?: continue
                                    val streamUri = MusicUtil.getStreamUri(songId)
                                    val artworkUri = CustomGlideRequest
                                        .createUrl(song.coverArtId, Preferences.getImageSize())
                                        ?.let(Uri::parse)

                                    val mediaMetadata = MediaMetadata.Builder().apply {
                                        setTitle(song.title)
                                        setAlbumTitle(song.album)
                                        setArtist(song.artist)
                                        setIsBrowsable(false)
                                        setIsPlayable(true)
                                        setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                                        artworkUri?.let(::setArtworkUri)
                                    }.build()

                                    MediaItem.Builder().apply {
                                        setMediaId(prefix + songId)
                                        setMediaMetadata(mediaMetadata)
                                        setUri(streamUri)
                                    }.build().also { mediaItems.add(it) }
                                }
                                setChildrenMetadata(children)
                            }

                            val libraryResult = LibraryResult.ofItemList(ImmutableList.copyOf(mediaItems), null)
                            listenableFuture.set(libraryResult)
                        } ?: listenableFuture.set(LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE))
                    } else {
                        listenableFuture.set(LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE))
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    listenableFuture.setException(t)
                }
            })

        return listenableFuture
    }

    fun getDirectories(prefix: String, id: String): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        val listenableFuture = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()

        App.getSubsonicClientInstance(false)
            .browsingClient
            .getMusicDirectory(id)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful) {
                        response.body()?.subsonicResponse?.directory?.children?.let { children ->
                            val mediaItems = children.mapNotNull { child ->
                                val childId = child.id ?: return@mapNotNull null
                                val artworkUri = CustomGlideRequest
                                    .createUrl(child.coverArtId, Preferences.getImageSize())
                                    ?.let(Uri::parse)

                                val mediaMetadata = MediaMetadata.Builder().apply {
                                    setTitle(child.title)
                                    setIsBrowsable(child.isDir)
                                    setIsPlayable(!child.isDir)
                                    setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                                    artworkUri?.let(::setArtworkUri)
                                }.build()

                                MediaItem.Builder().apply {
                                    setMediaId(if (child.isDir) prefix + childId else childId)
                                    setMediaMetadata(mediaMetadata)
                                    setUri(if (!child.isDir) MusicUtil.getStreamUri(childId) else Uri.parse(""))
                                }.build()
                            }

                            setChildrenMetadata(children.filter { !it.isDir })

                            val libraryResult = LibraryResult.ofItemList(ImmutableList.copyOf(mediaItems), null)
                            listenableFuture.set(libraryResult)
                        } ?: listenableFuture.set(LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE))
                    } else {
                        listenableFuture.set(LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE))
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    listenableFuture.setException(t)
                }
            })

        return listenableFuture
    }

    fun getPlaylists(prefix: String): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        val listenableFuture = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()

        App.getSubsonicClientInstance(false)
            .playlistClient
            .getPlaylists()
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful) {
                        response.body()?.subsonicResponse?.playlists?.playlists?.let { playlists ->
                            val mediaItems = playlists.mapNotNull { playlist ->
                                val playlistId = playlist.id ?: return@mapNotNull null
                                val mediaMetadata = MediaMetadata.Builder().apply {
                                    setTitle(playlist.name)
                                    setIsBrowsable(true)
                                    setIsPlayable(false)
                                    setMediaType(MediaMetadata.MEDIA_TYPE_PLAYLIST)
                                }.build()

                                MediaItem.Builder().apply {
                                    setMediaId(prefix + playlistId)
                                    setMediaMetadata(mediaMetadata)
                                    setUri("")
                                }.build()
                            }
                            val libraryResult = LibraryResult.ofItemList(ImmutableList.copyOf(mediaItems), null)
                            listenableFuture.set(libraryResult)
                        } ?: listenableFuture.set(LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE))
                    } else {
                        listenableFuture.set(LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE))
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    listenableFuture.setException(t)
                }
            })

        return listenableFuture
    }

    fun getNewestPodcastEpisodes(count: Int): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        val listenableFuture = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()

        App.getSubsonicClientInstance(false)
            .podcastClient
            .getNewestPodcasts(count)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful) {
                        response.body()?.subsonicResponse?.newestPodcasts?.episodes?.let { episodes ->
                            val mediaItems = episodes.mapNotNull { episode ->
                                val episodeId = episode.id ?: return@mapNotNull null
                                val streamId = episode.streamId ?: return@mapNotNull null
                                val artworkUri = CustomGlideRequest
                                    .createUrl(episode.coverArtId, Preferences.getImageSize())
                                    ?.let(Uri::parse)

                                val mediaMetadata = MediaMetadata.Builder().apply {
                                    setTitle(episode.title)
                                    setIsBrowsable(false)
                                    setIsPlayable(true)
                                    setMediaType(MediaMetadata.MEDIA_TYPE_PODCAST_EPISODE)
                                    artworkUri?.let(::setArtworkUri)
                                }.build()

                                MediaItem.Builder().apply {
                                    setMediaId(episodeId)
                                    setMediaMetadata(mediaMetadata)
                                    setUri(MusicUtil.getStreamUri(streamId))
                                }.build()
                            }

                            setPodcastEpisodesMetadata(episodes)

                            val libraryResult = LibraryResult.ofItemList(ImmutableList.copyOf(mediaItems), null)
                            listenableFuture.set(libraryResult)
                        } ?: listenableFuture.set(LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE))
                    } else {
                        listenableFuture.set(LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE))
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    listenableFuture.setException(t)
                }
            })

        return listenableFuture
    }

    fun getInternetRadioStations(): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        val listenableFuture = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()

        App.getSubsonicClientInstance(false)
            .internetRadioClient
            .getInternetRadioStations()
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful) {
                        response.body()?.subsonicResponse?.internetRadioStations?.internetRadioStations?.let { radioStations ->
                            val mediaItems = radioStations.mapNotNull { radioStation ->
                                val stationId = radioStation.id ?: return@mapNotNull null
                                val streamUrl = radioStation.streamUrl ?: return@mapNotNull null

                                val mediaMetadata = MediaMetadata.Builder().apply {
                                    setTitle(radioStation.name)
                                    setIsBrowsable(false)
                                    setIsPlayable(true)
                                    setMediaType(MediaMetadata.MEDIA_TYPE_RADIO_STATION)
                                }.build()

                                MediaItem.Builder().apply {
                                    setMediaId(stationId)
                                    setMediaMetadata(mediaMetadata)
                                    setUri(streamUrl)
                                }.build()
                            }

                            setInternetRadioStationsMetadata(radioStations)

                            val libraryResult = LibraryResult.ofItemList(ImmutableList.copyOf(mediaItems), null)
                            listenableFuture.set(libraryResult)
                        } ?: listenableFuture.set(LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE))
                    } else {
                        listenableFuture.set(LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE))
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    listenableFuture.setException(t)
                }
            })

        return listenableFuture
    }

    fun getAlbumTracks(id: String): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        val listenableFuture = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()

        App.getSubsonicClientInstance(false)
            .browsingClient
            .getAlbum(id)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful) {
                        response.body()?.subsonicResponse?.album?.songs?.let { tracks ->
                            setChildrenMetadata(tracks)
                            val mediaItems = MappingUtil.mapMediaItems(tracks)
                            val libraryResult = LibraryResult.ofItemList(ImmutableList.copyOf(mediaItems), null)
                            listenableFuture.set(libraryResult)
                        } ?: listenableFuture.set(LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE))
                    } else {
                        listenableFuture.set(LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE))
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    listenableFuture.setException(t)
                }
            })

        return listenableFuture
    }

    fun getArtistAlbum(prefix: String, id: String): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        val listenableFuture = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()

        App.getSubsonicClientInstance(false)
            .browsingClient
            .getArtist(id)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful) {
                        response.body()?.subsonicResponse?.artist?.albums?.let { albums ->
                            val mediaItems = albums.mapNotNull { album ->
                                val albumId = album.id ?: return@mapNotNull null
                                val artworkUri = CustomGlideRequest
                                    .createUrl(album.coverArtId, Preferences.getImageSize())
                                    ?.let(Uri::parse)

                                val mediaMetadata = MediaMetadata.Builder().apply {
                                    setTitle(album.name)
                                    setAlbumTitle(album.name)
                                    setArtist(album.artist)
                                    setGenre(album.genre)
                                    setIsBrowsable(true)
                                    setIsPlayable(false)
                                    setMediaType(MediaMetadata.MEDIA_TYPE_ALBUM)
                                    artworkUri?.let(::setArtworkUri)
                                }.build()

                                MediaItem.Builder().apply {
                                    setMediaId(prefix + albumId)
                                    setMediaMetadata(mediaMetadata)
                                    setUri("")
                                }.build()
                            }
                            val libraryResult = LibraryResult.ofItemList(ImmutableList.copyOf(mediaItems), null)
                            listenableFuture.set(libraryResult)
                        } ?: listenableFuture.set(LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE))
                    } else {
                        listenableFuture.set(LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE))
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    listenableFuture.setException(t)
                }
            })

        return listenableFuture
    }

    fun getPlaylistSongs(id: String): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        val listenableFuture = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()

        App.getSubsonicClientInstance(false)
            .playlistClient
            .getPlaylist(id)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful) {
                        response.body()?.subsonicResponse?.playlist?.entries?.let { tracks ->
                            setChildrenMetadata(tracks)
                            val mediaItems = MappingUtil.mapMediaItems(tracks)
                            val libraryResult = LibraryResult.ofItemList(ImmutableList.copyOf(mediaItems), null)
                            listenableFuture.set(libraryResult)
                        } ?: listenableFuture.set(LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE))
                    } else {
                        listenableFuture.set(LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE))
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    listenableFuture.setException(t)
                }
            })

        return listenableFuture
    }

    fun getMadeForYou(id: String, count: Int): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        val listenableFuture = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()

        App.getSubsonicClientInstance(false)
            .browsingClient
            .getSimilarSongs2(id, count)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful) {
                        response.body()?.subsonicResponse?.similarSongs2?.songs?.let { tracks ->
                            setChildrenMetadata(tracks)
                            val mediaItems = MappingUtil.mapMediaItems(tracks)
                            val libraryResult = LibraryResult.ofItemList(ImmutableList.copyOf(mediaItems), null)
                            listenableFuture.set(libraryResult)
                        } ?: listenableFuture.set(LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE))
                    } else {
                        listenableFuture.set(LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE))
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    listenableFuture.setException(t)
                }
            })

        return listenableFuture
    }

    fun search(query: String, albumPrefix: String, artistPrefix: String): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        val listenableFuture = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()

        App.getSubsonicClientInstance(false)
            .searchingClient
            .search3(query, 20, 20, 20)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful) {
                        response.body()?.subsonicResponse?.searchResult3?.let { searchResult ->
                            val mediaItems = mutableListOf<MediaItem>()

                            searchResult.artists?.let { artists ->
                                for (artist in artists) {
                                    val artistId = artist.id ?: continue
                                    val artworkUri =
                                        CustomGlideRequest.createUrl(artist.coverArtId, Preferences.getImageSize())?.let(Uri::parse)

                                    val mediaMetadata = MediaMetadata.Builder().apply {
                                        setTitle(artist.name)
                                        setIsBrowsable(true)
                                        setIsPlayable(false)
                                        setMediaType(MediaMetadata.MEDIA_TYPE_PLAYLIST)
                                        artworkUri?.let(::setArtworkUri)
                                    }.build()

                                    MediaItem.Builder().apply {
                                        setMediaId(artistPrefix + artistId)
                                        setMediaMetadata(mediaMetadata)
                                        setUri("")
                                    }.build().also { mediaItems.add(it) }
                                }
                            }

                            searchResult.albums?.let { albums ->
                                for (album in albums) {
                                    val albumId = album.id ?: continue
                                    val artworkUri =
                                        CustomGlideRequest.createUrl(album.coverArtId, Preferences.getImageSize())?.let(Uri::parse)

                                    val mediaMetadata = MediaMetadata.Builder().apply {
                                        setTitle(album.name)
                                        setAlbumTitle(album.name)
                                        setArtist(album.artist)
                                        setGenre(album.genre)
                                        setIsBrowsable(true)
                                        setIsPlayable(false)
                                        setMediaType(MediaMetadata.MEDIA_TYPE_ALBUM)
                                        artworkUri?.let(::setArtworkUri)
                                    }.build()

                                    MediaItem.Builder().apply {
                                        setMediaId(albumPrefix + albumId)
                                        setMediaMetadata(mediaMetadata)
                                        setUri("")
                                    }.build().also { mediaItems.add(it) }
                                }
                            }

                            searchResult.songs?.let { tracks ->
                                setChildrenMetadata(tracks)
                                mediaItems.addAll(MappingUtil.mapMediaItems(tracks))
                            }

                            val libraryResult = LibraryResult.ofItemList(ImmutableList.copyOf(mediaItems), null)
                            listenableFuture.set(libraryResult)
                        } ?: listenableFuture.set(LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE))
                    } else {
                        listenableFuture.set(LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE))
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    listenableFuture.setException(t)
                }
            })

        return listenableFuture
    }

    @OptIn(UnstableApi::class)
    fun setChildrenMetadata(children: List<Child>) {
        val timestamp = System.currentTimeMillis()
        val sessionMediaItems = children.map { child ->
            SessionMediaItem(child).apply {
                this.timestamp = timestamp
            }
        }
        Thread(InsertAllThreadSafe(sessionMediaItemDao, sessionMediaItems)).start()
    }

    @OptIn(UnstableApi::class)
    fun setPodcastEpisodesMetadata(podcastEpisodes: List<PodcastEpisode>) {
        val timestamp = System.currentTimeMillis()
        val sessionMediaItems = podcastEpisodes.map { podcastEpisode ->
            SessionMediaItem(podcastEpisode).apply {
                this.timestamp = timestamp
            }
        }
        Thread(InsertAllThreadSafe(sessionMediaItemDao, sessionMediaItems)).start()
    }

    @OptIn(UnstableApi::class)
    fun setInternetRadioStationsMetadata(internetRadioStations: List<InternetRadioStation>) {
        val timestamp = System.currentTimeMillis()
        val sessionMediaItems = internetRadioStations.map { internetRadioStation ->
            SessionMediaItem(internetRadioStation).apply {
                this.timestamp = timestamp
            }
        }
        Thread(InsertAllThreadSafe(sessionMediaItemDao, sessionMediaItems)).start()
    }

    fun getSessionMediaItem(id: String): SessionMediaItem? {
        val getMediaItemThreadSafe = GetMediaItemThreadSafe(sessionMediaItemDao, id)
        val thread = Thread(getMediaItemThreadSafe)
        thread.start()

        runCatching {
            thread.join()
        }.onFailure { e ->
            e.printStackTrace()
        }

        return getMediaItemThreadSafe.sessionMediaItem
    }

    fun getMetadatas(timestamp: Long): List<MediaItem> {
        val getMediaItemsThreadSafe = GetMediaItemsThreadSafe(sessionMediaItemDao, timestamp)
        val thread = Thread(getMediaItemsThreadSafe)
        thread.start()

        runCatching {
            thread.join()
        }.onFailure { e ->
            e.printStackTrace()
        }

        return getMediaItemsThreadSafe.mediaItems
    }

    fun deleteMetadata() {
        Thread(DeleteAllThreadSafe(sessionMediaItemDao)).start()
    }

    private class GetMediaItemThreadSafe(
        private val sessionMediaItemDao: SessionMediaItemDao,
        private val id: String
    ) : Runnable {
        var sessionMediaItem: SessionMediaItem? = null

        override fun run() {
            sessionMediaItem = sessionMediaItemDao.get(id)
        }
    }

    @OptIn(UnstableApi::class)
    private class GetMediaItemsThreadSafe(
        private val sessionMediaItemDao: SessionMediaItemDao,
        private val timestamp: Long
    ) : Runnable {
        val mediaItems: MutableList<MediaItem> = mutableListOf()

        override fun run() {
            val sessionMediaItems = sessionMediaItemDao.get(timestamp)
            sessionMediaItems.forEach { sessionMediaItem ->
                mediaItems.add(sessionMediaItem.getMediaItem())
            }
        }
    }

    private class InsertAllThreadSafe(
        private val sessionMediaItemDao: SessionMediaItemDao,
        private val sessionMediaItems: List<SessionMediaItem>
    ) : Runnable {
        override fun run() {
            sessionMediaItemDao.insertAll(sessionMediaItems)
        }
    }

    private class DeleteAllThreadSafe(
        private val sessionMediaItemDao: SessionMediaItemDao
    ) : Runnable {
        override fun run() {
            sessionMediaItemDao.deleteAll()
        }
    }
}
