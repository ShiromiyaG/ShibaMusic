package com.shirou.shibamusic.github.utils

import com.shirou.shibamusic.BuildConfig
import com.shirou.shibamusic.github.models.LatestRelease

object UpdateUtil {

    fun showUpdateDialog(release: LatestRelease): Boolean {
        if (release.draft == true || release.prerelease == true) {
            return false
        }

        val localVersion = parseVersion(BuildConfig.VERSION_NAME)
        val remoteVersion = parseVersion(release.tagName) ?: parseVersion(release.name)

        if (localVersion.isEmpty() || remoteVersion.isEmpty()) {
            return false
        }

        val maxSize = maxOf(localVersion.size, remoteVersion.size)
        for (index in 0 until maxSize) {
            val localPart = localVersion.getOrElse(index) { 0 }
            val remotePart = remoteVersion.getOrElse(index) { 0 }

            when {
                remotePart > localPart -> return true
                remotePart < localPart -> return false
            }
        }

        return false
    }

    private fun parseVersion(raw: String?): List<Int> {
        if (raw.isNullOrBlank()) return emptyList()

        return Regex("\\d+")
            .findAll(raw.trim().removePrefix("v").removePrefix("V"))
            .mapNotNull { match ->
                match.value.toIntOrNull()
            }
            .toList()
    }
}
