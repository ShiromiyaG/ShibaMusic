package com.shirou.shibamusic.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

import com.shirou.shibamusic.App
import com.shirou.shibamusic.database.AppDatabase
import com.shirou.shibamusic.database.dao.QueueDao
import com.shirou.shibamusic.model.Queue
import com.shirou.shibamusic.subsonic.base.ApiResponse
import com.shirou.shibamusic.subsonic.models.Child
import com.shirou.shibamusic.subsonic.models.PlayQueue

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class QueueRepository {

    private companion object {
        private const val TAG = "QueueRepository"
    }

    private val queueDao: QueueDao = AppDatabase.getInstance().queueDao()

    fun getLiveQueue(): LiveData<List<Queue>> = queueDao.getAll()

    fun getMedia(): List<Child> {
        var media: List<Child> = emptyList()

        val getMediaTask = GetMediaThreadSafe(queueDao)
        val thread = Thread(getMediaTask)
        thread.start()

        try {
            thread.join()
            media = getMediaTask.media.map { it as Child }
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        return media
    }

    fun getPlayQueue(): MutableLiveData<PlayQueue?> {
        val playQueue = MutableLiveData<PlayQueue?>(null)

        App.getSubsonicClientInstance(false)
            .bookmarksClient
            .getPlayQueue()
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    val receivedPlayQueue = response.body()?.subsonicResponse?.playQueue
                    if (response.isSuccessful && receivedPlayQueue != null) {
                        playQueue.postValue(receivedPlayQueue)
                    } else if (!response.isSuccessful) {
                        playQueue.postValue(null)
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    playQueue.postValue(null)
                }
            })

        return playQueue
    }

    fun savePlayQueue(ids: List<String>, current: String, position: Long) {
        App.getSubsonicClientInstance(false)
            .bookmarksClient
            .savePlayQueue(ids, current, position)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    // Empty body as per original Java
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    // Empty body as per original Java
                }
            })
    }

    fun insert(media: Child, reset: Boolean, afterIndex: Int) {
        try {
            var mediaList: MutableList<Queue> = mutableListOf()

            if (!reset) {
                val getMediaThreadSafe = GetMediaThreadSafe(queueDao)
                val getMediaThread = Thread(getMediaThreadSafe)
                getMediaThread.start()
                getMediaThread.join()

                mediaList = getMediaThreadSafe.media.toMutableList()
            }

            val queueItem = Queue(media)
            mediaList.add(afterIndex, queueItem)

            mediaList.forEachIndexed { index, queue ->
                queue.trackOrder = index
            }

            Thread(DeleteAllThreadSafe(queueDao)).apply {
                start()
                join()
            }

            Thread(InsertAllThreadSafe(queueDao, mediaList)).apply {
                start()
                join()
            }
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    fun insertAll(toAdd: List<Child>, reset: Boolean, afterIndex: Int) {
        try {
            var media: MutableList<Queue> = mutableListOf()

            if (!reset) {
                val getMediaThreadSafe = GetMediaThreadSafe(queueDao)
                val getMediaThread = Thread(getMediaThreadSafe)
                getMediaThread.start()
                getMediaThread.join()

                media = getMediaThreadSafe.media.toMutableList()
            }

            for (i in toAdd.indices) {
                val queueItem = Queue(toAdd[i])
                media.add(afterIndex + i, queueItem)
            }

            media.forEachIndexed { index, queue ->
                queue.trackOrder = index
            }

            Thread(DeleteAllThreadSafe(queueDao)).apply {
                start()
                join()
            }

            Thread(InsertAllThreadSafe(queueDao, media)).apply {
                start()
                join()
            }
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    fun delete(position: Int) {
        val deleteTask = DeleteThreadSafe(queueDao, position)
        val thread = Thread(deleteTask)
        thread.start()
    }

    fun deleteAll() {
        val deleteAllTask = DeleteAllThreadSafe(queueDao)
        val thread = Thread(deleteAllTask)
        thread.start()
    }

    fun count(): Int {
        var count = 0

        val countThreadTask = CountThreadSafe(queueDao)
        val thread = Thread(countThreadTask)
        thread.start()

        try {
            thread.join()
            count = countThreadTask.count
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        return count
    }

    fun setLastPlayedTimestamp(id: String) {
        val timestampTask = SetLastPlayedTimestampThreadSafe(queueDao, id)
        val thread = Thread(timestampTask)
        thread.start()
    }

    fun setPlayingPausedTimestamp(id: String, ms: Long) {
        val timestampTask = SetPlayingPausedTimestampThreadSafe(queueDao, id, ms)
        val thread = Thread(timestampTask)
        thread.start()
    }

    fun getLastPlayedMediaIndex(): Int {
        var index = 0

        val getLastPlayedMediaThreadSafe = GetLastPlayedMediaThreadSafe(queueDao)
        val thread = Thread(getLastPlayedMediaThreadSafe)
        thread.start()

        try {
            thread.join()
            val lastMediaPlayed: Queue = getLastPlayedMediaThreadSafe.queueItem!!
            index = lastMediaPlayed.trackOrder
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        return index
    }

    fun getLastPlayedMediaTimestamp(): Long {
        var timestamp: Long = 0

        val getLastPlayedMediaThreadSafe = GetLastPlayedMediaThreadSafe(queueDao)
        val thread = Thread(getLastPlayedMediaThreadSafe)
        thread.start()

        try {
            thread.join()
            val lastMediaPlayed: Queue = getLastPlayedMediaThreadSafe.queueItem!!
            timestamp = lastMediaPlayed.playingChanged
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        return timestamp
    }

    private class GetMediaThreadSafe(private val queueDao: QueueDao) : Runnable {
        var media: List<Queue> = emptyList()

        override fun run() {
            media = queueDao.getAllSimple()
        }
    }

    private class InsertAllThreadSafe(private val queueDao: QueueDao, private val media: List<Queue>) : Runnable {
        override fun run() {
            queueDao.insertAll(media)
        }
    }

    private class DeleteThreadSafe(private val queueDao: QueueDao, private val position: Int) : Runnable {
        override fun run() {
            queueDao.delete(position)
        }
    }

    private class DeleteAllThreadSafe(private val queueDao: QueueDao) : Runnable {
        override fun run() {
            queueDao.deleteAll()
        }
    }

    private class CountThreadSafe(private val queueDao: QueueDao) : Runnable {
        var count: Int = 0
            private set

        override fun run() {
            count = queueDao.count()
        }
    }

    private class SetLastPlayedTimestampThreadSafe(private val queueDao: QueueDao, private val mediaId: String) : Runnable {
        override fun run() {
            queueDao.setLastPlay(mediaId, System.currentTimeMillis())
        }
    }

    private class SetPlayingPausedTimestampThreadSafe(
        private val queueDao: QueueDao,
        private val mediaId: String,
        private val ms: Long
    ) : Runnable {
        override fun run() {
            queueDao.setPlayingChanged(mediaId, ms)
        }
    }

    private class GetLastPlayedMediaThreadSafe(private val queueDao: QueueDao) : Runnable {
        var queueItem: Queue? = null

        override fun run() {
            queueItem = queueDao.getLastPlayed()
        }
    }
}
