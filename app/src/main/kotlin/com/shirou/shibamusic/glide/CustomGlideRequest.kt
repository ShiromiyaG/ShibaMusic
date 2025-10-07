package com.shirou.shibamusic.glide

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.appcompat.content.res.AppCompatResources
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.signature.ObjectKey
import com.shirou.shibamusic.App
import com.shirou.shibamusic.R
import com.shirou.shibamusic.util.Preferences
import com.shirou.shibamusic.util.Util
import com.google.android.material.elevation.SurfaceColors

object CustomGlideRequest {
    private const val TAG = "CustomGlideRequest"

    val CORNER_RADIUS: Int = if (Preferences.isCornerRoundingEnabled()) Preferences.getRoundedCornerSize() else 1

    val DEFAULT_DISK_CACHE_STRATEGY: DiskCacheStrategy = DiskCacheStrategy.ALL

    enum class ResourceType {
        Unknown,
        Album,
        Artist,
        Folder,
        Directory,
        Playlist,
        Podcast,
        Radio,
        Song,
    }

    fun createRequestOptions(context: Context, item: String?, type: ResourceType): RequestOptions {
        val signatureKey = if (!item.isNullOrBlank()) item else "placeholder_${type.name}"

        return RequestOptions()
            .placeholder(ColorDrawable(SurfaceColors.SURFACE_5.getColor(context)))
            .fallback(getPlaceholder(context, type))
            .error(getPlaceholder(context, type))
            .diskCacheStrategy(DEFAULT_DISK_CACHE_STRATEGY)
            .signature(ObjectKey(signatureKey))
            .transform(CenterCrop(), RoundedCorners(CORNER_RADIUS))
    }

    private fun getPlaceholder(context: Context, type: ResourceType): Drawable? {
        return when (type) {
            ResourceType.Album -> AppCompatResources.getDrawable(context, R.drawable.ic_placeholder_album)
            ResourceType.Artist -> AppCompatResources.getDrawable(context, R.drawable.ic_placeholder_artist)
            ResourceType.Folder -> AppCompatResources.getDrawable(context, R.drawable.ic_placeholder_folder)
            ResourceType.Directory -> AppCompatResources.getDrawable(context, R.drawable.ic_placeholder_directory)
            ResourceType.Playlist -> AppCompatResources.getDrawable(context, R.drawable.ic_placeholder_playlist)
            ResourceType.Podcast -> AppCompatResources.getDrawable(context, R.drawable.ic_placeholder_podcast)
            ResourceType.Radio -> AppCompatResources.getDrawable(context, R.drawable.ic_placeholder_radio)
            ResourceType.Song -> AppCompatResources.getDrawable(context, R.drawable.ic_placeholder_song)
            ResourceType.Unknown -> ColorDrawable(SurfaceColors.SURFACE_5.getColor(context))
        }
    }

    fun createUrl(item: String?, size: Int): String? {
        // Validate cover art ID
        if (item.isNullOrBlank()) {
            Log.w(TAG, "createUrl() - coverArtId is null or empty, returning null")
            return null
        }

        // Special handling for Navidrome - check if coverArtId is valid
        if (isInvalidNavidromeCoverArtId(item)) {
            Log.w(TAG, "createUrl() - detected invalid Navidrome coverArtId: $item")
            return null
        }

        val params = App.getSubsonicClientInstance(false).params

        return buildString {
            append(App.getSubsonicClientInstance(false).url)
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
        }.also {
            Log.d(TAG, "createUrl() $it")
        }
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

    class Builder private constructor(context: Context, item: String?, type: ResourceType) {
        private val requestManager: RequestManager
        private var item: Any? = null // item can be null initially, then set to URL or remain null

        init {
            this.requestManager = Glide.with(context)

            // Only create URL if item is valid and not in data saving mode
            if (!item.isNullOrBlank() && !Preferences.isDataSavingMode()) {
                val url = CustomGlideRequest.createUrl(item, Preferences.getImageSize())
                url?.let {
                    this.item = it
                }
                // If URL creation fails, item remains null and placeholder will be used
            }

            requestManager.applyDefaultRequestOptions(CustomGlideRequest.createRequestOptions(context, item, type))
        }

        companion object {
            fun from(context: Context, item: String?, type: ResourceType): Builder {
                return Builder(context, item, type)
            }
        }

        fun build(): RequestBuilder<Drawable> {
            return requestManager
                .load(item)
                .transition(DrawableTransitionOptions.withCrossFade())
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>,
                        isFirstResource: Boolean
                    ): Boolean {
                        Log.w(TAG, "Failed to load cover art: $model", e)
                        return false // Return false to allow Glide to show error placeholder
                    }

                    override fun onResourceReady(
                        resource: Drawable,
                        model: Any,
                        target: Target<Drawable>,
                        dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        Log.d(TAG, "Successfully loaded cover art from: $dataSource")
                        return false // Return false to allow Glide to proceed normally
                    }
                })
        }
    }
}
