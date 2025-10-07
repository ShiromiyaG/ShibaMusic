package com.shirou.shibamusic.di

import android.content.Context
import androidx.room.Room
import com.shirou.shibamusic.data.database.ShibaMusicLocalDatabase
import com.shirou.shibamusic.data.database.dao.AlbumDao
import com.shirou.shibamusic.data.database.dao.ArtistDao
import com.shirou.shibamusic.data.database.dao.PlaylistDao
import com.shirou.shibamusic.data.database.dao.SongDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): ShibaMusicLocalDatabase = Room.databaseBuilder(
        context,
        ShibaMusicLocalDatabase::class.java,
        ShibaMusicLocalDatabase.DATABASE_NAME
    )
        .fallbackToDestructiveMigration()
        .build()

    @Provides
    fun provideSongDao(database: ShibaMusicLocalDatabase): SongDao = database.songDao()

    @Provides
    fun provideAlbumDao(database: ShibaMusicLocalDatabase): AlbumDao = database.albumDao()

    @Provides
    fun provideArtistDao(database: ShibaMusicLocalDatabase): ArtistDao = database.artistDao()

    @Provides
    fun providePlaylistDao(database: ShibaMusicLocalDatabase): PlaylistDao = database.playlistDao()
}
