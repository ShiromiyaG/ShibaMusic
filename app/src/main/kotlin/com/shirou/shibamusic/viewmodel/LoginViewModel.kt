package com.shirou.shibamusic.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.shirou.shibamusic.model.Server
import com.shirou.shibamusic.repository.ServerRepository

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val serverRepository = ServerRepository()

    var serverToEdit: Server? = null

    val serverList: LiveData<List<Server>>
        get() = serverRepository.getLiveServer()

    fun addServer(server: Server) {
        serverRepository.insert(server)
    }

    fun deleteServer(server: Server?) {
        server?.let { serverRepository.delete(it) }
            ?: serverToEdit?.let { serverRepository.delete(it) }
    }
}
