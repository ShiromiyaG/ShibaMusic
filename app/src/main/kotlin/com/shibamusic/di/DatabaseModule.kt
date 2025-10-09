package com.shibamusic.di

import android.content.Context
import androidx.room.Room
import com.shibamusic.data.dao.OfflineTrackDao
import com.shibamusic.data.database.ShibaMusicDatabase
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
        return Room.databaseBuilder(
            context,
            ShibaMusicDatabase::class.java,
            ShibaMusicDatabase.DATABASE_NAME
        )
        .addMigrations(
            ShibaMusicDatabase.MIGRATION_1_2,
            ShibaMusicDatabase.MIGRATION_2_3 // Nova migração para codec
        )
        .fallbackToDestructiveMigration() // Para desenvolvimento, remover em produção
        .build()
    }
    
    @Provides
    fun provideOfflineTrackDao(
        database: ShibaMusicDatabase
    ): OfflineTrackDao {
        return database.offlineTrackDao()
    }
}