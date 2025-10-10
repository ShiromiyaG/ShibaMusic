package com.shirou.shibamusic.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import com.shirou.shibamusic.data.dao.OfflineTrackDao
import com.shirou.shibamusic.data.database.ShibaMusicDatabase
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

/**
 * Módulo Hilt para prover dependências relacionadas ao banco de dados
 * Inclui suporte para codec Opus em downloads offline
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideShibaMusicDatabase(
        @ApplicationContext context: Context
    ): ShibaMusicDatabase {
        val ioExecutor = Dispatchers.IO.asExecutor()
        return Room.databaseBuilder(
            context,
            ShibaMusicDatabase::class.java,
            ShibaMusicDatabase.DATABASE_NAME
        )
        .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
        .setQueryExecutor(ioExecutor)
        .setTransactionExecutor(ioExecutor)
        .addMigrations(
            ShibaMusicDatabase.MIGRATION_1_2,
            ShibaMusicDatabase.MIGRATION_2_3 // Nova migração para codec
        )
        .fallbackToDestructiveMigration() // Para desenvolvimento, remover em produção
        .build()
    }
    
    @Provides
    @Singleton
    fun provideShibaMusicLocalDatabase(
        @ApplicationContext context: Context
    ): ShibaMusicLocalDatabase {
        val ioExecutor = Dispatchers.IO.asExecutor()
        return Room.databaseBuilder(
            context,
            ShibaMusicLocalDatabase::class.java,
            ShibaMusicLocalDatabase.DATABASE_NAME
        )
        .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
        .setQueryExecutor(ioExecutor)
        .setTransactionExecutor(ioExecutor)
        .addMigrations(
            ShibaMusicLocalDatabase.MIGRATION_2_3
        )
        .fallbackToDestructiveMigration()
        .build()
    }
    
    @Provides
    fun provideOfflineTrackDao(
        database: ShibaMusicDatabase
    ): OfflineTrackDao {
        return database.offlineTrackDao()
    }
    
    @Provides
    fun provideAlbumDao(
        database: ShibaMusicLocalDatabase
    ): AlbumDao {
        return database.albumDao()
    }
    
    @Provides
    fun provideArtistDao(
        database: ShibaMusicLocalDatabase
    ): ArtistDao {
        return database.artistDao()
    }
    
    @Provides
    fun providePlaylistDao(
        database: ShibaMusicLocalDatabase
    ): PlaylistDao {
        return database.playlistDao()
    }
    
    @Provides
    fun provideSongDao(
        database: ShibaMusicLocalDatabase
    ): SongDao {
        return database.songDao()
    }
}
