package com.shirou.shibamusic.di

import android.content.Context
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.util.DebugLogger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Módulo Hilt para configuração otimizada do Coil ImageLoader.
 * 
 * Otimizações incluídas:
 * - Cache de memória ajustado para melhor performance
 * - Cache de disco para imagens frequentemente acessadas
 * - Crossfade desabilitado por padrão para listas (mais rápido)
 * - Políticas de cache agressivas para album art
 */
@Module
@InstallIn(SingletonComponent::class)
object CoilModule {

    @Provides
    @Singleton
    fun provideImageLoader(
        @ApplicationContext context: Context
    ): ImageLoader {
        return ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder(context)
                    // 25% da memória disponível para cache de imagens
                    .maxSizePercent(0.25)
                    .strongReferencesEnabled(true)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache"))
                    // 100MB de cache em disco para album art
                    .maxSizeBytes(100 * 1024 * 1024L)
                    .build()
            }
            // Cache agressivo - ideal para album art que raramente muda
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            // Desabilita crossfade por padrão para listas mais fluidas
            .crossfade(false)
            // Respeita o aspect ratio
            .respectCacheHeaders(false)
            // Logger apenas em debug
            .apply {
                if (com.shirou.shibamusic.BuildConfig.DEBUG) {
                    logger(DebugLogger())
                }
            }
            .build()
    }
}
