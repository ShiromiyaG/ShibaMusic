package com.shirou.shibamusic.util

import android.graphics.Bitmap
import android.util.Log
import com.shirou.shibamusic.App
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Simple disk cache for album artwork so we can re-use covers offline without re-syncing.
 *
 * We store artwork under the app's files directory to keep it available between sessions.
 * The cache is trimmed opportunistically whenever we add new artwork.
 */
object AlbumArtCache {
    private const val TAG = "AlbumArtCache"
    private const val DIRECTORY_NAME = "album_art"
    private const val FILE_EXTENSION = ".webp"
    private const val MAX_CACHE_BYTES = 50L * 1024L * 1024L // 50MB

    private val ioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val trimmingInProgress = AtomicBoolean(false)

    fun getCachedFile(coverArtId: String, requestedSize: Int): File? {
        val file = getCacheFile(coverArtId, requestedSize)
        return if (file.exists() && file.length() > 0) file else null
    }

    fun storeAsync(coverArtId: String, requestedSize: Int, bitmap: Bitmap) {
        ioScope.launch {
            runCatching {
                val file = getCacheFile(coverArtId, requestedSize)
                val parent = file.parentFile
                if (parent == null) {
                    Log.w(TAG, "Album art cache directory is null for coverArtId=$coverArtId")
                    return@runCatching
                }

                if (!parent.exists() && !parent.mkdirs()) {
                    Log.w(TAG, "Failed to create album art cache directory: $parent")
                    return@runCatching
                }

                FileOutputStream(file).use { output ->
                    if (!bitmap.compress(Bitmap.CompressFormat.WEBP, 85, output)) {
                        Log.w(TAG, "Could not compress bitmap for coverArtId=$coverArtId")
                        file.delete()
                        return@runCatching
                    }
                }
                file.setLastModified(System.currentTimeMillis())
                trimCacheIfNeeded(parent)
            }.onFailure { error ->
                Log.w(TAG, "Failed to persist album art coverArtId=$coverArtId", error)
            }
        }
    }

    private fun trimCacheIfNeeded(directory: File?) {
        if (directory == null) return
        if (!trimmingInProgress.compareAndSet(false, true)) {
            return
        }

        ioScope.launch {
            runCatching {
                val files = directory.listFiles() ?: return@runCatching
                var total = files.sumOf { it.length() }
                if (total <= MAX_CACHE_BYTES) {
                    return@runCatching
                }

                files
                    .sortedBy { it.lastModified() }
                    .forEach { file ->
                        if (total <= MAX_CACHE_BYTES) return@forEach
                        val length = file.length()
                        if (file.delete()) {
                            total -= length
                        }
                    }
            }.also {
                trimmingInProgress.set(false)
            }
        }
    }

    private fun getCacheFile(coverArtId: String, requestedSize: Int): File {
        val hashedId = coverArtId.hashForFileName()
        val baseDir = App.getContext().filesDir ?: App.getContext().cacheDir
        val directory = File(baseDir, "$DIRECTORY_NAME/$requestedSize")
        return File(directory, hashedId + FILE_EXTENSION)
    }

    private fun String.hashForFileName(): String {
        val messageDigest = MessageDigest.getInstance("SHA-256")
        val bytes = messageDigest.digest(toByteArray(Charsets.UTF_8))
        return buildString(bytes.size * 2) {
            bytes.forEach { byte ->
                append(((byte.toInt() shr 4) and 0xF).toString(16))
                append((byte.toInt() and 0xF).toString(16))
            }
        }
    }
}
