package com.shirou.shibamusic.repository

import androidx.lifecycle.LiveData
import com.shirou.shibamusic.database.AppDatabase
import com.shirou.shibamusic.database.dao.ChronologyDao
import com.shirou.shibamusic.model.Chronology
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ChronologyRepository {
    private val chronologyDao: ChronologyDao = AppDatabase.getInstance().chronologyDao()

    fun getChronology(server: String, start: Long, end: Long): LiveData<List<Chronology>> {
        return chronologyDao.getAllFrom(start, end, server)
    }
    
    suspend fun getLastPlayed(server: String, count: Int): List<Chronology> {
        return chronologyDao.getLastPlayedSuspend(server, count)
    }

    fun insert(item: Chronology) {
        Thread {
            chronologyDao.insert(item)
        }.start()
    }
    
    suspend fun insertSync(item: Chronology) = withContext(Dispatchers.IO) {
        chronologyDao.insert(item)
    }
}
