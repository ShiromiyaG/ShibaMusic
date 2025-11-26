package com.shirou.shibamusic.ui.model

data class SongState(
    val title: String,
    val artist: String,
    val thumbnailUrl: String?,
    val isPlaying: Boolean,
    val progress: Float
)
