package com.shirou.shibamusic.util

import android.app.Notification
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.Log

import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.DatabaseProvider
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.RenderersFactory
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadNotificationHelper

import com.shirou.shibamusic.R
import com.shirou.shibamusic.service.DownloaderManager // Assumed to exist

import java.io.File
import java.net.CookieHandler
import java.net.CookieManager
import java.net.CookiePolicy
import java.util.ArrayList
import java.util.concurrent.Executors

@UnstableApi
object DownloadUtil {

    const val DOWNLOAD_NOTIFICATION_CHANNEL_ID = "download_channel"
    const val DOWNLOAD_NOTIFICATION_SUCCESSFUL_GROUP = "com.ShiromiyaG.ShibaMusic.SuccessfulDownload"
    const val DOWNLOAD_NOTIFICATION_FAILED_GROUP = "com.ShiromiyaG.ShibaMusic.FailedDownload"

    private const val STREAMING_CACHE_CONTENT_DIRECTORY = "streaming_cache"
    private const val DOWNLOAD_CONTENT_DIRECTORY = "downloads"

    private var dataSourceFactory: DataSource.Factory? = null
    private var httpDataSourceFactory: DataSource.Factory? = null
    private var databaseProvider: DatabaseProvider? = null
    private var streamingCacheDirectory: File? = null
    private var downloadDirectory: File? = null
    private var downloadCache: Cache? = null
    private var streamingCache: SimpleCache? = null
    private var downloadManager: DownloadManager? = null
    private var downloaderManager: DownloaderManager? = null
    private var downloadNotificationHelper: DownloadNotificationHelper? = null
    private var appIconBitmap: Bitmap? = null
    private var appIconCompat: IconCompat? = null

    fun useExtensionRenderers(): Boolean {
        return true
    }

    fun buildRenderersFactory(context: Context, preferExtensionRenderer: Boolean): RenderersFactory {
    val extensionRendererMode = if (useExtensionRenderers()) {
            if (preferExtensionRenderer) DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
            else DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON
        } else {
            DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF
        }

        return DefaultRenderersFactory(context.applicationContext)
            .setExtensionRendererMode(extensionRendererMode)
    }

