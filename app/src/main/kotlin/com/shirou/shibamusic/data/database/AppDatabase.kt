package com.shirou.shibamusic.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 4,
    exportSchema = true
)
abstract class ShibaMusicLocalDatabase : RoomDatabase() {

    abstract fun songDao(): SongDao
    abstract fun albumDao(): AlbumDao
    abstract fun artistDao(): ArtistDao
    abstract fun playlistDao(): PlaylistDao

    companion object {
        const val DATABASE_NAME = "ShibaMusic_local.db"

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_albums_artist_name`
                    ON `albums`(`artist_name`)
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_albums_artist_name_title`
                    ON `albums`(`artist_name`, `title`)
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_albums_year`
                    ON `albums`(`year`)
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_albums_play_count`
                    ON `albums`(`play_count`)
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_songs_artist_name`
                    ON `songs`(`artist_name`)
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_songs_album_name`
                    ON `songs`(`album_name`)
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_songs_last_played_timestamp`
                    ON `songs`(`last_played_timestamp`)
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_songs_play_count`
                    ON `songs`(`play_count`)
                    """.trimIndent()
                )
            }
        }

        /**
         * Migration 3 -> 4: Add composite indexes for optimized sorting in Library tabs
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Songs - composite indexes for sorting
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_songs_title_id` ON `songs`(`title`, `id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_songs_artist_name_title` ON `songs`(`artist_name`, `title`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_songs_album_name_title` ON `songs`(`album_name`, `title`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_songs_duration_ms_title` ON `songs`(`duration_ms`, `title`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_songs_date_added_title` ON `songs`(`date_added`, `title`)")
                
                // Albums - composite indexes for sorting
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_albums_title_id` ON `albums`(`title`, `id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_albums_year_title` ON `albums`(`year`, `title`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_albums_date_added_title` ON `albums`(`date_added`, `title`)")
                
                // Artists - indexes for sorting and filtering
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_artists_name_id` ON `artists`(`name`, `id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_artists_album_count` ON `artists`(`album_count`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_artists_song_count` ON `artists`(`song_count`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_artists_date_added` ON `artists`(`date_added`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_artists_is_favorite` ON `artists`(`is_favorite`)")
                
                // Playlists - indexes for sorting
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_playlists_name_id` ON `playlists`(`name`, `id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_playlists_date_created` ON `playlists`(`date_created`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_playlists_song_count` ON `playlists`(`song_count`)")
            }
        }
    }
}
