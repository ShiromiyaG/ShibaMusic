package com.shirou.shibamusic.ui.model

/**
 * Represents the current playback state
 */
enum class PlaybackState {
    /** Player is ready but not playing */
    IDLE,
    
    /** Player is buffering content */
    BUFFERING,
    
    /** Player is ready to play */
    READY,
    
    /** Player has ended playback */
    ENDED
}

/**
 * Represents whether media is currently playing
 */
data class IsPlayingState(
    val isPlaying: Boolean = false,
    val playWhenReady: Boolean = false
)

/**
 * Represents the playback progress
 */
data class PlaybackProgress(
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val bufferedPosition: Long = 0L
) {
    /**
     * Progress as a percentage (0.0 to 1.0)
     */
    val progress: Float
        get() = if (duration > 0) currentPosition / duration.toFloat() else 0f
    
    /**
     * Buffered progress as a percentage (0.0 to 1.0)
     */
    val bufferedProgress: Float
        get() = if (duration > 0) bufferedPosition / duration.toFloat() else 0f
}

/**
 * Represents repeat mode
 */
enum class RepeatMode {
    /** No repeat */
    OFF,
    
    /** Repeat current song */
    ONE,
    
    /** Repeat entire queue */
    ALL;
    
    /**
     * Get next repeat mode in cycle
     */
    fun next(): RepeatMode = when (this) {
        OFF -> ONE
        ONE -> ALL
        ALL -> OFF
    }
}

/**
 * Complete player state combining all aspects
 */
data class PlayerState(
    val nowPlaying: SongItem? = null,
    val queue: List<SongItem> = emptyList(),
    val currentIndex: Int = -1,
    val playbackState: PlaybackState = PlaybackState.IDLE,
    val isPlaying: Boolean = false,
    val playWhenReady: Boolean = false,
    val progress: PlaybackProgress = PlaybackProgress(),
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val shuffleMode: Boolean = false,
    val isFavorite: Boolean = false,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false
) {
    /**
     * Check if player has active media
     */
    val hasMedia: Boolean
        get() = nowPlaying != null
    
    /**
     * Check if player is actively playing
     */
    val isActive: Boolean
        get() = hasMedia && isPlaying
    
    /**
     * Check if player is ready to play
     */
    val isReady: Boolean
        get() = playbackState == PlaybackState.READY || playbackState == PlaybackState.ENDED
}
