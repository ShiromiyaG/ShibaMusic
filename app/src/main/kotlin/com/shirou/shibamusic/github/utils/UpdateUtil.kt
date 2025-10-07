package com.shirou.shibamusic.github.utils

import com.shirou.shibamusic.BuildConfig
import com.shirou.shibamusic.github.models.LatestRelease

object UpdateUtil {

    fun showUpdateDialog(release: LatestRelease): Boolean {
        val remoteTagName = release.tagName ?: return false

        return try {
            val local = BuildConfig.VERSION_NAME.split(Regex("\\."))
            val remote = remoteTagName.split(Regex("\\."))

            for (i in local.indices) {
                val localPart = local[i].toInt()
                val remotePart = remote[i].toInt()

                when {
                    localPart > remotePart -> return false
                    localPart < remotePart -> return true
                }
            }
            // If the loop completes without returning, it means all compared parts were equal.
            // This implies no update, or local version is longer (e.g., 1.0.0 vs 1.0)
            // and the first 'local.indices' parts matched.
            // The original Java code returns false here.
            false
        } catch (e: Exception) {
            // Catches NumberFormatException if a version part is not an integer.
            // Catches IndexOutOfBoundsException if remote version has fewer parts than local
            // and the loop tries to access a non-existent part.
            false
        }
    }
}
