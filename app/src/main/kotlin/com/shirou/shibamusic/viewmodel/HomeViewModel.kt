package com.shirou.shibamusic.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.shirou.shibamusic.interfaces.StarCallback
import com.shirou.shibamusic.model.Chronology
import com.shirou.shibamusic.model.Favorite
import com.shirou.shibamusic.model.HomeSector
import com.shirou.shibamusic.repository.AlbumRepository
import com.shirou.shibamusic.repository.ArtistRepository
import com.shirou.shibamusic.repository.ChronologyRepository
import com.shirou.shibamusic.repository.FavoriteRepository
import com.shirou.shibamusic.repository.PlaylistRepository
import com.shirou.shibamusic.repository.SharingRepository
import com.shirou.shibamusic.repository.SongRepository
import com.shirou.shibamusic.subsonic.models.AlbumID3
import com.shirou.shibamusic.subsonic.models.ArtistID3
import com.shirou.shibamusic.subsonic.models.Child
import com.shirou.shibamusic.subsonic.models.Playlist
import com.shirou.shibamusic.subsonic.models.Share
import com.shirou.shibamusic.util.Preferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Calendar

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val songRepository: SongRepository
    private val albumRepository: AlbumRepository
    private val artistRepository: ArtistRepository
    private val chronologyRepository: ChronologyRepository
    private val favoriteRepository: FavoriteRepository
    private val playlistRepository: PlaylistRepository
    private val sharingRepository: SharingRepository

    private val albumsSyncViewModel: StarredAlbumsSyncViewModel

    private val discoverSongSample = MutableLiveData<List<Child>?>(null)
    private val newReleasedAlbum = MutableLiveData<List<AlbumID3>?>(null)
    private val starredTracksSample = MutableLiveData<List<Child>?>(null)
    private val starredArtistsSample = MutableLiveData<List<ArtistID3>?>(null)
    private val bestOfArtists = MutableLiveData<List<ArtistID3>?>(null)
    private val starredTracks = MutableLiveData<List<Child>?>(null)
    private val starredAlbums = MutableLiveData<List<AlbumID3>?>(null)
    private val starredArtists = MutableLiveData<List<ArtistID3>?>(null)
    private val mostPlayedAlbumSample = MutableLiveData<List<AlbumID3>?>(null)
    private val recentlyPlayedAlbumSample = MutableLiveData<List<AlbumID3>?>(null)
    private val years = MutableLiveData<List<Int>?>(null)
    private val recentlyAddedAlbumSample = MutableLiveData<List<AlbumID3>?>(null)

    private val thisGridTopSong = MutableLiveData<List<Chronology>?>(null)
    private val mediaInstantMix = MutableLiveData<List<Child>?>(null)
    private val artistInstantMix = MutableLiveData<List<Child>?>(null)
    private val artistBestOf = MutableLiveData<List<Child>?>(null)
    private val pinnedPlaylists = MutableLiveData<List<Playlist>?>(null)
    private val shares = MutableLiveData<List<Share>?>(null)

    private var sectors: List<HomeSector>? = null

    init {
        setHomeSectorList()

        songRepository = SongRepository()
        albumRepository = AlbumRepository()
        artistRepository = ArtistRepository()
        chronologyRepository = ChronologyRepository()
        favoriteRepository = FavoriteRepository()
        playlistRepository = PlaylistRepository()
        sharingRepository = SharingRepository()

        albumsSyncViewModel = StarredAlbumsSyncViewModel(application)

        setOfflineFavorite()
    }

    fun getDiscoverSongSample(owner: LifecycleOwner): LiveData<List<Child>?> {
        if (discoverSongSample.value == null) {
            songRepository.getRandomSample(10, null, null).observe(owner, discoverSongSample::postValue)
        }
        return discoverSongSample
    }

    fun getRandomShuffleSample(): LiveData<List<Child>?> {
        return songRepository.getRandomSample(1000, null, null)
    }

    fun getChronologySample(owner: LifecycleOwner): LiveData<List<Chronology>?> {
        val cal = Calendar.getInstance()
        val server = Preferences.getServerId() ?: ""

        val currentWeek = cal.get(Calendar.WEEK_OF_YEAR)
        val start = cal.timeInMillis

        cal.set(Calendar.WEEK_OF_YEAR, currentWeek - 1)
        val end = cal.timeInMillis

        chronologyRepository.getChronology(server, start, end).observe(owner, thisGridTopSong::postValue)
        return thisGridTopSong
    }

    fun getRecentlyReleasedAlbums(owner: LifecycleOwner): LiveData<List<AlbumID3>?> {
        if (newReleasedAlbum.value == null) {
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)

            albumRepository.getAlbums("byYear", 500, currentYear, currentYear).observe(owner) { albums: List<AlbumID3>? ->
                albums?.let {
                    val sorted = it.sortedByDescending { album -> album.created }
                    newReleasedAlbum.postValue(sorted.take(20))
                }
            }
        }
        return newReleasedAlbum
    }

    fun getStarredTracksSample(owner: LifecycleOwner): LiveData<List<Child>?> {
        if (starredTracksSample.value == null) {
            songRepository.getStarredSongs(true, 10).observe(owner, starredTracksSample::postValue)
        }
        return starredTracksSample
    }

    fun getStarredArtistsSample(owner: LifecycleOwner): LiveData<List<ArtistID3>?> {
        if (starredArtistsSample.value == null) {
            artistRepository.getStarredArtists(true, 10).observe(owner, starredArtistsSample::postValue)
        }
        return starredArtistsSample
    }

    fun getBestOfArtists(owner: LifecycleOwner): LiveData<List<ArtistID3>?> {
        if (bestOfArtists.value == null) {
            artistRepository.getStarredArtists(true, 20).observe(owner, bestOfArtists::postValue)
        }
        return bestOfArtists
    }

    fun getStarredTracks(owner: LifecycleOwner): LiveData<List<Child>?> {
        if (starredTracks.value == null) {
            songRepository.getStarredSongs(true, 20).observe(owner, starredTracks::postValue)
        }
        return starredTracks
    }

    fun getStarredAlbums(owner: LifecycleOwner): LiveData<List<AlbumID3>?> {
        if (starredAlbums.value == null) {
            albumRepository.getStarredAlbums(true, 20).observe(owner, starredAlbums::postValue)
        }
        return starredAlbums
    }

    fun getAllStarredAlbumSongs(): LiveData<List<Child>> {
        return albumsSyncViewModel.getAllStarredAlbumSongs()
    }

    fun getStarredArtists(owner: LifecycleOwner): LiveData<List<ArtistID3>?> {
        if (starredArtists.value == null) {
            artistRepository.getStarredArtists(true, 20).observe(owner, starredArtists::postValue)
        }
        return starredArtists
    }

    fun getYearList(owner: LifecycleOwner): LiveData<List<Int>?> {
        if (years.value == null) {
            albumRepository.getDecades().observe(owner, years::postValue)
        }
        return years
    }

    fun getMostPlayedAlbums(owner: LifecycleOwner): LiveData<List<AlbumID3>?> {
        if (mostPlayedAlbumSample.value == null) {
            albumRepository.getAlbums("frequent", 20, null, null).observe(owner, mostPlayedAlbumSample::postValue)
        }
        return mostPlayedAlbumSample
    }

    fun getMostRecentlyAddedAlbums(owner: LifecycleOwner): LiveData<List<AlbumID3>?> {
        if (recentlyAddedAlbumSample.value == null) {
            albumRepository.getAlbums("newest", 20, null, null).observe(owner, recentlyAddedAlbumSample::postValue)
        }
        return recentlyAddedAlbumSample
    }

    fun getRecentlyPlayedAlbumList(owner: LifecycleOwner): LiveData<List<AlbumID3>?> {
        if (recentlyPlayedAlbumSample.value == null) {
            albumRepository.getAlbums("recent", 20, null, null).observe(owner, recentlyPlayedAlbumSample::postValue)
        }
        return recentlyPlayedAlbumSample
    }

    fun getMediaInstantMix(owner: LifecycleOwner, media: Child): LiveData<List<Child>?> {
        mediaInstantMix.value = emptyList()

        songRepository.getInstantMix(media.id ?: "", 20).observe(owner, mediaInstantMix::postValue)

        return mediaInstantMix
    }

    fun getArtistInstantMix(owner: LifecycleOwner, artist: ArtistID3): LiveData<List<Child>?> {
        artistInstantMix.value = emptyList()

        artistRepository.getTopSongs(artist.name ?: "", 10).observe(owner, artistInstantMix::postValue)

        return artistInstantMix
    }

    fun getArtistBestOf(owner: LifecycleOwner, artist: ArtistID3): LiveData<List<Child>?> {
        artistBestOf.value = emptyList()

        artistRepository.getTopSongs(artist.name ?: "", 10).observe(owner, artistBestOf::postValue)

        return artistBestOf
    }

    fun getPinnedPlaylists(owner: LifecycleOwner): LiveData<List<Playlist>?> {
        pinnedPlaylists.value = emptyList()

        playlistRepository.getPlaylists(false, -1).observe(owner) { remotes: List<Playlist>? ->
            playlistRepository.getPinnedPlaylists().observe(owner) { locals: List<Playlist>? ->
                if (remotes != null && locals != null) {
                    val toReturn = remotes.filter { remote -> locals.any { local -> local.id == remote.id } }
                    pinnedPlaylists.value = toReturn
                }
            }
        }
        return pinnedPlaylists
    }

    fun getShares(owner: LifecycleOwner): LiveData<List<Share>?> {
        if (shares.value == null) {
            sharingRepository.getShares().observe(owner, shares::postValue)
        }
        return shares
    }

    fun getAllStarredTracks(): LiveData<List<Child>?> {
        return songRepository.getStarredSongs(false, -1)
    }

    fun changeChronologyPeriod(owner: LifecycleOwner, period: Int) {
        val cal = Calendar.getInstance()
        val server = Preferences.getServerId() ?: ""
        val currentWeek = cal.get(Calendar.WEEK_OF_YEAR)

        val start: Long
        val end: Long

        when (period) {
            0 -> {
                start = cal.timeInMillis
                cal.set(Calendar.WEEK_OF_YEAR, currentWeek - 1)
                end = cal.timeInMillis
            }
            1 -> {
                start = cal.timeInMillis
                cal.set(Calendar.WEEK_OF_YEAR, currentWeek - 4)
                end = cal.timeInMillis
            }
            2 -> {
                start = cal.timeInMillis
                cal.set(Calendar.WEEK_OF_YEAR, currentWeek - 52)
                end = cal.timeInMillis
            }
            else -> {
                start = 0L
                end = 0L
            }
        }

        chronologyRepository.getChronology(server, start, end).observe(owner, thisGridTopSong::postValue)
    }

    fun refreshDiscoverySongSample(owner: LifecycleOwner) {
        songRepository.getRandomSample(10, null, null).observe(owner, discoverSongSample::postValue)
    }

    fun refreshSimilarSongSample(owner: LifecycleOwner) {
        songRepository.getStarredSongs(true, 10).observe(owner, starredTracksSample::postValue)
    }

    fun refreshRadioArtistSample(owner: LifecycleOwner) {
        artistRepository.getStarredArtists(true, 10).observe(owner, starredArtistsSample::postValue)
    }

    fun refreshBestOfArtist(owner: LifecycleOwner) {
        artistRepository.getStarredArtists(true, 20).observe(owner, bestOfArtists::postValue)
    }

    fun refreshStarredTracks(owner: LifecycleOwner) {
        songRepository.getStarredSongs(true, 20).observe(owner, starredTracks::postValue)
    }

    fun refreshStarredAlbums(owner: LifecycleOwner) {
        albumRepository.getStarredAlbums(true, 20).observe(owner, starredAlbums::postValue)
    }

    fun refreshStarredArtists(owner: LifecycleOwner) {
        artistRepository.getStarredArtists(true, 20).observe(owner, starredArtists::postValue)
    }

    fun refreshMostPlayedAlbums(owner: LifecycleOwner) {
        albumRepository.getAlbums("frequent", 20, null, null).observe(owner, mostPlayedAlbumSample::postValue)
    }

    fun refreshMostRecentlyAddedAlbums(owner: LifecycleOwner) {
        albumRepository.getAlbums("newest", 20, null, null).observe(owner, recentlyAddedAlbumSample::postValue)
    }

    fun refreshRecentlyPlayedAlbumList(owner: LifecycleOwner) {
        albumRepository.getAlbums("recent", 20, null, null).observe(owner, recentlyPlayedAlbumSample::postValue)
    }

    fun refreshShares(owner: LifecycleOwner) {
        sharingRepository.getShares().observe(owner, shares::postValue)
    }

    private fun setHomeSectorList() {
        Preferences.getHomeSectorList()
            ?.takeUnless { it == "null" }
            ?.let { json ->
                sectors = Gson().fromJson(
                    json,
                    object : TypeToken<List<HomeSector>>() {}.type
                )
            }
    }

    fun getHomeSectorList(): List<HomeSector>? {
        return sectors
    }

    fun checkHomeSectorVisibility(sectorId: String): Boolean {
        return sectors?.none { it.id == sectorId } == true
    }

    private fun setOfflineFavorite() {
        val favorites = getFavorites()
        val favoritesToSave = getFavoritesToSave(favorites)
        val favoritesToDelete = getFavoritesToDelete(favorites, favoritesToSave)

        manageFavoriteToSave(favoritesToSave)
        manageFavoriteToDelete(favoritesToDelete)
    }

    private fun getFavorites(): List<Favorite> {
        return favoriteRepository.getFavorites()
    }

    private fun getFavoritesToSave(favorites: List<Favorite>): List<Favorite> {
        val filteredMap = mutableMapOf<String, Favorite>()

        for (favorite in favorites) {
            val key = favorite.toString()

            if (!filteredMap.containsKey(key) || favorite.timestamp > filteredMap[key]!!.timestamp) {
                filteredMap[key] = favorite
            }
        }
        return filteredMap.values.toList()
    }

    private fun getFavoritesToDelete(favorites: List<Favorite>, favoritesToSave: List<Favorite>): List<Favorite> {
        return favorites.filter { it !in favoritesToSave }
    }

    private fun manageFavoriteToSave(favoritesToSave: List<Favorite>) {
        for (favorite in favoritesToSave) {
            if (favorite.toStar) {
                favoriteToStar(favorite)
            } else {
                favoriteToUnstar(favorite)
            }
        }
    }

    private fun manageFavoriteToDelete(favoritesToDelete: List<Favorite>) {
        for (favorite in favoritesToDelete) {
            favoriteRepository.delete(favorite)
        }
    }

    private fun favoriteToStar(favorite: Favorite) {
        when {
            favorite.songId != null -> favorite.songId?.takeIf { it.isNotBlank() }?.let { songId ->
                favoriteRepository.star(
                    songId,
                    null,
                    null,
                    object : StarCallback {
                        override fun onError() {
                            favoriteRepository.delete(favorite)
                        }
                    }
                )
            }
            favorite.albumId != null -> favorite.albumId?.takeIf { it.isNotBlank() }?.let { albumId ->
                favoriteRepository.star(
                    null,
                    albumId,
                    null,
                    object : StarCallback {
                        override fun onError() {
                            favoriteRepository.delete(favorite)
                        }
                    }
                )
            }
            favorite.artistId != null -> favorite.artistId?.takeIf { it.isNotBlank() }?.let { artistId ->
                favoriteRepository.star(
                    null,
                    null,
                    artistId,
                    object : StarCallback {
                        override fun onError() {
                            favoriteRepository.delete(favorite)
                        }
                    }
                )
            }
        }
    }

    private fun favoriteToUnstar(favorite: Favorite) {
        when {
            favorite.songId != null -> favorite.songId?.takeIf { it.isNotBlank() }?.let { songId ->
                favoriteRepository.unstar(
                    songId,
                    null,
                    null,
                    object : StarCallback {
                        override fun onError() {
                            favoriteRepository.delete(favorite)
                        }
                    }
                )
            }
            favorite.albumId != null -> favorite.albumId?.takeIf { it.isNotBlank() }?.let { albumId ->
                favoriteRepository.unstar(
                    null,
                    albumId,
                    null,
                    object : StarCallback {
                        override fun onError() {
                            favoriteRepository.delete(favorite)
                        }
                    }
                )
            }
            favorite.artistId != null -> favorite.artistId?.takeIf { it.isNotBlank() }?.let { artistId ->
                favoriteRepository.unstar(
                    null,
                    null,
                    artistId,
                    object : StarCallback {
                        override fun onError() {
                            favoriteRepository.delete(favorite)
                        }
                    }
                )
            }
        }
    }
}
