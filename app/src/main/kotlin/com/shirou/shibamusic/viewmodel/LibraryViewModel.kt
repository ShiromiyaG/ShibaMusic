package com.shirou.shibamusic.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.shirou.shibamusic.repository.AlbumRepository
import com.shirou.shibamusic.repository.ArtistRepository
import com.shirou.shibamusic.repository.DirectoryRepository
import com.shirou.shibamusic.repository.GenreRepository
import com.shirou.shibamusic.repository.PlaylistRepository
import com.shirou.shibamusic.subsonic.models.AlbumID3
import com.shirou.shibamusic.subsonic.models.ArtistID3
import com.shirou.shibamusic.subsonic.models.Genre
import com.shirou.shibamusic.subsonic.models.Indexes
import com.shirou.shibamusic.subsonic.models.MusicFolder

import com.shirou.shibamusic.subsonic.models.Playlist

class LibraryViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "LibraryViewModel"

    private val directoryRepository = DirectoryRepository()
    private val albumRepository = AlbumRepository()
    private val artistRepository = ArtistRepository()
    private val genreRepository = GenreRepository()
    private val playlistRepository = PlaylistRepository()

    private val musicFolders = MutableLiveData<List<MusicFolder>?>(null)
    private val indexes = MutableLiveData<Indexes?>(null)
    private val playlistSample = MutableLiveData<List<Playlist>?>(null)
    private val sampleAlbum = MutableLiveData<List<AlbumID3>?>(null)
    private val sampleArtist = MutableLiveData<List<ArtistID3>?>(null)
    private val sampleGenres = MutableLiveData<List<Genre>?>(null)

    fun getMusicFolders(owner: LifecycleOwner): LiveData<List<MusicFolder>?> {
        if (musicFolders.value == null) {
            directoryRepository.getMusicFolders().observe(owner, musicFolders::postValue)
        }
        return musicFolders
    }

    fun getIndexes(owner: LifecycleOwner): LiveData<Indexes?> {
        if (indexes.value == null) {
            directoryRepository.getIndexes("0", null).observe(owner, indexes::postValue)
        }
        return indexes
    }

    fun getAlbumSample(owner: LifecycleOwner): LiveData<List<AlbumID3>?> {
        if (sampleAlbum.value == null) {
            albumRepository.getAlbums("random", 10, null, null).observe(owner, sampleAlbum::postValue)
        }
        return sampleAlbum
    }

    fun getArtistSample(owner: LifecycleOwner): LiveData<List<ArtistID3>?> {
        if (sampleArtist.value == null) {
            artistRepository.getArtists(true, 10).observe(owner, sampleArtist::postValue)
        }
        return sampleArtist
    }

    fun getGenreSample(owner: LifecycleOwner): LiveData<List<Genre>?> {
        if (sampleGenres.value == null) {
            genreRepository.getGenres(true, 15).observe(owner, sampleGenres::postValue)
        }
        return sampleGenres
    }

    fun getPlaylistSample(owner: LifecycleOwner): LiveData<List<Playlist>?> {
        if (playlistSample.value == null) {
            playlistRepository.getPlaylists(true, 10).observe(owner, playlistSample::postValue)
        }
        return playlistSample
    }

    fun refreshAlbumSample(owner: LifecycleOwner) {
        albumRepository.getAlbums("random", 10, null, null).observe(owner, sampleAlbum::postValue)
    }

    fun refreshArtistSample(owner: LifecycleOwner) {
        artistRepository.getArtists(true, 10).observe(owner, sampleArtist::postValue)
    }

    fun refreshGenreSample(owner: LifecycleOwner) {
        genreRepository.getGenres(true, 15).observe(owner, sampleGenres::postValue)
    }

    fun refreshPlaylistSample(owner: LifecycleOwner) {
        playlistRepository.getPlaylists(true, 10).observe(owner, playlistSample::postValue)
    }
}
