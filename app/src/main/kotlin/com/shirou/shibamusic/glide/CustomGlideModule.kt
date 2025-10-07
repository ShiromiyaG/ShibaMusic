package com.shirou.shibamusic.glide

import android.content.Context
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory
import com.bumptech.glide.module.AppGlideModule
import com.bumptech.glide.request.RequestOptions
import com.shirou.shibamusic.util.Preferences

@GlideModule
class CustomGlideModule : AppGlideModule() {
    override fun applyOptions(context: Context, builder: GlideBuilder) {
        val diskCacheSize = Preferences.getImageCacheSize()
            .toLong()
            .coerceAtLeast(0L)
            .times(1024L * 1024L)
        
        builder.apply {
            setDiskCache(InternalCacheDiskCacheFactory(context, "cache", diskCacheSize))
            setDefaultRequestOptions(RequestOptions().format(DecodeFormat.PREFER_RGB_565))
        }
    }
}
