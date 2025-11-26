package com.shirou.shibamusic.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.shirou.shibamusic.App
import com.shirou.shibamusic.database.dao.QueueDao
import com.shirou.shibamusic.model.Queue
import com.shirou.shibamusic.subsonic.base.ApiResponse
import com.shirou.shibamusic.subsonic.models.Child
import com.shirou.shibamusic.subsonic.models.PlayQueue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject

class QueueRepository @Inject constructor(
    private val queueDao: QueueDao
) {

    private companion object {
        private const val TAG = "QueueRepository"
    }
    
    private val scope = CoroutineScope(Dispatchers.IO)

    fun getLiveQueue(): LiveData<List<Queue>> = queueDao.getAll()

    fun getMedia(): List<Child> = runBlocking(Dispatchers.IO) {
        queueDao.getAllSimple().map { it as Child }
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
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {}
                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {}
            })
    }

    fun insert(media: Child, reset: Boolean, afterIndex: Int) = runBlocking(Dispatchers.IO) {
        val mediaList = if (!reset) {
            queueDao.getAllSimple().toMutableList()
        } else {
            mutableListOf()
        }

        val queueItem = Queue(media)
        mediaList.add(afterIndex, queueItem)

        mediaList.forEachIndexed { index, queue ->
            queue.trackOrder = index
        }

        queueDao.deleteAll()
        queueDao.insertAll(mediaList)
    }

    fun insertAll(toAdd: List<Child>, reset: Boolean, afterIndex: Int) = runBlocking(Dispatchers.IO) {
        val media = if (!reset) {
            queueDao.getAllSimple().toMutableList()
        } else {
            mutableListOf()
        }

        for (i in toAdd.indices) {
            val queueItem = Queue(toAdd[i])
            media.add(afterIndex + i, queueItem)
        }

        media.forEachIndexed { index, queue ->
            queue.trackOrder = index
        }

        queueDao.deleteAll()
        queueDao.insertAll(media)
    }

    fun delete(position: Int) {
        scope.launch {
            queueDao.delete(position)
        }
    }

    fun deleteAll() {
        scope.launch {
            queueDao.deleteAll()
        }
    }

    fun count(): Int = runBlocking(Dispatchers.IO) {
        queueDao.count()
    }

    fun setLastPlayedTimestamp(id: String) {
        scope.launch {
            queueDao.setLastPlay(id, System.currentTimeMillis())
        }
    }

    fun setPlayingPausedTimestamp(id: String, ms: Long) {
        scope.launch {
            queueDao.setPlayingChanged(id, ms)
        }
    }

    fun getLastPlayedMediaIndex(): Int = runBlocking(Dispatchers.IO) {
        queueDao.getLastPlayed()?.trackOrder ?: 0
    }

    fun getLastPlayedMediaTimestamp(): Long = runBlocking(Dispatchers.IO) {
        queueDao.getLastPlayed()?.playingChanged ?: 0L
    }
}