    @Synchronized
    fun getHttpDataSourceFactory(): DataSource.Factory {
        return httpDataSourceFactory ?: run {
            val cookieManager = CookieManager()
            cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER)
            CookieHandler.setDefault(cookieManager)
            DefaultHttpDataSource.Factory()
                .setAllowCrossProtocolRedirects(true)
                .also { httpDataSourceFactory = it }
        }
    }

    @Synchronized
    fun getDataSourceFactory(context: Context): DataSource.Factory {
        return dataSourceFactory ?: run {
            val appContext = context.applicationContext
            val upstreamFactory = DefaultDataSource.Factory(appContext, getHttpDataSourceFactory())

            if (Preferences.getStreamingCacheSize() > 0) {
                CacheDataSource.Factory()
                    .setCache(getStreamingCache(appContext))
                    .setUpstreamDataSourceFactory(upstreamFactory)
                    .also { dataSourceFactory = it }
            } else {
                upstreamFactory.also { dataSourceFactory = it }
            }
        }
    }

    @Synchronized
    fun getDownloadNotificationHelper(context: Context): DownloadNotificationHelper {
        return downloadNotificationHelper ?: run {
            DownloadNotificationHelper(context, DOWNLOAD_NOTIFICATION_CHANNEL_ID)
                .also { downloadNotificationHelper = it }
        }
    }

    @Synchronized
    fun getDownloadManager(context: Context): DownloadManager {
        ensureDownloadManagerInitialized(context)
        return downloadManager!!
    }

    @Synchronized
    fun getDownloadTracker(context: Context): DownloaderManager {
        ensureDownloadManagerInitialized(context)
        return downloaderManager!!
    }

    @Synchronized
    private fun getDownloadCache(context: Context): Cache {
        return downloadCache ?: run {
            val downloadContentDirectory = File(getDownloadDirectory(context), DOWNLOAD_CONTENT_DIRECTORY)
            SimpleCache(downloadContentDirectory, NoOpCacheEvictor(), getDatabaseProvider(context))
                .also { downloadCache = it }
        }
    }

    @Synchronized
    private fun getStreamingCache(context: Context): SimpleCache {
        return streamingCache ?: run {
            val streamingCacheDir = File(getStreamingCacheDirectory(context), STREAMING_CACHE_CONTENT_DIRECTORY)

            SimpleCache(
                streamingCacheDir,
                LeastRecentlyUsedCacheEvictor(Preferences.getStreamingCacheSize() * 1024 * 1024),
                getDatabaseProvider(context)
            ).also { streamingCache = it }
        }
    }

    @Synchronized
    private fun ensureDownloadManagerInitialized(context: Context) {
        if (downloadManager == null) {
            downloadManager = DownloadManager(
                context,
                getDatabaseProvider(context),
                getDownloadCache(context),
                getHttpDataSourceFactory(),
                Executors.newFixedThreadPool(6)
            )

            downloaderManager = DownloaderManager(context, getHttpDataSourceFactory(), downloadManager!!)
        }
    }

    @Synchronized
    private fun getDatabaseProvider(context: Context): DatabaseProvider {
        return databaseProvider ?: run {
            StandaloneDatabaseProvider(context)
                .also { databaseProvider = it }
        }
    }

    @Synchronized
    private fun getStreamingCacheDirectory(context: Context): File {
        return streamingCacheDirectory ?: run {
            try {
                if (Preferences.getStreamingCacheStoragePreference() == 0) {
                    // Armazenamento interno
                    context.getExternalFilesDirs(null).firstOrNull()?.also {
                        streamingCacheDirectory = it
                    } ?: run {
                        Log.w("DownloadUtil", "External files dir not available for cache, using internal")
                        streamingCacheDirectory = context.filesDir
                    }
                } else {
                    // Tentar armazenamento externo (cartão SD)
                    try {
                        val externalDirs = context.getExternalFilesDirs(null)
                        if (externalDirs.size > 1 && externalDirs[1] != null) {
                            streamingCacheDirectory = externalDirs[1]
                            Log.d("DownloadUtil", "Using external storage for streaming cache")
                        } else {
                            Log.w("DownloadUtil", "External storage not available for cache, falling back to internal")
                            streamingCacheDirectory = externalDirs.firstOrNull()
                            if (streamingCacheDirectory == null) {
                                streamingCacheDirectory = context.filesDir
                            }
                            // Atualizar preferência para refletir mudança
                            Preferences.setStreamingCacheStoragePreference(0)
                        }
                    } catch (exception: Exception) {
                        Log.e("DownloadUtil", "Error accessing external storage for cache", exception)
                        context.getExternalFilesDirs(null).firstOrNull()?.also {
                            streamingCacheDirectory = it
                        } ?: run {
                            streamingCacheDirectory = context.filesDir
                        }
                        Preferences.setStreamingCacheStoragePreference(0)
                    }
                }

                // Garantir que o diretório existe
                streamingCacheDirectory?.let {
                    if (!it.exists()) {
                        val created = it.mkdirs()
                        Log.d("DownloadUtil", "Streaming cache directory created: $created - ${it.absolutePath}")
                    }
                }

            } catch (e: Exception) {
                Log.e("DownloadUtil", "Error setting up streaming cache directory", e)
                // Fallback final para diretório interno da app
                streamingCacheDirectory = context.filesDir
                Preferences.setStreamingCacheStoragePreference(0)
            }
            streamingCacheDirectory!!
        }
    }

    @Synchronized
    private fun getDownloadDirectory(context: Context): File {
        return downloadDirectory ?: run {
            try {
                if (Preferences.getDownloadStoragePreference() == 0) {
                    // Armazenamento interno
                    context.getExternalFilesDirs(null).firstOrNull()?.also {
                        downloadDirectory = it
                    } ?: run {
                        Log.w("DownloadUtil", "External files dir not available, using internal")
                        downloadDirectory = context.filesDir
                    }
                } else {
                    // Tentar armazenamento externo (cartão SD)
                    try {
                        val externalDirs = context.getExternalFilesDirs(null)
                        if (externalDirs.size > 1 && externalDirs[1] != null) {
                            downloadDirectory = externalDirs[1]
                            Log.d("DownloadUtil", "Using external storage for downloads")
                        } else {
                            Log.w("DownloadUtil", "External storage not available, falling back to internal")
                            downloadDirectory = externalDirs.firstOrNull()
                            if (downloadDirectory == null) {
                                downloadDirectory = context.filesDir
                            }
                            // Atualizar preferência para refletir mudança
                            Preferences.setDownloadStoragePreference(0)
                        }
                    } catch (exception: Exception) {
                        Log.e("DownloadUtil", "Error accessing external storage", exception)
                        context.getExternalFilesDirs(null).firstOrNull()?.also {
                            downloadDirectory = it
                        } ?: run {
                            downloadDirectory = context.filesDir
                        }
                        Preferences.setDownloadStoragePreference(0)
                    }
                }

                // Garantir que o diretório existe
                downloadDirectory?.let {
                    if (!it.exists()) {
                        val created = it.mkdirs()
                        Log.d("DownloadUtil", "Download directory created: $created - ${it.absolutePath}")
                    }
                }

            } catch (e: Exception) {
                Log.e("DownloadUtil", "Error setting up download directory", e)
                // Fallback final para diretório interno da app
                downloadDirectory = context.filesDir
                Preferences.setDownloadStoragePreference(0)
            }
            downloadDirectory!!
        }
    }

    private fun buildReadOnlyCacheDataSource(
        upstreamFactory: DataSource.Factory,
        cache: Cache
    ): CacheDataSource.Factory {
        return CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setCacheWriteDataSinkFactory(null)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    @Synchronized
    fun eraseDownloadFolder(context: Context) {
        val directory = getDownloadDirectory(context)

        val files = listFiles(directory, ArrayList())

        for (file in files) {
            file.delete()
        }
    }

    @Synchronized
    private fun listFiles(directory: File, files: ArrayList<File>): ArrayList<File> {
        if (directory.isDirectory) {
            directory.listFiles()?.let { list ->
                for (file in list) {
                    if (file.isFile && file.name.lowercase().endsWith(".exo")) {
                        files.add(file)
                    } else if (file.isDirectory) {
                        listFiles(file, files)
                    }
                }
            }
        }
        return files
    }

    @Synchronized
    fun getStreamingCacheSize(context: Context): Long {
        return getStreamingCache(context).cacheSpace
    }

    fun buildGroupSummaryNotification(
        context: Context,
        channelId: String,
        groupId: String,
        icon: Int,
        title: String
    ): Notification {
        return NotificationCompat.Builder(context, channelId)
            .setContentTitle(title)
            .setSmallIcon(getAppIconCompat(context))
            .setLargeIcon(getAppIconBitmap(context))
            .setGroup(groupId)
            .setGroupSummary(true)
            .build()
    }

    @Synchronized
    fun getAppIconBitmap(context: Context): Bitmap {
        return appIconBitmap ?: run {
            val drawable = ContextCompat.getDrawable(context, R.drawable.shiba_vector)
                ?: throw IllegalStateException("Missing shiba_vector drawable resource")
            val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 128
            val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 128
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            appIconBitmap = bitmap
            bitmap
        }
    }

    @Synchronized
    fun getAppIconCompat(context: Context): IconCompat {
        return appIconCompat ?: IconCompat.createWithResource(context, R.drawable.shiba_vector).also {
            appIconCompat = it
        }
    }
}
