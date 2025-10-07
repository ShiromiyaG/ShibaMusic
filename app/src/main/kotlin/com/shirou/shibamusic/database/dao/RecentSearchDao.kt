package com.shirou.shibamusic.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.shirou.shibamusic.model.RecentSearch
import kotlin.collections.List

@Dao
interface RecentSearchDao {
    @Query("SELECT * FROM recent_search ORDER BY search DESC")
    fun getRecent(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(search: RecentSearch)

    @Delete
    fun delete(search: RecentSearch)
}
