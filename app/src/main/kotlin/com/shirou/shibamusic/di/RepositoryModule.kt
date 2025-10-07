package com.shirou.shibamusic.di

import com.shirou.shibamusic.data.repository.MusicRepository
import com.shirou.shibamusic.data.repository.MusicRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing Repository dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    
    @Binds
    @Singleton
    abstract fun bindMusicRepository(
        impl: MusicRepositoryImpl
    ): MusicRepository
}
