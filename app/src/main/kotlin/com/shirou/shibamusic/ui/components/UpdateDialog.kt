package com.shirou.shibamusic.ui.components

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.text.format.Formatter
import android.widget.TextView
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.shirou.shibamusic.BuildConfig
import com.shirou.shibamusic.R
import com.shirou.shibamusic.github.models.Assets
import com.shirou.shibamusic.github.models.LatestRelease
import io.noties.markwon.Markwon
import android.text.method.LinkMovementMethod

@Composable
fun UpdateAvailableDialog(
    release: LatestRelease,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val markwon = remember(context) { Markwon.builder(context).build() }
    val releaseVersionLabel = release.tagName?.takeIf { it.isNotBlank() }
        ?: release.name?.takeIf { it.isNotBlank() }
        ?: ""
    val changelog = release.body?.takeIf { it.isNotBlank() }
    val apkAsset = remember(release) { findApkAsset(release) }
    val formattedSize = apkAsset?.size?.let { Formatter.formatFileSize(context, it.toLong()) }
    val bodyColor = MaterialTheme.colorScheme.onSurface
    val linkColor = MaterialTheme.colorScheme.primary

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            val dialogTitle = if (releaseVersionLabel.isNotBlank()) {
                stringResource(
                    R.string.settings_update_dialog_title,
                    releaseVersionLabel
                )
            } else {
                stringResource(R.string.settings_update_title_item)
            }

            Text(text = dialogTitle)
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                if (formattedSize != null) {
                    Text(
                        text = stringResource(
                            R.string.settings_update_dialog_size,
                            formattedSize
                        ),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(8.dp))
                }

                if (changelog != null) {
                    AndroidView(
                        modifier = Modifier.fillMaxWidth(),
                        factory = { ctx ->
                            TextView(ctx).apply {
                                movementMethod = LinkMovementMethod.getInstance()
                                setTextColor(bodyColor.toArgb())
                                setLinkTextColor(linkColor.toArgb())
                            }
                        },
                        update = { textView ->
                            markwon.setMarkdown(textView, changelog)
                        }
                    )
                } else {
                    Text(
                        text = stringResource(R.string.settings_update_dialog_no_notes),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val started = startUpdateDownload(context, release)
                    val label = releaseVersionLabel.ifBlank {
                        release.tagName ?: release.name ?: BuildConfig.VERSION_NAME
                    }
                    val message = if (started) {
                        context.getString(R.string.settings_update_download_started, label)
                    } else {
                        context.getString(R.string.settings_update_download_error)
                    }
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    onDismiss()
                }
            ) {
                Text(stringResource(R.string.settings_update_dialog_download))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.settings_update_dialog_later))
            }
        }
    )
}

private fun findApkAsset(release: LatestRelease): Assets? {
    return release.assets.firstOrNull { asset ->
        val url = asset.browserDownloadUrl?.lowercase() ?: return@firstOrNull false
        url.endsWith(".apk") || asset.contentType?.equals(
            "application/vnd.android.package-archive",
            ignoreCase = true
        ) == true
    }
}

private fun startUpdateDownload(context: Context, release: LatestRelease): Boolean {
    val asset = findApkAsset(release) ?: return false
    val downloadUrl = asset.browserDownloadUrl ?: return false

    val versionLabel = "${release.tagName ?: release.name ?: ""}".trim()
    val title = context.getString(R.string.update_download_title, versionLabel)

    val request = DownloadManager.Request(Uri.parse(downloadUrl))
        .setTitle(title)
        .setDescription(context.getString(R.string.settings_update_download_description))
        .setMimeType("application/vnd.android.package-archive")
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        .setAllowedOverMetered(true)
        .setAllowedOverRoaming(true)

    val fileName = sanitizeFileName(asset.name ?: "ShibaMusic-${release.tagName ?: release.name ?: "update"}.apk")
    request.setDestinationInExternalFilesDir(
        context,
        Environment.DIRECTORY_DOWNLOADS,
        fileName
    )

    val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager ?: return false
    return runCatching { manager.enqueue(request); true }.getOrDefault(false)
}

private fun sanitizeFileName(raw: String): String {
    return raw.replace(Regex("[^A-Za-z0-9._-]"), "_")
}
