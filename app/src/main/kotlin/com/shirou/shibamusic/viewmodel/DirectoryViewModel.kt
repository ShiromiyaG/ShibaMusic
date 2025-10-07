package com.shirou.shibamusic.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.shirou.shibamusic.repository.DirectoryRepository
import com.shirou.shibamusic.subsonic.models.Directory

class DirectoryViewModel(application: Application) : AndroidViewModel(application) {
    private val directoryRepository: DirectoryRepository = DirectoryRepository()

    fun loadMusicDirectory(id: String): LiveData<Directory> =
        directoryRepository.getMusicDirectory(id)
}
