package com.shirou.shibamusic.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

import com.shirou.shibamusic.model.Server

@Dao
interface ServerDao {
    @Query("SELECT * FROM server")
    fun getAll(): LiveData<List<Server>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(server: Server)

    @Delete
    fun delete(server: Server)
}
