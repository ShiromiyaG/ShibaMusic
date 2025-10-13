package com.shirou.shibamusic.worker

import android.content.Context
import androidx.lifecycle.asFlow
import androidx.work.BackoffPolicy
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit

@Singleton
class AlbumSyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val workManager: WorkManager
        get() = WorkManager.getInstance(context)

    fun enqueueAlbumSync(
        force: Boolean = false,
        expedited: Boolean = false,
        pageSize: Int = DEFAULT_PAGE_SIZE,
        maxPages: Int = DEFAULT_MAX_PAGES,
        throttleMs: Long = DEFAULT_THROTTLE_MS,
        syncSongs: Boolean = DEFAULT_SYNC_SONGS,
        artistLimit: Int? = null,
        albumLimitPerArtist: Int? = null
    ) {
        val inputData = Data.Builder()
            .putInt(AlbumSyncWorker.KEY_PAGE_SIZE, pageSize)
            .putInt(AlbumSyncWorker.KEY_MAX_PAGES, maxPages)
            .putLong(AlbumSyncWorker.KEY_THROTTLE_MS, throttleMs)
            .putBoolean(AlbumSyncWorker.KEY_SYNC_SONGS, syncSongs)
            .putBoolean(AlbumSyncWorker.KEY_FORCE, force)
            .putInt(AlbumSyncWorker.KEY_ARTIST_LIMIT, artistLimit ?: UNSET_VALUE)
            .putInt(AlbumSyncWorker.KEY_ALBUM_LIMIT_PER_ARTIST, albumLimitPerArtist ?: UNSET_VALUE)
            .build()

        val requestBuilder = OneTimeWorkRequestBuilder<AlbumSyncWorker>()
            .setInputData(inputData)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                INITIAL_BACKOFF_DURATION_MINUTES,
                TimeUnit.MINUTES
            )

        if (expedited) {
            requestBuilder.setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
        }

        val policy = if (force) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP

        workManager.enqueueUniqueWork(
            AlbumSyncWorker.UNIQUE_WORK_NAME,
            policy,
            requestBuilder.build()
        )
    }

    fun observeStatus(): Flow<List<WorkInfo>> {
        return workManager
            .getWorkInfosForUniqueWorkLiveData(AlbumSyncWorker.UNIQUE_WORK_NAME)
            .asFlow()
    }

    companion object {
        private const val INITIAL_BACKOFF_DURATION_MINUTES = 1L
        private const val DEFAULT_PAGE_SIZE = 500
        private const val DEFAULT_MAX_PAGES = 500
        private const val DEFAULT_THROTTLE_MS = 50L
        private const val DEFAULT_SYNC_SONGS = true
        private const val UNSET_VALUE = -1
    }
}
