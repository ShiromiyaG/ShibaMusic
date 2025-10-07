package com.shirou.shibamusic.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.shirou.shibamusic.interfaces.StarCallback
import com.shirou.shibamusic.repository.ArtistRepository
import com.shirou.shibamusic.repository.FavoriteRepository
import com.shirou.shibamusic.subsonic.models.ArtistID3
import com.shirou.shibamusic.util.NetworkUtil
import java.util.Date

class ArtistBottomSheetViewModel(application: Application) : AndroidViewModel(application) {
    private val artistRepository: ArtistRepository = ArtistRepository()
    private val favoriteRepository: FavoriteRepository = FavoriteRepository()

    lateinit var artist: ArtistID3

    private fun artistIdOrNull(): String? = artist.id?.takeIf { it.isNotBlank() }

    fun setFavorite() {
        if (artist.starred != null) {
            if (NetworkUtil.isOffline()) {
                removeFavoriteOffline()
            } else {
                removeFavoriteOnline()
            }
        } else {
            if (NetworkUtil.isOffline()) {
                setFavoriteOffline()
            } else {
                setFavoriteOnline()
            }
        }
    }

    private fun removeFavoriteOffline() {
        val artistId = artistIdOrNull() ?: run {
            artist.starred = null
            return
        }

        favoriteRepository.starLater(null, null, artistId, false)
        artist.starred = null
    }

    private fun removeFavoriteOnline() {
        val artistId = artistIdOrNull() ?: run {
            artist.starred = null
            return
        }

        favoriteRepository.unstar(null, null, artistId, object : StarCallback {
            override fun onError() {
                favoriteRepository.starLater(null, null, artistId, false)
            }
        })
        artist.starred = null
    }

    private fun setFavoriteOffline() {
        val artistId = artistIdOrNull() ?: return

        favoriteRepository.starLater(null, null, artistId, true)
        artist.starred = Date()
    }

    private fun setFavoriteOnline() {
        val artistId = artistIdOrNull() ?: return

        favoriteRepository.star(null, null, artistId, object : StarCallback {
            override fun onError() {
                favoriteRepository.starLater(null, null, artistId, true)
            }
        })
        artist.starred = Date()
    }
}
