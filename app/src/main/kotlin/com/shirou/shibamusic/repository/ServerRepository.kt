package com.shirou.shibamusic.repository

import androidx.lifecycle.LiveData
import com.shirou.shibamusic.database.AppDatabase
import com.shirou.shibamusic.database.dao.ServerDao
import com.shirou.shibamusic.model.Server
import kotlin.collections.List

class ServerRepository {

    private val serverDao: ServerDao = AppDatabase.getInstance().serverDao()

    fun getLiveServer(): LiveData<List<Server>> = serverDao.getAll()

    fun insert(server: Server) {
        Thread {
            serverDao.insert(server)
        }.start()
    }

    fun delete(server: Server) {
        Thread {
            serverDao.delete(server)
        }.start()
    }

    companion object {
        private const val TAG = "QueueRepository"
    }
}
