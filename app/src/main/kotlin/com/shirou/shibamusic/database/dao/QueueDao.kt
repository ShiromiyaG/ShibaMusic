package com.shirou.shibamusic.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.shirou.shibamusic.model.Queue

@Dao
interface QueueDao {
    @Query("SELECT * FROM queue")
    fun getAll(): LiveData<List<Queue>>

    @Query("SELECT * FROM queue")
    fun getAllSimple(): List<Queue>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(songQueueObject: Queue)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(songQueueObjects: List<Queue>)

    @Query("DELETE FROM queue WHERE queue.track_order=:position")
    fun delete(position: Int)

    @Query("DELETE FROM queue")
    fun deleteAll()

    @Query("SELECT COUNT(*) FROM queue")
    fun count(): Int

    @Query("UPDATE queue SET last_play=:timestamp WHERE id=:id")
    fun setLastPlay(id: String, timestamp: Long)

    @Query("UPDATE queue SET playing_changed=:timestamp WHERE id=:id")
    fun setPlayingChanged(id: String, timestamp: Long)

    @Query("SELECT * FROM queue ORDER BY last_play DESC LIMIT 1")
    fun getLastPlayed(): Queue? // Assuming Room might return null if no item matches
}
