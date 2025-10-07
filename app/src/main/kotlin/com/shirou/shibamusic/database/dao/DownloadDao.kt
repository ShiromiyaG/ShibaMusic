package com.shirou.shibamusic.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.shirou.shibamusic.model.Download

@Dao
interface DownloadDao {
    @Query("SELECT * FROM download WHERE download_state = 1 ORDER BY artist, album, discNumber, track ASC")
    fun getAll(): LiveData<List<Download>>

    @Query("SELECT * FROM download WHERE id = :id")
    fun getOne(id: String): Download?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(download: Download)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(downloads: List<Download>)

    @Query("UPDATE download SET download_state = 1 WHERE id = :id")
    fun update(id: String)

    @Query("DELETE FROM download WHERE id = :id")
    fun delete(id: String)

    @Query("DELETE FROM download")
    fun deleteAll()
}
