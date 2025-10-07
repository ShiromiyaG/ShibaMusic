package com.shirou.shibamusic.interfaces

import androidx.annotation.Keep

@Keep
interface MediaCallback {
    fun onError(exception: Exception) {}
    fun onLoadMedia(media: List<*>) {}
}
