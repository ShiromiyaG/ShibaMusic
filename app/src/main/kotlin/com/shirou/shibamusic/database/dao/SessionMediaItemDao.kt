package com.shirou.shibamusic.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

import com.shirou.shibamusic.model.SessionMediaItem

@Dao
interface SessionMediaItemDao {
    @Query("SELECT * FROM session_media_item WHERE id = :id")
    fun get(id: String): SessionMediaItem?

    @Query("SELECT * FROM session_media_item WHERE timestamp = :timestamp")
    fun get(timestamp: Long): List<SessionMediaItem>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(sessionMediaItem: SessionMediaItem)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertAll(sessionMediaItems: List<SessionMediaItem>)

    @Query("DELETE FROM session_media_item")
    fun deleteAll()
}
