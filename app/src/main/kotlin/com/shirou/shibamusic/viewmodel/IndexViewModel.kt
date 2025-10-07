package com.shirou.shibamusic.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData

import com.shirou.shibamusic.repository.DirectoryRepository
import com.shirou.shibamusic.subsonic.models.Indexes
import com.shirou.shibamusic.subsonic.models.MusicFolder

class IndexViewModel(application: Application) : AndroidViewModel(application) {

    private val directoryRepository: DirectoryRepository = DirectoryRepository()

    private var _musicFolder: MusicFolder? = null

    fun getIndexes(musicFolderId: String?): MutableLiveData<Indexes> {
        return directoryRepository.getIndexes(musicFolderId, null)
    }

    val musicFolderName: String
        get() = _musicFolder?.name ?: ""

    fun setMusicFolder(musicFolder: MusicFolder?) {
        _musicFolder = musicFolder
    }
}
