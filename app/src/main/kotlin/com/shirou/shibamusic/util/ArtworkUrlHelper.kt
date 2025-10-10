package com.shirou.shibamusic.util

import android.net.Uri
import com.shirou.shibamusic.glide.CustomGlideRequest

/**
 * Centralises logic for building cover art URLs with desired sizes so that
 * different parts of the UI can share cached responses without over-fetching.
 */
object ArtworkUrlHelper {

    private const val LIST_IMAGE_SIZE_DEFAULT = 340
    private const val LIST_IMAGE_SIZE_DATA_SAVER = 220
    private const val PLAYER_IMAGE_SIZE_DEFAULT = 1024

    fun forList(rawValue: String?): String? {
        val targetSize = if (Preferences.isDataSavingMode()) {
            LIST_IMAGE_SIZE_DATA_SAVER
        } else {
            LIST_IMAGE_SIZE_DEFAULT
        }
        return ensureSizedUrl(rawValue, targetSize, clampDown = true)
    }

    fun forPlayer(rawValue: String?): String? {
        val targetSize = PLAYER_IMAGE_SIZE_DEFAULT
        return ensureSizedUrl(rawValue, targetSize, clampDown = false)
    }

    private fun ensureSizedUrl(
        rawValue: String?,
        targetSize: Int,
        clampDown: Boolean
    ): String? {
        if (rawValue.isNullOrBlank()) return null

        // Already a full URL?
        return if (rawValue.startsWith("http", ignoreCase = true) ||
            rawValue.startsWith("file:", ignoreCase = true)
        ) {
            val uri = Uri.parse(rawValue)
            val existingSize = uri.getQueryParameter("size")?.toIntOrNull()

            val shouldUpdate = when {
                existingSize == null -> true
                existingSize < 0 -> !clampDown // unlimited/full, only enlarge if requested
                clampDown -> existingSize > targetSize
                else -> existingSize < targetSize
            }

            if (!shouldUpdate) {
                rawValue
            } else {
                val builder = uri.buildUpon()
                builder.clearQuery()
                uri.queryParameterNames.forEach { name ->
                    if (!name.equals("size", ignoreCase = true)) {
                        uri.getQueryParameterValues(name).forEach { value ->
                            builder.appendQueryParameter(name, value)
                        }
                    }
                }
                builder.appendQueryParameter("size", targetSize.toString())
                builder.build().toString()
            }
        } else {
            // Treat as coverArt id
            CustomGlideRequest.createUrl(rawValue, targetSize)
        }
    }

    private fun Uri.getQueryParameterValues(name: String): List<String> {
        return getQueryParameters(name)?.takeIf { it.isNotEmpty() } ?: emptyList()
    }
}
