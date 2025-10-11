package com.shirou.shibamusic.service

import android.app.Notification
import android.content.Context
import android.graphics.drawable.Icon
import android.os.Build
import androidx.media3.common.util.NotificationUtil
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import androidx.media3.exoplayer.scheduler.PlatformScheduler
import androidx.media3.exoplayer.scheduler.Requirements
import androidx.media3.exoplayer.scheduler.Scheduler
import com.shirou.shibamusic.R
import com.shirou.shibamusic.util.DownloadUtil

@UnstableApi
class DownloaderService : androidx.media3.exoplayer.offline.DownloadService(
    FOREGROUND_NOTIFICATION_ID,
    DEFAULT_FOREGROUND_NOTIFICATION_UPDATE_INTERVAL,
    DownloadUtil.DOWNLOAD_NOTIFICATION_CHANNEL_ID,
    R.string.exo_download_notification_channel_name,
    0
) {

    companion object {
        private const val JOB_ID = 1
        private const val FOREGROUND_NOTIFICATION_ID = 1
    }

    override fun getDownloadManager(): DownloadManager {
        val downloadManager = DownloadUtil.getDownloadManager(this)
        val downloadNotificationHelper = DownloadUtil.getDownloadNotificationHelper(this)
        downloadManager.addListener(TerminalStateNotificationHelper(this, downloadNotificationHelper, FOREGROUND_NOTIFICATION_ID + 1))
        return downloadManager
    }

    override fun getScheduler(): Scheduler {
        return PlatformScheduler(this, JOB_ID)
    }

    override fun getForegroundNotification(
        downloads: List<Download>,
        @Requirements.RequirementFlags notMetRequirements: Int
    ): Notification {
        val notification = DownloadUtil.getDownloadNotificationHelper(this)
            .buildProgressNotification(
                this,
                R.drawable.shiba_vector,
                null,
                null,
                downloads,
                notMetRequirements
            )

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Notification.Builder.recoverBuilder(this, notification)
                .setLargeIcon(DownloadUtil.getAppIconBitmap(this))
                .apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        setSmallIcon(Icon.createWithResource(this@DownloaderService, R.drawable.shiba_vector))
                    }
                }
                .build()
        } else {
            notification
        }
    }

    private class TerminalStateNotificationHelper(
        context: Context,
        private val notificationHelper: DownloadNotificationHelper,
        firstNotificationId: Int
    ) : DownloadManager.Listener {

        private val context: Context = context.applicationContext
        private var nextNotificationId: Int = firstNotificationId

        private val successfulDownloadGroupNotification: Notification = DownloadUtil.buildGroupSummaryNotification(
            this.context,
            DownloadUtil.DOWNLOAD_NOTIFICATION_CHANNEL_ID,
            DownloadUtil.DOWNLOAD_NOTIFICATION_SUCCESSFUL_GROUP,
            R.drawable.shiba_vector,
            "Downloads completed"
        )

        private val failedDownloadGroupNotification: Notification = DownloadUtil.buildGroupSummaryNotification(
            this.context,
            DownloadUtil.DOWNLOAD_NOTIFICATION_CHANNEL_ID,
            DownloadUtil.DOWNLOAD_NOTIFICATION_FAILED_GROUP,
            R.drawable.shiba_vector,
            "Downloads failed"
        )

        private val successfulDownloadGroupNotificationId: Int = nextNotificationId++
        private val failedDownloadGroupNotificationId: Int = nextNotificationId++

        override fun onDownloadChanged(
            downloadManager: DownloadManager,
            download: Download,
            finalException: Exception?
        ) {
            val notification: Notification = if (download.state == Download.STATE_COMPLETED) {
                val completedNotification = notificationHelper.buildDownloadCompletedNotification(
                    context,
                    R.drawable.shiba_vector,
                    null,
                    DownloaderManager.getDownloadNotificationMessage(download.request.id)
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    Notification.Builder.recoverBuilder(context, completedNotification)
                        .setGroup(DownloadUtil.DOWNLOAD_NOTIFICATION_SUCCESSFUL_GROUP)
                        .setLargeIcon(DownloadUtil.getAppIconBitmap(context))
                        .apply {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                setSmallIcon(Icon.createWithResource(context, R.drawable.shiba_vector))
                            }
                        }
                        .build().also {
                            NotificationUtil.setNotification(this.context, successfulDownloadGroupNotificationId, successfulDownloadGroupNotification)
                            DownloaderManager.updateRequestDownload(download)
                        }
                } else {
                    completedNotification.also {
                        NotificationUtil.setNotification(this.context, successfulDownloadGroupNotificationId, successfulDownloadGroupNotification)
                        DownloaderManager.updateRequestDownload(download)
                    }
                }
            } else if (download.state == Download.STATE_FAILED) {
                val failedNotification = notificationHelper.buildDownloadFailedNotification(
                    context,
                    R.drawable.shiba_vector,
                    null,
                    DownloaderManager.getDownloadNotificationMessage(download.request.id)
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    Notification.Builder.recoverBuilder(context, failedNotification)
                        .setGroup(DownloadUtil.DOWNLOAD_NOTIFICATION_FAILED_GROUP)
                        .setLargeIcon(DownloadUtil.getAppIconBitmap(context))
                        .apply {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                setSmallIcon(Icon.createWithResource(context, R.drawable.shiba_vector))
                            }
                        }
                        .build().also {
                            NotificationUtil.setNotification(this.context, failedDownloadGroupNotificationId, failedDownloadGroupNotification)
                        }
                } else {
                    failedNotification.also {
                        NotificationUtil.setNotification(this.context, failedDownloadGroupNotificationId, failedDownloadGroupNotification)
                    }
                }
            } else {
                return
            }

            NotificationUtil.setNotification(context, nextNotificationId++, notification)
        }

        override fun onDownloadRemoved(downloadManager: DownloadManager, download: Download) {
            DownloaderManager.removeRequestDownload(download)
        }
    }
}
