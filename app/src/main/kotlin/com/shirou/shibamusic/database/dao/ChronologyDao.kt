package com.shirou.shibamusic.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

import com.shirou.shibamusic.model.Chronology

@Dao
interface ChronologyDao {
    @Query("SELECT * FROM chronology WHERE server == :server GROUP BY id ORDER BY timestamp DESC LIMIT :count")
    fun getLastPlayed(server: String, count: Int): LiveData<List<Chronology>>
    
    @Query("SELECT * FROM chronology WHERE server == :server GROUP BY id ORDER BY timestamp DESC LIMIT :count")
    suspend fun getLastPlayedSuspend(server: String, count: Int): List<Chronology>

    @Query("SELECT * FROM chronology WHERE timestamp >= :endDate AND timestamp < :startDate AND server == :server GROUP BY id ORDER BY COUNT(id) DESC LIMIT 20")
    fun getAllFrom(startDate: Long, endDate: Long, server: String): LiveData<List<Chronology>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(chronologyObject: Chronology)
}
