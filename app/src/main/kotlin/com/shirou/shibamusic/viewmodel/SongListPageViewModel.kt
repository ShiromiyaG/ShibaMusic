package com.shirou.shibamusic.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.shirou.shibamusic.repository.ArtistRepository
import com.shirou.shibamusic.repository.SongRepository
import com.shirou.shibamusic.subsonic.models.AlbumID3
import com.shirou.shibamusic.subsonic.models.ArtistID3
import com.shirou.shibamusic.subsonic.models.Child
import com.shirou.shibamusic.subsonic.models.Genre
import com.shirou.shibamusic.util.Constants
import java.util.ArrayList

class SongListPageViewModel(application: Application) : AndroidViewModel(application) {
    private val songRepository = SongRepository()
    private val artistRepository = ArtistRepository()

    var title: String = ""
    var toolbarTitle: String? = null
    var genre: Genre? = null
    var artist: ArtistID3? = null
    var album: AlbumID3? = null

    private var _songList: MutableLiveData<List<Child>>? = null

    private val filterIds = ArrayList<String>()
    val filters: List<String>
        get() = filterIds

    private val filterDisplayNames = ArrayList<String>()
    val filterNames: List<String>
        get() = filterDisplayNames

    var year: Int = 0
    var maxNumberByYear: Int = 500
    var maxNumberByGenre: Int = 100

    fun getSongList(): LiveData<List<Child>> {
        if (_songList == null) {
            _songList = when (title) {
                Constants.MEDIA_BY_GENRE -> genre?.genre?.let { id ->
                    songRepository.getSongsByGenre(id, 0)
                }
                Constants.MEDIA_BY_ARTIST -> artist?.name?.let { artistName ->
                    artistRepository.getTopSongs(artistName, 50)
                }
                Constants.MEDIA_BY_GENRES -> if (filterIds.isNotEmpty()) {
                    songRepository.getSongsByGenres(ArrayList(filterIds))
                } else {
                    MutableLiveData<List<Child>>(emptyList())
                }
                Constants.MEDIA_BY_YEAR -> songRepository.getRandomSample(maxNumberByYear, year, year + 10)
                Constants.MEDIA_STARRED -> songRepository.getStarredSongs(false, -1)
                else -> MutableLiveData<List<Child>>(emptyList())
            }
        }
        return _songList ?: MutableLiveData<List<Child>>(emptyList()).also { _songList = it }
    }

    fun getSongsByPage(owner: LifecycleOwner) {
        when (title) {
            Constants.MEDIA_BY_GENRE -> {
                val songCount = _songList?.value?.size ?: 0

                if (songCount > 0 && songCount % maxNumberByGenre != 0) return

                val page = songCount / maxNumberByGenre
                val genreId = genre?.genre ?: return
                songRepository.getSongsByGenre(genreId, page).observe(owner) { children ->
                    if (!children.isNullOrEmpty()) {
                        val currentMedia = _songList?.value.orEmpty()
                        (_songList as? MutableLiveData<List<Child>>)?.postValue(currentMedia + children)
                    }
                }
            }
            Constants.MEDIA_BY_ARTIST,
            Constants.MEDIA_BY_GENRES,
            Constants.MEDIA_BY_YEAR,
            Constants.MEDIA_STARRED -> {
                // No implementation for these cases in the original Java code.
            }
        }
    }

    fun getFiltersTitle(): String {
        return filterDisplayNames.joinToString(", ")
    }

    fun setFilters(values: List<String?>?) {
        filterIds.clear()
        values?.forEach { value ->
            if (!value.isNullOrEmpty()) {
                filterIds.add(value)
            }
        }
    }

    fun setFilterNames(values: List<String?>?) {
        filterDisplayNames.clear()
        values?.forEach { value ->
            if (!value.isNullOrEmpty()) {
                filterDisplayNames.add(value)
            }
        }
    }
}
