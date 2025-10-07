package com.shirou.shibamusic.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.shirou.shibamusic.data.repository.SyncRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException

@HiltWorker
class AlbumSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncRepository: SyncRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val pageSize = inputData.getInt(KEY_PAGE_SIZE, DEFAULT_PAGE_SIZE)
            val maxPages = inputData.getInt(KEY_MAX_PAGES, DEFAULT_MAX_PAGES)
            val throttleMs = inputData.getLong(KEY_THROTTLE_MS, DEFAULT_THROTTLE_MS)
            val syncSongs = inputData.getBoolean(KEY_SYNC_SONGS, DEFAULT_SYNC_SONGS)

            val artistLimit = inputData.getInt(KEY_ARTIST_LIMIT, VALUE_UNSET)
                .takeIf { it != VALUE_UNSET }
            val albumLimitPerArtist = inputData.getInt(KEY_ALBUM_LIMIT_PER_ARTIST, VALUE_UNSET)
                .takeIf { it != VALUE_UNSET }

            val pageReport = syncRepository.syncAllAlbumsPaged(
                pageSize = pageSize,
                maxPages = maxPages,
                throttleMs = throttleMs
            )

            val deepReport = syncRepository.syncArtistsAndAlbumsDeep(
                artistLimit = artistLimit,
                albumLimitPerArtist = albumLimitPerArtist,
                syncSongs = syncSongs,
                throttleMs = throttleMs
            )

            Result.success(
                workDataOf(
                    KEY_FETCHED_COUNT to pageReport.fetched,
                    KEY_INSERTED_COUNT to pageReport.inserted,
                    KEY_PAGE_COUNT to pageReport.pages,
                    KEY_ARTISTS_SYNCED to deepReport.artists,
                    KEY_DEEP_ALBUM_COUNT to deepReport.albums,
                    KEY_SONG_COUNT to deepReport.songs
                )
            )
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            if (runAttemptCount >= MAX_ATTEMPTS - 1) {
                Result.failure(
                    workDataOf(KEY_ERROR_MESSAGE to (exception.message ?: "Unknown error"))
                )
            } else {
                Result.retry()
            }
        }
    }

    companion object {
        const val UNIQUE_WORK_NAME = "album_sync_worker"

        const val KEY_PAGE_SIZE = "page_size"
        const val KEY_MAX_PAGES = "max_pages"
        const val KEY_THROTTLE_MS = "throttle_ms"
        const val KEY_SYNC_SONGS = "sync_songs"
        const val KEY_ARTIST_LIMIT = "artist_limit"
        const val KEY_ALBUM_LIMIT_PER_ARTIST = "album_limit_per_artist"
        const val KEY_FETCHED_COUNT = "fetched_count"
        const val KEY_INSERTED_COUNT = "inserted_count"
        const val KEY_PAGE_COUNT = "page_count"
        const val KEY_ARTISTS_SYNCED = "artists_synced"
        const val KEY_DEEP_ALBUM_COUNT = "deep_album_count"
        const val KEY_SONG_COUNT = "song_count"
        const val KEY_ERROR_MESSAGE = "error_message"

        private const val DEFAULT_PAGE_SIZE = 500
        private const val DEFAULT_MAX_PAGES = 500
        private const val DEFAULT_THROTTLE_MS = 50L
        private const val DEFAULT_SYNC_SONGS = true
        private const val MAX_ATTEMPTS = 3
        private const val VALUE_UNSET = -1

        fun defaultInputData(): Data = workDataOf(
            KEY_PAGE_SIZE to DEFAULT_PAGE_SIZE,
            KEY_MAX_PAGES to DEFAULT_MAX_PAGES,
            KEY_THROTTLE_MS to DEFAULT_THROTTLE_MS,
            KEY_SYNC_SONGS to DEFAULT_SYNC_SONGS,
            KEY_ARTIST_LIMIT to VALUE_UNSET,
            KEY_ALBUM_LIMIT_PER_ARTIST to VALUE_UNSET
        )

    }
}
