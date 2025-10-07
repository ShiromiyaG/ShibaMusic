package com.shirou.shibamusic.service

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.util.Assertions.checkNotNull
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.datasource.DataSource
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadHelper
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadIndex
import androidx.media3.exoplayer.offline.DownloadService
import com.shirou.shibamusic.repository.DownloadRepository
import com.shirou.shibamusic.util.DownloadUtil
import java.io.IOException

@UnstableApi
class DownloaderManager(
    context: Context,
    private val dataSourceFactory: DataSource.Factory,
    downloadManager: DownloadManager
) {
    private val context: Context = context.applicationContext
    private val downloadIndex: DownloadIndex

    init {
        // Initializes the companion object's cache to mirror Java's static field behavior.
        Companion.resetDownloadsCache()
        downloadIndex = downloadManager.downloadIndex
        loadDownloads()
    }

    private fun buildDownloadRequest(mediaItem: MediaItem): DownloadRequest {
        return DownloadHelper
            .forMediaItem(
                context,
                mediaItem,
                DownloadUtil.buildRenderersFactory(context, false),
                dataSourceFactory
            )
            .getDownloadRequest(Util.getUtf8Bytes(checkNotNull(mediaItem.mediaId)))
            .copyWithId(mediaItem.mediaId)
    }

    fun isDownloaded(mediaId: String): Boolean {
        return Companion.getCachedDownload(mediaId)?.state != Download.STATE_FAILED
    }

    fun isDownloaded(mediaItem: MediaItem): Boolean {
        return isDownloaded(mediaItem.mediaId)
    }

    fun areDownloaded(mediaItems: List<MediaItem>): Boolean {
        return mediaItems.any(::isDownloaded)
    }

    fun download(mediaItem: MediaItem, download: com.shirou.shibamusic.model.Download) {
        download.downloadUri = mediaItem.requestMetadata.mediaUri.toString()
        DownloadService.sendAddDownload(context, DownloaderService::class.java, buildDownloadRequest(mediaItem), false)
        insertDatabase(download)
    }

    fun download(mediaItems: List<MediaItem>, downloads: List<com.shirou.shibamusic.model.Download>) {
        mediaItems.zip(downloads).forEach { (mediaItem, download) ->
            download(mediaItem, download)
        }
    }

    fun remove(mediaItem: MediaItem, download: com.shirou.shibamusic.model.Download) {
        DownloadService.sendRemoveDownload(context, DownloaderService::class.java, buildDownloadRequest(mediaItem).id, false)
        deleteDatabase(download.id)
        Companion.removeCachedDownload(download.id)
    }

    fun remove(mediaItems: List<MediaItem>, downloads: List<com.shirou.shibamusic.model.Download>) {
        mediaItems.zip(downloads).forEach { (mediaItem, download) ->
            remove(mediaItem, download)
        }
    }

    fun removeAll() {
        DownloadService.sendRemoveAllDownloads(context, DownloaderService::class.java, false)
        deleteAllDatabase()
        DownloadUtil.eraseDownloadFolder(context)
        Companion.clearCachedDownloads()
    }

    private fun loadDownloads() {
        try {
            downloadIndex.getDownloads().use { loadedDownloads ->
                Companion.resetDownloadsCache()
                while (loadedDownloads.moveToNext()) {
                    val download = loadedDownloads.download
                    Companion.cacheDownload(download)
                }
            }
        } catch (e: IOException) {
            Log.w(TAG, "Failed to query downloads", e)
        }
    }

    companion object {
        private const val TAG = "DownloaderManager"

        private val downloads: MutableMap<String, Download> = hashMapOf()

        internal fun resetDownloadsCache() {
            downloads.clear()
        }

        internal fun cacheDownload(download: Download) {
            downloads[download.request.id] = download
        }

        internal fun getCachedDownload(id: String): Download? {
            return downloads[id]
        }

        internal fun removeCachedDownload(id: String) {
            downloads.remove(id)
        }

        internal fun clearCachedDownloads() {
            downloads.clear()
        }

        fun getDownloadNotificationMessage(id: String): String? {
            val download = getDownloadRepository().getDownload(id)
            return download?.title
        }

        fun updateRequestDownload(download: Download) {
            updateDatabase(download.request.id)
            cacheDownload(download)
        }

        fun removeRequestDownload(download: Download) {
            deleteDatabase(download.request.id)
            removeCachedDownload(download.request.id)
        }

        private fun getDownloadRepository(): DownloadRepository {
            return DownloadRepository()
        }

        private fun insertDatabase(download: com.shirou.shibamusic.model.Download) {
            getDownloadRepository().insert(download)
        }

        private fun deleteDatabase(id: String) {
            getDownloadRepository().delete(id)
        }

        private fun deleteAllDatabase() {
            getDownloadRepository().deleteAll()
        }

        private fun updateDatabase(id: String) {
            getDownloadRepository().update(id)
        }
    }
}
