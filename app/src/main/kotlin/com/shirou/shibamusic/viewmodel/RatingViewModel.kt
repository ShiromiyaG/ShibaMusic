package com.shirou.shibamusic.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.shirou.shibamusic.repository.AlbumRepository
import com.shirou.shibamusic.repository.ArtistRepository
import com.shirou.shibamusic.repository.SongRepository
import com.shirou.shibamusic.subsonic.models.AlbumID3
import com.shirou.shibamusic.subsonic.models.ArtistID3
import com.shirou.shibamusic.subsonic.models.Child

class RatingViewModel(application: Application) : AndroidViewModel(application) {

    private val songRepository: SongRepository = SongRepository()
    private val albumRepository: AlbumRepository = AlbumRepository()
    private val artistRepository: ArtistRepository = ArtistRepository()

    var song: Child? = null
        set(value) {
            field = value // Assign value to the backing field for 'song'
            // When 'song' is set, ensure 'album' and 'artist' are nullified.
            // Using 'this.album' and 'this.artist' will invoke their setters.
            this.album = null
            this.artist = null
        }

    var album: AlbumID3? = null
        set(value) {
            // When 'album' is set, ensure 'song' and 'artist' are nullified.
            this.song = null
            field = value // Assign value to the backing field for 'album'
            this.artist = null
        }

    var artist: ArtistID3? = null
        set(value) {
            // When 'artist' is set, ensure 'song' and 'album' are nullified.
            this.song = null
            this.album = null
            field = value // Assign value to the backing field for 'artist'
        }

    val liveSong: LiveData<Child?>
        get() = songRepository.getSong(requireSongId())

    val liveAlbum: LiveData<AlbumID3?>
        get() = albumRepository.getAlbum(requireAlbumId())

    val liveArtist: LiveData<ArtistID3?>
        get() = artistRepository.getArtist(requireArtistId())

    fun rate(star: Int) {
        song?.id?.let {
            songRepository.setRating(it, star)
            return
        }

        album?.id?.let {
            albumRepository.setRating(it, star)
            return
        }

        artist?.id?.let {
            artistRepository.setRating(it, star)
        }
    }

    private fun requireSongId(): String {
        return song?.id ?: throw IllegalStateException("Song must be set before accessing liveSong")
    }

    private fun requireAlbumId(): String {
        return album?.id ?: throw IllegalStateException("Album must be set before accessing liveAlbum")
    }

    private fun requireArtistId(): String {
        return artist?.id ?: throw IllegalStateException("Artist must be set before accessing liveArtist")
    }
}
