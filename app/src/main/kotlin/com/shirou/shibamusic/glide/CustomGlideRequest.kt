package com.shirou.shibamusic.glide

import android.util.Log
import com.shirou.shibamusic.App
import com.shirou.shibamusic.BuildConfig
import com.shirou.shibamusic.util.Util
import java.util.concurrent.ConcurrentHashMap

object CustomGlideRequest {
    private const val TAG = "CustomGlideRequest"

    private data class CacheKey(
        val baseUrl: String,
        val paramsSignature: String,
        val size: Int,
        val coverArtId: String
    )

    private val urlCache = ConcurrentHashMap<CacheKey, String>()

    fun createUrl(item: String?, size: Int): String? {
        // Validate cover art ID
        if (item.isNullOrBlank()) {
            if (BuildConfig.DEBUG) {
                Log.w(TAG, "createUrl() - coverArtId is null or empty, returning null")
            }
            return null
        }

        // Special handling for Navidrome - check if coverArtId is valid
        if (isInvalidNavidromeCoverArtId(item)) {
            if (BuildConfig.DEBUG) {
                Log.w(TAG, "createUrl() - detected invalid Navidrome coverArtId: $item")
            }
            return null
        }

        val subsonicClient = App.getSubsonicClientInstance(false)
        val params = subsonicClient.params
        val paramsSignature = params.entries
            .sortedBy { it.key }
            .joinToString(separator = "&") { (key, value) -> "$key=$value" }
        val cacheKey = CacheKey(
            baseUrl = subsonicClient.url,
            paramsSignature = paramsSignature,
            size = size,
            coverArtId = item
        )

        urlCache[cacheKey]?.let { cached ->
            return cached
        }

        val generatedUrl = buildString {
            append(subsonicClient.url)
            append("getCoverArt")

            params["u"]?.let {
                append("?u=").append(Util.encode(it))
            }
            params["p"]?.let {
                append("&p=").append(it)
            }
            params["s"]?.let {
                append("&s=").append(it)
            }
            params["t"]?.let {
                append("&t=").append(it)
            }
            params["v"]?.let {
                append("&v=").append(it)
            }
            params["c"]?.let {
                append("&c=").append(it)
            }
            if (size != -1) {
                append("&size=").append(size)
            }

            append("&id=").append(item)
        }

        urlCache[cacheKey] = generatedUrl

        return generatedUrl
    }

    /**
     * Check if the coverArtId is invalid for Navidrome
     * Common invalid patterns: "0", "00000000-0000-0000-0000-000000000000", "null",
     * etc.
     */
    private fun isInvalidNavidromeCoverArtId(coverArtId: String?): Boolean {
        if (coverArtId == null) {
            return true
        }

        val trimmed = coverArtId.trim()

        // Check for common invalid values
        return trimmed == "0" ||
                trimmed == "00000000-0000-0000-0000-000000000000" ||
                trimmed.equals("null", ignoreCase = true) ||
                trimmed.matches("^0+$".toRegex()) // All zeros
    }
}