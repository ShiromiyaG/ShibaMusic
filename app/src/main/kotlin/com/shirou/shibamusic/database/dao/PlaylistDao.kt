package com.shirou.shibamusic.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

import com.shirou.shibamusic.subsonic.models.Playlist

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlist")
    fun getAll(): LiveData<List<Playlist>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(playlist: Playlist)

    @Delete
    fun delete(playlist: Playlist)
}
