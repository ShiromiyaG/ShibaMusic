package com.shirou.shibamusic.util

import android.net.Uri
import android.util.Log
import com.shirou.shibamusic.App

/**
 * Utility to build full cover art URLs from Subsonic/Navidrome identifiers.
 * Re-implements the legacy Glide helper in Kotlin so Compose UI can load images.
 */
object CoverArtUrlBuilder {

    private const val TAG = "CoverArtUrlBuilder"

    /**
     * Builds the full cover art URL for the given [coverArtId].
     * Returns null when data saving mode is enabled or the id is invalid.
     */
    @JvmStatic
    fun build(coverArtId: String?, size: Int = Preferences.getImageSize()): String? {
        if (Preferences.isDataSavingMode()) {
            Log.d(TAG, "Data saving mode enabled, skipping cover art load")
            return null
        }

        if (coverArtId.isNullOrBlank()) {
            Log.w(TAG, "Cover art id is empty")
            return null
        }

        if (isInvalidNavidromeCoverArtId(coverArtId)) {
            Log.w(TAG, "Invalid Navidrome coverArtId: $coverArtId")
            return null
        }

        return runCatching {
            val subsonic = App.getSubsonicClientInstance(false)
            val params = subsonic.params

            buildString {
                append(subsonic.url)
                append("getCoverArt")

                var isFirst = true
                fun appendQueryParam(key: String, value: String?, encode: Boolean = false) {
                    if (value.isNullOrBlank()) return
                    append(if (isFirst) "?" else "&")
                    append(key)
                    append("=")
                    append(if (encode) Util.encode(value) else value)
                    isFirst = false
                }

                appendQueryParam("u", params["u"], encode = true)
                appendQueryParam("p", params["p"])
                appendQueryParam("s", params["s"])
                appendQueryParam("t", params["t"])
                appendQueryParam("v", params["v"])
                appendQueryParam("c", params["c"])

                if (size != -1) {
                    append(if (isFirst) "?" else "&")
                    append("size=")
                    append(size)
                    isFirst = false
                }

                append(if (isFirst) "?" else "&")
                append("id=")
                append(coverArtId)
            }
        }.onFailure { error ->
            Log.e(TAG, "Failed to build cover art url", error)
        }.getOrNull()
    }

    /**
     * Extracts the original cover art id from a previously generated URL.
     * Returns null when the value cannot be parsed.
     */
    @JvmStatic
    fun extractId(value: String?): String? {
        if (value.isNullOrBlank()) return null
        if (!value.contains("getCoverArt", ignoreCase = true)) {
            return value
        }

        val uri = runCatching { Uri.parse(value) }.getOrNull()
        val idFromQuery = uri?.getQueryParameter("id")
        if (!idFromQuery.isNullOrBlank()) {
            return idFromQuery
        }

        return value.substringAfter("id=", missingDelimiterValue = value).substringBefore("&")
    }

    private fun isInvalidNavidromeCoverArtId(coverArtId: String): Boolean {
        val trimmed = coverArtId.trim()
        if (trimmed.isEmpty()) return true
        if (trimmed.equals("null", ignoreCase = true)) return true
        if (trimmed == "0") return true
        if (trimmed == "00000000-0000-0000-0000-000000000000") return true
        if (trimmed.matches(Regex("^0+$"))) return true
        return false
    }
}
