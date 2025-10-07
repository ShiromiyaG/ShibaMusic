package com.shirou.shibamusic.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.shirou.shibamusic.R
import com.shirou.shibamusic.model.HomeSector
import com.shirou.shibamusic.util.Constants
import com.shirou.shibamusic.util.Preferences
import com.google.gson.Gson

class HomeRearrangementViewModel(application: Application) : AndroidViewModel(application) {

    private val gson = Gson()
    private var sectors: List<HomeSector>? = null

    fun getHomeSectorList(): List<HomeSector> {
        val existing = sectors
        if (!existing.isNullOrEmpty()) {
            return existing
        }

        val loadedSectors = Preferences.getHomeSectorList()
            ?.takeUnless { it.isBlank() || it == "null" }
            ?.let { json ->
                runCatching {
                    gson.fromJson(json, Array<HomeSector>::class.java)?.toList().orEmpty()
                }.getOrElse { emptyList() }
            }
            ?.takeIf { it.isNotEmpty() }
            ?: fillStandardHomeSectorList()

        sectors = loadedSectors
        return loadedSectors
    }

    fun orderSectorLiveListAfterSwap(sectors: List<HomeSector>) {
        this.sectors = sectors
    }

    fun saveHomeSectorList(sectors: List<HomeSector>) {
        Preferences.setHomeSectorList(sectors)
    }

    fun resetHomeSectorList() {
        Preferences.setHomeSectorList(null)
    }

    fun closeDialog() {
        sectors = null
    }

    private fun fillStandardHomeSectorList(): List<HomeSector> {
        val application: Application = getApplication()
        return listOf(
            HomeSector(Constants.HOME_SECTOR_DISCOVERY, application.getString(R.string.home_title_discovery), true, 1),
            HomeSector(Constants.HOME_SECTOR_MADE_FOR_YOU, application.getString(R.string.home_title_made_for_you), true, 2),
            HomeSector(Constants.HOME_SECTOR_BEST_OF, application.getString(R.string.home_title_best_of), true, 3),
            HomeSector(Constants.HOME_SECTOR_RADIO_STATION, application.getString(R.string.home_title_radio_station), true, 4),
            HomeSector(Constants.HOME_SECTOR_TOP_SONGS, application.getString(R.string.home_title_top_songs), true, 5),
            HomeSector(Constants.HOME_SECTOR_STARRED_TRACKS, application.getString(R.string.home_title_starred_tracks), true, 6),
            HomeSector(Constants.HOME_SECTOR_STARRED_ALBUMS, application.getString(R.string.home_title_starred_albums), true, 7),
            HomeSector(Constants.HOME_SECTOR_STARRED_ARTISTS, application.getString(R.string.home_title_starred_artists), true, 8),
            HomeSector(Constants.HOME_SECTOR_NEW_RELEASES, application.getString(R.string.home_title_new_releases), true, 9),
            HomeSector(Constants.HOME_SECTOR_FLASHBACK, application.getString(R.string.home_title_flashback), true, 10),
            HomeSector(Constants.HOME_SECTOR_MOST_PLAYED, application.getString(R.string.home_title_most_played), true, 11),
            HomeSector(Constants.HOME_SECTOR_LAST_PLAYED, application.getString(R.string.home_title_last_played), true, 12),
            HomeSector(Constants.HOME_SECTOR_RECENTLY_ADDED, application.getString(R.string.home_title_recently_added), true, 13),
            HomeSector(Constants.HOME_SECTOR_PINNED_PLAYLISTS, application.getString(R.string.home_title_pinned_playlists), true, 14),
            HomeSector(Constants.HOME_SECTOR_SHARED, application.getString(R.string.home_title_shares), true, 15),
        )
    }
}
