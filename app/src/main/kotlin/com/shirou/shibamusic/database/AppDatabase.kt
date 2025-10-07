package com.shirou.shibamusic.database

import androidx.media3.common.util.UnstableApi
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.shirou.shibamusic.App
import com.shirou.shibamusic.database.converter.DateConverters
import com.shirou.shibamusic.database.dao.ChronologyDao
import com.shirou.shibamusic.database.dao.DownloadDao
import com.shirou.shibamusic.database.dao.FavoriteDao
import com.shirou.shibamusic.database.dao.PlaylistDao
import com.shirou.shibamusic.database.dao.QueueDao
import com.shirou.shibamusic.database.dao.RecentSearchDao
import com.shirou.shibamusic.database.dao.ServerDao
import com.shirou.shibamusic.database.dao.SessionMediaItemDao
import com.shirou.shibamusic.model.Chronology
import com.shirou.shibamusic.model.Download
import com.shirou.shibamusic.model.Favorite
import com.shirou.shibamusic.model.Queue
import com.shirou.shibamusic.model.RecentSearch
import com.shirou.shibamusic.model.Server
import com.shirou.shibamusic.model.SessionMediaItem
import com.shirou.shibamusic.subsonic.models.Playlist

@UnstableApi
@Database(
    version = 11,
    entities = [
        Server::class,
        RecentSearch::class,
        Favorite::class,
        Playlist::class,
        Queue::class,
        Download::class,
        Chronology::class,
        SessionMediaItem::class
    ]
)
@TypeConverters(DateConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun queueDao(): QueueDao
    abstract fun serverDao(): ServerDao
    abstract fun recentSearchDao(): RecentSearchDao
    abstract fun downloadDao(): DownloadDao
    abstract fun chronologyDao(): ChronologyDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun sessionMediaItemDao(): SessionMediaItemDao
    abstract fun playlistDao(): PlaylistDao

    companion object {
        private const val DB_NAME = "ShibaMusic_db"

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    App.getContext(),
                    AppDatabase::class.java,
                    DB_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
    }
}
