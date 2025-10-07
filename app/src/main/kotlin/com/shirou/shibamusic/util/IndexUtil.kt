package com.shirou.shibamusic.util

import androidx.media3.common.util.UnstableApi
import com.shirou.shibamusic.subsonic.models.Artist
import com.shirou.shibamusic.subsonic.models.Indexes

import kotlin.OptIn

@OptIn(UnstableApi::class)
object IndexUtil {
    fun getArtist(indexes: Indexes): List<Artist> {
        return indexes.indices?.flatMap { index ->
            index.artists ?: emptyList()
        } ?: emptyList()
    }
}
