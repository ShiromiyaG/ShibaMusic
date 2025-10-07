package com.shirou.shibamusic.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.shirou.shibamusic.data.database.dao.AlbumDao
import com.shirou.shibamusic.data.database.dao.ArtistDao
import com.shirou.shibamusic.data.database.dao.PlaylistDao
import com.shirou.shibamusic.data.database.dao.SongDao
import com.shirou.shibamusic.data.database.entity.AlbumEntity
import com.shirou.shibamusic.data.database.entity.ArtistEntity
import com.shirou.shibamusic.data.database.entity.PlaylistEntity
import com.shirou.shibamusic.data.database.entity.PlaylistSongEntity
import com.shirou.shibamusic.data.database.entity.SongEntity

@Database(
    entities = [
        SongEntity::class,
        AlbumEntity::class,
        ArtistEntity::class,
        PlaylistEntity::class,
        PlaylistSongEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class ShibaMusicLocalDatabase : RoomDatabase() {

    abstract fun songDao(): SongDao
    abstract fun albumDao(): AlbumDao
    abstract fun artistDao(): ArtistDao
    abstract fun playlistDao(): PlaylistDao

    companion object {
        const val DATABASE_NAME = "ShibaMusic_local.db"
    }
}
