package com.shirou.shibamusic.repository

import androidx.lifecycle.LiveData
import com.shirou.shibamusic.database.AppDatabase
import com.shirou.shibamusic.database.dao.DownloadDao
import com.shirou.shibamusic.model.Download

class DownloadRepository {
    private val downloadDao: DownloadDao = AppDatabase.getInstance().downloadDao()

    fun getLiveDownload(): LiveData<List<Download>> = downloadDao.getAll()

    fun getDownload(id: String): Download? {
        var download: Download? = null

        val getDownloadThreadSafe = GetDownloadThreadSafe(downloadDao, id)
        val thread = Thread(getDownloadThreadSafe)
        thread.start()

        try {
            thread.join()
            download = getDownloadThreadSafe.download
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        return download
    }

    private class GetDownloadThreadSafe(private val downloadDao: DownloadDao, private val id: String) : Runnable {
        var download: Download? = null

        override fun run() {
            download = downloadDao.getOne(id)
        }
    }

    fun insert(download: Download) {
        Thread { downloadDao.insert(download) }.start()
    }

    fun update(id: String) {
        Thread { downloadDao.update(id) }.start()
    }

    fun insertAll(downloads: List<Download>) {
        Thread { downloadDao.insertAll(downloads) }.start()
    }

    fun deleteAll() {
        Thread { downloadDao.deleteAll() }.start()
    }

    fun delete(id: String) {
        Thread { downloadDao.delete(id) }.start()
    }
}
