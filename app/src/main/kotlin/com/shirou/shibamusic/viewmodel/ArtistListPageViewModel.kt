package com.shirou.shibamusic.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.shirou.shibamusic.repository.ArtistRepository
import com.shirou.shibamusic.repository.DownloadRepository
import com.shirou.shibamusic.subsonic.models.ArtistID3
import com.shirou.shibamusic.util.Constants
import com.shirou.shibamusic.util.MappingUtil
import java.util.ArrayList

class ArtistListPageViewModel(application: Application) : AndroidViewModel(application) {
    private val artistRepository = ArtistRepository()
    private val downloadRepository = DownloadRepository()

    var title: String = ""

    // `artistList` is a field that can be reassigned to different MutableLiveData instances.
    // It's initialized in `getArtistList` before being returned, hence `lateinit`.
    private lateinit var artistList: MutableLiveData<List<ArtistID3>>

    fun getArtistList(owner: LifecycleOwner): LiveData<List<ArtistID3>> {
        // Initialize artistList here, as per the Java code's pattern.
        artistList = MutableLiveData(ArrayList())

        when (title) {
            Constants.ARTIST_STARRED -> {
                // Assuming `artistRepository.getStarredArtists` returns `MutableLiveData<List<ArtistID3>>`
                // to be compatible with the `artistList` field type, as implied by the Java code.
                artistList = artistRepository.getStarredArtists(false, -1)
            }
            Constants.ARTIST_DOWNLOADED -> {
                // Using the `liveDownload` property if `getLiveDownload()` is a getter.
                downloadRepository.getLiveDownload().observe(owner) { downloads ->
                    val uniqueDownloads = downloads.orEmpty().distinctBy { it.artistId ?: it.artist }
                    artistList.value = MappingUtil.mapDownloadsToArtists(uniqueDownloads)
                }
            }
        }

        return artistList
    }
}
